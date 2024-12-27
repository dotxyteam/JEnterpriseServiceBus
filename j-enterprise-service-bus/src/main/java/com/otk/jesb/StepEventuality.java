package com.otk.jesb;

import com.otk.jesb.Plan.ValidationContext;

public class StepEventuality implements ValidationContext.Declaration {

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
