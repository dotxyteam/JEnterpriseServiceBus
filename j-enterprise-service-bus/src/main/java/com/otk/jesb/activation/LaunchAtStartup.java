package com.otk.jesb.activation;

import com.otk.jesb.ValidationError;
import com.otk.jesb.solution.Plan;

public class LaunchAtStartup extends ActivationStrategy {

	private ActivationHandler activationHandler;

	@Override
	public Class<?> getInputClass() {
		return null;
	}

	@Override
	public Class<?> getOutputClass() {
		return null;
	}

	@Override
	public void initializeAutomaticTrigger(ActivationHandler activationHandler) throws Exception {
		this.activationHandler = activationHandler;
		activationHandler.trigger(null);
	}

	@Override
	public void finalizeAutomaticTrigger() throws Exception {
		this.activationHandler = null;
	}

	@Override
	public boolean isAutomaticTriggerReady() {
		return activationHandler != null;
	}

	@Override
	public boolean isAutomaticallyTriggerable() {
		return true;
	}

	@Override
	public void validate(boolean recursively, Plan plan) throws ValidationError {
	}
}
