package com.otk.jesb.operation.builtin;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

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
import com.otk.jesb.util.Accessor;

public class WriteFileOperation implements Operation {

	private SpecificWriteFileOperation specificOperation;

	public WriteFileOperation(SpecificWriteFileOperation specificOperation) {
		this.specificOperation = specificOperation;
	}

	public SpecificWriteFileOperation getSpecificOperation() {
		return specificOperation;
	}

	@Override
	public Object execute() throws Exception {
		return specificOperation.execute();
	}

	private static interface SpecificWriteFileOperation extends Operation {

	}

	public static class WriteTextFileOperation implements SpecificWriteFileOperation {

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

		public String getCharsetName() {
			return charsetName;
		}

		public void setCharsetName(String charsetName) {
			this.charsetName = charsetName;
		}

		public String getText() {
			return text;
		}

		public boolean isAppend() {
			return append;
		}

		@Override
		public Object execute() throws IOException {
			try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath, append),
					(charsetName != null) ? charsetName : Charset.defaultCharset().name()))) {
				bw.write(text);
			}
			return null;
		}
	}

	public static class WriteBinaryFileOperation implements SpecificWriteFileOperation {

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
		public Object execute() throws IOException {
			try (FileOutputStream fos = new FileOutputStream(filePath, append)) {
				fos.write(data);
			}
			return null;
		}
	}

	public static class Metadata implements OperationMetadata {

		@Override
		public String getOperationTypeName() {
			return "Write File";
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
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(WriteFileOperation.class.getName().replace(".", "/") + ".png"));
		}
	}

	public static class Builder implements OperationBuilder {

		public enum Mode {
			TEXT, BINARY
		};

		private Mode mode = Mode.TEXT;
		private RootInstanceBuilder instanceBuilder = new RootInstanceBuilder(
				WriteFileOperation.class.getSimpleName() + "Input", new Accessor<String>() {
					@Override
					public String get() {
						if (mode == Mode.TEXT) {
							return WriteTextFileOperation.class.getName();
						} else if (mode == Mode.BINARY) {
							return WriteBinaryFileOperation.class.getName();
						} else {
							throw new AssertionError();
						}
					}
				});

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
			return new WriteFileOperation(
					(SpecificWriteFileOperation) instanceBuilder.build(new EvaluationContext(context, null)));
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
