package com.otk.jesb.compiler;

import com.otk.jesb.JESBError;

public class CompilationError extends JESBError {

	private static final long serialVersionUID = 1L;

	private int startPosition;
	private int endPosition;
	private String sourceFilePath;
	private String sourceCode;

	public CompilationError(int startPosition, int endPosition, String message, String sourceFilePath,
			String sourceCode, Throwable cause) {
		super(message, cause);
		this.startPosition = startPosition;
		this.endPosition = endPosition;
		this.sourceFilePath = sourceFilePath;
		this.sourceCode = sourceCode;
	}

	public CompilationError(int startPosition, int endPosition, String message, String sourceFilePath,
			String sourceCode) {
		this(startPosition, endPosition, message, sourceFilePath, sourceCode, null);
	}

	public int getStartPosition() {
		return startPosition;
	}

	public int getEndPosition() {
		return endPosition;
	}

	public String getSourceFilePath() {
		return sourceFilePath;
	}

	public String getSourceCode() {
		return sourceCode;
	}

}
