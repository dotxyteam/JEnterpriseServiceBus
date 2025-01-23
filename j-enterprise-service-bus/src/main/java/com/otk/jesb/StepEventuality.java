package com.otk.jesb;

public class StepEventuality implements Plan.ValidationContext.VariableDeclaration {

	private Step step;

	public StepEventuality(Step step) {
		this.step = step;
	}

	@Override
	public Class<?> getVariableClass() {
		return step.getActivityBuilder().getActivityResultClass();
	}

	@Override
	public String getVariableName() {
		return step.getName();
	}
}
