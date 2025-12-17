package com.otk.jesb.solution;

import com.otk.jesb.ValidationError;

public abstract class PlanElement {
	public abstract void validate(boolean recursively, Solution solutionInstance, Plan plan) throws ValidationError;

	public abstract String getSummary();
}
