package com.otk.jesb;

import java.util.List;

import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.solution.StepCrossing;
import com.otk.jesb.solution.Transition;

/**
 * This class allows to run optimally a {@link Solution} in a production
 * environment.
 * 
 * @author olitank
 *
 */
public class Runner extends Debugger {

	public Runner(Solution solution) {
		this(solution, true);
	}

	public Runner(Solution solution, boolean sessionOpen) {
		super(solution, sessionOpen);
	}

	@Override
	protected void initiate() {
		activatePlans();
	}

	@Override
	protected void terminate() {
		deactivatePlans();
		stopExecutions();
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
		public List<PlanExecutor> getPlanExecutors() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected PlanExecutor createPlanExecutor(Object planInput) {
			return new RuntimePlanExecutor(getPlan(), planInput);
		}

		@Override
		protected void handleActivationError(Exception e) {
			Log.get().error(e);
		}

		@Override
		public String toString() {
			String result = "";
			if (isAutomaticallyTriggerable()) {
				if (isAutomaticTriggerReady()) {
					result += "*";
				}
			}
			result += getPlan().getName();
			return result;
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

		@Override
		public List<StepCrossing> getStepCrossings() {
			throw new UnsupportedOperationException();
		}

		@Override
		public StepCrossing getCurrentStepCrossing() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getTransitionOccurrenceCount(Transition transition) {
			throw new UnsupportedOperationException();
		}

	}

}
