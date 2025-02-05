package com.otk.jesb.resource.builtin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.ResourceMetadata;
import com.otk.jesb.util.MiscUtils;
import com.sun.tools.ws.wscompile.WsimportTool;

import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.util.ReflectionUIUtils;

public class WSDL extends Resource {

	private String text;

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
		generateClasses();
	}

	public void load(Source source) {
		try (InputStream in = source.getInputStream()) {
			setText(MiscUtils.read(in));
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private void generateClasses() {
		generatedClasses = null;
		try {
			File wsdlFile = MiscUtils.createTemporaryFile("wsdl");
			try {
				MiscUtils.write(wsdlFile, text, false);
				File sourceDirectory = MiscUtils.createTemporaryDirectory();
				try {
					try {
						String[] wsImportArguments = new String[] { "-s", sourceDirectory.getPath(), "-keep",
								"-Xnocompile", "-b", "http://www.w3.org/2001/XMLSchema.xsd", "-verbose",
								wsdlFile.getPath() };
						System.setProperty("javax.xml.accessExternalSchema", "all");
						System.setProperty("javax.xml.accessExternalDTD", "all");
						ByteArrayOutputStream logsBuffer = new ByteArrayOutputStream();
						if (!new WsimportTool(logsBuffer).run(wsImportArguments)) {
							throw new Exception("Failed to generate classes:\n" + logsBuffer.toString());
						}
					} catch (Throwable t) {
						throw new RuntimeException(t);
					}
					generatedClasses = MiscUtils.IN_MEMORY_JAVA_COMPILER.compile(sourceDirectory);
				} finally {
					MiscUtils.delete(sourceDirectory);
				}
			} finally {
				MiscUtils.delete(wsdlFile);
			}
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	public List<WSDL.ServiceDescriptor> getServiceDescriptors() {
		if (generatedClasses == null) {
			return Collections.emptyList();
		}
		return generatedClasses.stream().filter(c -> javax.xml.ws.Service.class.isAssignableFrom(c))
				.map(c -> new ServiceDescriptor(c)).collect(Collectors.toList());
	}

	public interface Source {

		InputStream getInputStream();

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
		public InputStream getInputStream() {
			try {
				return new FileInputStream(file);
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
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
		public InputStream getInputStream() {
			try {
				return new URL(urlSpecification).openStream();
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
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
