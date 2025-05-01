package com.otk.jesb;

public class StepSkipping extends StepOccurrence {

	public StepSkipping(Step step, Plan plan) {
		super(step, plan);
	}

	@Override
	public Object getValue() {
		return (getPlan().getResultVariableDeclaration(getStep()) != null) ? null : Variable.UNDEFINED_VALUE;
	}

	@Override
	public String toString() {
		return "[SKIPPED] " + getStep().getName();
	}

}
