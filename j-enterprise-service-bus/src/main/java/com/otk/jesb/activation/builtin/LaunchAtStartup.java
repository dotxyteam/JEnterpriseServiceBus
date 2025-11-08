package com.otk.jesb.activation.builtin;

import com.otk.jesb.activation.ActivationHandler;
import com.otk.jesb.activation.Activator;
import com.otk.jesb.activation.ActivatorMetadata;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.info.ResourcePath;

public class LaunchAtStartup extends Activator {

	private ActivationHandler activationHandler;
	private Thread thread;

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
		thread = new Thread("Worker[of=" + LaunchAtStartup.this + "]") {

			@Override
			public void run() {
				activationHandler.trigger(null);
			}

		};
		thread.start();
		this.activationHandler = activationHandler;
	}

	@Override
	public void finalizeAutomaticTrigger() throws Exception {
		while (thread.isAlive()) {
			thread.interrupt();
			MiscUtils.relieveCPU();
		}
		thread = null;
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
