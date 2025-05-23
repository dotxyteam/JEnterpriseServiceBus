package com.otk.jesb;

public class StepEventuality implements VariableDeclaration {

	private Step step;

	public StepEventuality(Step step) {
		this.step = step;
	}

	@Override
	public Class<?> getVariableType() {
		return step.getOperationBuilder().getOperationResultClass();
	}

	@Override
	public String getVariableName() {
		return step.getName();
	}
}
