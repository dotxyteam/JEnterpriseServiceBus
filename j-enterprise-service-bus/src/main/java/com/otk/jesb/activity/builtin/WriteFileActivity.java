package com.otk.jesb.activity.builtin;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Arrays;

import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.activity.Activity;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;
import com.otk.jesb.instantiation.Facade;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.InstantiationFunction;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;

import xy.reflect.ui.info.ResourcePath;
import com.otk.jesb.util.Accessor;

public class WriteFileActivity implements Activity {

	private SpecificWriteFileActivity specificActivity;

	public WriteFileActivity(SpecificWriteFileActivity specificActivity) {
		this.specificActivity = specificActivity;
	}

	public SpecificWriteFileActivity getAction() {
		return specificActivity;
	}

	@Override
	public Object execute() throws Throwable {
		return specificActivity.execute();
	}

	private static abstract class SpecificWriteFileActivity {
		protected abstract Object execute() throws Throwable;
	}

	public static class WriteTextFileActivity extends SpecificWriteFileActivity {

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

		public void setFilePath(String filePath) {
			this.filePath = filePath;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public boolean isAppend() {
			return append;
		}

		public void setAppend(boolean append) {
			this.append = append;
		}

		public String getCharsetName() {
			return charsetName;
		}

		public void setCharsetName(String charsetName) {
			this.charsetName = charsetName;
		}

		@Override
		protected Object execute() throws IOException {
			try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath, append),
					(charsetName != null) ? charsetName : Charset.defaultCharset().name()))) {
				bw.write(text);
			}
			return null;
		}
	}

	public static class WriteBinaryFileActivity extends SpecificWriteFileActivity {

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

		public void setFilePath(String filePath) {
			this.filePath = filePath;
		}

		public byte[] getData() {
			return data;
		}

		public void setData(byte[] data) {
			this.data = data;
		}

		public boolean isAppend() {
			return append;
		}

		public void setAppend(boolean append) {
			this.append = append;
		}

		@Override
		protected Object execute() throws IOException {
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
							throw new UnexpectedError();
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
			return new WriteFileActivity((SpecificWriteFileActivity) instanceBuilder.build(new InstantiationContext(
					context.getVariables(),
					context.getPlan().getValidationContext(context.getCurrentStep()).getVariableDeclarations())));
		}

		@Override
		public Class<?> getActivityResultClass(Plan currentPlan, Step currentStep) {
			return null;
		}

		@Override
		public Facade findInstantiationFunctionParentFacade(InstantiationFunction function) {
			return instanceBuilder.getFacade().findInstantiationFunctionParentFacade(function);
		}

		@Override
		public void validate(boolean recursively, Plan plan, Step step) throws ValidationError {
			if (mode == null) {
				throw new ValidationError("Mode not specified: Expected one of " + Arrays.toString(Mode.values()));
			}
			if (recursively) {
				if (instanceBuilder != null) {
					try {
						instanceBuilder.validate(recursively,
								plan.getValidationContext(step).getVariableDeclarations());
					} catch (ValidationError e) {
						throw new ValidationError("Failed to validate the input builder", e);
					}
				}
			}
		}
	}

}
