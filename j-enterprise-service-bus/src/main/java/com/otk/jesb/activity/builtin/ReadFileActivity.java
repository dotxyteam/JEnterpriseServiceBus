package com.otk.jesb.activity.builtin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import com.otk.jesb.InstanceBuilder;
import com.otk.jesb.InstanceBuilder.Function;
import com.otk.jesb.Plan.ExecutionContext;
import com.otk.jesb.Plan.ValidationContext;
import com.otk.jesb.activity.Activity;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;
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
	public Object execute() throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try (FileInputStream in = new FileInputStream(new File(filePath))) {
			int bytesRead;
			byte[] data = new byte[1024];
			while ((bytesRead = in.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, bytesRead);
			}
		}
		return new Result(new String(buffer.toByteArray(),
				(charsetName != null) ? charsetName : Charset.defaultCharset().name()));
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

		private InstanceBuilder instanceBuilder = new InstanceBuilder(ReadFileActivity.class.getName());

		public InstanceBuilder getInstanceBuilder() {
			return instanceBuilder;
		}

		public void setInstanceBuilder(InstanceBuilder instanceBuilder) {
			this.instanceBuilder = instanceBuilder;
		}

		@Override
		public Activity build(ExecutionContext context) throws Exception {
			return (ReadFileActivity) instanceBuilder.build(context);
		}

		@Override
		public Class<?> getActivityResultClass() {
			return Result.class;
		}

		@Override
		public boolean completeValidationContext(ValidationContext validationContext,
				Function currentFunction) {
			return instanceBuilder.completeValidationContext(validationContext, currentFunction);
		}

	}

	public static class Result {

		private String text;

		public Result(String text) {
			this.text = text;
		}

		public String getText() {
			return text;
		}

	}

}
