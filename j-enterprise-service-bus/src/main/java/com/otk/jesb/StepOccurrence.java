package com.otk.jesb;

import com.otk.jesb.activity.ActivityResult;

public class StepOccurrence implements Plan.ExecutionContext.Property{

	private Step step;
	private ActivityResult activityResult;

	public StepOccurrence() {
	}

	public StepOccurrence(Step step, ActivityResult activityResult) {
		this.step = step;
		this.activityResult = activityResult;
	}

	public Step getStep() {
		return step;
	}

	public ActivityResult getActivityResult() {
		return activityResult;
	}

	@Override
	public Object getValue() {
		return activityResult;
	}

	@Override
	public String getName() {
		return step.getName();
	}

}
