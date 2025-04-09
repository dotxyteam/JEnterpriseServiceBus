package com.otk.jesb.activity.builtin;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.otk.jesb.Asset;
import com.otk.jesb.AssetVisitor;
import com.otk.jesb.Plan.ExecutionContext;
import com.otk.jesb.Plan.ValidationContext;
import com.otk.jesb.Solution;
import com.otk.jesb.activity.Activity;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.instantiation.CompilationContext;
import com.otk.jesb.instantiation.EvaluationContext;
import com.otk.jesb.instantiation.Function;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.resource.builtin.WSDL;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.info.ResourcePath;

public class CallSOAPWebServiceActivity implements Activity {

	private WSDL wsdl;
	private Class<?> serviceClass;
	private Class<?> portInterface;
	private Method operationMethod;
	private OperationInput operationInput;

	public WSDL getWSDL() {
		return wsdl;
	}

	public void setWSDL(WSDL wsdl) {
		this.wsdl = wsdl;
	}

	public Class<?> getServiceClass() {
		return serviceClass;
	}

	public void setServiceClass(Class<?> serviceClass) {
		this.serviceClass = serviceClass;
	}

	public Class<?> getPortInterface() {
		return portInterface;
	}

	public void setPortInterface(Class<?> portInterface) {
		this.portInterface = portInterface;
	}

	public Method getOperationMethod() {
		return operationMethod;
	}

	public void setOperationMethod(Method operationMethod) {
		this.operationMethod = operationMethod;
	}

	public OperationInput getOperationInput() {
		return operationInput;
	}

	public void setOperationInput(OperationInput operationInput) {
		this.operationInput = operationInput;
	}

	@Override
	public Object execute() throws Exception {
		File wsdlFile = MiscUtils.createTemporaryFile("wsdl");
		try {
			MiscUtils.write(wsdlFile, wsdl.getText(), false);
			Object service = serviceClass.getConstructor(URL.class).newInstance(wsdlFile.toURI().toURL());
			Object port = serviceClass.getMethod("getPort", Class.class).invoke(service, portInterface);
			return operationMethod.invoke(port,
					(operationInput == null) ? new Object[0] : operationInput.listParameterValues());
		} finally {
			MiscUtils.delete(wsdlFile);
		}
	}

	public interface OperationInput {

		Object[] listParameterValues();

	}

	public static class Metadata implements ActivityMetadata {

		@Override
		public String getActivityTypeName() {
			return "Call SOAP Web Service";
		}

		@Override
		public String getCategoryName() {
			return "SOAP";
		}

		@Override
		public Class<? extends ActivityBuilder> getActivityBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getActivityIconImagePath() {
			return new ResourcePath(ResourcePath.specifyClassPathResourceLocation(
					CallSOAPWebServiceActivity.class.getName().replace(".", "/") + ".png"));
		}
	}

	public static class Builder implements ActivityBuilder {

		private Class<? extends OperationInput> operationInputClass;

		private WSDL wsdl;
		private RootInstanceBuilder operationInputBuilder = new RootInstanceBuilder(
				OperationInput.class.getSimpleName(), new Accessor<String>() {
					@Override
					public String get() {
						if (operationInputClass == null) {
							return null;
						}
						return operationInputClass.getName();
					}
				});
		private String serviceName;
		private String portName;
		private String operationSignature;

		public WSDL getWSDL() {
			return wsdl;
		}

		public void setWSDL(WSDL wsdl) {
			this.wsdl = wsdl;
			tryToSelectValuesAutomatically();
			tryToUpdateOperationInputClass();
		}

		public String getServiceName() {
			return serviceName;
		}

		public void setServiceName(String serviceName) {
			this.serviceName = serviceName;
			tryToSelectValuesAutomatically();
			tryToUpdateOperationInputClass();
		}

		public String getPortName() {
			return portName;
		}

		public void setPortName(String portName) {
			this.portName = portName;
			tryToSelectValuesAutomatically();
			tryToUpdateOperationInputClass();
		}

		public String getOperationSignature() {
			return operationSignature;
		}

		public void setOperationSignature(String operationSignature) {
			this.operationSignature = operationSignature;
			tryToSelectValuesAutomatically();
			tryToUpdateOperationInputClass();
		}

		public RootInstanceBuilder getOperationInputBuilder() {
			return operationInputBuilder;
		}

		public void setOperationInputBuilder(RootInstanceBuilder operationInputBuilder) {
			this.operationInputBuilder = operationInputBuilder;
		}

		private void tryToSelectValuesAutomatically() {
			try {
				if (serviceName == null) {
					WSDL wsdl = getWSDL();
					if (wsdl != null) {
						List<WSDL.ServiceDescriptor> services = wsdl.getServiceDescriptors();
						if (services.size() > 0) {
							serviceName = services.get(0).getServiceName();
						}
					}
				}
				if (portName == null) {
					WSDL.ServiceDescriptor service = retrieveServiceDescriptor();
					if (service != null) {
						List<WSDL.PortDescriptor> ports = service.getPortDescriptors();
						if (ports.size() > 0) {
							portName = ports.get(0).getPortName();
						}
					}
				}
				if (operationSignature == null) {
					WSDL.PortDescriptor port = retrievePortDescriptor();
					if (port != null) {
						List<WSDL.OperationDescriptor> operations = port.getOperationDescriptors();
						if (operations.size() > 0) {
							operationSignature = operations.get(0).getOperationSignature();
						}
					}
				}
			} catch (Throwable t) {
			}
		}

		private void tryToUpdateOperationInputClass() {
			try {
				operationInputClass = createOperationInputClass();
			} catch (Throwable t) {
			}
		}

		@SuppressWarnings("unchecked")
		private Class<? extends OperationInput> createOperationInputClass() {
			WSDL.OperationDescriptor operation = retrieveOperationDescriptor();
			if (operation == null) {
				return null;
			}
			Method operationMethod = operation.retrieveMethod();
			if (operationMethod.getParameterCount() == 0) {
				return null;
			}
			String className = OperationInput.class.getPackage().getName() + "." + OperationInput.class.getSimpleName()
					+ MiscUtils.getDigitalUniqueIdentifier();
			StringBuilder javaSource = new StringBuilder();
			javaSource.append("package " + MiscUtils.extractPackageNameFromClassName(className) + ";" + "\n");
			javaSource.append("public class " + MiscUtils.extractSimpleNameFromClassName(className) + " implements "
					+ MiscUtils.adaptClassNameToSourceCode(OperationInput.class.getName()) + "{" + "\n");
			for (Parameter parameter : operationMethod.getParameters()) {
				javaSource.append("  private " + MiscUtils.adaptClassNameToSourceCode(parameter.getType().getName())
						+ " " + parameter.getName() + ";\n");
			}
			List<String> constructorParameterDeclarations = new ArrayList<String>();
			for (Parameter parameter : operationMethod.getParameters()) {
				constructorParameterDeclarations.add(MiscUtils.adaptClassNameToSourceCode(parameter.getType().getName())
						+ " " + parameter.getName());
			}
			javaSource.append("  public " + MiscUtils.extractSimpleNameFromClassName(className) + "("
					+ MiscUtils.stringJoin(constructorParameterDeclarations, ", ") + "){" + "\n");
			for (Parameter parameter : operationMethod.getParameters()) {
				javaSource.append("    this." + parameter.getName() + " = " + parameter.getName() + ";\n");
			}
			javaSource.append("  }" + "\n");
			javaSource.append("  @Override" + "\n");
			javaSource.append("  public Object[] listParameterValues() {" + "\n");
			javaSource.append(
					"  return new Object[] {" + MiscUtils.stringJoin(Arrays.asList(operationMethod.getParameters())
							.stream().map(p -> p.getName()).collect(Collectors.toList()), ", ") + "};" + "\n");
			javaSource.append("  }" + "\n");
			javaSource.append("}" + "\n");
			try {
				return (Class<? extends OperationInput>) MiscUtils.IN_MEMORY_JAVA_COMPILER.compile(className,
						javaSource.toString());
			} catch (CompilationError e) {
				throw new AssertionError(e);
			}
		}

		public List<WSDL> getWSDLOptions() {
			final List<WSDL> result = new ArrayList<WSDL>();
			Solution.INSTANCE.visitAssets(new AssetVisitor() {
				@Override
				public boolean visitAsset(Asset asset) {
					if (asset instanceof WSDL) {
						result.add((WSDL) asset);
					}
					return true;
				}
			});
			return result;
		}

		public List<String> getServiceNameOptions() {
			WSDL wsdl = getWSDL();
			if (wsdl == null) {
				return Collections.emptyList();
			}
			return wsdl.getServiceDescriptors().stream().map(s -> s.getServiceName()).collect(Collectors.toList());
		}

		public List<String> getPortNameOptions() {
			WSDL.ServiceDescriptor service = retrieveServiceDescriptor();
			if (service == null) {
				return Collections.emptyList();
			}
			return service.getPortDescriptors().stream().map(p -> p.getPortName()).collect(Collectors.toList());
		}

		public List<String> getOperationSignatureOptions() {
			WSDL.PortDescriptor port = retrievePortDescriptor();
			if (port == null) {
				return Collections.emptyList();
			}
			return port.getOperationDescriptors().stream().map(o -> o.getOperationSignature())
					.collect(Collectors.toList());
		}

		@Override
		public Activity build(ExecutionContext context) throws Exception {
			CallSOAPWebServiceActivity result = new CallSOAPWebServiceActivity();
			result.setWSDL(wsdl);
			result.setServiceClass(retrieveServiceDescriptor().retrieveClass());
			result.setPortInterface(retrievePortDescriptor().retrieveInterface());
			result.setOperationMethod(retrieveOperationDescriptor().retrieveMethod());
			result.setOperationInput(
					(OperationInput) operationInputBuilder.build(new EvaluationContext(context, null)));
			return result;
		}

		@Override
		public Class<?> getActivityResultClass() {
			WSDL.OperationDescriptor operation = retrieveOperationDescriptor();
			if (operation == null) {
				return null;
			}
			return operation.retrieveMethod().getReturnType();
		}

		private WSDL.ServiceDescriptor retrieveServiceDescriptor() {
			WSDL wsdl = getWSDL();
			if (wsdl == null) {
				return null;
			}
			return wsdl.getServiceDescriptors().stream().filter(s -> s.getServiceName().equals(serviceName)).findFirst()
					.get();
		}

		private WSDL.PortDescriptor retrievePortDescriptor() {
			WSDL.ServiceDescriptor service = retrieveServiceDescriptor();
			if (service == null) {
				return null;
			}
			return service.getPortDescriptors().stream().filter(p -> p.getPortName().equals(portName)).findFirst()
					.get();
		}

		private WSDL.OperationDescriptor retrieveOperationDescriptor() {
			WSDL.PortDescriptor port = retrievePortDescriptor();
			if (port == null) {
				return null;
			}
			return port.getOperationDescriptors().stream()
					.filter(o -> o.getOperationSignature().equals(operationSignature)).findFirst().get();
		}

		@Override
		public CompilationContext findFunctionCompilationContext(Function function,
				ValidationContext validationContext) {
			return operationInputBuilder.getFacade().findFunctionCompilationContext(function, validationContext);
		}

	}

}
