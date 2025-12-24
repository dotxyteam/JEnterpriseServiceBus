package com.otk.jesb.operation;

import javax.swing.SwingUtilities;

import com.otk.jesb.AbstractExperiment;
import com.otk.jesb.JESB;
import com.otk.jesb.Session;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.operation.builtin.Evaluate;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.solution.Step;

public class Experiment extends AbstractExperiment implements AutoCloseable {

	public static void main(String... args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try (Experiment experiment = new Experiment(new Evaluate.Builder(), new Solution())) {
					JESB.UI.INSTANCE.openObjectFrame(experiment);
				} catch (Exception e) {
					throw new UnexpectedError(e);
				}
			}
		});
	}

	private ExperimentalStep experimentalStep;

	public Experiment(OperationBuilder<?> operationBuilder, Solution solutionInstance) {
		super(solutionInstance);
		experimentalStep = new ExperimentalStep(operationBuilder);
	}

	public ExperimentalStep getExperimentalStep() {
		return experimentalStep;
	}

	public Object carryOut(Solution solutionInstance) throws Throwable {
		OperationBuilder<?> operationBuilder = experimentalStep.getOperationBuilder();
		try (Session session = Session.openDummySession(solutionInstance)) {
			Operation operation = operationBuilder.build(new Plan.ExecutionContext(session, this),
					ExecutionInspector.DEFAULT);
			return operation.execute(solutionInstance);
		}
	}

	public static class ExperimentalStep extends Step {

		public ExperimentalStep(OperationBuilder<?> operationBuilder) {
			super(operationBuilder);
		}

	}

}
