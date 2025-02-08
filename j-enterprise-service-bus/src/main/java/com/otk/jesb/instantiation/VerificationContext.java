package com.otk.jesb.instantiation;

import com.otk.jesb.Plan.ValidationContext;

public class VerificationContext {
	private ValidationContext validationContext;
	private Facade currentFacade;

	public VerificationContext(ValidationContext validationContext, Facade currentFacade) {
		this.validationContext = validationContext;
		this.currentFacade = currentFacade;
	}

	public ValidationContext getValidationContext() {
		return validationContext;
	}

	public Facade getCurrentFacade() {
		return currentFacade;
	}

}