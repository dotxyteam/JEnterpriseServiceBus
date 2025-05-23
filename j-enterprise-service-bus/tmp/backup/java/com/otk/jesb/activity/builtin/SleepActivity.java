package com.otk.jesb.operation.builtin;

import java.io.IOException;

import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.instantiation.CompilationContext;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.InstantiationFunction;
import com.otk.jesb.instantiation.RootInstanceBuilder;

import xy.reflect.ui.info.ResourcePath;

public class SleepOperation implements Operation {

	private long milliseconds;

	public SleepOperation(long milliseconds) {
		this.milliseconds = milliseconds;
	}

	public long getMilliseconds() {
		return milliseconds;
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

	public static class Metadata implements OperationMetadata {

		@Override
		public String getOperationTypeName() {
			return "Sleep";
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
					.specifyClassPathResourceLocation(SleepOperation.class.getName().replace(".", "/") + ".png"));
		}
	}

	public static class Builder implements OperationBuilder {

		private RootInstanceBuilder instanceBuilder = new RootInstanceBuilder(
				SleepOperation.class.getSimpleName() + "Input", SleepOperation.class.getName());

		public RootInstanceBuilder getInstanceBuilder() {
			return instanceBuilder;
		}

		public void setInstanceBuilder(RootInstanceBuilder instanceBuilder) {
			this.instanceBuilder = instanceBuilder;
		}

		@Override
		public Operation build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			return (SleepOperation) instanceBuilder.build(new EvaluationContext(context, null));
		}

		@Override
		public Class<?> getOperationResultClass() {
			return null;
		}

		@Override
		public CompilationContext findFunctionCompilationContext(InstantiationFunction function, Step currentStep,
				Plan currentPlan) {
			return instanceBuilder.getFacade().findFunctionCompilationContext(function,
					currentPlan.getValidationContext(currentStep));
		}
	}

}
