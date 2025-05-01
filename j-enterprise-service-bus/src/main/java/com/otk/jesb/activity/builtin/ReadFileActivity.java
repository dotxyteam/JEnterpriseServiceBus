package com.otk.jesb.activity.builtin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import com.otk.jesb.activity.Activity;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;
import com.otk.jesb.instantiation.InstantiationFunctionCompilationContext;
import com.otk.jesb.instantiation.EvaluationContext;
import com.otk.jesb.instantiation.InstantiationFunction;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;

import xy.reflect.ui.info.ResourcePath;
import com.otk.jesb.util.Accessor;

public class ReadFileActivity implements Activity {

	private UnderlyingReadFileActivity underling;

	public ReadFileActivity(UnderlyingReadFileActivity underling) {
		this.underling = underling;
	}

	@Override
	public Object execute() throws Throwable {
		return underling.execute();
	}

	private static interface UnderlyingReadFileActivity extends Activity {

	}

	public static class ReadTextFileActivity implements UnderlyingReadFileActivity {

		private String filePath;
		private String charsetName;

		public ReadTextFileActivity(String filePath) {
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

	public static class ReadBinaryFileActivity implements UnderlyingReadFileActivity {

		private String filePath;

		public ReadBinaryFileActivity(String filePath) {
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

	public static class Metadata implements ActivityMetadata {

		@Override
		public String getActivityTypeName() {
			return "Read File";
		}

		@Override
		public String getCategoryName() {
			return "File";
		}

		@Override
		public Class<? extends ActivityBuilder> getActivityBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getActivityIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(ReadFileActivity.class.getName().replace(".", "/") + ".png"));
		}
	}

	public static class Builder implements ActivityBuilder {

		public enum Mode {
			TEXT, BINARY
		};

		private Mode mode = Mode.TEXT;
		private RootInstanceBuilder instanceBuilder = new RootInstanceBuilder(
				ReadFileActivity.class.getSimpleName() + "Input", new Accessor<String>() {
					@Override
					public String get() {
						if (mode == Mode.TEXT) {
							return ReadTextFileActivity.class.getName();
						} else if (mode == Mode.BINARY) {
							return ReadBinaryFileActivity.class.getName();
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
		public Activity build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			return new ReadFileActivity((UnderlyingReadFileActivity) instanceBuilder.build(
					new EvaluationContext(context.getVariables(), null, context.getCompilationContextProvider())));
		}

		@Override
		public Class<?> getActivityResultClass(Plan currentPlan, Step currentStep) {
			if (mode == Mode.TEXT) {
				return TextResult.class;
			} else if (mode == Mode.BINARY) {
				return BinaryResult.class;
			} else {
				throw new AssertionError();
			}
		}

		@Override
		public InstantiationFunctionCompilationContext findFunctionCompilationContext(InstantiationFunction function,
				Step currentStep, Plan currentPlan) {
			return instanceBuilder.getFacade().findFunctionCompilationContext(function,
					currentPlan.getValidationContext(currentStep).getVariableDeclarations());
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
