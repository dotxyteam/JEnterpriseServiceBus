package com.otk.jesb;

public abstract class StandardError extends Exception {

	private static final long serialVersionUID = 1L;

	public StandardError() {
		super();
	}

	public StandardError(String message, Throwable cause) {
		super(message, cause);
	}

	public StandardError(String message) {
		super(message);
	}

	public StandardError(Throwable cause) {
		super(cause);
	}

}
