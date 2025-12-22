package com.otk.jesb.operation.builtin;

import java.beans.Transient;
import java.util.Collections;
import java.util.List;

import com.otk.jesb.ValidationError;
import com.otk.jesb.instantiation.InstanceBuilderFacade;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.solution.Solution;

import xy.reflect.ui.info.ResourcePath;

public class Fail implements Operation {

	private Throwable exception;

	public Fail(Throwable exception) {
		this.exception = exception;
	}

	public Throwable getException() {
		return exception;
	}

	@Override
	public Object execute(Solution solutionInstance) throws Throwable {
		throw exception;
	}

	public static class Metadata implements OperationMetadata<Fail> {

		@Override
		public String getOperationTypeName() {
			return "Fail";
		}

		@Override
		public String getCategoryName() {
			return "General";
		}

		@Override
		public Class<? extends OperationBuilder<Fail>> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(
					ResourcePath.specifyClassPathResourceLocation(Fail.class.getName().replace(".", "/") + ".png"));
		}
	}

	public static class Builder implements OperationBuilder<Fail> {

		private String exceptionClassName = Exception.class.getName();
		private RootInstanceBuilder exceptionBuilder = new RootInstanceBuilder("ExceptionInput",
				new ExceptionClassNameAccessor());

		public String getExceptionClassName() {
			return exceptionClassName;
		}

		public void setExceptionClassName(String exceptionClassName) {
			this.exceptionClassName = exceptionClassName;
		}

		public RootInstanceBuilder getExceptionBuilder() {
			return exceptionBuilder;
		}

		public void setExceptionBuilder(RootInstanceBuilder exceptionBuilder) {
			this.exceptionBuilder = exceptionBuilder;
		}

		public List<String> getExceptionContructorSignatureOptions(Solution solutionInstance) {
			List<InstanceBuilderFacade> rootInstanceBuilderFacades = exceptionBuilder
					.getWrappedInstanceBuilderFacades(solutionInstance);
			if (rootInstanceBuilderFacades.isEmpty()) {
				return Collections.emptyList();
			}
			List<String> result = null;
			for (InstanceBuilderFacade facade : rootInstanceBuilderFacades) {
				try {
					if (result == null) {
						result = facade.getConstructorSignatureOptions();
					} else {
						if (!result.equals(facade.getConstructorSignatureOptions())) {
							throw new Throwable();
						}
					}
				} catch (Throwable e) {
					result = Collections.singletonList(null);
					break;
				}
			}
			return result;
		}

		@Transient
		public String getExceptionContructorSignature(Solution solutionInstance) {
			String result = null;
			List<InstanceBuilderFacade> rootInstanceBuilderFacades = exceptionBuilder
					.getWrappedInstanceBuilderFacades(solutionInstance);
			for (InstanceBuilderFacade facade : rootInstanceBuilderFacades) {
				if (result == null) {
					result = facade.getSelectedConstructorSignature();
				} else {
					if (!result.equals(facade.getSelectedConstructorSignature())) {
						result = null;
						break;
					}
				}
			}
			return result;
		}

		public void setExceptionContructorSignature(String constructorSignature, Solution solutionInstance) {
			if (constructorSignature == null) {
				return;
			}
			List<InstanceBuilderFacade> rootInstanceBuilderFacades = exceptionBuilder
					.getWrappedInstanceBuilderFacades(solutionInstance);
			for (InstanceBuilderFacade facade : rootInstanceBuilderFacades) {
				facade.setSelectedConstructorSignature(constructorSignature);
			}
		}

		@Override
		public Fail build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			Solution solutionInstance = context.getSession().getSolutionInstance();
			Throwable exception = (Throwable) exceptionBuilder.build(new InstantiationContext(
					context.getVariables(), context.getPlan()
							.getValidationContext(context.getCurrentStep(), solutionInstance).getVariableDeclarations(),
					solutionInstance));
			return new Fail(exception);
		}

		@Override
		public Class<?> getOperationResultClass(Solution solutionInstance, Plan currentPlan, Step currentStep) {
			return null;
		}

		@Override
		public void validate(boolean recursively, Solution solutionInstance, Plan plan, Step step)
				throws ValidationError {
			solutionInstance.getRuntime().getJESBClass(exceptionClassName);
			if (recursively) {
				if (exceptionBuilder != null) {
					try {
						exceptionBuilder.getFacade(solutionInstance).validate(recursively,
								plan.getValidationContext(step, solutionInstance).getVariableDeclarations());
					} catch (ValidationError e) {
						throw new ValidationError("Failed to validate the exception builder", e);
					}
				}
			}
		}

		public class ExceptionClassNameAccessor extends Accessor<Solution, String> {

			@Override
			public String get(Solution solutionInstance) {
				return exceptionClassName;
			}

		}
	}

}
