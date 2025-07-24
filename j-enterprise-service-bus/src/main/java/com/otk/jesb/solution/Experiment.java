package com.otk.jesb.solution;

import javax.swing.SwingUtilities;

import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.builtin.Evaluate;
import com.otk.jesb.ui.GUI;

public class Experiment extends Plan {

	public static void main(String... args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				GUI.INSTANCE.openObjectFrame(new Experiment(new Evaluate.Builder()));
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
		Operation operation = operationBuilder.build(new Plan.ExecutionContext(this), new Plan.ExecutionInspector() {

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

	public static class ExperimentalStep extends Step {

		public ExperimentalStep(OperationBuilder<?> operationBuilder) {
			super(operationBuilder);
		}

	}

}
