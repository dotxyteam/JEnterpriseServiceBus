package com.otk.jesb.instantiation;

import com.otk.jesb.ValidationContext;

public class CompilationContext {
	private ValidationContext validationContext;
	private Facade parentFacade;
	private Class<?> functionReturnType;

	public CompilationContext(ValidationContext validationContext, Facade parentFacade, Class<?> functionReturnType) {
		this.validationContext = validationContext;
		this.parentFacade = parentFacade;
		this.functionReturnType = functionReturnType;
	}

	public ValidationContext getValidationContext() {
		return validationContext;
	}

	public Facade getParentFacade() {
		return parentFacade;
	}

	public Class<?> getFunctionReturnType() {
		return functionReturnType;
	}

}