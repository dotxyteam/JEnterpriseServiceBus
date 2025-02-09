package com.otk.jesb.instantiation;

import com.otk.jesb.Plan.ValidationContext;

public class VerificationContext {
	private ValidationContext validationContext;
	private Facade parentFacade;

	public VerificationContext(ValidationContext validationContext, Facade parentFacade) {
		this.validationContext = validationContext;
		this.parentFacade = parentFacade;
	}

	public ValidationContext getValidationContext() {
		return validationContext;
	}

	public Facade getParentFacade() {
		return parentFacade;
	}

}