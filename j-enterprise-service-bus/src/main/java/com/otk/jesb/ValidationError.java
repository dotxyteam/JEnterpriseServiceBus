package com.otk.jesb;

public class ValidationError extends JESBError {

	private static final long serialVersionUID = 1L;

	public ValidationError(String message, Throwable cause) {
		super(message, cause);
	}

	public ValidationError(String message) {
		super(message);
	}

}
