package com.otk.jesb.util;

public class ScriptValidationError extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private int startPosition;
	private int endPosition;

	public ScriptValidationError(int startPosition, int endPosition, String message) {
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
