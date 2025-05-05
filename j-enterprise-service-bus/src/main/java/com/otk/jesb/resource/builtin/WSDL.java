package com.otk.jesb.resource.builtin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.otk.jesb.UnexpectedError;
import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.ResourceMetadata;
import com.otk.jesb.util.Listener;
import com.otk.jesb.util.MiscUtils;
import com.sun.tools.ws.processor.ProcessorException;
import com.sun.tools.ws.wscompile.WsimportTool;

import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.util.ReflectionUIUtils;

public class WSDL extends Resource {

	private String text;
	private Map<String, String> dependencyTextByFileName = new HashMap<String, String>();

	private List<Class<?>> generatedClasses;

	public WSDL() {
		this(WSDL.class.getSimpleName() + MiscUtils.getDigitalUniqueIdentifier());
	}

	public WSDL(String name) {
		super(name);
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
		generatedClasses = null;
	}

	public Map<String, String> getDependencyTextByFileName() {
		return Collections.unmodifiableMap(dependencyTextByFileName);
	}

	public void setDependencyTextByFileName(Map<String, String> dependencyTextByFileName) {
		this.dependencyTextByFileName = dependencyTextByFileName;
		generatedClasses = null;
	}

	public void load(Source source) {
		dependencyTextByFileName.clear();
		try {
			load(source, new Listener<String>() {
				@Override
				public void handle(String text) {
					setText(text);
				}
			});
		} catch (Exception e) {
			throw new UnexpectedError(e);
		}
	}

	private String load(Source source, Listener<String> textHandler) throws Exception {
		try (InputStream in = source.getInputStream()) {
			String text = MiscUtils.read(in);
			for (String dependencyLocation : locateDependencies(text)) {
				String dependencyFileName = loadDependency(source, dependencyLocation);
				text = text.replace(dependencyLocation, dependencyFileName);
			}
			textHandler.handle(text);
		}
		return dependencyTextByFileName.size() + "_" + source.extractFileName();
	}

	private List<String> locateDependencies(String text) {
		List<String> result = new ArrayList<String>();
		for (Pattern compiledPattern : new Pattern[] {
				Pattern.compile(
						"<(?:[a-zA-Z0-9_]+:)?import(?:\\s+namespace=\"[^\"]*\")?\\s+schemaLocation=\"([^\"]+)\"",
						Pattern.DOTALL),
				Pattern.compile("<\\s*!DOCTYPE[^>]+\"([^\"]+)\"\\s*>"),
				Pattern.compile("<\\s*!DOCTYPE[^>]+'([^']+)'\\s*>"),
				Pattern.compile("<\\s*!ENTITY[^>]+SYSTEM\\s+\"([^\"]+)\"\\s*>"),
				Pattern.compile("<\\s*!ENTITY[^>]+SYSTEM\\s+'([^']+)'\\s*>"),
				Pattern.compile("<\\s*!ENTITY[^>]+PUBLIC\\s+\"[^\"]+\"\\s+\"([^\"]+)\"\\s*>"),
				Pattern.compile("<\\s*!ENTITY[^>]+PUBLIC\\s+'[^']+'\\s+'([^']+)'\\s*>") }) {
			Matcher matcher = compiledPattern.matcher(text);
			while (matcher.find()) {
				result.add(matcher.group(1));
			}
		}
		return result;
	}

	private String loadDependency(Source source, String dependencyLocation) throws Exception {
		URI sourceURI = source.toURI();
		URI dependencyURI = sourceURI.resolve(dependencyLocation);
		Source dependencySource;
		try {
			URL url = dependencyURI.toURL();
			dependencySource = new URLSource();
			((URLSource) dependencySource).setUrlSpecification(url.toString());
		} catch (MalformedURLException e) {
			File file = new File(dependencyURI);
			dependencySource = new FileSource();
			((FileSource) dependencySource).setFile(file);
		}
		final String[] dependencyTextHolder = new String[1];
		String dependencyFileName = load(dependencySource, new Listener<String>() {
			@Override
			public void handle(String text) {
				dependencyTextHolder[0] = text;
			}
		});
		dependencyTextByFileName.put(dependencyFileName, dependencyTextHolder[0]);
		return dependencyFileName;
	}

	private void generateClasses() {
		if (text == null) {
			generatedClasses = Collections.emptyList();
			return;
		}
		generatedClasses = null;
		try {
			File metaSchemaDirectory = MiscUtils.createTemporaryDirectory();
			File wsdlFile = new File(metaSchemaDirectory, "main.wsdl");
			File metaSchemaFile = new File(metaSchemaDirectory, "XMLSchema.xsd");
			File metaSchemaDTDFile = new File(metaSchemaDirectory, "XMLSchema.dtd");
			File metaSchemaDatatypesDTDFile = new File(metaSchemaDirectory, "datatypes.dtd");
			Map<File, String> dependencyTextByFile = new HashMap<File, String>();
			for (Map.Entry<String, String> dependencyTextByFileNameEntry : dependencyTextByFileName.entrySet()) {
				dependencyTextByFile.put(new File(metaSchemaDirectory, dependencyTextByFileNameEntry.getKey()),
						dependencyTextByFileNameEntry.getValue());
			}
			try {
				MiscUtils.write(wsdlFile, text, false);
				MiscUtils.write(metaSchemaFile,
						MiscUtils.read(WSDL.class.getResourceAsStream(metaSchemaFile.getName())), false);
				MiscUtils.write(metaSchemaDTDFile,
						MiscUtils.read(WSDL.class.getResourceAsStream(metaSchemaDTDFile.getName())), false);
				MiscUtils.write(metaSchemaDatatypesDTDFile,
						MiscUtils.read(WSDL.class.getResourceAsStream(metaSchemaDatatypesDTDFile.getName())), false);
				for (Map.Entry<File, String> dependencyTextByFileEntry : dependencyTextByFile.entrySet()) {
					MiscUtils.write(dependencyTextByFileEntry.getKey(), dependencyTextByFileEntry.getValue(), false);
				}
				File sourceDirectory = MiscUtils.createTemporaryDirectory();
				try {
					String[] wsImportArguments = new String[] { "-s", sourceDirectory.getPath(), "-keep", "-Xnocompile",
							"-b", metaSchemaFile.toURI().toString(), "-verbose", wsdlFile.getPath() };
					System.setProperty("javax.xml.accessExternalSchema", "all");
					System.setProperty("javax.xml.accessExternalDTD", "all");
					ByteArrayOutputStream logsBuffer = new ByteArrayOutputStream();
					boolean importStatus;
					Throwable importException;
					try {
						importStatus = new WsimportTool(logsBuffer).run(wsImportArguments);
						importException = null;
					} catch (Throwable t) {
						importStatus = false;
						importException = t;
					}
					if (!importStatus || (importException != null)) {
						throw new ProcessorException(
								"Failed to generate WSDL classes"
										+ ((logsBuffer.size() > 0) ? (":\n" + logsBuffer.toString()) : ""),
								importException);
					}
					generatedClasses = MiscUtils.IN_MEMORY_COMPILER.compile(sourceDirectory);
				} finally {
					MiscUtils.delete(sourceDirectory);
				}
			} finally {
				for (Map.Entry<File, String> dependencyTextByFileEntry : dependencyTextByFile.entrySet()) {
					MiscUtils.delete(dependencyTextByFileEntry.getKey());
				}
				MiscUtils.delete(metaSchemaDatatypesDTDFile);
				MiscUtils.delete(metaSchemaDTDFile);
				MiscUtils.delete(metaSchemaFile);
				MiscUtils.delete(wsdlFile);
				MiscUtils.delete(metaSchemaDirectory);
			}
		} catch (Exception e) {
			throw new UnexpectedError(e);
		}
	}

	public List<WSDL.ServiceDescriptor> getServiceDescriptors() {
		if (generatedClasses == null) {
			generateClasses();
		}
		return generatedClasses.stream().filter(c -> javax.xml.ws.Service.class.isAssignableFrom(c))
				.map(c -> new ServiceDescriptor(c)).collect(Collectors.toList());
	}

	public interface Source {

		InputStream getInputStream() throws IOException;

		String extractFileName();

		URI toURI();

	}

	public static class FileSource implements Source {

		private File file;

		public File getFile() {
			return file;
		}

		public void setFile(File file) {
			this.file = file;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return new FileInputStream(file);
		}

		@Override
		public String extractFileName() {
			return file.getName();
		}

		@Override
		public URI toURI() {
			return file.toURI();
		}

	}

	public static class URLSource implements Source {

		private String urlSpecification;

		public String getUrlSpecification() {
			return urlSpecification;
		}

		public void setUrlSpecification(String urlSpecification) {
			this.urlSpecification = urlSpecification;
		}

		@Override
		public InputStream getInputStream() throws IOException{
			try {
				return new URL(urlSpecification).openStream();
			} catch (MalformedURLException e) {
				throw new IOException(e);
			}
		}

		@Override
		public String extractFileName() {
			try {
				return new File(new URL(urlSpecification).toURI().getPath()).getName();
			} catch (MalformedURLException e) {
				throw new UnexpectedError(e);
			} catch (URISyntaxException e) {
				throw new UnexpectedError(e);
			}
		}

		@Override
		public URI toURI() {
			try {
				return new URL(urlSpecification).toURI();
			} catch (MalformedURLException e) {
				throw new UnexpectedError(e);
			} catch (URISyntaxException e) {
				throw new UnexpectedError(e);
			}
		}

	}

	public class OperationDescriptor {

		private Method operationMethod;

		public OperationDescriptor(Method m) {
			this.operationMethod = m;
		}

		public String getOperationSignature() {
			return ReflectionUIUtils.buildMethodSignature(operationMethod);
		}

		public Method retrieveMethod() {
			return operationMethod;
		}

	}

	public class PortDescriptor {

		private Class<?> portInterface;

		public PortDescriptor(Class<?> c) {
			this.portInterface = c;
		}

		public String getPortName() {
			return portInterface.getSimpleName();
		}

		public List<WSDL.OperationDescriptor> getOperationDescriptors() {
			return Arrays.asList(portInterface.getDeclaredMethods()).stream().map(m -> new OperationDescriptor(m))
					.collect(Collectors.toList());
		}

		public Class<?> retrieveInterface() {
			return portInterface;
		}

	}

	public class ServiceDescriptor {

		private Class<?> serviceClass;

		public ServiceDescriptor(Class<?> c) {
			this.serviceClass = c;
		}

		public String getServiceName() {
			return serviceClass.getSimpleName();
		}

		public List<PortDescriptor> getPortDescriptors() {
			return Arrays.asList(serviceClass.getDeclaredMethods()).stream()
					.filter(m -> Modifier.isPublic(m.getModifiers()) && m.getName().startsWith("get")
							&& (m.getParameterCount() == 0))
					.map(m -> new PortDescriptor(m.getReturnType())).collect(Collectors.toList());
		}

		public Class<?> retrieveClass() {
			return serviceClass;
		}

	}

	public static class Metadata implements ResourceMetadata {

		@Override
		public ResourcePath getResourceIconImagePath() {
			return new ResourcePath(
					ResourcePath.specifyClassPathResourceLocation(WSDL.class.getName().replace(".", "/") + ".png"));
		}

		@Override
		public Class<? extends Resource> getResourceClass() {
			return WSDL.class;
		}

		@Override
		public String getResourceTypeName() {
			return "WSDL";
		}

	}

}
