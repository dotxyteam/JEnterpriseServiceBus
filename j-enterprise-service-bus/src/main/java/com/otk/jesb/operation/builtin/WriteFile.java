package com.otk.jesb.operation.builtin;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
import com.otk.jesb.util.Accessor;

public class WriteFile implements Operation {

	private SpecificWriteFileOperation specificOperation;

	public WriteFile(SpecificWriteFileOperation specificOperation) {
		this.specificOperation = specificOperation;
	}

	public SpecificWriteFileOperation getAction() {
		return specificOperation;
	}

	@Override
	public Object execute() throws Throwable {
		return specificOperation.execute();
	}

	private static abstract class SpecificWriteFileOperation {
		protected abstract Object execute() throws Throwable;
	}

	public static class WriteTextFileOperation extends SpecificWriteFileOperation {

		private String filePath;
		private String text;
		private boolean append = false;
		private String charsetName;

		public WriteTextFileOperation(String filePath, String text, boolean append) {
			this.filePath = filePath;
			this.text = text;
			this.append = append;
		}

		public String getFilePath() {
			return filePath;
		}

		public String getText() {
			return text;
		}

		public boolean isAppend() {
			return append;
		}

		public String getCharsetName() {
			return charsetName;
		}

		public void setCharsetName(String charsetName) {
			this.charsetName = charsetName;
		}

		@Override
		protected Object execute() throws IOException {
			Set<StandardOpenOption> options = new HashSet<StandardOpenOption>();
			options.add(StandardOpenOption.WRITE);
			options.add(StandardOpenOption.CREATE);
			if (append) {
				options.add(StandardOpenOption.APPEND);
			} else {
				options.add(StandardOpenOption.TRUNCATE_EXISTING);
			}
			Files.write(Paths.get(filePath),
					text.getBytes((charsetName != null) ? charsetName : Charset.defaultCharset().name()),
					options.toArray(new StandardOpenOption[options.size()]));
			return null;
		}
	}

	public static class WriteBinaryFileOperation extends SpecificWriteFileOperation {

		private String filePath;
		private byte[] data;
		private boolean append = false;

		public WriteBinaryFileOperation(String filePath, byte[] data, boolean append) {
			this.filePath = filePath;
			this.data = data;
			this.append = append;
		}

		public String getFilePath() {
			return filePath;
		}

		public byte[] getData() {
			return data;
		}

		public boolean isAppend() {
			return append;
		}

		@Override
		protected Object execute() throws IOException {
			Set<StandardOpenOption> options = new HashSet<StandardOpenOption>();
			options.add(StandardOpenOption.WRITE);
			options.add(StandardOpenOption.CREATE);
			if (append) {
				options.add(StandardOpenOption.APPEND);
			} else {
				options.add(StandardOpenOption.TRUNCATE_EXISTING);
			}
			Files.write(Paths.get(filePath), data, options.toArray(new StandardOpenOption[options.size()]));
			return null;
		}
	}

	public static class Metadata implements OperationMetadata<WriteFile> {

		@Override
		public String getOperationTypeName() {
			return "Write File";
		}

		@Override
		public String getCategoryName() {
			return "File System";
		}

		@Override
		public Class<? extends OperationBuilder<WriteFile>> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(WriteFile.class.getName().replace(".", "/") + ".png"));
		}
	}

	public static class Builder implements OperationBuilder<WriteFile> {

		public enum Mode {
			TEXT, BINARY
		};

		private Mode mode = Mode.TEXT;
		private RootInstanceBuilder instanceBuilder = new RootInstanceBuilder(WriteFile.class.getSimpleName() + "Input",
				new SpecificOperationClassNameAccessor());

		public Mode getMode() {
			return mode;
		}

		public void setMode(Mode mode) {
			this.mode = mode;
		}

		public RootInstanceBuilder getInstanceBuilder() {
			return instanceBuilder;
		}

		public void setInstanceBuilder(RootInstanceBuilder instanceBuilder) {
			this.instanceBuilder = instanceBuilder;
		}

		@Override
		public WriteFile build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			return new WriteFile((SpecificWriteFileOperation) instanceBuilder.build(new InstantiationContext(
					context.getVariables(),
					context.getPlan().getValidationContext(context.getCurrentStep()).getVariableDeclarations())));
		}

		@Override
		public Class<?> getOperationResultClass(Plan currentPlan, Step currentStep) {
			return null;
		}

		@Override
		public void validate(boolean recursively, Plan plan, Step step) throws ValidationError {
			if (mode == null) {
				throw new ValidationError("Mode not specified: Expected one of " + Arrays.toString(Mode.values()));
			}
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

		private class SpecificOperationClassNameAccessor extends Accessor<String> {
			@Override
			public String get() {
				if (mode == Mode.TEXT) {
					return WriteTextFileOperation.class.getName();
				} else if (mode == Mode.BINARY) {
					return WriteBinaryFileOperation.class.getName();
				} else {
					throw new UnexpectedError();
				}
			}
		}

	}

}
