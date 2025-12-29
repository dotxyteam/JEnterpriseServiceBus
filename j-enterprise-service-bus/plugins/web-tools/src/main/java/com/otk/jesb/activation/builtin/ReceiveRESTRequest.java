package com.otk.jesb.activation.builtin;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Priority;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.openapi.OpenApiCustomizer;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;
import org.apache.cxf.jaxrs.swagger.ui.SwaggerUiService;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.otk.jesb.Log;
import com.otk.jesb.PotentialError;
import com.otk.jesb.Reference;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.Variant;
import com.otk.jesb.activation.ActivationHandler;
import com.otk.jesb.activation.Activator;
import com.otk.jesb.activation.ActivatorMetadata;
import com.otk.jesb.resource.builtin.HTTPServer;
import com.otk.jesb.resource.builtin.HTTPServer.RequestHandler;
import com.otk.jesb.resource.builtin.OpenAPIDescription;
import com.otk.jesb.resource.builtin.OpenAPIDescription.APIOperationDescriptor;
import com.otk.jesb.resource.builtin.OpenAPIDescription.APIOperationDescriptor.OperationInput;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Plan.ExecutionError;
import com.otk.jesb.ui.GUI.JESBSubCustomizedUI;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.util.MiscUtils;

import io.swagger.v3.oas.models.OpenAPI;
import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.info.ValidationSession;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.factory.IInfoProxyFactory;
import xy.reflect.ui.info.type.factory.InfoProxyFactory;
import xy.reflect.ui.util.ReflectionUIUtils;

public class ReceiveRESTRequest extends HTTPRequestReceiver {

	private Reference<OpenAPIDescription> openAPIDescriptionReference;
	private String operationSignature;

	private ActivationHandler activationHandler;

	private OpenAPIDescription expectOpenAPIDescription(Solution solutionInstance) {
		OpenAPIDescription result = openAPIDescriptionReference.resolve(solutionInstance);
		if (result == null) {
			throw new IllegalStateException("Failed to resolve the OpenAPI Description reference");
		}
		return result;
	}

	public ReceiveRESTRequest() {
		super();
		openAPIDescriptionReference = new Reference<OpenAPIDescription>(OpenAPIDescription.class);
	}

	public Reference<OpenAPIDescription> getOpenAPIDescriptionReference() {
		return openAPIDescriptionReference;
	}

	public void setOpenAPIDescriptionReference(Reference<OpenAPIDescription> openAPIDescriptionReference) {
		this.openAPIDescriptionReference = openAPIDescriptionReference;
	}

	public void setOpenAPIDescriptionReferenceAndAutoConfigure(
			Reference<OpenAPIDescription> openAPIDescriptionReference, Solution solutionInstance) {
		setOpenAPIDescriptionReference(openAPIDescriptionReference);
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

	private void tryToSelectValuesAutomatically(Solution solutionInstance) {
		try {
			if (operationSignature == null) {
				List<String> options = getOperationSignatureOptions(solutionInstance);
				if (options.size() > 0) {
					operationSignature = options.get(0);
				}
			}
		} catch (Throwable t) {
		}
	}

	@Override
	protected RESTRequestHandler createRequestHandler(String servicePath, Solution solutionInstance) {
		expectOpenAPIDescription(solutionInstance);
		return new RESTRequestHandler(servicePath, openAPIDescriptionReference);
	}

	@Override
	protected boolean isCompatibleWith(RequestHandler requestHandler, Solution solutionInstance) {
		if (!(requestHandler instanceof RESTRequestHandler)) {
			return false;
		}
		OpenAPIDescription openAPIDescription = openAPIDescriptionReference.resolve(solutionInstance);
		if (openAPIDescription == null) {
			return false;
		}
		if (((RESTRequestHandler) requestHandler).getOpenAPIDescriptionReference()
				.resolve(solutionInstance) != openAPIDescription) {
			return false;
		}
		return true;
	}

	public List<String> getOperationSignatureOptions(Solution solutionInstance) {
		try {
			OpenAPIDescription openAPIDescription = expectOpenAPIDescription(solutionInstance);
			return openAPIDescription.getServiceOperationDescriptors(solutionInstance).stream()
					.map(o -> o.getOperationSignature()).collect(Collectors.toList());
		} catch (IllegalStateException e) {
			return Collections.emptyList();
		}
	}

	private OpenAPIDescription.APIOperationDescriptor expectOperationDescriptor(Solution solutionInstance) {
		OpenAPIDescription openAPIDescription = expectOpenAPIDescription(solutionInstance);
		APIOperationDescriptor result = openAPIDescription.getServiceOperationDescriptor(operationSignature,
				solutionInstance);
		if (result == null) {
			throw new IllegalStateException(
					"Failed to get the operation descriptor: Invalid operation signature '" + operationSignature + "'");
		}
		return result;
	}

	@Override
	public Class<?> getInputClass(Solution solutionInstance) {
		try {
			OpenAPIDescription.APIOperationDescriptor operation = expectOperationDescriptor(solutionInstance);
			return operation.getOperationInputClass();
		} catch (IllegalStateException e) {
			return null;
		}
	}

	@Override
	public Class<?> getOutputClass(Solution solutionInstance) {
		try {
			OpenAPIDescription.APIOperationDescriptor operation = expectOperationDescriptor(solutionInstance);
			return operation.getOperationOutputClass();
		} catch (IllegalStateException e) {
			return null;
		}
	}

	@Override
	public void initializeAutomaticTrigger(ActivationHandler activationHandler, Solution solutionInstance)
			throws Exception {
		this.activationHandler = activationHandler;
		HTTPServer server = expectServer(solutionInstance);
		OpenAPIDescription openAPIDescription = expectOpenAPIDescription(solutionInstance);
		OpenAPIDescription.APIOperationDescriptor operation = expectOperationDescriptor(solutionInstance);
		String servicePath = getServicePath(solutionInstance);
		RequestHandler requestHandler = server.expectRequestHandler(servicePath, solutionInstance);
		if (!(requestHandler instanceof RESTRequestHandler)) {
			throw new IllegalStateException(
					"Cannot register " + operation + " on " + requestHandler + ": REST request handler required");
		}
		if (((RESTRequestHandler) requestHandler).getOpenAPIDescriptionReference()
				.resolve(solutionInstance) != openAPIDescription) {
			throw new IllegalStateException("Cannot register " + operation + " on " + requestHandler
					+ ": REST request handler based on OpenAPI description '" + openAPIDescriptionReference.getPath()
					+ "' required");
		}
		if (((RESTRequestHandler) requestHandler).getActivationHandlerByOperation().get(operation) != null) {
			throw new PotentialError(
					"Cannot configure " + operation + " of " + requestHandler + ": Operation already configured");
		}
		if (((RESTRequestHandler) requestHandler).getActivationHandlerByOperation().isEmpty()) {
			requestHandler.activate(server, solutionInstance);
		}
		((RESTRequestHandler) requestHandler).getActivationHandlerByOperation().put(operation, activationHandler);
	}

	@Override
	public void finalizeAutomaticTrigger(Solution solutionInstance) throws Exception {
		MiscUtils.willRethrowCommonly((compositeException) -> {
			HTTPServer server = compositeException.tryReturnCactch(() -> expectServer(solutionInstance));
			String servicePath = getServicePath(solutionInstance);
			RequestHandler requestHandler = (server == null) ? null
					: compositeException
							.tryReturnCactch(() -> server.expectRequestHandler(servicePath, solutionInstance));
			if (requestHandler != null) {
				OpenAPIDescription.APIOperationDescriptor operation = compositeException
						.tryReturnCactch(() -> expectOperationDescriptor(solutionInstance));
				if (operation != null) {
					((RESTRequestHandler) requestHandler).getActivationHandlerByOperation().remove(operation);
					if (((RESTRequestHandler) requestHandler).getActivationHandlerByOperation().isEmpty()) {
						compositeException.tryCactch(() -> {
							requestHandler.deactivate(server, solutionInstance);
						});
					}
				}
			}
			this.activationHandler = null;
		});
	}

	@Override
	public boolean isAutomaticTriggerReady() {
		return activationHandler != null;
	}

	@Override
	public void validate(boolean recursively, Solution solutionInstance, Plan plan) throws ValidationError {
		super.validate(recursively, solutionInstance, plan);
		try {
			expectOperationDescriptor(solutionInstance);
		} catch (IllegalStateException e) {
			throw new ValidationError(e.getMessage(), e);
		}
	}

	public static IInfoProxyFactory getUICustomizationsFactory(JESBSubCustomizedUI customizedUI) {
		return new InfoProxyFactory() {

			@Override
			public String getIdentifier() {
				return "MethodBasedSubInfoCustomizationsFactory [of=" + ReceiveRESTRequest.class.getName() + "]";
			}

			@Override
			protected void validate(ITypeInfo type, Object object, ValidationSession session) throws Exception {
				if (type.getName().equals(RESTRequestHandler.class.getName())) {
					Solution solutionInstance = ReflectionUIUtils.findRenderingContextualValue(customizedUI,
							Solution.class);
					if (solutionInstance == null) {
						throw new UnexpectedError();
					}
					HTTPServer server = ReflectionUIUtils.findRenderingContextualValue(customizedUI, HTTPServer.class);
					if (server == null) {
						HTTPRequestReceiver httpRequestReceiver = ReflectionUIUtils
								.findRenderingContextualValue(customizedUI, HTTPRequestReceiver.class);
						if (httpRequestReceiver == null) {
							throw new UnexpectedError();
						}
						server = httpRequestReceiver.getServerReference().resolve(solutionInstance);
						if (server == null) {
							throw new UnexpectedError();
						}
					}
					((HTTPServer.RequestHandler) object).validate(server, solutionInstance);
				} else {
					super.validate(type, object, session);
				}
			}

		};
	}

	public static class RESTRequestHandler extends RequestHandler {

		private Reference<OpenAPIDescription> openAPIDescriptionReference = new Reference<OpenAPIDescription>(
				OpenAPIDescription.class);
		private Variant<Boolean> webUIEnabledVariant = new Variant<Boolean>(Boolean.class, true);
		private WebUISupport webUISupport = new WebUISupport();

		private Map<OpenAPIDescription.APIOperationDescriptor, ActivationHandler> activationHandlerByOperation = new ConcurrentHashMap<OpenAPIDescription.APIOperationDescriptor, ActivationHandler>();
		private Server endpoint;

		public RESTRequestHandler() {
			super(null);
		}

		public RESTRequestHandler(String servicePath, Reference<OpenAPIDescription> openAPIDescriptionReference) {
			super(servicePath);
			this.openAPIDescriptionReference = openAPIDescriptionReference;
		}

		public Variant<Boolean> getWebUIEnabledVariant() {
			return webUIEnabledVariant;
		}

		public void setWebUIEnabledVariant(Variant<Boolean> webUIEnabledVariant) {
			this.webUIEnabledVariant = webUIEnabledVariant;
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

		public void setOpenAPIDescriptionReference(Reference<OpenAPIDescription> openAPIDescriptionReference) {
			this.openAPIDescriptionReference = openAPIDescriptionReference;
		}

		protected Map<OpenAPIDescription.APIOperationDescriptor, ActivationHandler> getActivationHandlerByOperation() {
			return activationHandlerByOperation;
		}

		private OpenAPIDescription expectOpenAPIDescription(Solution solutionInstance) {
			OpenAPIDescription result = openAPIDescriptionReference.resolve(solutionInstance);
			if (result == null) {
				throw new IllegalStateException("Failed to resolve the OpenAPI Description reference");
			}
			return result;
		}

		@Override
		protected void install(HTTPServer server, Solution solutionInstance) throws Exception {
			if (endpoint != null) {
				throw new UnexpectedError();
			}
			OpenAPIDescription openAPIDescription = expectOpenAPIDescription(solutionInstance);
			JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
			String servicePath = getServicePathVariant().getValue(solutionInstance);
			factory.setAddress(servicePath);
			factory.setServiceBeans(Arrays.asList(openAPIDescription.getAPIServiceImplementationClass(solutionInstance)
					.getConstructor(InvocationHandler.class).newInstance(new InvocationHandler() {
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							APIOperationDescriptor operation = new OpenAPIDescription.APIOperationDescriptor(method,
									solutionInstance);
							ActivationHandler registeredActivationHandler = getActivationHandlerByOperation()
									.get(operation);
							if (registeredActivationHandler == null) {
								throw new UnsupportedOperationException(operation + " not implemented");
							}
							OperationInput operationInput = (OperationInput) operation.getOperationInputClass()
									.getConstructor(method.getParameterTypes()).newInstance(args);
							Object operationOutput;
							try {
								operationOutput = registeredActivationHandler.trigger(operationInput);
							} catch (ExecutionError e) {
								if (e.getCause() instanceof OpenAPIDescription.ResponseException) {
									throw ((OpenAPIDescription.ResponseException) e.getCause())
											.toWebApplicationException();
								} else {
									throw e;
								}
							}
							Class<?> operationOutputClass = operation.getOperationOutputClass();
							if (operationOutputClass == null) {
								return null;
							}
							return operationOutputClass.getFields()[0].get(operationOutput);
						}
					}), openAPIDescription.getSwaggerInitializerResourceClass(solutionInstance).newInstance()));
			factory.setProvider(new JacksonJsonProvider());
			if (webUIEnabledVariant.getValue(solutionInstance)) {
				OpenApiFeature openApiFeature = new CustomOpenApiFeature();
				openApiFeature.setSupportSwaggerUi(true);
				openApiFeature.setUseContextBasedConfig(true);
				openApiFeature.setPrettyPrint(true);
				openApiFeature.setTitle(webUISupport.getTitleVariant().getValue(solutionInstance));
				openApiFeature.setContactName(webUISupport.getContactNameVariant().getValue(solutionInstance));
				openApiFeature.setContactEmail(webUISupport.getContactEmailVariant().getValue(solutionInstance));
				openApiFeature.setContactUrl(webUISupport.getContactUrlVariant().getValue(solutionInstance));
				openApiFeature.setDescription(webUISupport.getDescriptionVariant().getValue(solutionInstance));
				openApiFeature.setVersion(webUISupport.getVersionVariant().getValue(solutionInstance));
				openApiFeature.setLicense(webUISupport.getLicenseVariant().getValue(solutionInstance));
				openApiFeature
						.setTermsOfServiceUrl(webUISupport.getTermsOfServiceUrlVariant().getValue(solutionInstance));
				openApiFeature.setScan(false);
				openApiFeature.setResourceClasses(new HashSet<String>(
						Arrays.asList(openAPIDescription.getAPIServiceInterface(solutionInstance).getName())));
				OpenApiCustomizer openApiCustomizer = new OpenApiCustomizer() {
					@Override
					public void customize(OpenAPI openApi) {
						io.swagger.v3.oas.models.servers.Server server = new io.swagger.v3.oas.models.servers.Server();
						server.setUrl(servicePath);
						openApi.setServers(Collections.singletonList(server));
					}
				};
				{
					openApiFeature.setCustomizer(openApiCustomizer);
				}
				factory.setFeatures(Arrays.asList(openApiFeature));
			}
			endpoint = factory.create();
			Log.get()
					.information("Published REST service at: " + server.getLocaBaseURL(solutionInstance) + servicePath);
			if (webUIEnabledVariant.getValue(solutionInstance)) {
				Log.get().information("OpenAPI Description: " + server.getLocaBaseURL(solutionInstance) + servicePath
						+ "openapi.json");
				Log.get().information("Web UI: " + server.getLocaBaseURL(solutionInstance) + servicePath + "api-docs");
			}
		}

		@Override
		protected void uninstall(HTTPServer server, Solution solutionInstance) throws Exception {
			if (endpoint == null) {
				throw new UnexpectedError();
			}
			MiscUtils.willRethrowCommonly((compositeException) -> {
				compositeException.tryCactch(() -> {
					endpoint.destroy();
				});
				endpoint = null;
			});
			String servicePath = getServicePathVariant().getValue(solutionInstance);
			Log.get().information(
					"Unublished SOAP service: " + server.getLocaBaseURL(solutionInstance) + servicePath + "?WSDL");
		}

		@Override
		public void validate(HTTPServer server, Solution solutionInstance) throws ValidationError {
			super.validate(server, solutionInstance);
			try {
				expectOpenAPIDescription(solutionInstance);
			} catch (IllegalStateException e) {
				throw new ValidationError(e.getMessage(), e);
			}
			webUIEnabledVariant.validate(solutionInstance);
			webUISupport.validate(solutionInstance);
		}

		@Override
		public String toString() {
			return "RESTRequestHandler [openAPIDescription=" + openAPIDescriptionReference.getPath() + ", servicePath="
					+ getServicePathVariant() + "]";
		}

		public static class WebUISupport {
			private Variant<String> titleVariant = new Variant<String>(String.class);
			private Variant<String> contactNameVariant = new Variant<String>(String.class);
			private Variant<String> contactEmailVariant = new Variant<String>(String.class);
			private Variant<String> contactUrlVariant = new Variant<String>(String.class);
			private Variant<String> descriptionVariant = new Variant<String>(String.class);
			private Variant<String> versionVariant = new Variant<String>(String.class);
			private Variant<String> licenseVariant = new Variant<String>(String.class);
			private Variant<String> termsOfServiceUrlVariant = new Variant<String>(String.class);

			public Variant<String> getTitleVariant() {
				return titleVariant;
			}

			public void setTitleVariant(Variant<String> titleVariant) {
				this.titleVariant = titleVariant;
			}

			public Variant<String> getContactNameVariant() {
				return contactNameVariant;
			}

			public void setContactNameVariant(Variant<String> contactNameVariant) {
				this.contactNameVariant = contactNameVariant;
			}

			public Variant<String> getContactEmailVariant() {
				return contactEmailVariant;
			}

			public void setContactEmailVariant(Variant<String> contactEmailVariant) {
				this.contactEmailVariant = contactEmailVariant;
			}

			public Variant<String> getContactUrlVariant() {
				return contactUrlVariant;
			}

			public void setContactUrlVariant(Variant<String> contactUrlVariant) {
				this.contactUrlVariant = contactUrlVariant;
			}

			public Variant<String> getDescriptionVariant() {
				return descriptionVariant;
			}

			public void setDescriptionVariant(Variant<String> descriptionVariant) {
				this.descriptionVariant = descriptionVariant;
			}

			public Variant<String> getVersionVariant() {
				return versionVariant;
			}

			public void setVersionVariant(Variant<String> versionVariant) {
				this.versionVariant = versionVariant;
			}

			public Variant<String> getLicenseVariant() {
				return licenseVariant;
			}

			public void setLicenseVariant(Variant<String> licenseVariant) {
				this.licenseVariant = licenseVariant;
			}

			public Variant<String> getTermsOfServiceUrlVariant() {
				return termsOfServiceUrlVariant;
			}

			public void setTermsOfServiceUrlVariant(Variant<String> termsOfServiceUrlVariant) {
				this.termsOfServiceUrlVariant = termsOfServiceUrlVariant;
			}

			public void validate(Solution solutionInstance) throws ValidationError {
				titleVariant.validate(solutionInstance);
				contactNameVariant.validate(solutionInstance);
				contactEmailVariant.validate(solutionInstance);
				contactUrlVariant.validate(solutionInstance);
				descriptionVariant.validate(solutionInstance);
				versionVariant.validate(solutionInstance);
				licenseVariant.validate(solutionInstance);
				termsOfServiceUrlVariant.validate(solutionInstance);
			}
		}

		protected static class CustomOpenApiFeature extends OpenApiFeature {

			public CustomOpenApiFeature() {
				setDelegate(new Portable() {

					@Override
					public Registration getSwaggerUi(Bus bus, Properties swaggerProps, boolean runAsFilter) {
						final Registration registration = new Registration();

						if (checkSupportSwaggerUiProp(swaggerProps)) {
							String swaggerUiRoot = findSwaggerUiRoot();

							if (swaggerUiRoot != null) {
								final SwaggerUiResourceLocator locator = new SwaggerUiResourceLocator(swaggerUiRoot);
								SwaggerUiService swaggerUiService = new SwaggerUiService(locator,
										getSwaggerUiMediaTypes());
								swaggerUiService.setConfig(getSwaggerUiConfig());

								if (!runAsFilter) {
									registration.getResources().add(swaggerUiService);
								} else {
									registration.getProviders().add(new SwaggerUiServiceFilter(swaggerUiService));
								}

								registration.getProviders().add(new SwaggerUiResourceFilter(locator));
								bus.setProperty("swagger.service.ui.available", "true");
							}
						}

						return registration;
					}

				});
			}

			@PreMatching
			@Priority(Priorities.USER + 1)
			class SwaggerUiServiceFilter implements ContainerRequestFilter {
				private final SwaggerUiService uiService;

				SwaggerUiServiceFilter(SwaggerUiService uiService) {
					this.uiService = uiService;
				}

				@Override
				public void filter(ContainerRequestContext rc) throws IOException {
					if (HttpMethod.GET.equals(rc.getRequest().getMethod())) {
						UriInfo ui = rc.getUriInfo();
						String path = ui.getPath();
						int uiPathIndex = path.lastIndexOf("api-docs");
						if (uiPathIndex >= 0) {
							String resourcePath = uiPathIndex + 8 < path.length() ? path.substring(uiPathIndex + 8)
									: "";
							rc.abortWith(uiService.getResource(ui, resourcePath));
						}
					}
				}
			}

			@PreMatching
			@Priority(Priorities.USER)
			class SwaggerUiResourceFilter implements ContainerRequestFilter {
				private final Pattern PATTERN = Pattern
						.compile(".*[.]js|.*[.]gz|.*[.]map|oauth2*[.]html|.*[.]png|.*[.]css|.*[.]ico|"
								+ "/css/.*|/images/.*|/lib/.*|/fonts/.*");

				private final SwaggerUiResourceLocator locator;

				SwaggerUiResourceFilter(SwaggerUiResourceLocator locator) {
					this.locator = locator;
				}

				@Override
				public void filter(ContainerRequestContext rc) throws IOException {
					if (HttpMethod.GET.equals(rc.getRequest().getMethod())) {
						UriInfo ui = rc.getUriInfo();
						String path = "/" + ui.getPath();
						if (PATTERN.matcher(path).matches() && locator.exists(path)) {
							rc.setRequestUri(URI.create("api-docs" + path));
						}
					}
				}
			}

			class SwaggerUiResourceLocator extends org.apache.cxf.jaxrs.swagger.ui.SwaggerUiResourceLocator {
				private String swaggerUiRoot;

				public SwaggerUiResourceLocator(String swaggerUiRoot) {
					super(swaggerUiRoot);
					this.swaggerUiRoot = swaggerUiRoot;
				}

				/**
				 * Locate Swagger UI resource corresponding to resource path
				 * 
				 * @param resourcePath resource path
				 * @return Swagger UI resource URL
				 * @throws MalformedURLException
				 */
				public URL locate(String resourcePath) throws MalformedURLException {
					if ("/swagger-initializer.js".equals(resourcePath)) {
						throw new MalformedURLException();
					}
					if (StringUtils.isEmpty(resourcePath) || "/".equals(resourcePath)) {
						resourcePath = "index.html";
					}

					if (resourcePath.startsWith("/")) {
						resourcePath = resourcePath.substring(1);
					}
					URL ret;

					try {
						ret = URI.create(swaggerUiRoot + resourcePath).toURL();
					} catch (IllegalArgumentException ex) {
						throw new MalformedURLException(ex.getMessage());
					}
					return ret;
				}

				/**
				 * Checks the existence of the Swagger UI resource corresponding to resource
				 * path
				 * 
				 * @param resourcePath resource path
				 * @return "true" if Swagger UI resource exists, "false" otherwise
				 */
				public boolean exists(String resourcePath) {
					try {
						// The connect() will try to locate the entry (jar file, classpath resource)
						// and fail with FileNotFoundException /IOException if there is none.
						locate(resourcePath).openConnection().connect();
						return true;
					} catch (IOException ex) {
						return false;
					}
				}
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
