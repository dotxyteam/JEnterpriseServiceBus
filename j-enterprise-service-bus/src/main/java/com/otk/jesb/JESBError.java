package com.otk.jesb;

public class JESBError extends Exception {

	private static final long serialVersionUID = 1L;

	public JESBError() {
		super();
	}

	public JESBError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public JESBError(String message, Throwable cause) {
		super(message, cause);
	}

	public JESBError(String message) {
		super(message);
	}

	public JESBError(Throwable cause) {
		super(cause);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + getMessage();
	}

}
