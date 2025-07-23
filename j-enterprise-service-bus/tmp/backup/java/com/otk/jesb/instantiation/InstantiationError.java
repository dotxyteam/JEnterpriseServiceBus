package com.otk.jesb.instantiation;

import com.otk.jesb.StandardError;

public class InstantiationError extends StandardError {

	private static final long serialVersionUID = 1L;

	public InstantiationError(String message) {
		super(message);
	}

	public InstantiationError(String message, Throwable cause) {
		super(message, cause);
	}

}
