package com.otk.jesb;

public final class UnexpectedError extends AssertionError{

	private static final long serialVersionUID = 1L;

	public UnexpectedError() {
		super("Internal Error");
	}

	public UnexpectedError(Object detailMessage) {
		super(detailMessage);
	}

}
