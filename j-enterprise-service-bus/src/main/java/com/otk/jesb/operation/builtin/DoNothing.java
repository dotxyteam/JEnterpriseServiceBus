package com.otk.jesb.operation.builtin;

import java.io.IOException;

import com.otk.jesb.ValidationError;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.solution.Solution;

import xy.reflect.ui.info.ResourcePath;

public class DoNothing implements Operation {

	@Override
	public Object execute(Solution solutionInstance) throws IOException {
		return null;
	}

	public static class Metadata implements OperationMetadata<DoNothing> {

		@Override
		public String getOperationTypeName() {
			return "Do Nothing";
		}

		@Override
		public String getCategoryName() {
			return "General";
		}

		@Override
		public Class<? extends OperationBuilder<DoNothing>> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(DoNothing.class.getName().replace(".", "/") + ".png"));
		}
	}

	public static class Builder implements OperationBuilder<DoNothing> {

		@Override
		public DoNothing build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			return new DoNothing();
		}

		@Override
		public Class<?> getOperationResultClass(Solution solutionInstance, Plan currentPlan, Step currentStep) {
			return null;
		}

		@Override
		public void validate(boolean recursively, Solution solutionInstance, Plan plan, Step step)
				throws ValidationError {
		}
	}

}
