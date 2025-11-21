package com.otk.jesb.operation.builtin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientResponseException;

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
import com.otk.jesb.resource.builtin.OpenAPIDescription;
import com.otk.jesb.resource.builtin.OpenAPIDescription.APIOperationDescriptor.OperationInput;
import com.otk.jesb.resource.builtin.OpenAPIDescription.ResponseException;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.util.Accessor;
import xy.reflect.ui.info.ResourcePath;

public class CallRESTAPI implements Operation {

	private OpenAPIDescription openAPIDescription;
	private Class<?> apiClientClass;
	private Method operationMethod;
	private OperationInput operationInput;
	private String customBaseURL;
	private Class<?> operationOutputClass;

	public CallRESTAPI(OpenAPIDescription openAPIDescription, Class<?> apiClientClass, Method operationMethod,
			OperationInput operationInput, String customBaseURL, Class<?> operationOutputClass) {
		this.openAPIDescription = openAPIDescription;
		this.apiClientClass = apiClientClass;
		this.operationMethod = operationMethod;
		this.operationInput = operationInput;
		this.customBaseURL = customBaseURL;
		this.operationOutputClass = operationOutputClass;
	}

	public OpenAPIDescription getOpenAPIDescription() {
		return openAPIDescription;
	}

	public Class<?> getApiClientClass() {
		return apiClientClass;
	}

	public Method getOperationMethod() {
		return operationMethod;
	}

	public OperationInput getOperationInput() {
		return operationInput;
	}

	public String getCustomBaseURL() {
		return customBaseURL;
	}

	public Class<?> getOperationOutputClass() {
		return operationOutputClass;
	}

	@Override
	public Object execute() throws Exception {
		try {
			Object apiClient = apiClientClass.newInstance();
			if (customBaseURL != null) {
				Class<?> configurationClass = openAPIDescription.getAPIClientConfigurationClass();
				Object apiClientConfiguration = configurationClass.newInstance();
				Method basePathSetter = configurationClass.getMethod("setBasePath", String.class);
				basePathSetter.invoke(apiClientConfiguration, customBaseURL);
				Method configurationSetter = apiClientClass.getMethod("setApiClient", configurationClass);
				configurationSetter.invoke(apiClient, apiClientConfiguration);
			}
			Object operationResult = operationMethod.invoke(apiClient,
					(operationInput == null) ? new Object[0] : operationInput.listParameterValues());
			if (operationOutputClass == null) {
				return null;
			}
			return operationOutputClass.getConstructor(operationMethod.getReturnType()).newInstance(operationResult);
		} catch (InvocationTargetException e) {
			if (e.getTargetException() instanceof Exception) {
				if (e.getTargetException() instanceof RestClientResponseException) {
					RestClientResponseException clientException = (RestClientResponseException) e.getTargetException();
					ResponseException responseException = new OpenAPIDescription.ResponseException(
							clientException.getRawStatusCode());
					responseException
							.setContentType(clientException.getResponseHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
					responseException.setBody(clientException.getResponseBodyAsString());
					throw responseException;
				} else {
					throw (Exception) e.getTargetException();
				}
			} else {
				throw new PotentialError(e.getTargetException());
			}
		}
	}

	public static class Metadata implements OperationMetadata<CallRESTAPI> {

		@Override
		public String getOperationTypeName() {
			return "Call REST API";
		}

		@Override
		public String getCategoryName() {
			return "REST";
		}

		@Override
		public Class<? extends OperationBuilder<CallRESTAPI>> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(CallRESTAPI.class.getName().replace(".", "/") + ".png"));
		}
	}

	public static class Builder implements OperationBuilder<CallRESTAPI> {

		private Reference<OpenAPIDescription> openAPIDescriptionReference = new Reference<OpenAPIDescription>(
				OpenAPIDescription.class);
		private RootInstanceBuilder operationInputBuilder = new RootInstanceBuilder(
				OperationInput.class.getSimpleName(), new OperationInputClassNameAccessor());
		private String operationSignature;
		private Variant<String> customBaseURLVariant = new Variant<String>(String.class);

		private OpenAPIDescription getOpenAPIDescription() {
			return openAPIDescriptionReference.resolve();
		}

		public Reference<OpenAPIDescription> getOpenAPIDescriptionReference() {
			return openAPIDescriptionReference;
		}

		public void setOpenAPIDescriptionReference(Reference<OpenAPIDescription> openAPIDescriptionReference) {
			this.openAPIDescriptionReference = openAPIDescriptionReference;
			tryToSelectValuesAutomatically();
		}

		public RootInstanceBuilder getOperationInputBuilder() {
			return operationInputBuilder;
		}

		public void setOperationInputBuilder(RootInstanceBuilder operationInputBuilder) {
			this.operationInputBuilder = operationInputBuilder;
		}

		public String getOperationSignature() {
			return operationSignature;
		}

		public void setOperationSignature(String operationSignature) {
			this.operationSignature = operationSignature;
		}

		public Variant<String> getCustomBaseURLVariant() {
			return customBaseURLVariant;
		}

		public void setCustomBaseURLVariant(Variant<String> customBaseURLVariant) {
			this.customBaseURLVariant = customBaseURLVariant;
		}

		private void tryToSelectValuesAutomatically() {
			OpenAPIDescription openAPIDescription = getOpenAPIDescription();
			if (openAPIDescription == null) {
				return;
			}
			try {
				if (operationSignature == null) {
					List<OpenAPIDescription.APIOperationDescriptor> operations = openAPIDescription
							.getClientOperationDescriptors();
					if (operations.size() > 0) {
						operationSignature = operations.get(0).getOperationSignature();
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
			return openAPIDescription.getClientOperationDescriptors().stream().map(o -> o.getOperationSignature())
					.collect(Collectors.toList());
		}

		@Override
		public CallRESTAPI build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			OpenAPIDescription openAPIDescription = getOpenAPIDescription();
			Class<?> apiClientClass = openAPIDescription.getAPIClientClass();
			Method operationMethod = retrieveOperationDescriptor().retrieveMethod();
			OperationInput operationInput = (OperationInput) operationInputBuilder.build(new InstantiationContext(
					context.getVariables(),
					context.getPlan().getValidationContext(context.getCurrentStep()).getVariableDeclarations()));
			Class<?> operationOutputClass = retrieveOperationDescriptor().getOperationOutputClass();
			return new CallRESTAPI(openAPIDescription, apiClientClass, operationMethod, operationInput,
					customBaseURLVariant.getValue(), operationOutputClass);
		}

		@Override
		public Class<?> getOperationResultClass(Plan currentPlan, Step currentStep) {
			OpenAPIDescription.APIOperationDescriptor operation = retrieveOperationDescriptor();
			if (operation == null) {
				return null;
			}
			return operation.getOperationOutputClass();
		}

		private OpenAPIDescription.APIOperationDescriptor retrieveOperationDescriptor() {
			OpenAPIDescription openAPIDescription = getOpenAPIDescription();
			if (openAPIDescription == null) {
				return null;
			}
			return openAPIDescription.getClientOperationDescriptor(operationSignature);
		}

		@Override
		public void validate(boolean recursively, Plan plan, Step step) throws ValidationError {
			if (getOpenAPIDescription() == null) {
				throw new ValidationError("Failed to resolve the OpenAPI Description reference");
			}
			if (retrieveOperationDescriptor() == null) {
				throw new ValidationError("Invalid operation signature '" + operationSignature + "'");
			}
			String customBaseURL = customBaseURLVariant.getValue();
			if (customBaseURL != null) {
				try {
					new URL(customBaseURL);
				} catch (MalformedURLException e) {
					throw new ValidationError("Invalid custom base URL: '" + customBaseURL + "'", e);
				}
			}
			if (recursively) {
				operationInputBuilder.getFacade().validate(recursively,
						plan.getValidationContext(step).getVariableDeclarations());
			}
		}

		private class OperationInputClassNameAccessor extends Accessor<String> {
			@Override
			public String get() {
				OpenAPIDescription.APIOperationDescriptor operation = retrieveOperationDescriptor();
				if (operation == null) {
					return null;
				}
				return operation.getOperationInputClass().getName();
			}
		}

	}

}
