package com.otk.jesb.activation;

import com.otk.jesb.ValidationError;
import com.otk.jesb.solution.Plan;

public interface ActivatorStructure {
	
	public abstract void validate(boolean recursively, Plan plan) throws ValidationError;

}
