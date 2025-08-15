package com.otk.jesb.operation.builtin;

import com.otk.jesb.ValidationError;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;

import xy.reflect.ui.info.ResourcePath;

public class Fail implements Operation {

	private Exception exception;

	public Fail(Exception exception) {
		this.exception = exception;
	}

	public Exception getException() {
		return exception;
	}

	@Override
	public Object execute() throws Exception {
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

		private RootInstanceBuilder instanceBuilder = new RootInstanceBuilder(Fail.class.getSimpleName() + "Input",
				Fail.class.getName());

		public RootInstanceBuilder getInstanceBuilder() {
			return instanceBuilder;
		}

		public void setInstanceBuilder(RootInstanceBuilder instanceBuilder) {
			this.instanceBuilder = instanceBuilder;
		}

		@Override
		public Fail build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			Fail instance = (Fail) instanceBuilder.build(new InstantiationContext(context.getVariables(),
					context.getPlan().getValidationContext(context.getCurrentStep()).getVariableDeclarations()));
			return instance;
		}

		@Override
		public Class<?> getOperationResultClass(Plan currentPlan, Step currentStep) {
			return null;
		}

		@Override
		public void validate(boolean recursively, Plan plan, Step step) throws ValidationError {
			if (recursively) {
				if (instanceBuilder != null) {
					try {
						instanceBuilder.getFacade().validate(recursively,
								plan.getValidationContext(step).getVariableDeclarations());
					} catch (ValidationError e) {
						throw new ValidationError("Failed to validate the input builder", e);
					}
				}
			}
		}
	}

}
