package com.otk.jesb.compiler;

public class CompilationError extends Exception {

	private static final long serialVersionUID = 1L;

	private int startPosition;
	private int endPosition;
	private String sourceCode;

	public CompilationError(int startPosition, int endPosition, String message, String sourceCode, Throwable cause) {
		super(message, cause);
		this.startPosition = startPosition;
		this.endPosition = endPosition;
		this.sourceCode = sourceCode;
	}

	public CompilationError(int startPosition, int endPosition, String message, String sourceCode) {
		this(startPosition, endPosition, message, sourceCode, null);
	}

	public int getStartPosition() {
		return startPosition;
	}

	public int getEndPosition() {
		return endPosition;
	}

	public String getSourceCode() {
		return sourceCode;
	}

}
