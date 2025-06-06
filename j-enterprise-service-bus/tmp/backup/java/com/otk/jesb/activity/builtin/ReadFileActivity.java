package com.otk.jesb.operation.builtin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

public class ReadFileOperation implements Operation {

	private UnderlyingReadFileOperation underling;

	public ReadFileOperation(UnderlyingReadFileOperation underling) {
		this.underling = underling;
	}

	@Override
	public Object execute() throws Exception {
		return underling.execute();
	}

	private static interface UnderlyingReadFileOperation extends Operation {

	}

	public static class ReadTextFileOperation implements UnderlyingReadFileOperation {

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
		public Object execute() throws IOException {
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

	public static class ReadBinaryFileOperation implements UnderlyingReadFileOperation {

		private String filePath;

		public ReadBinaryFileOperation(String filePath) {
			this.filePath = filePath;
		}

		public String getFilePath() {
			return filePath;
		}

		@Override
		public Object execute() throws IOException {
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
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(ReadFileOperation.class.getName().replace(".", "/") + ".png"));
		}
	}

	public static class Builder implements OperationBuilder {

		public enum Mode {
			TEXT, BINARY
		};

		private Mode mode = Mode.TEXT;
		private RootInstanceBuilder instanceBuilder = new RootInstanceBuilder(
				ReadFileOperation.class.getSimpleName() + "Input", new Accessor<String>() {
					@Override
					public String get() {
						if (mode == Mode.TEXT) {
							return ReadTextFileOperation.class.getName();
						} else if (mode == Mode.BINARY) {
							return ReadBinaryFileOperation.class.getName();
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
			return new ReadFileOperation(
					(UnderlyingReadFileOperation) instanceBuilder.build(new EvaluationContext(context, null)));
		}

		@Override
		public Class<?> getOperationResultClass() {
			if (mode == Mode.TEXT) {
				return TextResult.class;
			} else if (mode == Mode.BINARY) {
				return BinaryResult.class;
			} else {
				throw new AssertionError();
			}
		}

		@Override
		public CompilationContext findFunctionCompilationContext(InstantiationFunction function, Step currentStep,
				Plan currentPlan) {
			return instanceBuilder.getFacade().findFunctionCompilationContext(function,
					currentPlan.getValidationContext(currentStep));
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
