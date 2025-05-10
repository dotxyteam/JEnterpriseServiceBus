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

import com.otk.jesb.solution.AssetVisitor;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Reference;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.activity.Activity;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.resource.builtin.WSDL;
import com.otk.jesb.solution.Asset;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.UpToDate;

import xy.reflect.ui.info.ResourcePath;

public class CallSOAPWebServiceActivity implements Activity {

	private WSDL wsdl;
	private Class<?> serviceClass;
	private Class<?> portInterface;
	private Method operationMethod;
	private OperationInput operationInput;

	public CallSOAPWebServiceActivity(WSDL wsdl, Class<?> serviceClass, Class<?> portInterface, Method operationMethod,
			OperationInput operationInput) {
		this.wsdl = wsdl;
		this.serviceClass = serviceClass;
		this.portInterface = portInterface;
		this.operationMethod = operationMethod;
		this.operationInput = operationInput;
	}

	public WSDL getWSDL() {
		return wsdl;
	}

	public Class<?> getServiceClass() {
		return serviceClass;
	}

	public Class<?> getPortInterface() {
		return portInterface;
	}

	public Method getOperationMethod() {
		return operationMethod;
	}

	public OperationInput getOperationInput() {
		return operationInput;
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

		private Reference<WSDL> wsdlReference = new Reference<WSDL>(WSDL.class);
		private RootInstanceBuilder operationInputBuilder = new RootInstanceBuilder(
				OperationInput.class.getSimpleName(), new Accessor<String>() {
					@Override
					public String get() {
						Class<?> operationInputClass = upToDateOperationInputClass.get();
						if (operationInputClass == null) {
							return null;
						}
						return operationInputClass.getName();
					}
				});
		private UpToDate<Class<?>> upToDateOperationInputClass = new UpToDate<Class<?>>() {
			@Override
			protected Object retrieveLastModificationIdentifier() {
				WSDL.OperationDescriptor operation = retrieveOperationDescriptor();
				if (operation == null) {
					return null;
				}
				return operation.retrieveMethod();
			}

			@Override
			protected Class<?> obtainLatest() {
				return obtainOperationInputClass();
			}
		};
		private String serviceName;
		private String portName;
		private String operationSignature;

		private WSDL getWSDL() {
			return wsdlReference.resolve();
		}

		public Reference<WSDL> getWsdlReference() {
			return wsdlReference;
		}

		public void setWsdlReference(Reference<WSDL> wsdlReference) {
			this.wsdlReference = wsdlReference;
			tryToSelectValuesAutomatically();
		}

		public String getServiceName() {
			return serviceName;
		}

		public void setServiceName(String serviceName) {
			this.serviceName = serviceName;
			tryToSelectValuesAutomatically();
		}

		public String getPortName() {
			return portName;
		}

		public void setPortName(String portName) {
			this.portName = portName;
			tryToSelectValuesAutomatically();
		}

		public String getOperationSignature() {
			return operationSignature;
		}

		public void setOperationSignature(String operationSignature) {
			this.operationSignature = operationSignature;
			tryToSelectValuesAutomatically();
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

		@SuppressWarnings("unchecked")
		private Class<? extends OperationInput> obtainOperationInputClass() {
			WSDL.OperationDescriptor operation = retrieveOperationDescriptor();
			if (operation == null) {
				return null;
			}
			Method operationMethod = operation.retrieveMethod();
			String className = OperationInput.class.getPackage().getName() + "." + OperationInput.class.getSimpleName()
					+ MiscUtils.toDigitalUniqueIdentifier(this);
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
				return (Class<? extends OperationInput>) MiscUtils.IN_MEMORY_COMPILER.compile(className,
						javaSource.toString());
			} catch (CompilationError e) {
				throw new UnexpectedError(e);
			}
		}

		public List<WSDL> getWSDLOptions() {
			final List<WSDL> result = new ArrayList<WSDL>();
			Solution.INSTANCE.visitContents(new AssetVisitor() {
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
		public Activity build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			return new CallSOAPWebServiceActivity(getWSDL(), retrieveServiceDescriptor().retrieveClass(),
					retrievePortDescriptor().retrieveInterface(), retrieveOperationDescriptor().retrieveMethod(),
					(OperationInput) operationInputBuilder
							.build(new InstantiationContext(context.getVariables(), context.getPlan()
									.getValidationContext(context.getCurrentStep()).getVariableDeclarations())));
		}

		@Override
		public Class<?> getActivityResultClass(Plan currentPlan, Step currentStep) {
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
					.orElse(null);
		}

		private WSDL.PortDescriptor retrievePortDescriptor() {
			WSDL.ServiceDescriptor service = retrieveServiceDescriptor();
			if (service == null) {
				return null;
			}
			return service.getPortDescriptors().stream().filter(p -> p.getPortName().equals(portName)).findFirst()
					.orElse(null);
		}

		private WSDL.OperationDescriptor retrieveOperationDescriptor() {
			WSDL.PortDescriptor port = retrievePortDescriptor();
			if (port == null) {
				return null;
			}
			return port.getOperationDescriptors().stream()
					.filter(o -> o.getOperationSignature().equals(operationSignature)).findFirst().orElse(null);
		}

		@Override
		public void validate(boolean recursively, Plan plan, Step step) throws ValidationError {
			if (getWSDL() == null) {
				throw new ValidationError("Failed to resolve the WSDL reference");
			}
			if (retrieveServiceDescriptor() == null) {
				throw new ValidationError("Invalid service name '" + serviceName + "'");
			}
			if (retrievePortDescriptor() == null) {
				throw new ValidationError("Invalid port name '" + portName + "'");
			}
			if (retrieveOperationDescriptor() == null) {
				throw new ValidationError("Invalid operation signature '" + operationSignature + "'");
			}
		}

	}

}
