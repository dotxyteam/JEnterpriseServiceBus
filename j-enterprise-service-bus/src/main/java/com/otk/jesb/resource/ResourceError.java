package com.otk.jesb.resource;

import com.otk.jesb.JESBError;

public class ResourceError extends JESBError {

	private static final long serialVersionUID = 1L;

	public ResourceError(String message) {
		super(message);
	}

	public ResourceError(String message, Throwable cause) {
		super(message, cause);
	}

}
