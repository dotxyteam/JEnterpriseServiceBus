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
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.Reference;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.solution.StepCrossing;
import com.otk.jesb.solution.Transition;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.MiscUtils;

/**
 * This class allows to run a {@link Solution} while collecting information to
 * analyze and understand the progress of the {@link Session}.
 * 
 * @author olitank
 *
 */
public class Debugger extends Session {

	private Solution solution;
	private List<PlanActivation> planActivations;
	private List<PlanExecutor> activePlanExecutors = new ArrayList<Debugger.PlanExecutor>();
	private PlanActivationFilter currentPlanActivationFilter = PlanActivationFilter.ACTIVABLE_PLANS;
	private Console console = Console.DEFAULT;
	private static boolean scrollLocked = false;

	public Debugger(Solution solution) {
		this.solution = solution;
		planActivations = collectPlanActivations();
	}

	public Console getConsole() {
		return console;
	}

	public List<PlanActivation> getPlanActivations() {
		return planActivations.stream().filter(currentPlanActivationFilter).collect(Collectors.toList());
	}

	private List<PlanActivation> collectPlanActivations() {
		final List<PlanActivation> result = new ArrayList<Debugger.PlanActivation>();
		solution.visitContents(new AssetVisitor() {
			@Override
			public boolean visitAsset(Asset asset) {
				if (asset instanceof Plan) {
					Plan plan = (Plan) asset;
					if (plan.getActivator().getEnabledVariant().getValue()) {
						result.add(createPlanActivation(plan));
					}
				}
				return true;
			}
		});
		return result;
	}

	protected PlanActivation createPlanActivation(Plan plan) {
		return new PlanActivation(plan);
	}

	public PlanActivationFilter getCurrentPlanActivationFilter() {
		return currentPlanActivationFilter;
	}

	public void setCurrentPlanActivationFilter(PlanActivationFilter currentPlanActivationFilter) {
		this.currentPlanActivationFilter = currentPlanActivationFilter;
	}

	@Override
	protected void initiate() {
	}

	@Override
	protected void terminate() {
		deactivatePlans();
		stopExecutions();
	}

	public void activatePlans() {
		for (PlanActivation planActivator : planActivations) {
			if (planActivator.isAutomaticallyTriggerable()) {
				if (planActivator.isAutomaticTriggerReady()) {
					planActivator.setAutomaticTriggerReady(false);
				}
				planActivator.setAutomaticTriggerReady(true);
			}
		}
	}

	public void deactivatePlans() {
		for (PlanActivation planActivator : planActivations) {
			if (planActivator.isAutomaticallyTriggerable()) {
				if (planActivator.isAutomaticTriggerReady()) {
					planActivator.setAutomaticTriggerReady(false);
				}
			}
		}
	}

	public void stopExecutions() {
		for (PlanActivation planActivator : planActivations) {
			planActivator.stopExecutions();
		}
	}

	protected synchronized void onExecutionStart(PlanExecutor planExecutor) {
		activePlanExecutors.add(planExecutor);
	}

	protected synchronized void onExecutionEnd(PlanExecutor planExecutor) {
		activePlanExecutors.remove(planExecutor);
	}

	public class PlanActivation {

		private Plan plan;
		private RootInstanceBuilder planInputBuilder;
		private List<PlanExecutor> planExecutors = new ArrayList<Debugger.PlanExecutor>();

		public PlanActivation(Plan plan) {
			this.plan = plan;
			planInputBuilder = new RootInstanceBuilder("Input", new Accessor<String>() {

				@Override
				public String get() {
					Class<?> inputClass = plan.getActivator().getInputClass();
					if (inputClass == null) {
						return null;
					}
					return inputClass.getName();
				}
			});
		}

		public Plan getPlan() {
			return plan;
		}

		public String getPlanReferencePath() {
			return Reference.get(plan).getPath();
		}

		public RootInstanceBuilder getPlanInputBuilder() {
			if (plan.getActivator().getInputClass() == null) {
				return null;
			}
			return planInputBuilder;
		}

		public List<PlanExecutor> getPlanExecutors() {
			return planExecutors;
		}

		public void setAutomaticTriggerReady(boolean ready) {
			if (ready) {
				ActivationHandler activationHandler = new ActivationHandler() {
					@Override
					public Object trigger(Object planInput) {
						PlanExecutor planExecutor = createPlanExecutor(planInput);
						planExecutors.add(planExecutor);
						try {
							planExecutor.join();
						} catch (InterruptedException e) {
							throw new UnexpectedError(e);
						}
						return planExecutor.getPlanOutput();
					}
				};
				try {
					plan.getActivator().initializeAutomaticTrigger(activationHandler);
				} catch (Exception e) {
					handleActivationError(e);
					try {
						plan.getActivator().finalizeAutomaticTrigger();
					} catch (Throwable ignore) {
					}
				}
			} else {
				try {
					plan.getActivator().finalizeAutomaticTrigger();
				} catch (Exception e) {
					handleDeactivationError(e);
				}
			}
		}

		protected PlanExecutor createPlanExecutor(Object planInput) {
			return new PlanExecutor(plan, planInput);
		}

		protected void handleActivationError(Exception e) {
			Log.get().err(e);
			planExecutors.add(new PlanActivationFailure(plan, e));
		}

		protected void handleDeactivationError(Exception e) {
			Log.get().err(e);
		}

		public boolean isAutomaticTriggerReady() {
			return plan.getActivator().isAutomaticTriggerReady();
		}

		public boolean isAutomaticallyTriggerable() {
			return plan.getActivator().isAutomaticallyTriggerable();
		}

		public void stopExecutions() {
			for (PlanExecutor planExecutor : planExecutors) {
				planExecutor.stop();
			}
		}

		public Activator getActivator() {
			return plan.getActivator();
		}

		public void executePlan() throws Exception {
			Object planInput = planInputBuilder
					.build(new InstantiationContext(Collections.emptyList(), Collections.emptyList()));
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

	public class PlanExecutor {

		protected Plan plan;
		protected Object planInput;
		protected Object planOutput;
		protected List<StepCrossing> stepCrossings = new ArrayList<StepCrossing>();
		protected StepCrossing currentStepCrossing;
		protected Throwable executionError;
		protected Thread thread;
		protected List<PlanExecutor> children = new ArrayList<Debugger.PlanExecutor>();
		protected Stack<SubPlanExecutor> subPlanExecutionStack = new Stack<SubPlanExecutor>();
		protected boolean interrupted = false;
		protected ExecutionInspector executionInspector;
		protected ExecutionContext executionContext;

		public PlanExecutor(Plan plan, Object planInput) {
			this.plan = plan;
			this.planInput = planInput;
			this.executionInspector = createExecutionInspector();
			this.executionContext = new ExecutionContext(Debugger.this, plan);
			start();
		}

		protected SubPlanExecutor createSubPlanExecutor(Plan subPlan) {
			return new SubPlanExecutor(subPlan);
		}

		protected ExecutionInspector createExecutionInspector() {
			return new Plan.ExecutionInspector() {
				@Override
				public void beforeOperation(StepCrossing stepCrossing) {
					getTopPlanExecutor().currentStepCrossing = stepCrossing;
					getTopPlanExecutor().stepCrossings.add(stepCrossing);
					if (stepCrossing.getStep().getOperationBuilder() instanceof ExecutePlan.Builder) {
						Plan subPlan = ((ExecutePlan.Builder) stepCrossing.getStep().getOperationBuilder())
								.getPlanReference().resolve();
						SubPlanExecutor subPlanExecutor = createSubPlanExecutor(subPlan);
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
			};
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
			Debugger.scrollLocked = scrollLocked;
		}

		public List<StepCrossing> getStepCrossings() {
			return stepCrossings;
		}

		public StepCrossing getCurrentStepCrossing() {
			return currentStepCrossing;
		}

		public Object getPlanIntput() {
			return planInput;
		}

		public Object getPlanOutput() {
			return planOutput;
		}

		public boolean isPlanInputRelevant() {
			if (!plan.isInputEnabled()) {
				return false;
			}
			if (isActive()) {
				return false;
			}
			return true;
		}

		public boolean isPlanOutputRelevant() {
			if (!plan.isOutputEnabled()) {
				return false;
			}
			if (isActive()) {
				return false;
			}
			return true;
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

		public List<Variable> getVariables() {
			synchronized (executionContext) {
				return executionContext.getVariables().stream()
						.filter(variable -> !variable.getName().equals(Plan.INPUT_VARIABLE_NAME)
								&& (variable.getValue() != Variable.UNDEFINED_VALUE))
						.map(variable -> new Variable.Proxy(variable)).collect(Collectors.toList());
			}
		}

		protected synchronized void start() {
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
			while (thread.isAlive()) {
				thread.interrupt();
				MiscUtils.relieveCPU();
			}
			thread = null;
		}

		public synchronized boolean isActive() {
			if (thread != null) {
				if (thread.isAlive()) {
					return true;
				}
			}
			return false;
		}

		public synchronized void join() throws InterruptedException {
			if (!isActive()) {
				return;
			}
			thread.join();
		}

		protected void execute() {
			onExecutionStart(this);
			try {
				planOutput = plan.execute(planInput, executionInspector, executionContext);
			} catch (Throwable t) {
				handleExecutionError(t);
			} finally {
				onExecutionEnd(this);
			}
		}

		protected void handleExecutionError(Throwable t) {
			Log.get().err(MiscUtils.getPrintedStackTrace(t));
			executionError = t;
		}

		@Override
		public String toString() {
			return (isActive() ? "RUNNING"
					: (interrupted ? "INTERRUPTED" : ((executionError == null) ? "DONE" : "FAILED")));
		}

		public class SubPlanExecutor extends PlanExecutor {

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

	public class PlanActivationFailure extends PlanExecutor {

		public PlanActivationFailure(Plan plan, Exception error) {
			super(plan, null);
			this.executionError = error;
		}

		@Override
		protected void execute() {
		}

	}

	public enum PlanActivationFilter implements Predicate<PlanActivation> {
		ALL {
			@Override
			public boolean test(PlanActivation planActivator) {
				return true;
			}
		},
		ACTIVABLE_PLANS {
			@Override
			public boolean test(PlanActivation planActivator) {
				return planActivator.isAutomaticallyTriggerable();
			}
		},
		ACTIVATED_PLANS {
			@Override
			public boolean test(PlanActivation planActivator) {
				return planActivator.isAutomaticallyTriggerable() && planActivator.isAutomaticTriggerReady();
			}
		},
		DEACTIVATED_PLANS {
			@Override
			public boolean test(PlanActivation planActivator) {
				return planActivator.isAutomaticallyTriggerable() && !planActivator.isAutomaticTriggerReady();
			}
		}
	}

}
