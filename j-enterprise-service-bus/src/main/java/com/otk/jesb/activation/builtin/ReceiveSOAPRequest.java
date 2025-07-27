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

public class ReceiveSOAPRequest extends Activator {

	private Reference<HTTPServer> serverReference = new Reference<HTTPServer>(HTTPServer.class);
	private Reference<WSDL> wsdlReference = new Reference<WSDL>(WSDL.class);
	private String serviceName;
	private String operationSignature;
	private String servicePath = "/";

	private ActivationHandler activationHandler;

	private HTTPServer getServer() {
		return serverReference.resolve();
	}

	private WSDL getWSDL() {
		return wsdlReference.resolve();
	}

	public Reference<HTTPServer> getServerReference() {
		return serverReference;
	}

	public void setServerReference(Reference<HTTPServer> serverReference) {
		this.serverReference = serverReference;
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

	public String getServicePath() {
		return servicePath;
	}

	public void setServicePath(String servicePath) {
		this.servicePath = servicePath;
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
		WSDL wsdl = getWSDL();
		if (wsdl == null) {
			return Collections.emptyList();
		}
		return wsdl.getServiceSpecificationDescriptors().stream().map(s -> s.getServiceName())
				.collect(Collectors.toList());
	}

	public List<String> getOperationSignatureOptions() {
		WSDL.ServiceSpecificationDescriptor service = retrieveServiceSpecificationDescriptor();
		if (service == null) {
			return Collections.emptyList();
		}
		return service.getOperationDescriptors().stream().map(o -> o.getOperationSignature())
				.collect(Collectors.toList());
	}

	private WSDL.ServiceSpecificationDescriptor retrieveServiceSpecificationDescriptor() {
		WSDL wsdl = getWSDL();
		if (wsdl == null) {
			return null;
		}
		return wsdl.getServiceSpecificationDescriptor(serviceName);
	}

	private WSDL.OperationDescriptor retrieveOperationDescriptor() {
		WSDL.ServiceSpecificationDescriptor service = retrieveServiceSpecificationDescriptor();
		if (service == null) {
			return null;
		}
		return service.getOperationDescriptor(operationSignature);
	}

	@Override
	public Class<?> getInputClass() {
		WSDL.OperationDescriptor operation = retrieveOperationDescriptor();
		if (operation == null) {
			return null;
		}
		return operation.getOperationInputClass();
	}

	@Override
	public Class<?> getOutputClass() {
		WSDL.OperationDescriptor operation = retrieveOperationDescriptor();
		if (operation == null) {
			return null;
		}
		return operation.getOperationOutputClass();
	}

	@Override
	public void initializeAutomaticTrigger(ActivationHandler activationHandler) throws Exception {
		ServiceSpecificationDescriptor service = retrieveServiceSpecificationDescriptor();
		if (service == null) {
			throw new UnexpectedError("Failed to get the service descriptor");
		}
		WSDL.OperationDescriptor operation = retrieveOperationDescriptor();
		if (operation == null) {
			throw new UnexpectedError("Failed to get the operation descriptor");
		}
		HTTPServer server = getServer();
		if (server == null) {
			throw new UnexpectedError("Failed to resolve the server reference");
		}
		synchronized (server) {
			RequestHandler requestHandler = server.getRequestHandler(servicePath);
			if (requestHandler == null) {
				requestHandler = new SOAPRequestHandler(service);
				server.addRequestHandler(servicePath, requestHandler);
			} else {
				if (!(requestHandler instanceof SOAPRequestHandler)) {
					throw new UnexpectedError("Cannot register SOAP request handler on service path '" + servicePath
							+ "': " + requestHandler + " already registered on this path");
				}
				if (!((SOAPRequestHandler) requestHandler).getService().equals(service)) {
					throw new UnexpectedError(
							"Cannot install " + service + " on SOAP request handler registered on service path '"
									+ servicePath + "': Already installed on this path: "
									+ ((SOAPRequestHandler) requestHandler).getService());
				}
				if (((SOAPRequestHandler) requestHandler).getActivationHandlerByOperation().get(operation) != null) {
					throw new UnexpectedError(
							"Cannot configure " + operation + " of SOAP request handler registered on service path '"
									+ servicePath + "': Operation already configured");
				}
			}
			((SOAPRequestHandler) requestHandler).getActivationHandlerByOperation().put(operation, activationHandler);
		}
		this.activationHandler = activationHandler;
	}

	@Override
	public void finalizeAutomaticTrigger() throws Exception {
		HTTPServer server = getServer();
		synchronized (server) {
			RequestHandler requestHandler = server.getRequestHandler(servicePath);
			WSDL.OperationDescriptor operation = retrieveOperationDescriptor();
			((SOAPRequestHandler) requestHandler).getActivationHandlerByOperation().remove(operation);
			if (((SOAPRequestHandler) requestHandler).getActivationHandlerByOperation().isEmpty()) {
				server.removeRequestHandler(servicePath);
			}
		}
		this.activationHandler = null;
	}

	@Override
	public boolean isAutomaticTriggerReady() {
		return activationHandler != null;
	}

	@Override
	public boolean isAutomaticallyTriggerable() {
		return true;
	}

	@Override
	public void validate(boolean recursively, Plan plan) throws ValidationError {
		if (getServer() == null) {
			throw new ValidationError("Failed to resolve the HTTP server reference");
		}
		if (getWSDL() == null) {
			throw new ValidationError("Failed to resolve the WSDL reference");
		}
		if (retrieveServiceSpecificationDescriptor() == null) {
			throw new ValidationError("Invalid service name '" + serviceName + "'");
		}
		if (retrieveOperationDescriptor() == null) {
			throw new ValidationError("Invalid operation signature '" + operationSignature + "'");
		}
	}

	public static class SOAPRequestHandler implements RequestHandler {

		private ServiceSpecificationDescriptor service;
		private Map<WSDL.OperationDescriptor, ActivationHandler> activationHandlerByOperation = new ConcurrentHashMap<WSDL.OperationDescriptor, ActivationHandler>();
		private EndpointImpl endpoint;

		public SOAPRequestHandler(ServiceSpecificationDescriptor service) {
			this.service = service;
		}

		public ServiceSpecificationDescriptor getService() {
			return service;
		}

		public Map<WSDL.OperationDescriptor, ActivationHandler> getActivationHandlerByOperation() {
			return activationHandlerByOperation;
		}

		@Override
		public void install(HTTPServer server, String servicePath) throws Exception {
			if (endpoint != null) {
				throw new UnexpectedError();
			}
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
				System.out.println("Published SOAP service at: " + server.getLocaBaseURL() + servicePath + "?WSDL");
			}
		}

		@Override
		public void uninstall(HTTPServer server, String servicePath) throws Exception {
			if (endpoint == null) {
				throw new UnexpectedError();
			}
			endpoint.stop();
			if (Preferences.INSTANCE.isLogVerbose()) {
				System.out.println("Unublished SOAP service: " + server.getLocaBaseURL() + "/" + servicePath + "?WSDL");
			}
		}

		@Override
		public String toString() {
			return "SOAPRequestHandler [service=" + service + "]";
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
