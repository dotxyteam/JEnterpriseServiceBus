package com.otk.jesb.activity.builtin;

import java.io.IOException;

import com.otk.jesb.activity.Activity;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;
import com.otk.jesb.instantiation.InstantiationFunctionCompilationContext;
import com.otk.jesb.instantiation.InstantiationFunction;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;

import xy.reflect.ui.info.ResourcePath;

public class DoNothingActivity implements Activity {

	

	@Override
	public Object execute() throws IOException {
		return null;
	}

	public static class Metadata implements ActivityMetadata {

		@Override
		public String getActivityTypeName() {
			return "Do Nothing";
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
					.specifyClassPathResourceLocation(DoNothingActivity.class.getName().replace(".", "/") + ".png"));
		}
	}

	public static class Builder implements ActivityBuilder {

		
		@Override
		public Activity build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			return new DoNothingActivity();
		}

		@Override
		public Class<?> getActivityResultClass(Plan currentPlan, Step currentStep) {
			return null;
		}

		@Override
		public InstantiationFunctionCompilationContext findFunctionCompilationContext(InstantiationFunction function,
				Step currentStep, Plan currentPlan) {
			throw new UnsupportedOperationException();
		}
	}

}
