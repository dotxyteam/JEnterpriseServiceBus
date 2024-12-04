package com.otk.jesb;

import java.io.FileWriter;
import java.io.IOException;

import com.otk.jesb.Plan.ExecutionContext;

public class WriteFileActivity implements Activity {

	private String filePath;
	private String text;
	private boolean append;

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

	@Override
	public ActivityResult execute() throws IOException {
		FileWriter fw = new FileWriter(filePath, append);
		try {
			fw.write(text);
		} finally {
			try {
				fw.close();
			} catch (IOException ignore) {
			}
		}
		return null;
	}

	public static class Builder implements ActivityBuilder {

		private ObjectSpecification objectSpecification = new ObjectSpecification(WriteFileActivity.class.getName());

		public ObjectSpecification getObjectSpecification() {
			return objectSpecification;
		}

		public void setObjectSpecification(ObjectSpecification objectSpecification) {
			this.objectSpecification = objectSpecification;
		}

		@Override
		public Activity build(ExecutionContext context) throws Exception {
			return (WriteFileActivity)objectSpecification.build(context);
		}

	}

}
