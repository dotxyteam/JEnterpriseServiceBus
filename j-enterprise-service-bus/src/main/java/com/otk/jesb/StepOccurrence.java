package com.otk.jesb;

import java.util.List;

import com.otk.jesb.activity.Activity;

public class StepOccurrence implements Variable {

	private Step step;
	private Activity activity;
	private Object activityResult;
	private Throwable activityError;
	private List<Transition> validTransitions;

	public StepOccurrence() {
	}

	public StepOccurrence(Step step) {
		this.step = step;
	}

	public Step getStep() {
		return step;
	}

	public Object getActivityResult() {
		return activityResult;
	}

	public void setActivityResult(Object activityResult) {
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

	public List<Transition> getValidTransitions() {
		return validTransitions;
	}

	public void setValidTransitions(List<Transition> validTransitions) {
		this.validTransitions = validTransitions;
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
