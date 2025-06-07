package com.otk.jesb.activation;

import com.otk.jesb.ValidationError;
import com.otk.jesb.solution.Plan;

public abstract class ActivationStrategy {

	public abstract void validate(boolean recursively, Plan plan) throws ValidationError;

	public abstract Class<?> getInputClass();

	public abstract Class<?> getOutputClass();

	public abstract void initializeAutomaticTrigger(ActivationHandler activationHandler) throws Exception;

	public abstract void finalizeAutomaticTrigger() throws Exception;

	public abstract boolean isAutomaticTriggerReady();

	public abstract boolean isAutomaticallyTriggerable();

	private boolean enabled = true;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
