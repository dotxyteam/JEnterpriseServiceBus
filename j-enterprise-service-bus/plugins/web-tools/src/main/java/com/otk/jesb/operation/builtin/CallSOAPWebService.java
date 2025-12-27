package com.otk.jesb.operation.builtin;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebFault;

import com.otk.jesb.solution.Plan;
import com.otk.jesb.PotentialError;
import com.otk.jesb.Reference;
import com.otk.jesb.ValidationError;
import com.otk.jesb.Variant;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.resource.builtin.WebDocumentBasedResource;
import com.otk.jesb.resource.builtin.WSDL;
import com.otk.jesb.resource.builtin.WSDL.OperationDescriptor.OperationInput;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.MiscUtils;
import xy.reflect.ui.info.ResourcePath;

public class CallSOAPWebService implements Operation {

	private WSDL wsdl;
	private Class<?> serviceClass;
	private Class<?> portInterface;
	private Method operationMethod;
	private OperationInput operationInput;
	private String customServiceEndpointURL;
	private Class<?> operationOutputClass;

	public CallSOAPWebService(WSDL wsdl, Class<?> serviceClass, Class<?> portInterface, Method operationMethod,
			OperationInput operationInput, String customServiceEndpointURL, Class<?> operationOutputClass) {
		this.wsdl = wsdl;
		this.serviceClass = serviceClass;
		this.portInterface = portInterface;
		this.operationMethod = operationMethod;
		this.operationInput = operationInput;
		this.customServiceEndpointURL = customServiceEndpointURL;
		this.operationOutputClass = operationOutputClass;
	}

	public WebDocumentBasedResource getWSDL() {
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

	public String getCustomServiceEndpointURL() {
		return customServiceEndpointURL;
	}

	public Class<?> getOperationOutputClass() {
		return operationOutputClass;
	}

	@Override
	public Object execute(Solution solutionInstance) throws Exception {
		File wsdlFile = MiscUtils.createTemporaryFile("wsdl");
		try {
			MiscUtils.write(wsdlFile, wsdl.getText(), false);
			Object service = serviceClass.getConstructor(URL.class).newInstance(wsdlFile.toURI().toURL());
			Object port = serviceClass.getMethod("getPort", Class.class).invoke(service, portInterface);
			if (customServiceEndpointURL != null) {
				BindingProvider bindingProvider = (BindingProvider) port;
				bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
						customServiceEndpointURL);
			}
			Object operationResult = operationMethod.invoke(port,
					(operationInput == null) ? new Object[0] : operationInput.listParameterValues());
			if (operationOutputClass == null) {
				return null;
			}
			return operationOutputClass.getConstructor(operationMethod.getReturnType()).newInstance(operationResult);
		} catch (InvocationTargetException e) {
			if (e.getTargetException() instanceof Exception) {
				if (e.getTargetException().getClass().getAnnotation(WebFault.class) != null) {
					throw new WSDL.ResponseException(wsdl.faultExceptionToDescription(
							(Exception) e.getTargetException(), operationMethod, solutionInstance));
				} else {
					throw (Exception) e.getTargetException();
				}
			} else {
				throw new PotentialError(e.getTargetException());
			}
		} finally {
			MiscUtils.delete(wsdlFile);
		}
	}

	public static class Metadata implements OperationMetadata<CallSOAPWebService> {

		@Override
		public String getOperationTypeName() {
			return "Call SOAP Web Service";
		}

		@Override
		public String getCategoryName() {
			return "SOAP";
		}

		@Override
		public Class<? extends OperationBuilder<CallSOAPWebService>> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(CallSOAPWebService.class.getName().replace(".", "/") + ".png"));
		}
	}

	public static class Builder implements OperationBuilder<CallSOAPWebService> {

		private Reference<WSDL> wsdlReference = new Reference<WSDL>(WSDL.class);
		private RootInstanceBuilder operationInputBuilder = new RootInstanceBuilder(
				OperationInput.class.getSimpleName(), new OperationInputClassNameAccessor());
		private String serviceName;
		private String portName;
		private String operationSignature;
		private Variant<String> customServiceEndpointURLVariant = new Variant<String>(String.class);

		private WSDL getWSDL(Solution solutionInstance) {
			return wsdlReference.resolve(solutionInstance);
		}

		public Reference<WSDL> getWsdlReference() {
			return wsdlReference;
		}

		public void setWsdlReference(Reference<WSDL> wsdlReference) {
			this.wsdlReference = wsdlReference;
		}

		public void setWsdlReferenceAndAutoConfigure(Reference<WSDL> wsdlReference, Solution solutionInstance) {
			setWsdlReference(wsdlReference);
			tryToSelectValuesAutomatically(solutionInstance);
		}

		public String getServiceName() {
			return serviceName;
		}

		public void setServiceName(String serviceName) {
			this.serviceName = serviceName;
		}

		public void setServiceNameAndAutoConfigure(String serviceName, Solution solutionInstance) {
			setServiceName(serviceName);
			tryToSelectValuesAutomatically(solutionInstance);
		}

		public String getPortName() {
			return portName;
		}

		public void setPortName(String portName) {
			this.portName = portName;
		}

		public void setPortNameAndAutoConfigure(String portName, Solution solutionInstance) {
			setPortName(portName);
			tryToSelectValuesAutomatically(solutionInstance);
		}

		public String getOperationSignature() {
			return operationSignature;
		}

		public void setOperationSignature(String operationSignature) {
			this.operationSignature = operationSignature;
		}

		public void setOperationSignatureAndAutoConfigure(String operationSignature, Solution solutionInstance) {
			setOperationSignature(operationSignature);
			tryToSelectValuesAutomatically(solutionInstance);
		}

		public RootInstanceBuilder getOperationInputBuilder() {
			return operationInputBuilder;
		}

		public void setOperationInputBuilder(RootInstanceBuilder operationInputBuilder) {
			this.operationInputBuilder = operationInputBuilder;
		}

		public Variant<String> getCustomServiceEndpointURLVariant() {
			return customServiceEndpointURLVariant;
		}

		public void setCustomServiceEndpointURLVariant(Variant<String> customServiceEndpointURLVariant) {
			this.customServiceEndpointURLVariant = customServiceEndpointURLVariant;
		}

		private void tryToSelectValuesAutomatically(Solution solutionInstance) {
			try {
				if (serviceName == null) {
					WSDL wsdl = getWSDL(solutionInstance);
					if (wsdl != null) {
						List<WSDL.ServiceClientDescriptor> services = wsdl
								.getServiceClientDescriptors(solutionInstance);
						if (services.size() > 0) {
							serviceName = services.get(0).getServiceName();
						}
					}
				}
				if (portName == null) {
					WSDL.ServiceClientDescriptor service = retrieveServiceClientDescriptor(solutionInstance);
					if (service != null) {
						List<WSDL.PortDescriptor> ports = service.getPortDescriptors();
						if (ports.size() > 0) {
							portName = ports.get(0).getPortName();
						}
					}
				}
				if (operationSignature == null) {
					WSDL.PortDescriptor port = retrievePortDescriptor(solutionInstance);
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

		public List<String> getServiceNameOptions(Solution solutionInstance) {
			WSDL wsdl = getWSDL(solutionInstance);
			if (wsdl == null) {
				return Collections.emptyList();
			}
			return wsdl.getServiceClientDescriptors(solutionInstance).stream().map(s -> s.getServiceName())
					.collect(Collectors.toList());
		}

		public List<String> getPortNameOptions(Solution solutionInstance) {
			WSDL.ServiceClientDescriptor service = retrieveServiceClientDescriptor(solutionInstance);
			if (service == null) {
				return Collections.emptyList();
			}
			return service.getPortDescriptors().stream().map(p -> p.getPortName()).collect(Collectors.toList());
		}

		public List<String> getOperationSignatureOptions(Solution solutionInstance) {
			WSDL.PortDescriptor port = retrievePortDescriptor(solutionInstance);
			if (port == null) {
				return Collections.emptyList();
			}
			return port.getOperationDescriptors().stream().map(o -> o.getOperationSignature())
					.collect(Collectors.toList());
		}

		@Override
		public CallSOAPWebService build(ExecutionContext context, ExecutionInspector executionInspector)
				throws Exception {
			Solution solutionInstance = context.getSession().getSolutionInstance();
			WSDL wsdl = getWSDL(solutionInstance);
			Class<?> serviceClass = retrieveServiceClientDescriptor(solutionInstance).retrieveClass();
			Class<?> portInterface = retrievePortDescriptor(solutionInstance).retrieveInterface();
			Method operationMethod = retrieveOperationDescriptor(solutionInstance).retrieveMethod();
			OperationInput operationInput = (OperationInput) operationInputBuilder.build(new InstantiationContext(
					context.getVariables(), context.getPlan()
							.getValidationContext(context.getCurrentStep(), solutionInstance).getVariableDeclarations(),
					solutionInstance));
			Class<?> operationOutputClass = retrieveOperationDescriptor(solutionInstance).getOperationOutputClass();
			return new CallSOAPWebService(wsdl, serviceClass, portInterface, operationMethod, operationInput,
					customServiceEndpointURLVariant.getValue(solutionInstance), operationOutputClass);
		}

		@Override
		public Class<?> getOperationResultClass(Solution solutionInstance, Plan currentPlan, Step currentStep) {
			WSDL.OperationDescriptor operation = retrieveOperationDescriptor(solutionInstance);
			if (operation == null) {
				return null;
			}
			return operation.getOperationOutputClass();
		}

		private WSDL.ServiceClientDescriptor retrieveServiceClientDescriptor(Solution solutionInstance) {
			WSDL wsdl = getWSDL(solutionInstance);
			if (wsdl == null) {
				return null;
			}
			return wsdl.getServiceClientDescriptor(serviceName, solutionInstance);
		}

		private WSDL.PortDescriptor retrievePortDescriptor(Solution solutionInstance) {
			WSDL.ServiceClientDescriptor service = retrieveServiceClientDescriptor(solutionInstance);
			if (service == null) {
				return null;
			}
			return service.getPortDescriptor(portName);
		}

		private WSDL.OperationDescriptor retrieveOperationDescriptor(Solution solutionInstance) {
			WSDL.PortDescriptor port = retrievePortDescriptor(solutionInstance);
			if (port == null) {
				return null;
			}
			return port.getOperationDescriptor(operationSignature);
		}

		@Override
		public void validate(boolean recursively, Solution solutionInstance, Plan plan, Step step)
				throws ValidationError {
			if (getWSDL(solutionInstance) == null) {
				throw new ValidationError("Failed to resolve the WSDL reference");
			}
			if (retrieveServiceClientDescriptor(solutionInstance) == null) {
				throw new ValidationError("Invalid service name '" + serviceName + "'");
			}
			if (retrievePortDescriptor(solutionInstance) == null) {
				throw new ValidationError("Invalid port name '" + portName + "'");
			}
			if (retrieveOperationDescriptor(solutionInstance) == null) {
				throw new ValidationError("Invalid operation signature '" + operationSignature + "'");
			}
			String customServiceEndpointURL = customServiceEndpointURLVariant.getValue(solutionInstance);
			if (customServiceEndpointURL != null) {
				try {
					new URL(customServiceEndpointURL);
				} catch (MalformedURLException e) {
					throw new ValidationError("Invalid custom endpoint URL: '" + customServiceEndpointURL + "'", e);
				}
			}
			if (recursively) {
				operationInputBuilder.getFacade(solutionInstance).validate(recursively,
						plan.getValidationContext(step, solutionInstance).getVariableDeclarations());
			}
		}

		private class OperationInputClassNameAccessor extends Accessor<Solution, String> {
			@Override
			public String get(Solution solutionInstance) {
				WSDL.OperationDescriptor operation = retrieveOperationDescriptor(solutionInstance);
				if (operation == null) {
					return null;
				}
				return operation.getOperationInputClass().getName();
			}
		}

	}

}
