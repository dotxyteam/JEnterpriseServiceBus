package com.otk.jesb.activation.builtin;

import com.otk.jesb.ValidationError;
import com.otk.jesb.activation.ActivationHandler;
import com.otk.jesb.activation.Activator;
import com.otk.jesb.activation.ActivatorMetadata;
import com.otk.jesb.solution.Plan;

import xy.reflect.ui.info.ResourcePath;

public class LaunchAtStartup extends Activator {

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

	public static class Metadata implements ActivatorMetadata {

		@Override
		public ResourcePath getActivatorIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(LaunchAtStartup.class.getName().replace(".", "/") + ".png"));
		}

		@Override
		public Class<? extends Activator> getActivatorClass() {
			return LaunchAtStartup.class;
		}

		@Override
		public String getActivatorName() {
			return "Launch At Startup";
		}

	}
}
