package com.otk.jesb.resource.builtin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import com.otk.jesb.Structure.ClassicStructure;
import com.otk.jesb.Structure.SimpleElement;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.instantiation.InstanceBuilder;
import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.ResourceMetadata;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.InstantiationUtils;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.UpToDate.VersionAccessException;
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
		String[] wsImportArguments = new String[] { "-s", outputDirectory.getPath(), "-p",
				InstantiationUtils.toRelativeTypeNameVariablePart(
						WSDL.class.getName().toLowerCase() + MiscUtils.toDigitalUniqueIdentifier(WSDL.this)),
				"-keep", "-Xnocompile", "-b", metaSchemaFile.toURI().toString(), "-verbose", mainFile.getPath() };
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

	public List<WSDL.ServiceClientDescriptor> getServiceClientDescriptors() {
		try {
			return upToDateGeneratedClasses.get().stream().filter(c -> javax.xml.ws.Service.class.isAssignableFrom(c))
					.map(c -> new ServiceClientDescriptor(c)).collect(Collectors.toList());
		} catch (VersionAccessException e) {
			throw new UnexpectedError(e);
		}
	}

	public List<WSDL.ServiceSpecificationDescriptor> getServiceSpecificationDescriptors() {
		try {
			return upToDateGeneratedClasses.get().stream()
					.filter(c -> c.isInterface() && c.getAnnotation(javax.jws.WebService.class) != null)
					.map(c -> new ServiceSpecificationDescriptor(c)).collect(Collectors.toList());
		} catch (VersionAccessException e) {
			throw new UnexpectedError(e);
		}
	}

	public WSDL.ServiceClientDescriptor getServiceClientDescriptor(String serviceName) {
		return getServiceClientDescriptors().stream().filter(s -> s.getServiceName().equals(serviceName)).findFirst()
				.orElse(null);
	}

	public WSDL.ServiceSpecificationDescriptor getServiceSpecificationDescriptor(String serviceName) {
		return getServiceSpecificationDescriptors().stream().filter(s -> s.getServiceName().equals(serviceName))
				.findFirst().orElse(null);
	}

	public static class OperationDescriptor {
		/*
		 * Cannot have simple inputClassByMethod/outputClassByMethod WeakHashMaps
		 * because the method object reference is not stable, unlike the method
		 * declaring class object reference that is then used as the WeakHashMap key.
		 */
		private static WeakHashMap<Class<?>, Map<Method, Class<? extends OperationInput>>> inputClassByMethodByDeclaringClass = new WeakHashMap<Class<?>, Map<Method, Class<? extends OperationInput>>>();
		private static WeakHashMap<Class<?>, Map<Method, Class<?>>> outputClassByMethodByDeclaringClass = new WeakHashMap<Class<?>, Map<Method, Class<?>>>();

		private Method operationMethod;

		public OperationDescriptor(Method m) {
			this.operationMethod = m;
		}

		public String getOperationSignature() {
			String result = ReflectionUIUtils.buildMethodSignature(operationMethod);
			result = InstantiationUtils.makeTypeNamesRelative(result, Arrays
					.asList(new InstanceBuilder(Accessor.returning(operationMethod.getDeclaringClass().getName()))));
			return result;
		}

		public Method retrieveMethod() {
			return operationMethod;
		}

		@SuppressWarnings("unchecked")
		public Class<? extends OperationInput> getOperationInputClass() {
			synchronized (inputClassByMethodByDeclaringClass) {
				inputClassByMethodByDeclaringClass.computeIfAbsent(operationMethod.getDeclaringClass(),
						(declaringClass -> new HashMap<Method, Class<? extends OperationInput>>()));
				inputClassByMethodByDeclaringClass.get(operationMethod.getDeclaringClass())
						.computeIfAbsent(operationMethod, operationMethod -> {
							String className = operationMethod.getDeclaringClass().getName() + "_"
									+ operationMethod.getName() + "." + OperationInput.class.getSimpleName();
							String additionalyImplemented = MiscUtils
									.adaptClassNameToSourceCode(OperationInput.class.getName());
							ClassicStructure stucture = new ClassicStructure();
							for (Parameter parameter : operationMethod.getParameters()) {
								SimpleElement element = new SimpleElement();
								element.setName(parameter.getName());
								element.setTypeName(parameter.getType().getName());
								stucture.getElements().add(element);
							}
							StringBuilder additionalMethodDeclarations = new StringBuilder();
							additionalMethodDeclarations.append("  @Override" + "\n");
							additionalMethodDeclarations.append("  public Object[] listParameterValues() {" + "\n");
							additionalMethodDeclarations
									.append("  return new Object[] {"
											+ MiscUtils.stringJoin(Arrays.asList(operationMethod.getParameters())
													.stream().map(p -> p.getName()).collect(Collectors.toList()), ", ")
											+ "};" + "\n");
							additionalMethodDeclarations.append("  }" + "\n");
							try {
								return (Class<? extends OperationInput>) MiscUtils.IN_MEMORY_COMPILER.compile(className,
										stucture.generateJavaTypeSourceCode(className, additionalyImplemented, null,
												additionalMethodDeclarations.toString()));
							} catch (CompilationError e) {
								throw new UnexpectedError(e);
							}
						});
				return inputClassByMethodByDeclaringClass.get(operationMethod.getDeclaringClass()).get(operationMethod);
			}
		}

		@SuppressWarnings("unchecked")
		public Class<?> getOperationOutputClass() {
			synchronized (outputClassByMethodByDeclaringClass) {
				outputClassByMethodByDeclaringClass.computeIfAbsent(operationMethod.getDeclaringClass(),
						(declaringClass -> new HashMap<Method, Class<?>>()));
				outputClassByMethodByDeclaringClass.get(operationMethod.getDeclaringClass())
						.computeIfAbsent(operationMethod, operationMethod -> {
							if (operationMethod.getReturnType() == void.class) {
								return null;
							}
							String className = operationMethod.getDeclaringClass().getName() + "_"
									+ operationMethod.getName() + ".OperationOutput";
							ClassicStructure stucture = new ClassicStructure();
							{
								SimpleElement resultElement = new SimpleElement();
								resultElement.setName("result");
								resultElement.setTypeName(operationMethod.getReturnType().getName());
								stucture.getElements().add(resultElement);
							}
							try {
								return (Class<? extends OperationInput>) MiscUtils.IN_MEMORY_COMPILER.compile(className,
										stucture.generateJavaTypeSourceCode(className));
							} catch (CompilationError e) {
								throw new UnexpectedError(e);
							}
						});
				return outputClassByMethodByDeclaringClass.get(operationMethod.getDeclaringClass())
						.get(operationMethod);
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((operationMethod == null) ? 0 : operationMethod.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OperationDescriptor other = (OperationDescriptor) obj;
			if (operationMethod == null) {
				if (other.operationMethod != null)
					return false;
			} else if (!operationMethod.equals(other.operationMethod))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "Operation [signature=" + getOperationSignature() + "]";
		}

		public interface OperationInput {

			Object[] listParameterValues();

		}
	}

	public static class PortDescriptor {

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

		public WSDL.OperationDescriptor getOperationDescriptor(String operationSignature) {
			return getOperationDescriptors().stream().filter(o -> o.getOperationSignature().equals(operationSignature))
					.findFirst().orElse(null);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((portInterface == null) ? 0 : portInterface.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PortDescriptor other = (PortDescriptor) obj;
			if (portInterface == null) {
				if (other.portInterface != null)
					return false;
			} else if (!portInterface.equals(other.portInterface))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "Port [name=" + getPortName() + "]";
		}

	}

	public static class ServiceClientDescriptor {

		private Class<?> serviceClass;

		public ServiceClientDescriptor(Class<?> c) {
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

		public WSDL.PortDescriptor getPortDescriptor(String portName) {
			return getPortDescriptors().stream().filter(p -> p.getPortName().equals(portName)).findFirst().orElse(null);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((serviceClass == null) ? 0 : serviceClass.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ServiceClientDescriptor other = (ServiceClientDescriptor) obj;
			if (serviceClass == null) {
				if (other.serviceClass != null)
					return false;
			} else if (!serviceClass.equals(other.serviceClass))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "ServiceClient [serviceName=" + getServiceName() + "]";
		}

	}

	public static class ServiceSpecificationDescriptor {

		private static WeakHashMap<Class<?>, Class<?>> implementationClassByInterface = new WeakHashMap<Class<?>, Class<?>>();

		private Class<?> serviceInterface;

		public ServiceSpecificationDescriptor(Class<?> c) {
			this.serviceInterface = c;
		}

		public String getServiceName() {
			javax.jws.WebService annotation = serviceInterface.getAnnotation(javax.jws.WebService.class);
			if (annotation.name() != null) {
				return annotation.name();
			} else {
				return serviceInterface.getSimpleName();
			}
		}

		public Class<?> retrieveInterface() {
			return serviceInterface;
		}

		public List<WSDL.OperationDescriptor> getOperationDescriptors() {
			return Arrays.asList(serviceInterface.getDeclaredMethods()).stream().map(m -> new OperationDescriptor(m))
					.collect(Collectors.toList());
		}

		public WSDL.OperationDescriptor getOperationDescriptor(String operationSignature) {
			return getOperationDescriptors().stream().filter(o -> o.getOperationSignature().equals(operationSignature))
					.findFirst().orElse(null);
		}

		public Class<?> getImplementationClass() {
			synchronized (implementationClassByInterface) {
				implementationClassByInterface.computeIfAbsent(serviceInterface, serviceInterface -> {
					String className = serviceInterface.getName() + "Impl";
					StringBuilder javaSource = new StringBuilder();
					javaSource.append("package " + MiscUtils.extractPackageNameFromClassName(className) + ";" + "\n");
					javaSource.append(
							"public class " + MiscUtils.extractSimpleNameFromClassName(className) + " implements "
									+ MiscUtils.adaptClassNameToSourceCode(serviceInterface.getName()) + "{" + "\n");
					javaSource.append("  private " + InvocationHandler.class.getName() + " invocationHandler;\n");
					javaSource.append("  public " + MiscUtils.extractSimpleNameFromClassName(className) + "("
							+ InvocationHandler.class.getName() + " invocationHandler){" + "\n");
					javaSource.append("    this.invocationHandler = invocationHandler;\n");
					javaSource.append("  }" + "\n");
					for (Method method : serviceInterface.getMethods()) {
						if (!Modifier.isAbstract(method.getModifiers())) {
							continue;
						}
						javaSource.append("  @Override\n");
						javaSource.append(
								"  public " + MiscUtils.adaptClassNameToSourceCode(method.getReturnType().getName())
										+ " " + method.getName() + "("
										+ Arrays.stream(method.getParameters())
												.map(parameter -> MiscUtils.adaptClassNameToSourceCode(
														parameter.getType().getName()) + " " + parameter.getName())
												.collect(Collectors.joining(", "))
										+ ") {" + "\n");
						String methodReflectionAccessInstruction = MiscUtils
								.adaptClassNameToSourceCode(serviceInterface.getName()) + ".class.getMethod(\""
								+ method.getName() + "\", new Class[]{"
								+ Arrays.stream(method.getParameters())
										.map(parameter -> MiscUtils
												.adaptClassNameToSourceCode(parameter.getType().getName()) + ".class")
										.collect(Collectors.joining(", "))
								+ "})";
						String methodArgumentArrayCreationInstruction = "new Object[]{"
								+ Arrays.stream(method.getParameters()).map(parameter -> parameter.getName())
										.collect(Collectors.joining(", "))
								+ "}";
						javaSource.append("    try {\n");
						javaSource
								.append("        "
										+ (method.getReturnType().equals(void.class) ? ""
												: ("return (" + MiscUtils.adaptClassNameToSourceCode(
														method.getReturnType().getName()) + ")"))
										+ "invocationHandler.invoke(this, " + methodReflectionAccessInstruction + ", "
										+ methodArgumentArrayCreationInstruction + ");" + "\n");
						javaSource.append("    } catch (Throwable t) {\n");
						for (Class<?> exceptionType : method.getExceptionTypes()) {
							javaSource.append("			if(t instanceof "
									+ MiscUtils.adaptClassNameToSourceCode(exceptionType.getName()) + ") {\n");
							javaSource.append("				throw ("
									+ MiscUtils.adaptClassNameToSourceCode(exceptionType.getName()) + ")t;\n");
							javaSource.append("			}\n");
						}
						javaSource.append("			throw new "
								+ MiscUtils.adaptClassNameToSourceCode(UnexpectedError.class.getName()) + "(t);\n");
						javaSource.append("    }");
						javaSource.append("  }" + "\n");
					}
					javaSource.append("}" + "\n");
					try {
						return (Class<?>) MiscUtils.IN_MEMORY_COMPILER.compile(className, javaSource.toString());
					} catch (CompilationError e) {
						throw new UnexpectedError(e);
					}
				});
				return implementationClassByInterface.get(serviceInterface);
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((serviceInterface == null) ? 0 : serviceInterface.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ServiceSpecificationDescriptor other = (ServiceSpecificationDescriptor) obj;
			if (serviceInterface == null) {
				if (other.serviceInterface != null)
					return false;
			} else if (!serviceInterface.equals(other.serviceInterface))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "ServiceSpecification [serviceName=" + getServiceName() + "]";
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
