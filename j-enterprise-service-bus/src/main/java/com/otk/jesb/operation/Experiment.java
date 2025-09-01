package com.otk.jesb.operation;

import java.util.AbstractList;
import java.util.List;

import javax.swing.SwingUtilities;

import com.otk.jesb.Session;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.operation.builtin.Evaluate;
import com.otk.jesb.resource.Resource;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.StepCrossing;
import com.otk.jesb.ui.GUI;

public class Experiment extends Plan implements AutoCloseable {

	public static void main(String... args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try (Experiment experiment = new Experiment(new Evaluate.Builder())) {
					GUI.INSTANCE.openObjectFrame(experiment);
				} catch (Exception e) {
					throw new UnexpectedError(e);
				}
			}
		});
	}

	private ExperimentalStep experimentalStep;
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

	public Experiment(OperationBuilder<?> operationBuilder) {
		experimentalStep = new ExperimentalStep(operationBuilder);
	}

	public ExperimentalStep getExperimentalStep() {
		return experimentalStep;
	}

	public List<Resource> getTemporaryTestResources() {
		return temporaryTestResources;
	}

	public void setTemporaryTestResources(List<Resource> temporaryTestResources) {
		this.temporaryTestResources = temporaryTestResources;
	}

	public Object carryOut() throws Throwable {
		OperationBuilder<?> operationBuilder = experimentalStep.getOperationBuilder();
		try (Session session = Session.createDummySession()) {
			Operation operation = operationBuilder.build(new Plan.ExecutionContext(session, this),
					new Plan.ExecutionInspector() {

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
					});
			return operation.execute();
		}
	}

	@Override
	public void close() throws Exception {
		temporaryTestResources.clear();
	}

	public static class ExperimentalStep extends Step {

		public ExperimentalStep(OperationBuilder<?> operationBuilder) {
			super(operationBuilder);
		}

	}

}
