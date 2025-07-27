package com.otk.jesb;

public final class UnexpectedError extends AssertionError{

	private static final long serialVersionUID = 1L;

	public UnexpectedError() {
		super("Internal Error");
	}

	public UnexpectedError(Object message) {
		super(message);
	}

	public UnexpectedError(String message, Throwable cause) {
		super(message, cause);
	}

}
