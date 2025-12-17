package com.otk.jesb.solution;

import java.util.List;

import com.otk.jesb.Variable;
import com.otk.jesb.operation.Operation;

public class StepCrossing extends StepOccurrence {

	private Operation operation;
	private Object operationResult;
	private Throwable operationError;
	private List<Transition> validTransitions;
	private Solution solutionInstance;

	public StepCrossing(Step step, Plan plan, Solution solutionInstance) {
		super(step, plan, solutionInstance);
		this.solutionInstance = solutionInstance;
	}

	public Object getOperationResult() {
		return operationResult;
	}

	public void setOperationResult(Object operationResult) {
		this.operationResult = operationResult;
	}

	public Operation getOperation() {
		return operation;
	}

	public void setOperation(Operation operation) {
		this.operation = operation;
	}

	public Throwable getOperationError() {
		return operationError;
	}

	public void setOperationError(Throwable operationError) {
		this.operationError = operationError;
	}

	public List<Transition> getValidTransitions() {
		return validTransitions;
	}

	public void setValidTransitions(List<Transition> validTransitions) {
		this.validTransitions = validTransitions;
	}

	@Override
	public Object getValue() {
		return (getPlan().getResultVariableDeclaration(getStep(), solutionInstance) != null) ? operationResult
				: Variable.UNDEFINED_VALUE;
	}

	@Override
	public String toString() {
		return "[" + ((operationError == null) ? "OK" : "KO") + "] " + getStep().getName();
	}

}
