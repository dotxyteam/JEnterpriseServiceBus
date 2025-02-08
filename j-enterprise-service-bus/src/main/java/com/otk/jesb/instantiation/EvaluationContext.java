package com.otk.jesb.instantiation;

import com.otk.jesb.Plan.ExecutionContext;

public class EvaluationContext {

	private ExecutionContext executionContext;
	private Facade currentFacade;

	public EvaluationContext(ExecutionContext executionContext, Facade currentFacade) {
		this.executionContext = executionContext;
		this.currentFacade = currentFacade;
	}

	public ExecutionContext getExecutionContext() {
		return executionContext;
	}

	public Facade getCurrentFacade() {
		return currentFacade;
	}

}