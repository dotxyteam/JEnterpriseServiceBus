package com.otk.jesb.activity.builtin;

import java.io.IOException;
import com.otk.jesb.ValidationError;
import com.otk.jesb.activity.Activity;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;

import xy.reflect.ui.info.ResourcePath;

public class SleepActivity implements Activity {

	private long milliseconds;

	public SleepActivity(long milliseconds) {
		this.milliseconds = milliseconds;
	}

	public long getMilliseconds() {
		return milliseconds;
	}

	public void setMilliseconds(long milliseconds) {
		this.milliseconds = milliseconds;
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

	public static class Metadata implements ActivityMetadata {

		@Override
		public String getActivityTypeName() {
			return "Sleep";
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
					.specifyClassPathResourceLocation(SleepActivity.class.getName().replace(".", "/") + ".png"));
		}
	}

	public static class Builder implements ActivityBuilder {

		private RootInstanceBuilder instanceBuilder = new RootInstanceBuilder(
				SleepActivity.class.getSimpleName() + "Input", SleepActivity.class.getName());

		public RootInstanceBuilder getInstanceBuilder() {
			return instanceBuilder;
		}

		public void setInstanceBuilder(RootInstanceBuilder instanceBuilder) {
			this.instanceBuilder = instanceBuilder;
		}

		@Override
		public Activity build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			return (SleepActivity) instanceBuilder.build(new InstantiationContext(context.getVariables(),
					context.getPlan().getValidationContext(context.getCurrentStep()).getVariableDeclarations()));
		}

		@Override
		public Class<?> getActivityResultClass(Plan currentPlan, Step currentStep) {
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
