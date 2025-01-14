package com.otk.jesb.activity.builtin;

import com.otk.jesb.InstanceBuilder;
import com.otk.jesb.InstanceBuilder.DynamicValue;
import com.otk.jesb.Plan;
import com.otk.jesb.Plan.ExecutionContext;
import com.otk.jesb.Plan.ValidationContext;
import com.otk.jesb.activity.Activity;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;

import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.util.Accessor;

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
		return plan.execute(planInput);
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
		private InstanceBuilder planInputBuilder = new InstanceBuilder(new Accessor<String>() {
			@Override
			public String get() {
				return plan.getInputClass().getName();
			}
		});

		public Plan getPlan() {
			return plan;
		}

		public void setPlan(Plan plan) {
			this.plan = plan;
		}

		public InstanceBuilder getPlanInputBuilder() {
			return planInputBuilder;
		}

		public void setPlanInputBuilder(InstanceBuilder planInputBuilder) {
			this.planInputBuilder = planInputBuilder;
		}

		@Override
		public Activity build(ExecutionContext context) throws Exception {
			ExecutePlanActivity result = new ExecutePlanActivity();
			result.setPlan(plan);
			result.setPlanInput(planInputBuilder.build(context));
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
		public boolean completeValidationContext(ValidationContext validationContext,
				DynamicValue currentDynamicValue) {
			return planInputBuilder.completeValidationContext(validationContext, currentDynamicValue);
		}

	}

}
