package com.otk.jesb.activity.builtin;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import com.otk.jesb.InstanceBuilder;
import com.otk.jesb.InstanceBuilder.DynamicValue;
import com.otk.jesb.Plan.ExecutionContext;
import com.otk.jesb.Plan.ValidationContext;
import com.otk.jesb.activity.Activity;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;
import com.otk.jesb.activity.ActivityResult;

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
	public ActivityResult execute() throws IOException {
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

		private InstanceBuilder objectSpecification = new InstanceBuilder(
				WriteFileActivity.class.getName());

		public InstanceBuilder getObjectSpecification() {
			return objectSpecification;
		}

		public void setObjectSpecification(InstanceBuilder objectSpecification) {
			this.objectSpecification = objectSpecification;
		}

		@Override
		public Activity build(ExecutionContext context) throws Exception {
			return (WriteFileActivity) objectSpecification.build(context);
		}

		@Override
		public Class<? extends ActivityResult> getActivityResultClass() {
			return null;
		}

		@Override
		public boolean completeValidationContext(ValidationContext validationContext,
				DynamicValue currentDynamicValue) {
			return objectSpecification.completeValidationContext(validationContext, currentDynamicValue);
		}

	}

}
