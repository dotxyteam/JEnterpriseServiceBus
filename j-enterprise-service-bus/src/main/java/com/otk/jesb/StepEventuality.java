package com.otk.jesb;

public class StepEventuality implements Plan.ValidationContext.Declaration {

	private Step step;

	public StepEventuality(Step step) {
		this.step = step;
	}

	@Override
	public Class<?> getPropertyClass() {
		return step.getActivityBuilder().getActivityResultClass();
	}

	@Override
	public String getPropertyName() {
		return step.getName();
	}
}
