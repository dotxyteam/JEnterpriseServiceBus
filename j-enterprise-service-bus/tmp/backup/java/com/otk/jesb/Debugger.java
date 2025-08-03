package com.otk.jesb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.otk.jesb.activation.ActivationHandler;
import com.otk.jesb.activation.Activator;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.builtin.ExecutePlan;
import com.otk.jesb.solution.Asset;
import com.otk.jesb.solution.AssetVisitor;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.Reference;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.solution.StepCrossing;
import com.otk.jesb.solution.Transition;

public class Debugger {

	private Solution solution;
	private List<PlanActivator> planActivations;
	private PlanActivationsFilter currentPlanActivationFilter = PlanActivationsFilter.ACTIVABLE_PLANS;

	public Debugger(Solution solution) {
		this.solution = solution;
		planActivations = collectPlanActivations();
	}

	public List<PlanActivator> getPlanActivations() {
		return planActivations.stream().filter(currentPlanActivationFilter).collect(Collectors.toList());
	}

	private List<PlanActivator> collectPlanActivations() {
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

	public PlanActivationsFilter getCurrentPlanActivationFilter() {
		return currentPlanActivationFilter;
	}

	public void setCurrentPlanActivationFilter(PlanActivationsFilter currentPlanActivationFilter) {
		this.currentPlanActivationFilter = currentPlanActivationFilter;
	}

	public void activatePlans() {
		for (PlanActivator planActivator : planActivations) {
			if (planActivator.isAutomaticallyTriggerable()) {
				if (planActivator.isAutomaticTriggerReady()) {
					planActivator.setAutomaticTriggerReady(false);
				}
				planActivator.setAutomaticTriggerReady(true);
			}
		}
	}

	public void deactivatePlans() {
		for (PlanActivator planActivator : planActivations) {
			if (planActivator.isAutomaticallyTriggerable()) {
				if (planActivator.isAutomaticTriggerReady()) {
					planActivator.setAutomaticTriggerReady(false);
				}
			}
		}
	}

	public void stopExecutions() {
		for (PlanActivator planActivator : planActivations) {
			planActivator.stopExecutions();
		}
	}

	public static class PlanActivator {

		private Plan plan;
		private RootInstanceBuilder planInputBuilder;
		private List<PlanExecutor> planExecutors = new ArrayList<Debugger.PlanExecutor>();

		public PlanActivator(Plan plan) {
			this.plan = plan;
			if (plan.getActivationStrategy().getInputClass() != null) {
				planInputBuilder = new RootInstanceBuilder("Input",
						plan.getActivationStrategy().getInputClass().getName());
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

		public void setAutomaticTriggerReady(boolean ready) {
			if (ready) {
				ActivationHandler activationHandler = new ActivationHandler() {
					@Override
					public void trigger(Object planInput) {
						planExecutors.add(new PlanExecutor(plan, planInput));
					}
				};
				try {
					plan.getActivationStrategy().initializeAutomaticTrigger(activationHandler);
				} catch (Exception e) {
					planExecutors.add(new PlanActivationFailure(plan, e));
				}
			} else {
				try {
					plan.getActivationStrategy().finalizeAutomaticTrigger();
				} catch (Exception e) {
					throw new UnexpectedError(e);
				}
			}
		}

		public boolean isAutomaticTriggerReady() {
			return plan.getActivationStrategy().isAutomaticTriggerReady();
		}

		public boolean isAutomaticallyTriggerable() {
			return plan.getActivationStrategy().isAutomaticallyTriggerable();
		}

		public void stopExecutions() {
			for (PlanExecutor planExecutor : planExecutors) {
				planExecutor.stop();
			}
		}

		public ActivationStrategy getActivationStrategy() {
			return plan.getActivationStrategy();
		}

		public void executePlan() throws Exception {
			Object planInput = (planInputBuilder != null)
					? planInputBuilder.build(new InstantiationContext(Collections.emptyList(), Collections.emptyList()))
					: null;
			planExecutors.add(new PlanExecutor(plan, planInput));
		}

		@Override
		public String toString() {
			String result = "(" + planExecutors.size() + ") ";
			if (isAutomaticallyTriggerable()) {
				if (isAutomaticTriggerReady()) {
					result += "*";
				}
			}
			result += plan.getName();
			return result;
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

	public static class PlanActivationFailure extends PlanExecutor {

		public PlanActivationFailure(Plan plan, Exception error) {
			super(plan, null);
			this.executionError = error;
		}

		@Override
		protected void execute() {
		}

	}

	public enum PlanActivationsFilter implements Predicate<PlanActivator> {
		ALL {
			@Override
			public boolean test(PlanActivator planActivator) {
				return true;
			}
		},
		ACTIVABLE_PLANS {
			@Override
			public boolean test(PlanActivator planActivator) {
				return planActivator.isAutomaticallyTriggerable();
			}
		},
		ACTIVATED_PLANS {
			@Override
			public boolean test(PlanActivator planActivator) {
				return planActivator.isAutomaticallyTriggerable() && planActivator.isAutomaticTriggerReady();
			}
		},
		DEACTIVATED_PLANS {
			@Override
			public boolean test(PlanActivator planActivator) {
				return planActivator.isAutomaticallyTriggerable() && !planActivator.isAutomaticTriggerReady();
			}
		}
	}

}
