package com.otk.jesb.activation.builtin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.otk.jesb.Preferences;
import com.otk.jesb.Reference;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.activation.ActivationHandler;
import com.otk.jesb.activation.Activator;
import com.otk.jesb.activation.ActivatorMetadata;
import com.otk.jesb.resource.builtin.HTTPServer;
import com.otk.jesb.resource.builtin.HTTPServer.RequestHandler;
import com.otk.jesb.resource.builtin.OpenAPIDescription;
import com.otk.jesb.resource.builtin.OpenAPIDescription.APIOperationDescriptor;
import com.otk.jesb.resource.builtin.OpenAPIDescription.APIOperationDescriptor.OperationInput;
import com.otk.jesb.solution.Plan;
import xy.reflect.ui.info.ResourcePath;

public class ReceiveRESTRequest extends Activator {

	private Reference<HTTPServer> serverReference = new Reference<HTTPServer>(HTTPServer.class);
	private Reference<OpenAPIDescription> openAPIDescriptionReference = new Reference<OpenAPIDescription>(
			OpenAPIDescription.class);
	private String operationSignature;
	private String servicePath = "/";

	private ActivationHandler activationHandler;

	private HTTPServer getServer() {
		return serverReference.resolve();
	}

	private OpenAPIDescription getOpenAPIDescription() {
		return openAPIDescriptionReference.resolve();
	}

	public Reference<HTTPServer> getServerReference() {
		return serverReference;
	}

	public void setServerReference(Reference<HTTPServer> serverReference) {
		this.serverReference = serverReference;
	}

	public Reference<OpenAPIDescription> getOpenAPIDescriptionReference() {
		return openAPIDescriptionReference;
	}

	public void setOpenAPIDescriptionReference(Reference<OpenAPIDescription> openAPIDescriptionReference) {
		this.openAPIDescriptionReference = openAPIDescriptionReference;
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
			if (operationSignature == null) {
				List<String> options = getOperationSignatureOptions();
				if (options.size() > 0) {
					operationSignature = options.get(0);
				}
			}
		} catch (Throwable t) {
		}
	}

	public List<String> getOperationSignatureOptions() {
		OpenAPIDescription openAPIDescription = getOpenAPIDescription();
		if (openAPIDescription == null) {
			return Collections.emptyList();
		}
		return openAPIDescription.getServiceOperationDescriptors().stream().map(o -> o.getOperationSignature())
				.collect(Collectors.toList());
	}

	private OpenAPIDescription.APIOperationDescriptor retrieveOperationDescriptor() {
		OpenAPIDescription openAPIDescription = getOpenAPIDescription();
		if (openAPIDescription == null) {
			return null;
		}
		return openAPIDescription.getServiceOperationDescriptor(operationSignature);
	}

	@Override
	public Class<?> getInputClass() {
		OpenAPIDescription.APIOperationDescriptor operation = retrieveOperationDescriptor();
		if (operation == null) {
			return null;
		}
		return operation.getOperationInputClass();
	}

	@Override
	public Class<?> getOutputClass() {
		OpenAPIDescription.APIOperationDescriptor operation = retrieveOperationDescriptor();
		if (operation == null) {
			return null;
		}
		return operation.getOperationOutputClass();
	}

	@Override
	public void initializeAutomaticTrigger(ActivationHandler activationHandler) throws Exception {
		OpenAPIDescription openAPIDescription = getOpenAPIDescription();
		if (openAPIDescription == null) {
			throw new ValidationError("Failed to resolve the OpenAPI Description reference");
		}
		OpenAPIDescription.APIOperationDescriptor operation = retrieveOperationDescriptor();
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
				requestHandler = new RESTRequestHandler(openAPIDescription);
				server.addRequestHandler(servicePath, requestHandler);
			} else {
				if (!(requestHandler instanceof RESTRequestHandler)) {
					throw new UnexpectedError("Cannot register REST request handler on service path '" + servicePath
							+ "': " + requestHandler + " already registered on this path");
				}
				if (((RESTRequestHandler) requestHandler).getOpenAPIDescription() != openAPIDescription) {
					throw new UnexpectedError(
							"Cannot install '" + openAPIDescription.getAPIServiceInterface().getSimpleName()
									+ "' API on REST request handler registered on service path '" + servicePath
									+ "': Already installed on this path: " + ((RESTRequestHandler) requestHandler)
											.getOpenAPIDescription().getAPIServiceInterface().getSimpleName());
				}
				if (((RESTRequestHandler) requestHandler).getActivationHandlerByOperation().get(operation) != null) {
					throw new UnexpectedError(
							"Cannot configure " + operation + " of REST request handler registered on service path '"
									+ servicePath + "': Operation already configured");
				}
			}
			((RESTRequestHandler) requestHandler).getActivationHandlerByOperation().put(operation, activationHandler);

		}
		this.activationHandler = activationHandler;
	}

	@Override
	public void finalizeAutomaticTrigger() throws Exception {
		HTTPServer server = getServer();
		synchronized (server) {
			RequestHandler requestHandler = server.getRequestHandler(servicePath);
			OpenAPIDescription.APIOperationDescriptor operation = retrieveOperationDescriptor();
			((RESTRequestHandler) requestHandler).getActivationHandlerByOperation().remove(operation);
			if (((RESTRequestHandler) requestHandler).getActivationHandlerByOperation().isEmpty()) {
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
		if (getOpenAPIDescription() == null) {
			throw new ValidationError("Failed to resolve the OpenAPI Description reference");
		}
		if (retrieveOperationDescriptor() == null) {
			throw new ValidationError("Invalid operation signature '" + operationSignature + "'");
		}
	}

	public static class RESTRequestHandler implements RequestHandler {

		private OpenAPIDescription openAPIDescription;
		private Map<OpenAPIDescription.APIOperationDescriptor, ActivationHandler> activationHandlerByOperation = new ConcurrentHashMap<OpenAPIDescription.APIOperationDescriptor, ActivationHandler>();
		private Server endpoint;

		public RESTRequestHandler(OpenAPIDescription openAPIDescription) {
			super();
			this.openAPIDescription = openAPIDescription;
		}

		public OpenAPIDescription getOpenAPIDescription() {
			return openAPIDescription;
		}

		public Map<OpenAPIDescription.APIOperationDescriptor, ActivationHandler> getActivationHandlerByOperation() {
			return activationHandlerByOperation;
		}

		@Override
		public void install(HTTPServer server, String servicePath) throws Exception {
			if (endpoint != null) {
				throw new UnexpectedError();
			}
			JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
			factory.setAddress(servicePath);
			factory.setServiceBeans(Arrays.asList(openAPIDescription.getAPIServiceImplementationClass()
					.getConstructor(InvocationHandler.class).newInstance(new InvocationHandler() {
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							APIOperationDescriptor operation = new OpenAPIDescription.APIOperationDescriptor(method);
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
					})));
			factory.setProvider(new JacksonJsonProvider());
			endpoint = factory.create();
			if (Preferences.INSTANCE.isLogVerbose()) {
				System.out.println("Published REST service at: " + server.getLocaBaseURL() + servicePath);
			}
		}

		@Override
		public void uninstall(HTTPServer server, String servicePath) throws Exception {
			if (endpoint == null) {
				throw new UnexpectedError();
			}
			endpoint.destroy();
			;
			if (Preferences.INSTANCE.isLogVerbose()) {
				System.out.println("Unublished SOAP service: " + server.getLocaBaseURL() + "/" + servicePath + "?WSDL");
			}
		}

		@Override
		public String toString() {
			return "RESTRequestHandler [API=" + openAPIDescription.getAPIServiceInterface().getSimpleName() + "]";
		}

	}

	public static class Metadata implements ActivatorMetadata {

		@Override
		public ResourcePath getActivatorIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(ReceiveRESTRequest.class.getName().replace(".", "/") + ".png"));
		}

		@Override
		public Class<? extends Activator> getActivatorClass() {
			return ReceiveRESTRequest.class;
		}

		@Override
		public String getActivatorName() {
			return "Receive REST Request";
		}

	}
}
