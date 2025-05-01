package com.otk.jesb;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.otk.jesb.activity.builtin.ExecutePlanActivity;
import com.otk.jesb.solution.Asset;
import com.otk.jesb.solution.AssetVisitor;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Reference;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.solution.StepCrossing;

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

		public String getPlanReferencePath() {
			return Reference.get(plan).getPath();
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
		private List<PlanExecutor> children = new ArrayList<Debugger.PlanExecutor>();
		private Stack<PlanExecutor> currentPlanExecutionStack = new Stack<Debugger.PlanExecutor>();

		public PlanExecutor(Plan plan) {
			this.plan = plan;
			start();
		}

		public Plan getPlan() {
			return plan;
		}

		public String getPlanReferencePath() {
			return Reference.get(plan).getPath();
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

		private PlanExecutor getTopPlanExecutor() {
			if (currentPlanExecutionStack.size() > 0) {
				return currentPlanExecutionStack.peek();
			}
			return this;
		}

		public List<PlanExecutor> getChildren() {
			return children;
		}

		protected void start() {
			thread = new Thread("PlanExecutor [plan=" + plan.getName() + "]") {

				@Override
				public void run() {
					execute();
				}

			};
			thread.start();
		}

		public synchronized void stop() {
			if (!isActive()) {
				return;
			}
			thread.interrupt();
		}

		private void execute() {
			try {
				plan.execute(null, new Plan.ExecutionInspector() {
					@Override
					public void beforeActivity(StepCrossing stepCrossing) {
						getTopPlanExecutor().currentStepCrossing = stepCrossing;
						getTopPlanExecutor().stepCrossings.add(stepCrossing);
						if (stepCrossing.getStep().getActivityBuilder() instanceof ExecutePlanActivity.Builder) {
							PlanExecutor newPlanExecutor = new SubPlanExecutor(
									((ExecutePlanActivity.Builder) stepCrossing.getStep().getActivityBuilder())
											.getPlanReference().resolve());
							getTopPlanExecutor().children.add(newPlanExecutor);
							currentPlanExecutionStack.add(newPlanExecutor);
						}
					}

					@Override
					public void afterActivity(StepCrossing stepCrossing) {
						if (stepCrossing.getStep().getActivityBuilder() instanceof ExecutePlanActivity.Builder) {
							currentPlanExecutionStack.pop();
						}
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
						getTopPlanExecutor().currentStepCrossing = null;
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

		public static class SubPlanExecutor extends PlanExecutor {

			public SubPlanExecutor(Plan subPlan) {
				super(subPlan);
			}

			@Override
			protected void start() {
			}

			@Override
			public String toString() {
				return super.toString() + " (" + getPlan().getName() + ")";
			}
		}

	}

}
