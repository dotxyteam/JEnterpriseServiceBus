package com.otk.jesb.operation.builtin;

import com.otk.jesb.solution.Plan;
import com.otk.jesb.Reference;
import com.otk.jesb.Session;
import com.otk.jesb.ValidationError;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionError;
import com.otk.jesb.solution.Plan.ExecutionInspector;

import xy.reflect.ui.info.ResourcePath;
import com.otk.jesb.util.Accessor;

public class ExecutePlan implements Operation {

	private Session session;
	private Plan plan;
	private Object planInput;
	private ExecutionInspector executionInspector;

	public ExecutePlan(Session session, Plan plan, Object planInput, ExecutionInspector executionInspector) {
		this.session = session;
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
	public Object execute() throws ExecutionError {
		return plan.execute(planInput, executionInspector, new ExecutionContext(session, plan));
	}

	public static class Metadata implements OperationMetadata<ExecutePlan> {

		@Override
		public String getOperationTypeName() {
			return "Execute Plan";
		}

		@Override
		public String getCategoryName() {
			return "General";
		}

		@Override
		public Class<? extends OperationBuilder<ExecutePlan>> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(ExecutePlan.class.getName().replace(".", "/") + ".png"));
		}
	}

	public static class Builder implements OperationBuilder<ExecutePlan> {

		private Reference<Plan> planReference = new Reference<Plan>(Plan.class);

		private RootInstanceBuilder planInputBuilder = new RootInstanceBuilder(Plan.class.getSimpleName() + "Input",
				new PlanInputClassNameAccessor());

		private Plan getTargetPlan() {
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
		public ExecutePlan build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			Object planInput = planInputBuilder.build(new InstantiationContext(context.getVariables(),
					context.getPlan().getValidationContext(context.getCurrentStep()).getVariableDeclarations()));
			return new ExecutePlan(context.getSession(), getTargetPlan(), planInput, executionInspector);
		}

		@Override
		public Class<?> getOperationResultClass(Plan currentPlan, Step currentStep) {
			if (getTargetPlan() == null) {
				return null;
			}
			return getTargetPlan().getActivator().getOutputClass();
		}

		@Override
		public void validate(boolean recursively, Plan plan, Step step) throws ValidationError {
			if (getTargetPlan() == null) {
				throw new ValidationError("Failed to resolve the target plan reference");
			}
			if (!getTargetPlan().getActivator().getEnabledVariant().getValue()) {
				throw new ValidationError("The target plan activation is disabled");
			}
			if (recursively) {
				if (planInputBuilder != null) {
					try {
						planInputBuilder.getFacade().validate(recursively,
								plan.getValidationContext(step).getVariableDeclarations());
					} catch (ValidationError e) {
						throw new ValidationError("Failed to validate the target plan input builder", e);
					}
				}
			}
		}

		private class PlanInputClassNameAccessor extends Accessor<String> {
			@Override
			public String get() {
				Plan plan = getTargetPlan();
				if (plan == null) {
					return null;
				}
				if (plan.getActivator().getInputClass() == null) {
					return null;
				}
				return plan.getActivator().getInputClass().getName();
			}
		}

	}

}
