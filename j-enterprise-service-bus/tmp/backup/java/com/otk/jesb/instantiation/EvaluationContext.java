package com.otk.jesb.instantiation;

import com.otk.jesb.solution.Plan.ExecutionContext;

public class EvaluationContext {

	private ExecutionContext executionContext;
	private Facade parentFacade;

	public EvaluationContext(ExecutionContext executionContext, Facade parentFacade) {
		this.executionContext = executionContext;
		this.parentFacade = parentFacade;
	}

	public ExecutionContext getExecutionContext() {
		return executionContext;
	}

	public Facade getParentFacade() {
		return parentFacade;
	}

}