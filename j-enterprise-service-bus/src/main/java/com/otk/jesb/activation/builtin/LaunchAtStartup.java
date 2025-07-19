package com.otk.jesb.activation.builtin;

import com.otk.jesb.ValidationError;
import com.otk.jesb.activation.ActivationHandler;
import com.otk.jesb.activation.ActivationStrategy;
import com.otk.jesb.activation.ActivationStrategyMetadata;
import com.otk.jesb.solution.Plan;

import xy.reflect.ui.info.ResourcePath;

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
		new Thread(LaunchAtStartup.class.getName() + "Executor[of=" + activationHandler + "]") {

			@Override
			public void run() {
				activationHandler.trigger(null);
			}

		}.start();
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

	public static class Metadata implements ActivationStrategyMetadata {

		@Override
		public ResourcePath getActivationStrategyIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(LaunchAtStartup.class.getName().replace(".", "/") + ".png"));
		}

		@Override
		public Class<? extends ActivationStrategy> getActivationStrategyClass() {
			return LaunchAtStartup.class;
		}

		@Override
		public String getActivationStrategyName() {
			return "Launch At Startup";
		}

	}
}
