package com.otk.jesb.activity.builtin;

import java.io.IOException;

import com.otk.jesb.InstanceBuilder;
import com.otk.jesb.InstanceBuilder.DynamicValue;
import com.otk.jesb.Plan.ExecutionContext;
import com.otk.jesb.Plan.ValidationContext;
import com.otk.jesb.activity.Activity;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;
import com.otk.jesb.activity.ActivityResult;

import xy.reflect.ui.info.ResourcePath;

public class SleepActivity implements Activity {

	private long milliseconds;

	public long getMilliseconds() {
		return milliseconds;
	}

	public void setMilliseconds(long milliseconds) {
		this.milliseconds = milliseconds;
	}

	@Override
	public ActivityResult execute() throws IOException {
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

		private InstanceBuilder objectSpecification = new InstanceBuilder(SleepActivity.class.getName());

		public InstanceBuilder getObjectSpecification() {
			return objectSpecification;
		}

		public void setObjectSpecification(InstanceBuilder objectSpecification) {
			this.objectSpecification = objectSpecification;
		}

		@Override
		public Activity build(ExecutionContext context) throws Exception {
			return (SleepActivity) objectSpecification.build(context);
		}

		@Override
		public Class<? extends ActivityResult> getActivityResultClass() {
			return null;
		}

		@Override
		public boolean completeValidationContext(ValidationContext validationContext,
				DynamicValue currentDynamicValue) {
			return objectSpecification.completeValidationContext(validationContext, currentDynamicValue);
		}

	}

}
