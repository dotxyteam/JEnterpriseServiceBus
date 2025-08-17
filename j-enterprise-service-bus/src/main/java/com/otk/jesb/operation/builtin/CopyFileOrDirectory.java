package com.otk.jesb.operation.builtin;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.otk.jesb.ValidationError;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.solution.Step;

import xy.reflect.ui.info.ResourcePath;

public class CopyFileOrDirectory extends MoveFileOrDirectory {

	public CopyFileOrDirectory(String sourceFilePath, String destinationFilePath) {
		super(sourceFilePath, destinationFilePath);
	}

	@Override
	protected void delete(Path path) throws IOException {
	}

	@Override
	protected void move(Path source, Path target, StandardCopyOption... options) throws IOException {
		super.copy(source, target, options);
	}

	public static class Builder implements OperationBuilder<CopyFileOrDirectory> {

		private RootInstanceBuilder instanceBuilder = new RootInstanceBuilder(
				CopyFileOrDirectory.class.getSimpleName() + "Input", CopyFileOrDirectory.class.getName());

		public RootInstanceBuilder getInstanceBuilder() {
			return instanceBuilder;
		}

		public void setInstanceBuilder(RootInstanceBuilder instanceBuilder) {
			this.instanceBuilder = instanceBuilder;
		}

		@Override
		public CopyFileOrDirectory build(ExecutionContext context, ExecutionInspector executionInspector)
				throws Exception {
			return (CopyFileOrDirectory) instanceBuilder.build(new InstantiationContext(context.getVariables(),
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

	public static class Metadata implements OperationMetadata<CopyFileOrDirectory> {

		@Override
		public String getOperationTypeName() {
			return "Copy File/Directory";
		}

		@Override
		public String getCategoryName() {
			return "File System";
		}

		@Override
		public Class<? extends OperationBuilder<CopyFileOrDirectory>> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(CopyFileOrDirectory.class.getName().replace(".", "/") + ".png"));
		}
	}

}
