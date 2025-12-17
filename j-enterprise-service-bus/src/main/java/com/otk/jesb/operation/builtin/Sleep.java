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
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.solution.Solution;

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
	public Object execute(Solution solutionInstance) throws IOException {
		MiscUtils.sleepSafely(milliseconds);
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
			return new ResourcePath(
					ResourcePath.specifyClassPathResourceLocation(Sleep.class.getName().replace(".", "/") + ".png"));
		}
	}

	public static class Builder implements OperationBuilder<Sleep> {

		private RootInstanceBuilder instanceBuilder = new RootInstanceBuilder(Sleep.class.getSimpleName() + "Input",
				Sleep.class.getName());

		public RootInstanceBuilder getInstanceBuilder() {
			return instanceBuilder;
		}

		public void setInstanceBuilder(RootInstanceBuilder instanceBuilder) {
			this.instanceBuilder = instanceBuilder;
		}

		@Override
		public Sleep build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			Solution solutionInstance = context.getSession().getSolutionInstance();
			return (Sleep) instanceBuilder.build(new InstantiationContext(
					context.getVariables(), context.getPlan()
							.getValidationContext(context.getCurrentStep(), solutionInstance).getVariableDeclarations(),
					solutionInstance));
		}

		@Override
		public Class<?> getOperationResultClass(Solution solutionInstance, Plan currentPlan, Step currentStep) {
			return null;
		}

		@Override
		public void validate(boolean recursively, Solution solutionInstance, Plan plan, Step step)
				throws ValidationError {
			if (recursively) {
				if (instanceBuilder != null) {
					try {
						instanceBuilder.getFacade(solutionInstance).validate(recursively,
								plan.getValidationContext(step, solutionInstance).getVariableDeclarations());
					} catch (ValidationError e) {
						throw new ValidationError("Failed to validate the input builder", e);
					}
				}
			}
		}
	}

}
