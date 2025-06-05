package com.otk.jesb.activation;

import com.otk.jesb.ValidationError;
import com.otk.jesb.solution.Plan;

public class StartupExecution implements Activation {

	@Override
	public void validate(boolean recursively, Plan plan) throws ValidationError {
	}

	@Override
	public Class<?> getInputClass() {
		return null;
	}

	@Override
	public Class<?> getOutputClass() {
		return null;
	}

}
