package com.otk.jesb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.builtin.ExecutePlan;
import com.otk.jesb.solution.Asset;
import com.otk.jesb.solution.AssetVisitor;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Reference;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.solution.StepCrossing;
import com.otk.jesb.solution.Transition;

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
		solution.visitContents(new AssetVisitor() {
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
		private RootInstanceBuilder planInputBuilder;
		private List<PlanExecutor> planExecutors = new ArrayList<Debugger.PlanExecutor>();

		public PlanActivator(Plan plan) {
			this.plan = plan;
			if (plan.getActivation().getInputClass() != null) {
				planInputBuilder = new RootInstanceBuilder("Input", plan.getActivation().getInputClass().getName());
			}
		}

		public Plan getPlan() {
			return plan;
		}

		public String getPlanReferencePath() {
			return Reference.get(plan).getPath();
		}

		public RootInstanceBuilder getPlanInputBuilder() {
			return planInputBuilder;
		}

		public List<PlanExecutor> getPlanExecutors() {
			return planExecutors;
		}

		public void executePlan() throws Exception {
			Object planInput = (planInputBuilder != null)
					? planInputBuilder.build(new InstantiationContext(Collections.emptyList(), Collections.emptyList()))
					: null;
			planExecutors.add(new PlanExecutor(plan, planInput));
		}

		@Override
		public String toString() {
			return "(" + planExecutors.size() + ") " + plan.getName();
		}
	}

	public static class PlanExecutor {

		protected Plan plan;
		protected Object planInput;
		protected List<StepCrossing> stepCrossings = new ArrayList<StepCrossing>();
		protected StepCrossing currentStepCrossing;
		protected Throwable executionError;
		protected Thread thread;
		protected List<PlanExecutor> children = new ArrayList<Debugger.PlanExecutor>();
		protected Stack<SubPlanExecutor> subPlanExecutionStack = new Stack<SubPlanExecutor>();
		protected static boolean scrollLocked = false;
		protected boolean interrupted = false;

		public PlanExecutor(Plan plan, Object planInput) {
			this.plan = plan;
			this.planInput = planInput;
			start();
		}

		public Plan getPlan() {
			return plan;
		}

		public String getPlanReferencePath() {
			return Reference.get(plan).getPath();
		}

		public boolean isScrollLocked() {
			return scrollLocked;
		}

		public void setScrollLocked(boolean scrollLocked) {
			PlanExecutor.scrollLocked = scrollLocked;
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
			if (subPlanExecutionStack.size() > 0) {
				return subPlanExecutionStack.peek();
			}
			return this;
		}

		public List<PlanExecutor> getChildren() {
			return children;
		}

		public int getTransitionOccurrenceCount(Transition transition) {
			int result = 0;
			for (StepCrossing stepCrossing : stepCrossings) {
				List<Transition> validaTransitions = stepCrossing.getValidTransitions();
				if (validaTransitions != null) {
					if (validaTransitions.contains(transition)) {
						result++;
					}
				}
			}
			return result;
		}

		protected void start() {
			thread = new Thread("PlanExecutor [plan=" + plan.getName() + "]") {

				@Override
				public void run() {
					execute();
					interrupted = isInterrupted();
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

		protected void execute() {
			try {
				plan.execute(planInput, new Plan.ExecutionInspector() {
					@Override
					public void beforeOperation(StepCrossing stepCrossing) {
						getTopPlanExecutor().currentStepCrossing = stepCrossing;
						getTopPlanExecutor().stepCrossings.add(stepCrossing);
						if (stepCrossing.getStep().getOperationBuilder() instanceof ExecutePlan.Builder) {
							Plan subPlan = ((ExecutePlan.Builder) stepCrossing.getStep().getOperationBuilder())
									.getPlanReference().resolve();
							SubPlanExecutor subPlanExecutor = new SubPlanExecutor(subPlan);
							getTopPlanExecutor().children.add(subPlanExecutor);
							subPlanExecutionStack.add(subPlanExecutor);
						}
					}

					@Override
					public void afterOperation(StepCrossing stepCrossing) {
						if (stepCrossing.getStep().getOperationBuilder() instanceof ExecutePlan.Builder) {
							subPlanExecutionStack.pop().executionError = stepCrossing.getOperationError();
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
				if (JESB.DEBUG) {
					t.printStackTrace();
				}
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
			return (isActive() ? "RUNNING"
					: (interrupted ? "INTERRUPTED" : ((executionError == null) ? "DONE" : "FAILED")));
		}

		public static class SubPlanExecutor extends PlanExecutor {

			public SubPlanExecutor(Plan plan) {
				super(plan, null);
			}

			@Override
			public void start() {
			}

			@Override
			public String toString() {
				return super.toString() + " (" + getPlan().getName() + ")";
			}
		}

	}

}
