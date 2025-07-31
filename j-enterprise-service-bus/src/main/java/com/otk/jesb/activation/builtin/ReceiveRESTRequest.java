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
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;
import org.apache.cxf.jaxrs.swagger.ui.SwaggerUiConfig;

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

public class ReceiveRESTRequest extends HTTPRequestReceiver {

	private Reference<OpenAPIDescription> openAPIDescriptionReference = new Reference<OpenAPIDescription>(
			OpenAPIDescription.class);
	private String operationSignature;

	private ActivationHandler activationHandler;

	private OpenAPIDescription expectOpenAPIDescription() {
		OpenAPIDescription result = openAPIDescriptionReference.resolve();
		if (result == null) {
			throw new IllegalStateException("Failed to resolve the OpenAPI Description reference");
		}
		return result;
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

	@Override
	protected RESTRequestHandler createRequestHandler(String servicePath) {
		expectOpenAPIDescription();
		return new RESTRequestHandler(servicePath, openAPIDescriptionReference);
	}

	@Override
	protected boolean isCompatibleWith(RequestHandler requestHandler) {
		if (!(requestHandler instanceof RESTRequestHandler)) {
			return false;
		}
		OpenAPIDescription openAPIDescription = openAPIDescriptionReference.resolve();
		if (openAPIDescription == null) {
			return false;
		}
		if (((RESTRequestHandler) requestHandler).getOpenAPIDescriptionReference().resolve() != openAPIDescription) {
			return false;
		}
		return true;
	}

	public List<String> getOperationSignatureOptions() {
		try {
			OpenAPIDescription openAPIDescription = expectOpenAPIDescription();
			return openAPIDescription.getServiceOperationDescriptors().stream().map(o -> o.getOperationSignature())
					.collect(Collectors.toList());
		} catch (IllegalStateException e) {
			return Collections.emptyList();
		}
	}

	private OpenAPIDescription.APIOperationDescriptor expectOperationDescriptor() {
		OpenAPIDescription openAPIDescription = expectOpenAPIDescription();
		APIOperationDescriptor result = openAPIDescription.getServiceOperationDescriptor(operationSignature);
		if (result == null) {
			throw new IllegalStateException(
					"Failed to get the operation descriptor: Invalid operation signature '" + operationSignature + "'");
		}
		return result;
	}

	@Override
	public Class<?> getInputClass() {
		try {
			OpenAPIDescription.APIOperationDescriptor operation = expectOperationDescriptor();
			return operation.getOperationInputClass();
		} catch (IllegalStateException e) {
			return null;
		}
	}

	@Override
	public Class<?> getOutputClass() {
		try {
			OpenAPIDescription.APIOperationDescriptor operation = expectOperationDescriptor();
			return operation.getOperationOutputClass();
		} catch (IllegalStateException e) {
			return null;
		}
	}

	@Override
	public void initializeAutomaticTrigger(ActivationHandler activationHandler) throws Exception {
		HTTPServer server = expectServer();
		OpenAPIDescription openAPIDescription = expectOpenAPIDescription();
		OpenAPIDescription.APIOperationDescriptor operation = expectOperationDescriptor();
		RequestHandler requestHandler = server.expectRequestHandler(getServicePath());
		if (!(requestHandler instanceof RESTRequestHandler)) {
			throw new IllegalStateException(
					"Cannot register " + operation + " on " + requestHandler + ": REST request handler required");
		}
		if (((RESTRequestHandler) requestHandler).getOpenAPIDescriptionReference().resolve() != openAPIDescription) {
			throw new IllegalStateException("Cannot register " + operation + " on " + requestHandler
					+ ": REST request handler based on OpenAPI description '" + openAPIDescriptionReference.getPath()
					+ "' required");
		}
		if (((RESTRequestHandler) requestHandler).getActivationHandlerByOperation().get(operation) != null) {
			throw new UnexpectedError(
					"Cannot configure " + operation + " of " + requestHandler + ": Operation already configured");
		}
		if (((RESTRequestHandler) requestHandler).getActivationHandlerByOperation().isEmpty()) {
			requestHandler.activate(server);
		}
		((RESTRequestHandler) requestHandler).getActivationHandlerByOperation().put(operation, activationHandler);
		this.activationHandler = activationHandler;
	}

	@Override
	public void finalizeAutomaticTrigger() throws Exception {
		HTTPServer server = expectServer();
		RequestHandler requestHandler = server.expectRequestHandler(getServicePath());
		OpenAPIDescription.APIOperationDescriptor operation = expectOperationDescriptor();
		((RESTRequestHandler) requestHandler).getActivationHandlerByOperation().remove(operation);
		if (((RESTRequestHandler) requestHandler).getActivationHandlerByOperation().isEmpty()) {
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

	public static class RESTRequestHandler extends RequestHandler {

		private Reference<OpenAPIDescription> openAPIDescriptionReference = new Reference<OpenAPIDescription>(
				OpenAPIDescription.class);
		private WebUISupport webUISupport;

		private Map<OpenAPIDescription.APIOperationDescriptor, ActivationHandler> activationHandlerByOperation = new ConcurrentHashMap<OpenAPIDescription.APIOperationDescriptor, ActivationHandler>();
		private Server endpoint;

		public RESTRequestHandler(String servicePath, Reference<OpenAPIDescription> openAPIDescriptionReference) {
			super(servicePath);
			this.openAPIDescriptionReference = openAPIDescriptionReference;
		}

		public WebUISupport getWebUISupport() {
			return webUISupport;
		}

		public void setWebUISupport(WebUISupport webUISupport) {
			this.webUISupport = webUISupport;
		}

		public Reference<OpenAPIDescription> getOpenAPIDescriptionReference() {
			return openAPIDescriptionReference;
		}

		public Map<OpenAPIDescription.APIOperationDescriptor, ActivationHandler> getActivationHandlerByOperation() {
			return activationHandlerByOperation;
		}

		private OpenAPIDescription expectOpenAPIDescription() {
			OpenAPIDescription result = openAPIDescriptionReference.resolve();
			if (result == null) {
				throw new IllegalStateException("Failed to resolve the OpenAPI Description reference");
			}
			return result;
		}

		@Override
		protected void install(HTTPServer server) throws Exception {
			if (endpoint != null) {
				throw new UnexpectedError();
			}
			OpenAPIDescription openAPIDescription = expectOpenAPIDescription();
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
								throw new UnsupportedOperationException(operation + " not implemented");
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
			if (webUISupport != null) {
				OpenApiFeature openApiFeature = new OpenApiFeature();
				openApiFeature.setSupportSwaggerUi(true);
				openApiFeature.setPrettyPrint(true);
				openApiFeature.setTitle(webUISupport.getTitle());
				openApiFeature.setContactName(webUISupport.getContactName());
				openApiFeature.setContactEmail(webUISupport.getContactEmail());
				openApiFeature.setContactUrl(webUISupport.getContactUrl());
				openApiFeature.setDescription(webUISupport.getDescription());
				openApiFeature.setVersion(webUISupport.getVersion());
				openApiFeature.setLicense(webUISupport.getLicense());
				openApiFeature.setTermsOfServiceUrl(webUISupport.getTermsOfServiceUrl());
				openApiFeature.setSwaggerUiConfig(new SwaggerUiConfig().url(webUISupport.getUrlSuffix()));
				factory.setFeatures(Arrays.asList(openApiFeature));
			}
			endpoint = factory.create();
			if (Preferences.INSTANCE.isLogVerbose()) {
				System.out.println("Published REST service at: " + server.getLocaBaseURL() + servicePath
						+ ((webUISupport != null)
								? (" (Web UI: " + server.getLocaBaseURL() + servicePath + webUISupport.getUrlSuffix()
										+ ")")
								: ""));
			}
		}

		@Override
		protected void uninstall(HTTPServer server) throws Exception {
			if (endpoint == null) {
				throw new UnexpectedError();
			}
			endpoint.destroy();
			endpoint = null;
			if (Preferences.INSTANCE.isLogVerbose()) {
				System.out.println("Unublished SOAP service: " + server.getLocaBaseURL() + "/" + servicePath + "?WSDL");
			}
		}

		@Override
		public void validate(HTTPServer server) throws ValidationError {
			super.validate(server);
			try {
				expectOpenAPIDescription();
			} catch (IllegalStateException e) {
				throw new ValidationError(e.getMessage(), e);
			}
		}

		@Override
		public String toString() {
			return "RESTRequestHandler [openAPIDescription=" + openAPIDescriptionReference.getPath() + ", servicePath="
					+ servicePath + "]";
		}

		public static class WebUISupport {
			private String title;
			private String contactName;
			private String contactEmail;
			private String contactUrl;
			private String description;
			private String version;
			private String license;
			private String termsOfServiceUrl;
			private String urlSuffix = "api-docs";

			public String getTitle() {
				return title;
			}

			public void setTitle(String title) {
				this.title = title;
			}

			public String getContactName() {
				return contactName;
			}

			public void setContactName(String contactName) {
				this.contactName = contactName;
			}

			public String getContactEmail() {
				return contactEmail;
			}

			public void setContactEmail(String contactEmail) {
				this.contactEmail = contactEmail;
			}

			public String getContactUrl() {
				return contactUrl;
			}

			public void setContactUrl(String contactUrl) {
				this.contactUrl = contactUrl;
			}

			public String getDescription() {
				return description;
			}

			public void setDescription(String description) {
				this.description = description;
			}

			public String getVersion() {
				return version;
			}

			public void setVersion(String version) {
				this.version = version;
			}

			public String getLicense() {
				return license;
			}

			public void setLicense(String license) {
				this.license = license;
			}

			public String getTermsOfServiceUrl() {
				return termsOfServiceUrl;
			}

			public void setTermsOfServiceUrl(String termsOfServiceUrl) {
				this.termsOfServiceUrl = termsOfServiceUrl;
			}

			public String getUrlSuffix() {
				return urlSuffix;
			}

			public void setUrlSuffix(String urlSuffix) {
				this.urlSuffix = urlSuffix;
			}

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
