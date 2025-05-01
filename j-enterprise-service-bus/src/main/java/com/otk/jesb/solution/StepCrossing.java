package com.otk.jesb.solution;

import java.util.List;

import com.otk.jesb.Variable;
import com.otk.jesb.activity.Activity;

public class StepCrossing extends StepOccurrence {

	private Activity activity;
	private Object activityResult;
	private Throwable activityError;
	private List<Transition> validTransitions;

	public StepCrossing(Step step, Plan plan) {
		super(step, plan);
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
		return (getPlan().getResultVariableDeclaration(getStep()) != null) ? activityResult : Variable.UNDEFINED_VALUE;
	}

	@Override
	public String toString() {
		return "[" + ((activityError == null) ? "OK" : "KO") + "] " + getStep().getName();
	}

}
