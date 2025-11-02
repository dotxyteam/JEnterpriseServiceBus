package com.otk.jesb.operation.builtin;

import java.io.IOException;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;

import xy.reflect.ui.info.ResourcePath;

public class Log implements Operation {

	private String message;
	private Level level = Level.INFORMATION;

	public Log(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public Level getLevel() {
		return level;
	}

	public void setLevel(Level level) {
		this.level = level;
	}


	@Override
	public Object execute() throws IOException {
		if (level == Level.INFORMATION) {
			com.otk.jesb.Log.get().information(message);
		} else if (level == Level.WARNING) {
			com.otk.jesb.Log.get().warning(message);
		} else if (level == Level.INFORMATION) {
			com.otk.jesb.Log.get().error(message);
		} else {
			throw new UnexpectedError();
		}
		return null;
	}

	public enum Level {
		INFORMATION, WARNING, EERROR
	}

	public static class Metadata implements OperationMetadata<Log> {

		@Override
		public String getOperationTypeName() {
			return "Log";
		}

		@Override
		public String getCategoryName() {
			return "General";
		}

		@Override
		public Class<? extends OperationBuilder<Log>> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(
					ResourcePath.specifyClassPathResourceLocation(Log.class.getName().replace(".", "/") + ".png"));
		}
	}

	public static class Builder implements OperationBuilder<Log> {

		private RootInstanceBuilder instanceBuilder = new RootInstanceBuilder(Log.class.getSimpleName() + "Input",
				Log.class.getName());

		public RootInstanceBuilder getInstanceBuilder() {
			return instanceBuilder;
		}

		public void setInstanceBuilder(RootInstanceBuilder instanceBuilder) {
			this.instanceBuilder = instanceBuilder;
		}

		@Override
		public Log build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			Log instance = (Log) instanceBuilder.build(new InstantiationContext(context.getVariables(),
					context.getPlan().getValidationContext(context.getCurrentStep()).getVariableDeclarations()));
			return instance;
		}

		@Override
		public Class<?> getOperationResultClass(Plan currentPlan, Step currentStep) {
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
