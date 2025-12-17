package com.otk.jesb.solution;

import com.otk.jesb.Variable;

public class StepSkipping extends StepOccurrence {

	public StepSkipping(Step step, Plan plan, Solution solutionInstance) {
		super(step, plan, solutionInstance);
	}

	@Override
	public Object getValue() {
		return (getPlan().getResultVariableDeclaration(getStep(), getSolutionInstance()) != null) ? null
				: Variable.UNDEFINED_VALUE;
	}

	@Override
	public String toString() {
		return "[SKIPPED] " + getStep().getName();
	}

}
