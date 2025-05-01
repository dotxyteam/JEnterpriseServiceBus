package com.otk.jesb;

import java.util.ArrayList;
import java.util.List;

public class Debugger {

	private Solution solution;
	private List<PlanActivator> planActivators;

	public Debugger(Solution solution) {
		this.solution = solution;
		planActivators = collectPlanActivators();
	}

	public List<PlanActivator> getPlanActivators() {
		return planActivators;
	}

	private List<PlanActivator> collectPlanActivators() {
		final List<PlanActivator> result = new ArrayList<Debugger.PlanActivator>();
		solution.visitAssets(new AssetVisitor() {
			@Override
			public boolean visitAsset(Asset asset) {
				if (asset instanceof Plan) {
					result.add(new PlanActivator((Plan) asset));
				}
				return true;
			}
		});
		return result;
	}

	public static class PlanActivator {

		private Plan plan;
		private List<PlanExecutor> planExecutors = new ArrayList<Debugger.PlanExecutor>();

		public PlanActivator(Plan plan) {
			this.plan = plan;
		}

		public Plan getPlan() {
			return plan;
		}

		public List<PlanExecutor> getPlanExecutors() {
			return planExecutors;
		}

		public void executePlan() {
			planExecutors.add(new PlanExecutor(plan));
		}

		@Override
		public String toString() {
			return "(" + planExecutors.size() + ") " + plan.getName();
		}
	}

	public static class PlanExecutor {

		private Plan plan;
		private List<StepCrossing> stepCrossings = new ArrayList<StepCrossing>();
		private StepCrossing currentStepCrossing;
		private Throwable executionError;
		private Thread thread;

		public PlanExecutor(Plan plan) {
			this.plan = plan;
			thread = new Thread("PlanExecutor [plan=" + plan.getName() + "]") {

				@Override
				public void run() {
					try {
						plan.execute(null, new Plan.ExecutionInspector() {
							@Override
							public void beforeActivityCreation(StepCrossing stepCrossing) {
								currentStepCrossing = stepCrossing;
								stepCrossings.add(stepCrossing);
							}

							@Override
							public void afterActivityExecution(StepCrossing stepCrossing) {
								try {
									Thread.sleep(500);
								} catch (InterruptedException e) {
									Thread.currentThread().interrupt();
								}
								currentStepCrossing = null;
							}

							@Override
							public boolean isExecutionInterrupted() {
								return Thread.currentThread().isInterrupted();
							}
						});
					} catch (Throwable t) {
						executionError = t;
					}
				}

			};
			thread.start();
		}

		public Plan getPlan() {
			return plan;
		}

		public List<StepCrossing> getStepCrossings() {
			return stepCrossings;
		}

		public StepCrossing getCurrentStepCrossing() {
			return currentStepCrossing;
		}

		public Throwable getExecutionError() {
			return executionError;
		}

		public synchronized void stop() {
			if (!isActive()) {
				return;
			}
			thread.interrupt();
		}

		public synchronized boolean isActive() {
			if (thread != null) {
				if (thread.isAlive()) {
					return true;
				}
			}
			return false;
		}

		@Override
		public String toString() {
			return (isActive() ? "RUNNING" : (executionError == null) ? "DONE" : "FAILED");
		}

	}

}
