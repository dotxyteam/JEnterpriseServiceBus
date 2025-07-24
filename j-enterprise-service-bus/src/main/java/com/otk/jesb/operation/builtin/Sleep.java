package com.otk.jesb.operation.builtin;

import java.io.IOException;
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

public class Sleep implements Operation {

	private long milliseconds;

	public Sleep(long milliseconds) {
		this.milliseconds = milliseconds;
	}

	public long getMilliseconds() {
		return milliseconds;
	}

	@Override
	public Object execute() throws IOException {
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return null;
	}

	public static class Metadata implements OperationMetadata<Sleep> {

		@Override
		public String getOperationTypeName() {
			return "Sleep";
		}

		@Override
		public String getCategoryName() {
			return "General";
		}

		@Override
		public Class<? extends OperationBuilder<Sleep>> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(Sleep.class.getName().replace(".", "/") + ".png"));
		}
	}

	public static class Builder implements OperationBuilder<Sleep> {

		private RootInstanceBuilder instanceBuilder = new RootInstanceBuilder(
				Sleep.class.getSimpleName() + "Input", Sleep.class.getName());

		public RootInstanceBuilder getInstanceBuilder() {
			return instanceBuilder;
		}

		public void setInstanceBuilder(RootInstanceBuilder instanceBuilder) {
			this.instanceBuilder = instanceBuilder;
		}

		@Override
		public Sleep build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			return (Sleep) instanceBuilder.build(new InstantiationContext(context.getVariables(),
					context.getPlan().getValidationContext(context.getCurrentStep()).getVariableDeclarations()));
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
