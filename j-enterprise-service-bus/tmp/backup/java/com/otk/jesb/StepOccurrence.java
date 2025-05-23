package com.otk.jesb;

import com.otk.jesb.operation.Operation;

public class StepCrossing implements Variable {

	private Step step;
	private Operation operation;
	private Object operationResult;
	private Throwable operationError;

	public StepCrossing() {
	}

	public StepCrossing(Step step) {
		this.step = step;
	}

	public Step getStep() {
		return step;
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

	@Override
	public Object getValue() {
		return operationResult;
	}

	@Override
	public String getName() {
		return step.getName();
	}

	@Override
	public String toString() {
		return "[" + ((operationError == null) ? "OK" : "KO") + "] " + step.getName();
	}

}
