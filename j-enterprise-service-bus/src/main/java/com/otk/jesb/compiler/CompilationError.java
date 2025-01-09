package com.otk.jesb.compiler;

public class CompilationError extends Exception {

	private static final long serialVersionUID = 1L;

	private int startPosition;
	private int endPosition;

	public CompilationError(int startPosition, int endPosition, String message) {
		super(message);
		this.startPosition = startPosition;
		this.endPosition = endPosition;
	}

	public int getStartPosition() {
		return startPosition;
	}

	public int getEndPosition() {
		return endPosition;
	}

}
