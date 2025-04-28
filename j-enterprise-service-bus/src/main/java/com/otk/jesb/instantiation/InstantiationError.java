package com.otk.jesb.instantiation;

import com.otk.jesb.JESBError;

public class InstantiationError extends JESBError {

	private static final long serialVersionUID = 1L;

	public InstantiationError(String message) {
		super(message);
	}

	public InstantiationError(String message, Throwable cause) {
		super(message, cause);
	}

}
