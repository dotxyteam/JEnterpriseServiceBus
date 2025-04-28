package com.otk.jesb.activity;

public class ActivityError extends Exception {

	private static final long serialVersionUID = 1L;

	public ActivityError(String message) {
		super(message);
	}

	public ActivityError(String message, Throwable cause) {
		super(message, cause);
	}

}
