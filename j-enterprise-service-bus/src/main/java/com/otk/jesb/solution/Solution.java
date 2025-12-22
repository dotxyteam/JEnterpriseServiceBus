package com.otk.jesb.solution;

import java.beans.PropertyDescriptor;
import java.beans.Transient;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.otk.jesb.solution.AssetVisitor;
import com.otk.jesb.Debugger;
import com.otk.jesb.EnvironmentSettings;
import com.otk.jesb.JESB;
import com.otk.jesb.PluginBuilder;
import com.otk.jesb.PotentialError;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.activation.ActivatorMetadata;
import com.otk.jesb.activation.builtin.WatchFileSystem;
import com.otk.jesb.compiler.InMemoryCompiler;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.operation.builtin.Evaluate;
import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.ResourceMetadata;
import com.otk.jesb.util.MiscUtils;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.javabean.BeanProvider;
import com.thoughtworks.xstream.converters.javabean.JavaBeanConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import com.thoughtworks.xstream.security.AnyTypePermission;

import xy.reflect.ui.control.plugin.AbstractSimpleCustomizableFieldControlPlugin;
import xy.reflect.ui.util.ClassUtils;

/**
 * This class allows to represent a coherent set of more or less interdependent
 * processing assets (instances of {@link Plan}, {@link Resource}, ...).
 * 
 * @author olitank
 *
 */
public class Solution {

	private static final String SORTED_NAMES_FILE_NAME = ".sortedNames" + MiscUtils.SERIALIZED_FILE_NAME_SUFFIX;
	private static final String REQUIRED_JARS_DIRECTORY_NAME = "lib";

	private Folder rootFolder = new Folder("rootFolder");
	private EnvironmentSettings environmentSettings = new EnvironmentSettings();
	private com.otk.jesb.operation.Experiment defualtOperationExperiment = new com.otk.jesb.operation.Experiment(
			new Evaluate.Builder(), this);
	private com.otk.jesb.activation.Experiment defualtActivationExperiment = new com.otk.jesb.activation.Experiment(
			new WatchFileSystem(), this);
	private List<JAR> requiredJARs = new ArrayList<JAR>();

	private Runtime runtime = new Runtime();

	public Runtime getRuntime() {
		return runtime;
	}

	public List<JAR> getRequiredJARs() {
		return requiredJARs;
	}

	public void setRequiredJARs(List<JAR> requiredJARs) {
		this.requiredJARs = requiredJARs;
		runtime.configureSolutionDependencies(requiredJARs);
	}

	public EnvironmentSettings getEnvironmentSettings() {
		return environmentSettings;
	}

	public void setEnvironmentSettings(EnvironmentSettings environmentSettings) {
		this.environmentSettings = environmentSettings;
	}

	public List<Asset> getContents() {
		return rootFolder.getContents();
	}

	public void setContents(List<Asset> contents) {
		rootFolder.setContents(contents);
	}

	@Transient
	public com.otk.jesb.operation.Experiment getDefualtOperationExperiment() {
		return defualtOperationExperiment;
	}

	public void setDefualtOperationExperiment(com.otk.jesb.operation.Experiment defualtOperationExperiment) {
		this.defualtOperationExperiment = defualtOperationExperiment;
	}

	@Transient
	public com.otk.jesb.activation.Experiment getDefualtActivationExperiment() {
		return defualtActivationExperiment;
	}

	public void setDefualtActivationExperiment(com.otk.jesb.activation.Experiment defualtActivationExperiment) {
		this.defualtActivationExperiment = defualtActivationExperiment;
	}

	public void visitContents(AssetVisitor assetVisitor) {
		rootFolder.visitContents(assetVisitor);
	}

	public void validate() throws ValidationError {
		environmentSettings.validate(this);
		rootFolder.validate(true, this);
	}

	public Debugger createDebugger() {
		return new Debugger(this, false);
	}

	public PluginBuilder accessPluginBuilder() {
		return new PluginBuilder(this);
	}

	public void loadFromArchiveFile(File archiveFile) throws IOException {
		if (!archiveFile.isFile()) {
			throw new IllegalArgumentException("'" + archiveFile + "' is not a valid file");
		}
		URI uri = URI.create("jar:" + archiveFile.toURI());
		try (FileSystem zipFs = FileSystems.newFileSystem(uri, Collections.singletonMap("create", "false"))) {
			load(zipFs.getPath("/"));
		}
	}

	public void loadFromDirectory(File directory) throws IOException {
		if (!directory.isDirectory()) {
			throw new IllegalArgumentException("'" + directory + "' is not a valid directory");
		}
		load(directory.toPath());
	}

	private void load(Path rootPath) throws IOException {
		loadRequiredJARs(rootPath);
		try (ByteArrayInputStream fileInputStream = new ByteArrayInputStream(
				Files.readAllBytes(rootPath.resolve("." + environmentSettings.getClass().getSimpleName().toLowerCase()
						+ MiscUtils.SERIALIZED_FILE_NAME_SUFFIX)))) {
			environmentSettings = (EnvironmentSettings) MiscUtils.deserialize(fileInputStream,
					getRuntime().getXstream());
		}
		rootFolder = loadFolder(rootPath, rootFolder.getName());
	}

	private void loadRequiredJARs(Path parentPath) throws IOException {
		Folder jarFolder = loadFolder(parentPath, REQUIRED_JARS_DIRECTORY_NAME);
		setRequiredJARs(jarFolder.getContents().stream().map(JAR.class::cast).collect(Collectors.toList()));
	}

	private Folder loadFolder(Path parentPath, String folderName) throws IOException {
		Path folderPath = parentPath.resolve(folderName);
		if (!Files.isDirectory(folderPath)) {
			throw new IllegalArgumentException("'" + folderPath + "' is not a valid directory");
		}
		Folder folder = new Folder(folderName);
		try (Stream<Path> pathStream = Files.walk(folderPath, 1).skip(1)) {
			for (Path path : pathStream.collect(Collectors.toList())) {
				String name = path.getFileName().toString();
				{
					if (name.endsWith("/")) {
						name = name.substring(0, name.length() - 1);
					}
				}
				if (name.equals(SORTED_NAMES_FILE_NAME)) {
					continue;
				}
				folder.getContents().add(loadAsset(folderPath, name));
			}
		}
		try (ByteArrayInputStream fileInputStream = new ByteArrayInputStream(
				Files.readAllBytes(folderPath.resolve(SORTED_NAMES_FILE_NAME)))) {
			@SuppressWarnings("unchecked")
			final List<String> sortedNames = (List<String>) MiscUtils.deserialize(fileInputStream,
					getRuntime().getXstream());
			Collections.sort(folder.getContents(), new Comparator<Asset>() {
				@Override
				public int compare(Asset asset1, Asset asset2) {
					return new Integer(sortedNames.indexOf(asset1.getName()))
							.compareTo(new Integer(sortedNames.indexOf(asset2.getName())));
				}
			});
		}
		return folder;
	}

	private Asset loadAsset(Path parentPath, String name) throws IOException {
		Path fileOrDirectoryPath = parentPath.resolve(name);
		if (Files.isDirectory(fileOrDirectoryPath)) {
			return loadFolder(parentPath, name);
		} else {
			try (ByteArrayInputStream fileInputStream = new ByteArrayInputStream(
					Files.readAllBytes(fileOrDirectoryPath))) {
				if (name.endsWith(".jar")) {
					return new JAR(name, MiscUtils.readBinary(fileInputStream));
				} else {
					Asset asset = (Asset) MiscUtils.deserialize(fileInputStream, getRuntime().getXstream());
					return asset;
				}
			}
		}
	}

	public void saveToArchiveFile(File archiveFile) throws IOException {
		if (archiveFile.exists()) {
			MiscUtils.delete(archiveFile);
		}
		URI uri = URI.create("jar:" + archiveFile.toURI());
		try (FileSystem zipFs = FileSystems.newFileSystem(uri, Collections.singletonMap("create", "true"))) {
			save(zipFs.getPath("/"));
		}
	}

	public void saveToDirectory(File directory) throws IOException {
		if (directory.exists()) {
			MiscUtils.delete(directory);
		}
		MiscUtils.createDirectory(directory);
		save(directory.toPath());
	}

	private void save(Path rootPath) throws IOException {
		saveRequiredJARs(rootPath);
		try (ByteArrayOutputStream fileOutputStream = new ByteArrayOutputStream()) {
			MiscUtils.serialize(environmentSettings, fileOutputStream, getRuntime().getXstream());
			Files.write(rootPath.resolve("." + environmentSettings.getClass().getSimpleName().toLowerCase()
					+ MiscUtils.SERIALIZED_FILE_NAME_SUFFIX), fileOutputStream.toByteArray());
		}
		saveFolder(rootPath, rootFolder);
	}

	private void saveRequiredJARs(Path parentPath) throws IOException {
		Folder jarFolder = new Folder(REQUIRED_JARS_DIRECTORY_NAME);
		jarFolder.getContents().addAll(requiredJARs);
		saveFolder(parentPath, jarFolder);
	}

	private void saveFolder(Path parentPath, Folder folder) throws IOException {
		Path folderPath = parentPath.resolve(folder.getName());
		Files.createDirectories(folderPath);
		for (Asset asset : folder.getContents()) {
			saveAsset(folderPath, asset);
		}
		try (ByteArrayOutputStream fileOutputStream = new ByteArrayOutputStream()) {
			List<String> sortedNames = folder.getContents().stream().map(asset -> asset.getName())
					.collect(Collectors.toList());
			MiscUtils.serialize(sortedNames, fileOutputStream, getRuntime().getXstream());
			Files.write(folderPath.resolve(SORTED_NAMES_FILE_NAME), fileOutputStream.toByteArray());
		}
	}

	private void saveAsset(Path parentPath, Asset asset) throws IOException {
		if (asset instanceof Folder) {
			saveFolder(parentPath, (Folder) asset);
		} else {
			Path assetPath = parentPath.resolve(asset.getFileSystemResourceName());
			if (Files.exists(assetPath)) {
				throw new PotentialError("Duplicate file detected while saving: " + assetPath);
			}
			try (ByteArrayOutputStream fileOutputStream = new ByteArrayOutputStream()) {
				if (asset.getName().endsWith(".jar")) {
					try (InputStream inputStream = ((JAR) asset).getURL().openStream()) {
						fileOutputStream.write(MiscUtils.readBinary(inputStream));
					}
				} else {
					MiscUtils.serialize(asset, fileOutputStream, getRuntime().getXstream());
				}
				Files.write(assetPath, fileOutputStream.toByteArray());
			}
		}
	}

	public class Runtime {
		private final InMemoryCompiler inMemoryCompiler = new InMemoryCompiler();

		private final List<OperationMetadata<?>> pluginOperationMetadatas = new ArrayList<OperationMetadata<?>>();
		private final List<ResourceMetadata> pluginResourceMetadatas = new ArrayList<ResourceMetadata>();
		private final List<ActivatorMetadata> pluginActivatorMetadatas = new ArrayList<ActivatorMetadata>();

		private final XStream xstream = new XStream() {
			@Override
			protected MapperWrapper wrapMapper(MapperWrapper next) {
				return new MapperWrapper(next) {
					@Override
					public String serializedClass(@SuppressWarnings("rawtypes") Class type) {
						if ((type != null) && type.isAnonymousClass()) {
							throw new UnexpectedError("Cannot serialize instance of class " + type
									+ ": Anonymous class instance serialization is forbidden");
						}
						return super.serializedClass(type);
					}

					@Override
					public boolean shouldSerializeMember(@SuppressWarnings("rawtypes") Class definedIn,
							String fieldName) {
						if (Throwable.class.isAssignableFrom(definedIn)) {
							if (fieldName.equals("stackTrace")) {
								return false;
							}
							if (fieldName.equals("suppressedExceptions")) {
								return false;
							}
						}
						return super.shouldSerializeMember(definedIn, fieldName);
					}

				};
			}
		};

		public Runtime() {
			configureXstream();
			configureSolutionDependencies(Collections.emptyList());
		}

		public InMemoryCompiler getInMemoryCompiler() {
			return inMemoryCompiler;
		}

		public List<OperationMetadata<?>> getPluginOperationMetadatas() {
			return pluginOperationMetadatas;
		}

		public List<ResourceMetadata> getPluginResourceMetadatas() {
			return pluginResourceMetadatas;
		}

		public List<ActivatorMetadata> getPluginActivatorMetadatas() {
			return pluginActivatorMetadatas;
		}

		public XStream getXstream() {
			return xstream;
		}

		private void configureXstream() {
			xstream.setClassLoader(inMemoryCompiler.getCompiledClassesLoader());
			xstream.registerConverter(new JavaBeanConverter(xstream.getMapper(), new BeanProvider() {
				@Override
				protected boolean canStreamProperty(PropertyDescriptor descriptor) {

					final boolean canStream = super.canStreamProperty(descriptor);
					if (!canStream) {
						return false;
					}

					final boolean readMethodIsTransient = descriptor.getReadMethod() == null
							|| descriptor.getReadMethod().getAnnotation(Transient.class) != null;
					final boolean writeMethodIsTransient = descriptor.getWriteMethod() == null
							|| descriptor.getWriteMethod().getAnnotation(Transient.class) != null;
					final boolean isTransient = readMethodIsTransient || writeMethodIsTransient;
					if (isTransient) {
						return false;
					}

					return true;
				}

				@Override
				public void writeProperty(Object object, String propertyName, Object value) {
					if (!propertyWriteable(propertyName, object.getClass())) {
						return;
					}
					super.writeProperty(object, propertyName, value);
				}
			}), XStream.PRIORITY_VERY_LOW);
			xstream.registerConverter(new ReflectionConverter(xstream.getMapper(), xstream.getReflectionProvider()) {
				@Override
				public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
					if ((type != null) && AbstractSimpleCustomizableFieldControlPlugin.AbstractConfiguration.class
							.isAssignableFrom(type)) {
						return true;
					}
					if ((type != null) && Throwable.class.isAssignableFrom(type)) {
						return true;
					}
					return false;
				}
			}, XStream.PRIORITY_VERY_HIGH);
			xstream.addPermission(AnyTypePermission.ANY);
			xstream.ignoreUnknownElements();
		}

		private void configureSolutionDependencies(List<JAR> jars) {
			URLClassLoader jarsClassLoader = new URLClassLoader(
					jars.stream().map(JAR::getURL).toArray(length -> new URL[length]), JESB.class.getClassLoader());
			inMemoryCompiler.setFirstClassLoader(jarsClassLoader);
			pluginOperationMetadatas.clear();
			pluginActivatorMetadatas.clear();
			pluginResourceMetadatas.clear();
			for (JAR jar : jars) {
				JarURLConnection connection;
				try {
					connection = (JarURLConnection) new URL("jar:" + jar.getURL().toString() + "!/").openConnection();
				} catch (IOException e) {
					throw new UnexpectedError(e);
				}
				Manifest manifest;
				try {
					manifest = connection.getManifest();
				} catch (IOException e) {
					continue;
				}
				Attributes attributes = manifest.getMainAttributes();
				String operationMetadataClassNames = attributes
						.getValue(JAR.PLUGIN_OPERATION_METADATA_CLASSES_MANIFEST_KEY);
				String activatorMetadataClassNames = attributes
						.getValue(JAR.PLUGIN_ACTIVATOR_METADATA_CLASSES_MANIFEST_KEY);
				String resourceMetadataClassNames = attributes
						.getValue(JAR.PLUGIN_RESOURCE_METADATA_CLASSES_MANIFEST_KEY);
				if (operationMetadataClassNames != null) {
					for (String className : operationMetadataClassNames.split(",")) {
						if (className.isEmpty()) {
							continue;
						}
						try {
							pluginOperationMetadatas.add((OperationMetadata<?>) getJESBClass(className).newInstance());
						} catch (InstantiationException | IllegalAccessException e) {
							throw new UnexpectedError(e);
						}
					}
				}
				if (activatorMetadataClassNames != null) {
					for (String className : activatorMetadataClassNames.split(",")) {
						if (className.isEmpty()) {
							continue;
						}
						try {
							pluginActivatorMetadatas.add((ActivatorMetadata) getJESBClass(className).newInstance());
						} catch (InstantiationException | IllegalAccessException e) {
							throw new UnexpectedError(e);
						}
					}
				}
				if (resourceMetadataClassNames != null) {
					for (String className : resourceMetadataClassNames.split(",")) {
						if (className.isEmpty()) {
							continue;
						}
						try {
							pluginResourceMetadatas.add((ResourceMetadata) getJESBClass(className).newInstance());
						} catch (InstantiationException | IllegalAccessException e) {
							throw new UnexpectedError(e);
						}
					}
				}
			}
		}

		public ClassLoader getJESBResourceLoader() {
			return inMemoryCompiler.getFirstClassLoader();
		}

		public Class<?> getJESBClass(String typeName) throws PotentialError {
			String arrayComponentTypeName = MiscUtils.getArrayComponentTypeName(typeName);
			if (arrayComponentTypeName != null) {
				return MiscUtils.getArrayType(getJESBClass(arrayComponentTypeName));
			}
			if (ClassUtils.PRIMITIVE_CLASS_BY_NAME.containsKey(typeName)) {
				return ClassUtils.PRIMITIVE_CLASS_BY_NAME.get(typeName);
			}
			try {
				return inMemoryCompiler.loadClassThroughCache(typeName);
			} catch (ClassNotFoundException e1) {
				throw new PotentialError(e1);
			}
		}

		public Class<?> getJESBClassFromCanonicalName(String canonicalName) {
			try {
				return getJESBClass(canonicalName);
			} catch (PotentialError e1) {
				try {
					int lastDotIndex = canonicalName.lastIndexOf('.');
					if (lastDotIndex == -1) {
						throw new PotentialError(new UnexpectedError());
					}
					return getJESBClassFromCanonicalName(
							canonicalName.substring(0, lastDotIndex) + "$" + canonicalName.substring(lastDotIndex + 1));
				} catch (PotentialError e2) {
					throw new PotentialError(new ClassNotFoundException("Canonical name: " + canonicalName));
				}
			}
		}

	}

}
