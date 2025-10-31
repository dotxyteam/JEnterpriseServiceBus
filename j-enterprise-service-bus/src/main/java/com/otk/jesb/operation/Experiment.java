package com.otk.jesb.operation;

import javax.swing.SwingUtilities;

import com.otk.jesb.AbstractExperiment;
import com.otk.jesb.Session;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.operation.builtin.Evaluate;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;
import com.otk.jesb.ui.GUI;

public class Experiment extends AbstractExperiment implements AutoCloseable {

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

	public Experiment(OperationBuilder<?> operationBuilder) {
		experimentalStep = new ExperimentalStep(operationBuilder);
	}

	public ExperimentalStep getExperimentalStep() {
		return experimentalStep;
	}

	public Object carryOut() throws Throwable {
		OperationBuilder<?> operationBuilder = experimentalStep.getOperationBuilder();
		try (Session session = Session.openDummySession()) {
			Operation operation = operationBuilder.build(new Plan.ExecutionContext(session, this),
					ExecutionInspector.DEFAULT);
			return operation.execute();
		}
	}

	public static class ExperimentalStep extends Step {

		public ExperimentalStep(OperationBuilder<?> operationBuilder) {
			super(operationBuilder);
		}

	}

}
