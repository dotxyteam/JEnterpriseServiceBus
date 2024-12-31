package com.otk.jesb;

import com.otk.jesb.activity.Activity;
import com.otk.jesb.activity.ActivityResult;

public class StepOccurrence implements Plan.ExecutionContext.Property {

	private Step step;
	private Activity activity;
	private ActivityResult activityResult;
	private Throwable activityError;

	public StepOccurrence() {
	}

	public StepOccurrence(Step step) {
		this.step = step;
	}

	public Step getStep() {
		return step;
	}

	public ActivityResult getActivityResult() {
		return activityResult;
	}

	public void setActivityResult(ActivityResult activityResult) {
		this.activityResult = activityResult;
	}

	public Activity getActivity() {
		return activity;
	}

	public void setActivity(Activity activity) {
		this.activity = activity;
	}

	public Throwable getActivityError() {
		return activityError;
	}

	public void setActivityError(Throwable activityError) {
		this.activityError = activityError;
	}

	@Override
	public Object getValue() {
		return activityResult;
	}

	@Override
	public String getName() {
		return step.getName();
	}

	@Override
	public String toString() {
		return "[" + ((activityError == null) ? "OK" : "KO") + "] " + step.getName();
	}

}
