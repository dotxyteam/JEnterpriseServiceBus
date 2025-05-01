package com.otk.jesb.solution;

import com.otk.jesb.VariableDeclaration;

public class StepEventuality implements VariableDeclaration {

	private Step step;
	private Plan currentPlan;

	public StepEventuality(Step step, Plan currentPlan) {
		this.step = step;
		this.currentPlan = currentPlan;
		}

	@Override
	public Class<?> getVariableType() {
		return step.getActivityBuilder().getActivityResultClass(currentPlan, step);
	}

	@Override
	public String getVariableName() {
		return step.getName();
	}
}
