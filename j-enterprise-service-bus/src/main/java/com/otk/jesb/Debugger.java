package com.otk.jesb;

import java.util.ArrayList;
import java.util.List;

public class Debugger {

	private Solution solution;
	private List<PlanExecutor> planExecutors;

	public Debugger(Solution solution) {
		this.solution = solution;
		planExecutors = collectPlanExecutors();
	}

	public List<PlanExecutor> getPlanExecutors() {
		return planExecutors;
	}

	private List<PlanExecutor> collectPlanExecutors() {
		final List<PlanExecutor> result = new ArrayList<Debugger.PlanExecutor>();
		solution.visitAssets(new AssetVisitor() {
			@Override
			public boolean visitAsset(Asset asset) {
				if (asset instanceof Plan) {
					result.add(new PlanExecutor((Plan) asset));
				}
				return true;
			}
		});
		return result;
	}

	public static class PlanExecutor {

		private Plan plan;
		private List<StepOccurrence> stepOccurrences = new ArrayList<StepOccurrence>();
		private Throwable executionError;
		private Thread thread;
		
		public PlanExecutor(Plan plan) {
			this.plan = plan;
		}

		public List<StepOccurrence> getStepOccurrences() {
			return stepOccurrences;
		}

		public Throwable getExecutionError() {
			return executionError;
		}

		public void start() {
			if (isActive()) {
				return;
			}
			stepOccurrences.clear();
			executionError = null;
			thread = new Thread("PlanExecutor [plan=" + plan.getName() + "]") {

				@Override
				public void run() {
					try {
						plan.execute(new Plan.ExecutionInspector() {
							@Override
							public void beforeActivityExecution(StepOccurrence stepOccurrence) {
								stepOccurrences.add(stepOccurrence);
							}

							@Override
							public void afterActivityExecution(StepOccurrence stepOccurrence) {
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									throw new AssertionError(e);
								}
							}
						});
					} catch (Throwable t) {
						executionError = t;
					}
				}

			};
			thread.start();
		}

		public boolean isActive() {
			if (thread != null) {
				if (thread.isAlive()) {
					return true;
				}
			}
			return false;
		}
	}

}
