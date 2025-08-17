package com.otk.jesb;

public class PotentialError extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PotentialError(String message, Throwable cause) {
		super(message, cause);
	}

	public PotentialError(String message) {
		super(message);
	}

	public PotentialError(Throwable cause) {
		super(cause);
	}

	@Override
	public String toString() {
		return getMessage();
	}

}
