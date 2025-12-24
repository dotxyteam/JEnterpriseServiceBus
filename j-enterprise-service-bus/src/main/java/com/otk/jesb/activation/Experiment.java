package com.otk.jesb.activation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.SwingUtilities;

import com.otk.jesb.Session;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.activation.builtin.WatchFileSystem;
import com.otk.jesb.AbstractExperiment;
import com.otk.jesb.JESB;
import com.otk.jesb.PotentialError;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Solution;

public class Experiment extends AbstractExperiment implements AutoCloseable {

	public static void main(String... args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try (Experiment experiment = new Experiment(new WatchFileSystem(), new Solution())) {
					JESB.UI.INSTANCE.openObjectFrame(experiment);
				} catch (Exception e) {
					throw new UnexpectedError(e);
				}
			}
		});
	}

	public Experiment(Activator activator, Solution solutionInstance) {
		super(solutionInstance);
		setActivator(activator);
	}

	public ActivationEventBasket carryOut() throws Throwable {
		return new ActivationEventBasket();
	}

	public class ActivationEventBasket extends Session {

		public ActivationEventBasket() {
			super(new Solution());
		}

		private List<ActivationEvent> activationEvents = new ArrayList<ActivationEvent>();

		@Override
		protected void initiate() {
			try {
				Experiment.this.getActivator().initializeAutomaticTrigger(getActivationHandler(), solutionInstance);
			} catch (Exception e) {
				throw new PotentialError(e);
			}
		}

		@Override
		protected void terminate() {
			try {
				Experiment.this.getActivator().finalizeAutomaticTrigger(solutionInstance);
			} catch (Exception e) {
				throw new PotentialError(e);
			}
		}

		private ActivationHandler getActivationHandler() {
			return new ActivationHandler() {
				@Override
				public Object trigger(Object planInput) throws ExecutionError {
					long eventTimestamp = System.currentTimeMillis();
					Object planOutput = Experiment.this.execute(planInput, Plan.ExecutionInspector.DEFAULT,
							new Plan.ExecutionContext(ActivationEventBasket.this, Experiment.this));
					activationEvents.add(new ActivationEvent(eventTimestamp, planInput, planOutput));
					return planOutput;
				}
			};
		}

		public List<ActivationEvent> getActivationEvents() {
			return activationEvents;
		}

	}

	public static class ActivationEvent {

		private long timestamp;
		private Object input;
		private Object output;

		public ActivationEvent(long timestamp, Object input, Object output) {
			this.timestamp = timestamp;
			this.input = input;
			this.output = output;
		}

		public String getFormattedTimestamp() {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(timestamp));
		}

		public Object getInput() {
			return input;
		}

		public Object getOutput() {
			return output;
		}

	}

}
