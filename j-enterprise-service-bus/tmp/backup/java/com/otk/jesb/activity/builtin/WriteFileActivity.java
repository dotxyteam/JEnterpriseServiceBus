package com.otk.jesb.activity.builtin;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;
import com.otk.jesb.activity.Activity;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;
import com.otk.jesb.instantiation.CompilationContext;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.InstantiationFunction;
import com.otk.jesb.instantiation.RootInstanceBuilder;

import xy.reflect.ui.info.ResourcePath;
import com.otk.jesb.util.Accessor;

public class WriteFileActivity implements Activity {

	private SpecificWriteFileActivity specificActivity;

	public WriteFileActivity(SpecificWriteFileActivity specificActivity) {
		this.specificActivity = specificActivity;
	}

	public SpecificWriteFileActivity getSpecificActivity() {
		return specificActivity;
	}

	@Override
	public Object execute() throws Exception {
		return specificActivity.execute();
	}

	private static interface SpecificWriteFileActivity extends Activity {

	}

	public static class WriteTextFileActivity implements SpecificWriteFileActivity {

		private String filePath;
		private String text;
		private boolean append = false;
		private String charsetName;

		public WriteTextFileActivity(String filePath, String text, boolean append) {
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

	public static class WriteBinaryFileActivity implements SpecificWriteFileActivity {

		private String filePath;
		private byte[] data;
		private boolean append = false;

		public WriteBinaryFileActivity(String filePath, byte[] data, boolean append) {
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

	public static class Metadata implements ActivityMetadata {

		@Override
		public String getActivityTypeName() {
			return "Write File";
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
					.specifyClassPathResourceLocation(WriteFileActivity.class.getName().replace(".", "/") + ".png"));
		}
	}

	public static class Builder implements ActivityBuilder {

		public enum Mode {
			TEXT, BINARY
		};

		private Mode mode = Mode.TEXT;
		private RootInstanceBuilder instanceBuilder = new RootInstanceBuilder(
				WriteFileActivity.class.getSimpleName() + "Input", new Accessor<String>() {
					@Override
					public String get() {
						if (mode == Mode.TEXT) {
							return WriteTextFileActivity.class.getName();
						} else if (mode == Mode.BINARY) {
							return WriteBinaryFileActivity.class.getName();
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
			return new WriteFileActivity(
					(SpecificWriteFileActivity) instanceBuilder.build(new EvaluationContext(context, null)));
		}

		@Override
		public Class<?> getActivityResultClass() {
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
