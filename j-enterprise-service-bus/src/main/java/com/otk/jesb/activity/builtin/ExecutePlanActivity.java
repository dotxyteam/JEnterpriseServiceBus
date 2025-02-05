package com.otk.jesb.activity.builtin;

import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.Asset;
import com.otk.jesb.AssetVisitor;
import com.otk.jesb.InstanceBuilder;
import com.otk.jesb.InstanceBuilder.Function;
import com.otk.jesb.InstanceBuilder.NullInstance;
import com.otk.jesb.InstanceBuilder.RootInstanceBuilder;
import com.otk.jesb.InstanceBuilder.VerificationContext;
import com.otk.jesb.Plan;
import com.otk.jesb.Plan.ExecutionContext;
import com.otk.jesb.Plan.ValidationContext;
import com.otk.jesb.Solution;
import com.otk.jesb.activity.Activity;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;

import xy.reflect.ui.info.ResourcePath;
import com.otk.jesb.util.Accessor;

public class ExecutePlanActivity implements Activity {

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

	public static class Metadata implements ActivityMetadata {

		@Override
		public String getActivityTypeName() {
			return "Execute Plan";
		}

		@Override
		public String getCategoryName() {
			return "General";
		}

		@Override
		public Class<? extends ActivityBuilder> getActivityBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getActivityIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(ExecutePlanActivity.class.getName().replace(".", "/") + ".png"));
		}
	}

	public static class Builder implements ActivityBuilder {

		private Plan plan;
		private RootInstanceBuilder planInputBuilder = new RootInstanceBuilder(Plan.class.getSimpleName() + "Input",
				new Accessor<String>() {
					@Override
					public String get() {
						if ((plan == null) || (plan.getInputClass() == null)) {
							return NullInstance.class.getName();
						}
						return plan.getInputClass().getName();
					}
				});

		public Plan getPlan() {
			return plan;
		}

		public void setPlan(Plan plan) {
			this.plan = plan;
		}

		public List<Plan> getPlanChoices() {
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
		public Activity build(ExecutionContext context) throws Exception {
			ExecutePlanActivity result = new ExecutePlanActivity();
			result.setPlan(plan);
			result.setPlanInput(planInputBuilder.build(new InstanceBuilder.EvaluationContext(context, null)));
			return result;
		}

		@Override
		public Class<?> getActivityResultClass() {
			if (plan == null) {
				return null;
			}
			return plan.getOutputClass();
		}

		@Override
		public VerificationContext findFunctionVerificationContext(Function function,
				ValidationContext validationContext) {
			return planInputBuilder.findFunctionVerificationContext(function, validationContext, null);
		}

	}

}
