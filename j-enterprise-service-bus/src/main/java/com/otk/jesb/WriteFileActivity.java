package com.otk.jesb;

import java.io.FileWriter;
import java.io.IOException;

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
	public Result execute() throws IOException {
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

}
