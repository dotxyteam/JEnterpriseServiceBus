package com.otk.jesb;

import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.solution.StepCrossing;

public class Runner extends Debugger {

	
	public Runner(Solution solution) {
		super(solution);
	}

	

	

	@Override
	protected PlanActivation createPlanActivation(Plan plan) {
		return new RuntimePlanActivation(plan);
	}

	protected class RuntimePlanActivation extends PlanActivation {

		public RuntimePlanActivation(Plan plan) {
			super(plan);
		}

		@Override
		protected PlanExecutor createPlanExecutor(Object planInput) {
			return new RuntimePlanExecutor(getPlan(), planInput);
		}

		@Override
		protected void handleActivationError(Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

	protected class RuntimePlanExecutor extends PlanExecutor {

		public RuntimePlanExecutor(Plan plan, Object planInput) {
			super(plan, planInput);
		}

		@Override
		protected ExecutionInspector createExecutionInspector() {
			return new Plan.ExecutionInspector() {

				@Override
				public void beforeOperation(StepCrossing stepCrossing) {
				}

				@Override
				public void afterOperation(StepCrossing stepCrossing) {
				}

				@Override
				public boolean isExecutionInterrupted() {
					return Thread.currentThread().isInterrupted();
				}

				@Override
				public void logInformation(String message) {
					Log.INSTANCE.info(message);
				}

				@Override
				public void logWarning(String message) {
					Log.INSTANCE.warn(message);
				}

				@Override
				public void logError(String message) {
					Log.INSTANCE.error(message);
				}

			};
		}

	}

	

}
