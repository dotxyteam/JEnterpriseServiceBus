package com.otk.jesb.operation.builtin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

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

public class ReadFile implements Operation {

	private SpecificReadFileOperation specificOperation;

	public ReadFile(SpecificReadFileOperation specificOperation) {
		this.specificOperation = specificOperation;
	}

	public SpecificReadFileOperation getAction() {
		return specificOperation;
	}

	@Override
	public Object execute() throws Throwable {
		return specificOperation.execute();
	}

	private static abstract class SpecificReadFileOperation {
		protected abstract Object execute() throws Throwable;
	}

	public static class ReadTextFileOperation extends SpecificReadFileOperation {

		private String filePath;
		private String charsetName;

		public ReadTextFileOperation(String filePath) {
			this.filePath = filePath;
		}

		public String getFilePath() {
			return filePath;
		}

		public String getCharsetName() {
			return charsetName;
		}

		public void setCharsetName(String charsetName) {
			this.charsetName = charsetName;
		}

		@Override
		protected Object execute() throws IOException {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			try (FileInputStream in = new FileInputStream(new File(filePath))) {
				int bytesRead;
				byte[] data = new byte[1024];
				while ((bytesRead = in.read(data, 0, data.length)) != -1) {
					buffer.write(data, 0, bytesRead);
				}
			}
			return new TextResult(new String(buffer.toByteArray(),
					(charsetName != null) ? charsetName : Charset.defaultCharset().name()));
		}
	}

	public static class ReadBinaryFileOperation extends SpecificReadFileOperation {

		private String filePath;

		public ReadBinaryFileOperation(String filePath) {
			this.filePath = filePath;
		}

		public String getFilePath() {
			return filePath;
		}

		@Override
		protected Object execute() throws IOException {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			try (FileInputStream in = new FileInputStream(new File(filePath))) {
				int bytesRead;
				byte[] data = new byte[1024];
				while ((bytesRead = in.read(data, 0, data.length)) != -1) {
					buffer.write(data, 0, bytesRead);
				}
			}
			return new BinaryResult(buffer.toByteArray());
		}
	}

	public static class Metadata implements OperationMetadata {

		@Override
		public String getOperationTypeName() {
			return "Read File";
		}

		@Override
		public String getCategoryName() {
			return "File";
		}

		@Override
		public Class<? extends OperationBuilder> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(
					ResourcePath.specifyClassPathResourceLocation(ReadFile.class.getName().replace(".", "/") + ".png"));
		}
	}

	public static class Builder implements OperationBuilder {

		public enum Mode {
			TEXT, BINARY
		};

		private Mode mode = Mode.TEXT;
		private RootInstanceBuilder instanceBuilder = new RootInstanceBuilder(ReadFile.class.getSimpleName() + "Input",
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
		public Operation build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			return new ReadFile((SpecificReadFileOperation) instanceBuilder.build(new InstantiationContext(
					context.getVariables(),
					context.getPlan().getValidationContext(context.getCurrentStep()).getVariableDeclarations())));
		}

		@Override
		public Class<?> getOperationResultClass(Plan currentPlan, Step currentStep) {
			if (mode == Mode.TEXT) {
				return TextResult.class;
			} else if (mode == Mode.BINARY) {
				return BinaryResult.class;
			} else {
				throw new UnexpectedError();
			}
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
					return ReadTextFileOperation.class.getName();
				} else if (mode == Mode.BINARY) {
					return ReadBinaryFileOperation.class.getName();
				} else {
					throw new UnexpectedError();
				}
			}
		}

	}

	public static class TextResult {

		private String text;

		public TextResult(String text) {
			this.text = text;
		}

		public String getText() {
			return text;
		}

	}

	public static class BinaryResult {

		private byte[] data;

		public BinaryResult(byte[] data) {
			this.data = data;
		}

		public byte[] getData() {
			return data;
		}

	}

}
