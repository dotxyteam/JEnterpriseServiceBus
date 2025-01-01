package com.otk.jesb.activity.builtin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.otk.jesb.InstanceSpecification;
import com.otk.jesb.InstanceSpecification.DynamicValue;
import com.otk.jesb.Plan.ExecutionContext;
import com.otk.jesb.Plan.ValidationContext;
import com.otk.jesb.activity.Activity;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;
import com.otk.jesb.activity.ActivityResult;

import xy.reflect.ui.info.ResourcePath;

public class ReadFileActivity implements Activity {

	private String filePath;
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

	@Override
	public ActivityResult execute() throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try (FileInputStream in = new FileInputStream(new File(filePath))) {
			int bytesRead;
			byte[] data = new byte[1024];
			while ((bytesRead = in.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, bytesRead);
			}
		}
		return new Result(new String(buffer.toByteArray(), charsetName));
	}

	public static class Metadata implements ActivityMetadata {

		@Override
		public String getActivityTypeName() {
			return "Read File";
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

		private InstanceSpecification objectSpecification = new InstanceSpecification(ReadFileActivity.class.getName());

		public InstanceSpecification getObjectSpecification() {
			return objectSpecification;
		}

		public void setObjectSpecification(InstanceSpecification objectSpecification) {
			this.objectSpecification = objectSpecification;
		}

		@Override
		public Activity build(ExecutionContext context) throws Exception {
			return (ReadFileActivity) objectSpecification.build(context);
		}

		@Override
		public Class<? extends ActivityResult> getActivityResultClass() {
			return Result.class;
		}

		@Override
		public boolean completeValidationContext(ValidationContext validationContext,
				DynamicValue currentDynamicValue) {
			return objectSpecification.completeValidationContext(validationContext, currentDynamicValue);
		}

	}

	public static class Result implements ActivityResult {

		private String text;

		public Result(String text) {
			this.text = text;
		}

		public String getText() {
			return text;
		}

	}

}
