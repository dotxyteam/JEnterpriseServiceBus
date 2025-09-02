package com.otk.jesb.activation;

import java.text.SimpleDateFormat;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.SwingUtilities;

import com.otk.jesb.Session;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.activation.builtin.WatchFileSystem;
import com.otk.jesb.PotentialError;
import com.otk.jesb.resource.Resource;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.solution.StepCrossing;
import com.otk.jesb.ui.GUI;

public class Experiment extends Plan implements AutoCloseable {

	public static void main(String... args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try (Experiment experiment = new Experiment(new WatchFileSystem())) {
					GUI.INSTANCE.openObjectFrame(experiment);
				} catch (Exception e) {
					throw new UnexpectedError(e);
				}
			}
		});
	}

	private List<Resource> temporaryTestResources = new AbstractList<Resource>() {

		int size = 0;

		@Override
		public Resource get(int index) {
			return (Resource) Solution.INSTANCE.getContents().get(index);
		}

		@Override
		public Resource set(int index, Resource element) {
			return (Resource) Solution.INSTANCE.getContents().set(index, element);
		}

		@Override
		public void add(int index, Resource element) {
			Solution.INSTANCE.getContents().add(index, element);
			size++;
		}

		@Override
		public Resource remove(int index) {
			Resource result = (Resource) Solution.INSTANCE.getContents().remove(index);
			size--;
			return result;
		}

		@Override
		public int size() {
			return size;
		}
	};

	public Experiment(Activator activator) {
		setActivator(activator);
	}

	public List<Resource> getTemporaryTestResources() {
		return temporaryTestResources;
	}

	public void setTemporaryTestResources(List<Resource> temporaryTestResources) {
		this.temporaryTestResources = temporaryTestResources;
	}

	public ActivationEventBasket carryOut() throws Throwable {
		return new ActivationEventBasket();
	}

	@Override
	public void close() throws Exception {
		temporaryTestResources.clear();
	}

	public class ActivationEventBasket implements AutoCloseable {

		private Session session;
		private List<ActivationEvent> activationEvents = new ArrayList<ActivationEvent>();

		public ActivationEventBasket() throws Exception {
			session = Session.createDummySession();
			Experiment.this.getActivator().initializeAutomaticTrigger(getActivationHandler());
		}

		@Override
		public void close() throws Exception {
			try {
				Experiment.this.getActivator().finalizeAutomaticTrigger();
			} finally {
				session.close();
			}
		}

		private ActivationHandler getActivationHandler() {
			return new ActivationHandler() {
				@Override
				public Object trigger(Object planInput) {
					long eventTimestamp = System.currentTimeMillis();
					Object planOutput;
					try {
						planOutput = Experiment.this.execute(planInput, new Plan.ExecutionInspector() {

							@Override
							public void logWarning(String message) {
							}

							@Override
							public void logInformation(String message) {
							}

							@Override
							public void logError(String message) {
							}

							@Override
							public boolean isExecutionInterrupted() {
								return false;
							}

							@Override
							public void beforeOperation(StepCrossing stepCrossing) {
							}

							@Override
							public void afterOperation(StepCrossing stepCrossing) {
							}
						}, new Plan.ExecutionContext(session, Experiment.this));
					} catch (ExecutionError e) {
						throw new PotentialError(e);
					}
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
