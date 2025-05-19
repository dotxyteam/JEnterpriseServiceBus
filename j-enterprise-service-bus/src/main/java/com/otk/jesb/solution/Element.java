package com.otk.jesb.solution;

import com.otk.jesb.ValidationError;

public abstract class Element {
	public abstract void validate(boolean recursively, Plan plan) throws ValidationError;

	public abstract String getSummary();
}
