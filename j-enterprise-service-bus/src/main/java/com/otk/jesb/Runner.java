package com.otk.jesb;

import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.solution.StepCrossing;

/**
 * This class allows to run optimally a {@link Solution} in a production
 * environment.
 * 
 * @author olitank
 *
 */
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
			Log.get().err(e);
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

			};
		}

	}

}
