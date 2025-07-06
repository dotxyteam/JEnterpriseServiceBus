package com.otk.jesb.resource.builtin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.ResourceMetadata;
import com.otk.jesb.util.MiscUtils;
import com.sun.tools.ws.processor.ProcessorException;
import com.sun.tools.ws.wscompile.WsimportTool;

import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.util.ReflectionUIUtils;

public class WSDL extends XMLBasedDocumentResource {

	public WSDL() {
		this(WSDL.class.getSimpleName() + MiscUtils.getDigitalUniqueIdentifier());
	}

	public WSDL(String name) {
		super(name);
	}

	@Override
	protected void runClassesGenerationTool(File mainFile, File metaSchemaFile, File outputDirectory) {
		String[] wsImportArguments = new String[] { "-s", outputDirectory.getPath(), "-keep", "-Xnocompile", "-b",
				metaSchemaFile.toURI().toString(), "-verbose", mainFile.getPath() };
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
			throw new ProcessorException("Failed to generate WSDL classes"
					+ ((logsBuffer.size() > 0) ? (":\n" + logsBuffer.toString()) : ""), importException);
		}
	}

	public List<WSDL.ServiceDescriptor> getServiceDescriptors() {
		if (generatedClasses == null) {
			generateClasses();
		}
		return generatedClasses.stream().filter(c -> javax.xml.ws.Service.class.isAssignableFrom(c))
				.map(c -> new ServiceDescriptor(c)).collect(Collectors.toList());
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
