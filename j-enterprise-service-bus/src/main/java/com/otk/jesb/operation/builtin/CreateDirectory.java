package com.otk.jesb.operation.builtin;

import java.nio.file.Files;
import java.nio.file.LinkOption;
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
import com.otk.jesb.solution.Solution;
import com.otk.jesb.solution.Step;

import xy.reflect.ui.info.ResourcePath;

public class CreateDirectory implements Operation {

	private String path;
	private boolean preExistingHierarchyRequired = true;
	private boolean nonExistingTargetRequired = false;

	public CreateDirectory(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}

	public boolean isPreExistingHierarchyRequired() {
		return preExistingHierarchyRequired;
	}

	public void setPreExistingHierarchyRequired(boolean preExistingHierarchyRequired) {
		this.preExistingHierarchyRequired = preExistingHierarchyRequired;
	}

	public boolean isNonExistingTargetRequired() {
		return nonExistingTargetRequired;
	}

	public void setNonExistingTargetRequired(boolean nonExistingTargetRequired) {
		this.nonExistingTargetRequired = nonExistingTargetRequired;
	}

	@Override
	public Object execute(Solution solutionInstance) throws Throwable {
		Path nioPath = Paths.get(path);
		if (!nonExistingTargetRequired && Files.isDirectory(nioPath, LinkOption.NOFOLLOW_LINKS)) {
			return null;
		}
		if (preExistingHierarchyRequired) {
			Files.createDirectory(nioPath);
		} else {
			Files.createDirectories(nioPath);
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
			Solution solutionInstance = context.getSession().getSolutionInstance();
			return (CreateDirectory) instanceBuilder.build(new InstantiationContext(
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
