package com.otk.jesb.operation.builtin;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
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

public class MoveFileOrDirectory implements Operation {

	private String sourceFilePath;
	private String targetFilePath;
	private boolean existingReplaced;

	public MoveFileOrDirectory(String sourceFilePath, String destinationFilePath, boolean existingReplaced) {
		this.sourceFilePath = sourceFilePath;
		this.targetFilePath = destinationFilePath;
		this.existingReplaced = existingReplaced;
	}

	public String getSourceFilePath() {
		return sourceFilePath;
	}

	public String getTargetFilePath() {
		return targetFilePath;
	}

	public boolean isExistingReplaced() {
		return existingReplaced;
	}

	@Override
	public Object execute() throws IOException {
		Path source = Paths.get(sourceFilePath);
		Path target = Paths.get(targetFilePath);
		try {
			move(source, target, existingReplaced ? new StandardCopyOption[] { StandardCopyOption.REPLACE_EXISTING }
					: new StandardCopyOption[] {});
			return null;
		} catch (AtomicMoveNotSupportedException | DirectoryNotEmptyException ignore) {
		}
		if (Files.isDirectory(source)) {
			Files.walkFileTree(source, new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attrs) throws IOException {
					Path relativeDirectory = source.relativize(directory);
					Path targetDirectory = target.resolve(relativeDirectory);
					if (!Files.exists(targetDirectory)) {
						Files.createDirectories(targetDirectory);
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Path relativeFile = source.relativize(file);
					Path targetFile = target.resolve(relativeFile);
					move(file, targetFile,
							existingReplaced ? new StandardCopyOption[] { StandardCopyOption.REPLACE_EXISTING }
									: new StandardCopyOption[] {});
					return FileVisitResult.CONTINUE;
				}
			});
			Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult postVisitDirectory(Path directory, IOException exc) throws IOException {
					delete(directory);
					return FileVisitResult.CONTINUE;
				}
			});
		} else {
			copy(source, target,
					existingReplaced
							? new StandardCopyOption[] { StandardCopyOption.REPLACE_EXISTING,
									StandardCopyOption.COPY_ATTRIBUTES }
							: new StandardCopyOption[] { StandardCopyOption.COPY_ATTRIBUTES });
			delete(source);
		}
		return null;
	}

	protected void delete(Path path) throws IOException {
		Files.delete(path);
	}

	protected void move(Path source, Path target, StandardCopyOption... options) throws IOException {
		Files.move(source, target, options);
	}

	protected void copy(Path source, Path target, StandardCopyOption... options) throws IOException {
		Files.copy(source, target, options);
	}

	public static class Builder implements OperationBuilder<MoveFileOrDirectory> {

		private RootInstanceBuilder instanceBuilder = new RootInstanceBuilder(
				MoveFileOrDirectory.class.getSimpleName() + "Input", MoveFileOrDirectory.class.getName());

		public RootInstanceBuilder getInstanceBuilder() {
			return instanceBuilder;
		}

		public void setInstanceBuilder(RootInstanceBuilder instanceBuilder) {
			this.instanceBuilder = instanceBuilder;
		}

		@Override
		public MoveFileOrDirectory build(ExecutionContext context, ExecutionInspector executionInspector)
				throws Exception {
			return (MoveFileOrDirectory) instanceBuilder.build(new InstantiationContext(context.getVariables(),
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

	public static class Metadata implements OperationMetadata<MoveFileOrDirectory> {

		@Override
		public String getOperationTypeName() {
			return "Move File/Directory";
		}

		@Override
		public String getCategoryName() {
			return "File";
		}

		@Override
		public Class<? extends OperationBuilder<MoveFileOrDirectory>> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(MoveFileOrDirectory.class.getName().replace(".", "/") + ".png"));
		}
	}

}
