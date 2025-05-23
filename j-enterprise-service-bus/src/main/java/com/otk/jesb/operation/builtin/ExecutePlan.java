package com.otk.jesb.operation.builtin;

import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Reference;
import com.otk.jesb.ValidationError;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;

import xy.reflect.ui.info.ResourcePath;
import com.otk.jesb.util.Accessor;

public class ExecutePlan implements Operation {

	private Plan plan;
	private Object planInput;
	private ExecutionInspector executionInspector;

	public ExecutePlan(Plan plan, Object planInput, ExecutionInspector executionInspector) {
		this.plan = plan;
		this.planInput = planInput;
		this.executionInspector = executionInspector;
	}

	public Plan getPlan() {
		return plan;
	}

	public Object getPlanInput() {
		return planInput;
	}

	@Override
	public Object execute() throws Throwable {
		return plan.execute(planInput, executionInspector);
	}

	public static class Metadata implements OperationMetadata {

		@Override
		public String getOperationTypeName() {
			return "Execute Plan";
		}

		@Override
		public String getCategoryName() {
			return "General";
		}

		@Override
		public Class<? extends OperationBuilder> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(ExecutePlan.class.getName().replace(".", "/") + ".png"));
		}
	}

	public static class Builder implements OperationBuilder {

		private Reference<Plan> planReference = new Reference<Plan>(Plan.class);
		private RootInstanceBuilder planInputBuilder = new RootInstanceBuilder(Plan.class.getSimpleName() + "Input",
				new Accessor<String>() {
					@Override
					public String get() {
						Plan plan = getPlan();
						if (plan == null) {
							return null;
						}
						if (plan.getInputClass() == null) {
							return null;
						}
						return plan.getInputClass().getName();
					}
				});

		private Plan getPlan() {
			return planReference.resolve();
		}

		public Reference<Plan> getPlanReference() {
			return planReference;
		}

		public void setPlanReference(Reference<Plan> planReference) {
			this.planReference = planReference;
		}

		public RootInstanceBuilder getPlanInputBuilder() {
			return planInputBuilder;
		}

		public void setPlanInputBuilder(RootInstanceBuilder planInputBuilder) {
			this.planInputBuilder = planInputBuilder;
		}

		@Override
		public Operation build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			Object planInput = planInputBuilder.build(new InstantiationContext(context.getVariables(),
					context.getPlan().getValidationContext(context.getCurrentStep()).getVariableDeclarations()));
			return new ExecutePlan(getPlan(), planInput, executionInspector);
		}

		@Override
		public Class<?> getOperationResultClass(Plan currentPlan, Step currentStep) {
			Plan plan = getPlan();
			if (plan == null) {
				return null;
			}
			return plan.getOutputClass();
		}

		@Override
		public void validate(boolean recursively, Plan plan, Step step) throws ValidationError {
			if (getPlan() == null) {
				throw new ValidationError("Failed to resolve the plan reference");
			}
			if (recursively) {
				if (planInputBuilder != null) {
					try {
						planInputBuilder.getFacade().validate(recursively,
								plan.getValidationContext(step).getVariableDeclarations());
					} catch (ValidationError e) {
						throw new ValidationError("Failed to validate the plan input builder", e);
					}
				}
			}
		}

	}

}
