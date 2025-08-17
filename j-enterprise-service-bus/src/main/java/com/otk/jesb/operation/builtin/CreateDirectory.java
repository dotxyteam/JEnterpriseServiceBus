package com.otk.jesb.operation.builtin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.otk.jesb.ValidationError;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.solution.Step;

import xy.reflect.ui.info.ResourcePath;

public class CreateDirectory implements Operation {

	private String path;
	private boolean preExistingParentDirectoryOptional = false;

	public CreateDirectory(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}

	public boolean isPreExistingParentDirectoryOptional() {
		return preExistingParentDirectoryOptional;
	}

	public void setPreExistingParentDirectoryOptional(boolean preExistingParentDirectoryOptional) {
		this.preExistingParentDirectoryOptional = preExistingParentDirectoryOptional;
	}

	@Override
	public Object execute() throws Throwable {
		Path nioPath = Paths.get(path);
		if (preExistingParentDirectoryOptional) {
			Files.createDirectories(nioPath);
		} else {
			Files.createDirectory(nioPath);
		}
		return null;
	}

	public static class Builder implements OperationBuilder<CreateDirectory> {

		private RootInstanceBuilder instanceBuilder = new RootInstanceBuilder(
				CreateDirectory.class.getSimpleName() + "Input", CreateDirectory.class.getName());

		public RootInstanceBuilder getInstanceBuilder() {
			return instanceBuilder;
		}

		public void setInstanceBuilder(RootInstanceBuilder instanceBuilder) {
			this.instanceBuilder = instanceBuilder;
		}

		@Override
		public CreateDirectory build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			return (CreateDirectory) instanceBuilder.build(new InstantiationContext(context.getVariables(),
					context.getPlan().getValidationContext(context.getCurrentStep()).getVariableDeclarations()));
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

	public static class Metadata implements OperationMetadata<CreateDirectory> {

		@Override
		public String getOperationTypeName() {
			return "Create Directory";
		}

		@Override
		public String getCategoryName() {
			return "File System";
		}

		@Override
		public Class<? extends OperationBuilder<CreateDirectory>> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(CreateDirectory.class.getName().replace(".", "/") + ".png"));
		}
	}

}
