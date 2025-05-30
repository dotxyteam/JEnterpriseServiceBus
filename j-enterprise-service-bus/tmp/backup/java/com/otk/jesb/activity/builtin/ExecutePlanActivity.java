package com.otk.jesb.operation.builtin;

import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.solution.Asset;
import com.otk.jesb.solution.AssetVisitor;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.solution.Reference;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.solution.Step;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.instantiation.CompilationContext;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.InstantiationFunction;
import com.otk.jesb.instantiation.RootInstanceBuilder;

import xy.reflect.ui.info.ResourcePath;
import com.otk.jesb.util.Accessor;

public class ExecutePlanOperation implements Operation {

	private Plan plan;
	private Object planInput;

	public Plan getPlan() {
		return plan;
	}

	public void setPlan(Plan plan) {
		this.plan = plan;
	}

	public Object getPlanInput() {
		return planInput;
	}

	public void setPlanInput(Object planInput) {
		this.planInput = planInput;
	}

	@Override
	public Object execute() throws Exception {
		try {
			return plan.execute(planInput);
		} catch (Exception e) {
			throw e;
		} catch (Throwable t) {
			throw new AssertionError(t);
		}
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
					.specifyClassPathResourceLocation(ExecutePlanOperation.class.getName().replace(".", "/") + ".png"));
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
							plan.setInputStructure(plan.getInputStructure());
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

		public List<Plan> getPlanOptions() {
			final List<Plan> result = new ArrayList<Plan>();
			Solution.INSTANCE.visitAssets(new AssetVisitor() {
				@Override
				public boolean visitAsset(Asset asset) {
					if (asset instanceof Plan) {
						result.add((Plan) asset);
					}
					return true;
				}
			});
			return result;
		}

		public RootInstanceBuilder getPlanInputBuilder() {
			return planInputBuilder;
		}

		public void setPlanInputBuilder(RootInstanceBuilder planInputBuilder) {
			this.planInputBuilder = planInputBuilder;
		}

		@Override
		public Operation build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			ExecutePlanOperation result = new ExecutePlanOperation();
			result.setPlan(getPlan());
			result.setPlanInput(planInputBuilder.build(new EvaluationContext(context, null)));
			return result;
		}

		@Override
		public Class<?> getOperationResultClass() {
			Plan plan = getPlan();
			if (plan == null) {
				return null;
			}
			return plan.getOutputClass();
		}

		@Override
		public CompilationContext findFunctionCompilationContext(InstantiationFunction function, Step currentStep,
				Plan currentPlan) {
			return planInputBuilder.getFacade().findFunctionCompilationContext(function,
					currentPlan.getValidationContext(currentStep));
		}

	}

}
