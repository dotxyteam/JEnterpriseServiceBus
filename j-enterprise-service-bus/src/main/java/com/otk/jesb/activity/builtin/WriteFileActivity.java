package com.otk.jesb.activity.builtin;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Collections;

import com.otk.jesb.InstanceBuilder;
import com.otk.jesb.InstanceBuilder.Function;
import com.otk.jesb.InstanceBuilder.VerificationContext;
import com.otk.jesb.Plan.ExecutionContext;
import com.otk.jesb.activity.Activity;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;
import xy.reflect.ui.info.ResourcePath;

public class WriteFileActivity implements Activity {

	private String filePath;
	private String text;
	private boolean append = false;
	private String charsetName;

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
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

	public void setText(String text) {
		this.text = text;
	}

	public boolean isAppend() {
		return append;
	}

	public void setAppend(boolean append) {
		this.append = append;
	}

	@Override
	public Object execute() throws IOException {
		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath, append),
				(charsetName != null) ? charsetName : Charset.defaultCharset().name()))) {
			bw.write(text);
		}
		return null;
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

		private InstanceBuilder instanceBuilder = new InstanceBuilder(WriteFileActivity.class.getName());

		public InstanceBuilder getInstanceBuilder() {
			return instanceBuilder;
		}

		public void setInstanceBuilder(InstanceBuilder instanceBuilder) {
			this.instanceBuilder = instanceBuilder;
		}

		@Override
		public Activity build(ExecutionContext context) throws Exception {
			return (WriteFileActivity) instanceBuilder
					.build(new InstanceBuilder.EvaluationContext(context, Collections.emptyList()));
		}

		@Override
		public Class<?> getActivityResultClass() {
			return null;
		}

		@Override
		public boolean completeVerificationContext(VerificationContext verificationContext, Function currentFunction) {
			return instanceBuilder.completeVerificationContext(verificationContext, currentFunction);
		}

	}

}
