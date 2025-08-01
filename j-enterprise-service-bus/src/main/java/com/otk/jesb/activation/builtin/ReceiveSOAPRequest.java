package com.otk.jesb.activation.builtin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.cxf.jaxws.EndpointImpl;
import com.otk.jesb.Preferences;
import com.otk.jesb.Reference;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.activation.ActivationHandler;
import com.otk.jesb.activation.Activator;
import com.otk.jesb.activation.ActivatorMetadata;
import com.otk.jesb.resource.builtin.HTTPServer;
import com.otk.jesb.resource.builtin.HTTPServer.RequestHandler;
import com.otk.jesb.resource.builtin.WSDL;
import com.otk.jesb.resource.builtin.WSDL.OperationDescriptor;
import com.otk.jesb.resource.builtin.WSDL.OperationDescriptor.OperationInput;
import com.otk.jesb.resource.builtin.WSDL.ServiceSpecificationDescriptor;
import com.otk.jesb.solution.Plan;
import xy.reflect.ui.info.ResourcePath;

public class ReceiveSOAPRequest extends HTTPRequestReceiver {

	private Reference<WSDL> wsdlReference = new Reference<WSDL>(WSDL.class);
	private String serviceName;
	private String operationSignature;

	private ActivationHandler activationHandler;

	@Override
	protected SOAPRequestHandler createRequestHandler(String servicePath) {
		return new SOAPRequestHandler(servicePath, wsdlReference);
	}

	@Override
	protected boolean isCompatibleWith(RequestHandler requestHandler) {
		if (!(requestHandler instanceof SOAPRequestHandler)) {
			return false;
		}
		WSDL wsdl = wsdlReference.resolve();
		if (wsdl == null) {
			return false;
		}
		if (((SOAPRequestHandler) requestHandler).getWsdlReference().resolve() != wsdl) {
			return false;
		}
		return true;
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

	public String getOperationSignature() {
		return operationSignature;
	}

	public void setOperationSignature(String operationSignature) {
		this.operationSignature = operationSignature;
		tryToSelectValuesAutomatically();
	}

	private void tryToSelectValuesAutomatically() {
		try {
			if (serviceName == null) {
				List<String> options = getServiceNameOptions();
				if (options.size() > 0) {
					serviceName = options.get(0);
				}
			}
			if (operationSignature == null) {
				List<String> options = getOperationSignatureOptions();
				if (options.size() > 0) {
					operationSignature = options.get(0);
				}
			}
		} catch (Throwable t) {
		}
	}

	public List<String> getServiceNameOptions() {
		try {
			WSDL wsdl = expectWSDL();
			return wsdl.getServiceSpecificationDescriptors().stream().map(s -> s.getServiceName())
					.collect(Collectors.toList());
		} catch (IllegalStateException e) {
			return Collections.emptyList();
		}
	}

	public List<String> getOperationSignatureOptions() {
		try {
			WSDL.ServiceSpecificationDescriptor service = expectServiceSpecificationDescriptor();
			return service.getOperationDescriptors().stream().map(o -> o.getOperationSignature())
					.collect(Collectors.toList());
		} catch (IllegalStateException e) {
			return Collections.emptyList();
		}
	}

	private WSDL expectWSDL() {
		WSDL result = wsdlReference.resolve();
		if (result == null) {
			throw new IllegalStateException("Failed to resolve the WSDL reference");
		}
		return result;
	}

	private WSDL.ServiceSpecificationDescriptor expectServiceSpecificationDescriptor() {
		WSDL wsdl = expectWSDL();
		ServiceSpecificationDescriptor result = wsdl.getServiceSpecificationDescriptor(serviceName);
		if (result == null) {
			throw new IllegalStateException("Invalid service name '" + serviceName + "'");
		}
		return result;
	}

	private WSDL.OperationDescriptor expectOperationDescriptor() {
		WSDL.ServiceSpecificationDescriptor service = expectServiceSpecificationDescriptor();
		OperationDescriptor result = service.getOperationDescriptor(operationSignature);
		if (result == null) {
			throw new IllegalStateException("Invalid operation signature '" + operationSignature + "'");
		}
		return result;
	}

	@Override
	public Class<?> getInputClass() {
		WSDL.OperationDescriptor operation = expectOperationDescriptor();
		if (operation == null) {
			return null;
		}
		return operation.getOperationInputClass();
	}

	@Override
	public Class<?> getOutputClass() {
		WSDL.OperationDescriptor operation = expectOperationDescriptor();
		if (operation == null) {
			return null;
		}
		return operation.getOperationOutputClass();
	}

	@Override
	public void initializeAutomaticTrigger(ActivationHandler activationHandler) throws Exception {
		WSDL wsdl = expectWSDL();
		WSDL.OperationDescriptor operation = expectOperationDescriptor();
		HTTPServer server = expectServer();
		RequestHandler requestHandler = server.expectRequestHandler(getServicePath());
		if (!(requestHandler instanceof SOAPRequestHandler)) {
			throw new IllegalStateException(
					"Cannot register " + operation + " on " + requestHandler + ": SOAP request handler required");
		}
		if (((SOAPRequestHandler) requestHandler).getWsdlReference().resolve() != wsdl) {
			throw new IllegalStateException("Cannot register " + operation + " on " + requestHandler
					+ ": SOAP request handler based on WSDL '" + wsdlReference.getPath() + "' required");
		}
		if ((((SOAPRequestHandler) requestHandler).getWsdlReference().resolve() != wsdl)
				|| !serviceName.equals(((SOAPRequestHandler) requestHandler).getServiceName())) {
			throw new IllegalStateException("Cannot register " + operation + " on " + requestHandler
					+ ": SOAP request handler based on service '" + serviceName + "' of WSDL '"
					+ wsdlReference.getPath() + "' required");
		}
		if (((SOAPRequestHandler) requestHandler).getActivationHandlerByOperation().get(operation) != null) {
			throw new UnexpectedError(
					"Cannot configure " + operation + " of " + requestHandler + ": Operation already configured");
		}
		if (((SOAPRequestHandler) requestHandler).getActivationHandlerByOperation().isEmpty()) {
			requestHandler.activate(server);
		}
		((SOAPRequestHandler) requestHandler).getActivationHandlerByOperation().put(operation, activationHandler);
		this.activationHandler = activationHandler;
	}

	@Override
	public void finalizeAutomaticTrigger() throws Exception {
		HTTPServer server = expectServer();
		RequestHandler requestHandler = server.expectRequestHandler(getServicePath());
		WSDL.OperationDescriptor operation = expectOperationDescriptor();
		((SOAPRequestHandler) requestHandler).getActivationHandlerByOperation().remove(operation);
		if (((SOAPRequestHandler) requestHandler).getActivationHandlerByOperation().isEmpty()) {
			requestHandler.deactivate(server);
		}
		this.activationHandler = null;
	}

	@Override
	public boolean isAutomaticTriggerReady() {
		return activationHandler != null;
	}

	@Override
	public void validate(boolean recursively, Plan plan) throws ValidationError {
		super.validate(recursively, plan);
		try {
			expectOperationDescriptor();
		} catch (IllegalStateException e) {
			throw new ValidationError(e.getMessage(), e);
		}
	}

	public static class SOAPRequestHandler extends RequestHandler {

		private Reference<WSDL> wsdlReference = new Reference<WSDL>(WSDL.class);
		private String serviceName;

		private Map<WSDL.OperationDescriptor, ActivationHandler> activationHandlerByOperation = new ConcurrentHashMap<WSDL.OperationDescriptor, ActivationHandler>();
		private EndpointImpl endpoint;

		public SOAPRequestHandler(String servicePath, Reference<WSDL> wsdlReference) {
			super(servicePath);
			this.wsdlReference = wsdlReference;
			List<String> serviceNameOptions = getServiceNameOptions();
			if (serviceNameOptions.size() > 0) {
				serviceName = serviceNameOptions.get(0);
			}
		}

		public String getServiceName() {
			return serviceName;
		}

		public void setServiceName(String serviceName) {
			this.serviceName = serviceName;
		}

		public List<String> getServiceNameOptions() {
			WSDL wsdl = wsdlReference.resolve();
			if (wsdl == null) {
				return Collections.emptyList();
			}
			return wsdl.getServiceSpecificationDescriptors().stream().map(s -> s.getServiceName())
					.collect(Collectors.toList());
		}

		public Reference<WSDL> getWsdlReference() {
			return wsdlReference;
		}

		public Map<WSDL.OperationDescriptor, ActivationHandler> getActivationHandlerByOperation() {
			return activationHandlerByOperation;
		}

		private WSDL expectWSDL() {
			WSDL result = wsdlReference.resolve();
			if (result == null) {
				throw new IllegalStateException("Failed to resolve the WSDL reference");
			}
			return result;
		}

		private WSDL.ServiceSpecificationDescriptor expectServiceSpecificationDescriptor() {
			WSDL wsdl = expectWSDL();
			ServiceSpecificationDescriptor result = wsdl.getServiceSpecificationDescriptor(serviceName);
			if (result == null) {
				throw new IllegalStateException("Invalid service name '" + serviceName + "'");
			}
			return result;
		}

		@Override
		public void install(HTTPServer server) throws Exception {
			if (endpoint != null) {
				throw new UnexpectedError();
			}
			ServiceSpecificationDescriptor service = expectServiceSpecificationDescriptor();
			endpoint = new EndpointImpl(service.getImplementationClass().getConstructor(InvocationHandler.class)
					.newInstance(new InvocationHandler() {
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							OperationDescriptor operation = new WSDL.OperationDescriptor(method);
							ActivationHandler registeredActivationHandler = getActivationHandlerByOperation()
									.get(operation);
							if (registeredActivationHandler == null) {
								throw new UnsupportedOperationException();
							}
							OperationInput operationInput = (OperationInput) operation.getOperationInputClass()
									.getConstructor(method.getParameterTypes()).newInstance(args);
							Object operationOutput = registeredActivationHandler.trigger(operationInput);
							Class<?> operationOutputClass = operation.getOperationOutputClass();
							if (operationOutputClass == null) {
								return null;
							}
							return operationOutputClass.getFields()[0].get(operationOutput);
						}
					}));
			endpoint.publish(servicePath);
			if (Preferences.INSTANCE.isLogVerbose()) {
				System.out.println("Published SOAP service at: " + server.getLocaBaseURL() + servicePath);
				System.out.println("WSDL: " + server.getLocaBaseURL() + servicePath + "?wsdl");
			}
		}

		@Override
		public void uninstall(HTTPServer server) throws Exception {
			if (endpoint == null) {
				throw new UnexpectedError();
			}
			endpoint.stop();
			endpoint = null;
			if (Preferences.INSTANCE.isLogVerbose()) {
				System.out.println("Unublished SOAP service: " + server.getLocaBaseURL() + "/" + servicePath);
			}
		}

		@Override
		public void validate(HTTPServer server) throws ValidationError {
			super.validate(server);
			try {
				expectServiceSpecificationDescriptor();
			} catch (IllegalStateException e) {
				throw new ValidationError(e.getMessage(), e);
			}
		}

		@Override
		public String toString() {
			return "SOAPRequestHandler [wsdl=" + wsdlReference.getPath() + ", servicePath=" + servicePath + "]";
		}

	}

	public static class Metadata implements ActivatorMetadata {

		@Override
		public ResourcePath getActivatorIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(ReceiveSOAPRequest.class.getName().replace(".", "/") + ".png"));
		}

		@Override
		public Class<? extends Activator> getActivatorClass() {
			return ReceiveSOAPRequest.class;
		}

		@Override
		public String getActivatorName() {
			return "Receive SOAP Request";
		}

	}
}
