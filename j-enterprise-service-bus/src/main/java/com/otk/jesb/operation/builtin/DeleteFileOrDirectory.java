package com.otk.jesb.operation.builtin;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.otk.jesb.ValidationError;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.solution.Step;

import xy.reflect.ui.info.ResourcePath;

public class DeleteFileOrDirectory implements Operation {

	private MoveFileOrDirectory util;

	public DeleteFileOrDirectory(String path) {
		util = new MoveFileOrDirectory(path, path) {
			@Override
			protected void move(Path source, Path target, StandardCopyOption... options) throws IOException {
				super.delete(source);
			}

			@Override
			protected void copy(Path source, Path target, StandardCopyOption... options) throws IOException {
			}
		};
	}

	@Override
	public Object execute(Solution solutionInstance) throws Throwable {
		return util.execute(solutionInstance);
	}

	public static class Builder implements OperationBuilder<DeleteFileOrDirectory> {

		private RootInstanceBuilder instanceBuilder = new RootInstanceBuilder(
				DeleteFileOrDirectory.class.getSimpleName() + "Input", DeleteFileOrDirectory.class.getName());

		public RootInstanceBuilder getInstanceBuilder() {
			return instanceBuilder;
		}

		public void setInstanceBuilder(RootInstanceBuilder instanceBuilder) {
			this.instanceBuilder = instanceBuilder;
		}

		@Override
		public DeleteFileOrDirectory build(ExecutionContext context, ExecutionInspector executionInspector)
				throws Exception {
			Solution solutionInstance = context.getSession().getSolutionInstance();
			return (DeleteFileOrDirectory) instanceBuilder.build(new InstantiationContext(
					context.getVariables(), context.getPlan()
							.getValidationContext(context.getCurrentStep(), solutionInstance).getVariableDeclarations(),
					solutionInstance));
		}

		@Override
		public Class<?> getOperationResultClass(Solution solutionInstance, Plan currentPlan, Step currentStep) {
			return null;
		}

		@Override
		public void validate(boolean recursively, Solution solutionInstance, Plan plan, Step step)
				throws ValidationError {
			if (recursively) {
				if (instanceBuilder != null) {
					try {
						instanceBuilder.getFacade(solutionInstance).validate(recursively,
								plan.getValidationContext(step, solutionInstance).getVariableDeclarations());
					} catch (ValidationError e) {
						throw new ValidationError("Failed to validate the input builder", e);
					}
				}
			}
		}

	}

	public static class Metadata implements OperationMetadata<DeleteFileOrDirectory> {

		@Override
		public String getOperationTypeName() {
			return "Delete File/Directory";
		}

		@Override
		public String getCategoryName() {
			return "File System";
		}

		@Override
		public Class<? extends OperationBuilder<DeleteFileOrDirectory>> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(ResourcePath.specifyClassPathResourceLocation(
					DeleteFileOrDirectory.class.getName().replace(".", "/") + ".png"));
		}
	}

}
