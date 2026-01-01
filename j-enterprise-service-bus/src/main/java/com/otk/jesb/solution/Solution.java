package com.otk.jesb.solution;

import java.beans.Transient;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.otk.jesb.solution.AssetVisitor;
import com.otk.jesb.Debugger;
import com.otk.jesb.EnvironmentSettings;
import com.otk.jesb.PluginBuilder;
import com.otk.jesb.PotentialError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.activation.builtin.WatchFileSystem;
import com.otk.jesb.operation.builtin.Evaluate;
import com.otk.jesb.resource.Resource;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.Serializer;
import com.thoughtworks.xstream.XStream;

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
	private Runtime runtime = createRuntime();
	private Serializer serializer = createSerializer();

	protected Runtime createRuntime() {
		return new Runtime();
	}

	protected Serializer createSerializer() {
		return new Serializer() {

			@Override
			protected XStream createXstream() {
				XStream result = super.createXstream();
				result.setClassLoader(runtime.getInMemoryCompiler().getCompiledClassesLoader());
				return result;
			}

		};
	}

	public Runtime getRuntime() {
		return runtime;
	}

	public Serializer getSerializer() {
		return serializer;
	}

	public List<JAR> getRequiredJARs() {
		return requiredJARs;
	}

	public void setRequiredJARs(List<JAR> requiredJARs) {
		this.requiredJARs = requiredJARs;
		runtime.configureDependencies(requiredJARs);
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
			environmentSettings = (EnvironmentSettings) getSerializer().read(fileInputStream);
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
			final List<String> sortedNames = (List<String>) getSerializer().read(fileInputStream);
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
					Asset asset = (Asset) getSerializer().read(fileInputStream);
					return asset;
				}
			} catch (Exception e) {
				throw new IOException("Failed to load '" + fileOrDirectoryPath + "'", e);
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
			getSerializer().write(environmentSettings, fileOutputStream);
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
			getSerializer().write(sortedNames, fileOutputStream);
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
					getSerializer().write(asset, fileOutputStream);
				}
				Files.write(assetPath, fileOutputStream.toByteArray());
			}
		}
	}

}
