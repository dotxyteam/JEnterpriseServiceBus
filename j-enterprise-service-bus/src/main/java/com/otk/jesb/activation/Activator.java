package com.otk.jesb.activation;

import com.otk.jesb.ValidationError;
import com.otk.jesb.Variant;
import com.otk.jesb.solution.Plan;

public abstract class Activator {

	public abstract void validate(boolean recursively, Plan plan) throws ValidationError;

	public abstract Class<?> getInputClass();

	public abstract Class<?> getOutputClass();

	public abstract void initializeAutomaticTrigger(ActivationHandler activationHandler) throws Exception;

	public abstract void finalizeAutomaticTrigger() throws Exception;

	public abstract boolean isAutomaticTriggerReady();

	public abstract boolean isAutomaticallyTriggerable();

	private Variant<Boolean> enabledVariant = new Variant<Boolean>(Boolean.class, true);

	public Variant<Boolean> getEnabledVariant() {
		return enabledVariant;
	}

	public void setEnabledVariant(Variant<Boolean> enabledVariant) {
		this.enabledVariant = enabledVariant;
	}

}
