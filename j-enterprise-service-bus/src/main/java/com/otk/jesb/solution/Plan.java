package com.otk.jesb.solution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.otk.jesb.CompositeStep;
import com.otk.jesb.StandardError;
import com.otk.jesb.Variable;
import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.Structure.ClassicStructure;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.activity.Activity;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.compiler.CompiledFunction.FunctionCallError;
import com.otk.jesb.instantiation.EvaluationContext;
import com.otk.jesb.instantiation.InstantiationFunction;
import com.otk.jesb.instantiation.InstantiationFunctionCompilationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.solution.Transition.ElseCondition;
import com.otk.jesb.solution.Transition.ExceptionCondition;
import com.otk.jesb.solution.Transition.IfCondition;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.UpToDate;

public class Plan extends Asset {

	private static final String INPUT_VARIABLE_NAME = "PLAN_INPUT";

	public Plan() {
		this(Plan.class.getSimpleName() + MiscUtils.getDigitalUniqueIdentifier());
	}

	public Plan(String name) {
		super(name);
	}

	private List<Step> steps = new ArrayList<Step>();
	private List<Transition> transitions = new ArrayList<Transition>();
	private Object focusedStepOrTransition;
	private ClassicStructure inputStructure;
	private ClassicStructure outputStructure;
	private RootInstanceBuilder outputBuilder = new RootInstanceBuilder(Plan.class.getSimpleName() + "Output",
			new Accessor<String>() {
				@Override
				public String get() {
					Class<?> outputClass = upToDateOutputClass.get();
					if (outputClass == null) {
						return null;
					}
					return outputClass.getName();
				}
			});

	private UpToDate<Class<?>> upToDateInputClass = new UpToDate<Class<?>>() {
		@Override
		protected Object retrieveLastModificationIdentifier() {
			return (inputStructure != null) ? MiscUtils.serialize(inputStructure) : null;
		}

		@Override
		protected Class<?> obtainLatest() {
			if (inputStructure == null) {
				return null;
			} else {
				try {
					String className = Plan.class.getPackage().getName() + "." + Plan.class.getSimpleName() + "Input"
							+ MiscUtils.getDigitalUniqueIdentifier(Plan.this);
					return MiscUtils.IN_MEMORY_JAVA_COMPILER.compile(className,
							inputStructure.generateJavaTypeSourceCode(className));
				} catch (CompilationError e) {
					throw new UnexpectedError(e);
				}
			}
		}
	};
	private UpToDate<Class<?>> upToDateOutputClass = new UpToDate<Class<?>>() {
		@Override
		protected Object retrieveLastModificationIdentifier() {
			return (outputStructure != null) ? MiscUtils.serialize(outputStructure) : null;
		}

		@Override
		protected Class<?> obtainLatest() {
			if (outputStructure == null) {
				return null;
			} else {
				try {
					String className = Plan.class.getPackage().getName() + "." + Plan.class.getSimpleName() + "Output"
							+ MiscUtils.getDigitalUniqueIdentifier(Plan.this);
					return MiscUtils.IN_MEMORY_JAVA_COMPILER.compile(className,
							outputStructure.generateJavaTypeSourceCode(className));
				} catch (CompilationError e) {
					throw new UnexpectedError(e);
				}
			}
		}
	};

	public List<Step> getSteps() {
		return steps;
	}

	public void setSteps(List<Step> steps) {
		this.steps = steps;
	}

	public List<Transition> getTransitions() {
		return transitions;
	}

	public void setTransitions(List<Transition> transitions) {
		this.transitions = transitions;
	}

	public ClassicStructure getInputStructure() {
		return inputStructure;
	}

	public void setInputStructure(ClassicStructure inputStructure) {
		this.inputStructure = inputStructure;
	}

	public ClassicStructure getOutputStructure() {
		return outputStructure;
	}

	public void setOutputStructure(ClassicStructure outputStructure) {
		this.outputStructure = outputStructure;
	}

	public RootInstanceBuilder getOutputBuilder() {
		return outputBuilder;
	}

	public void setOutputBuilder(RootInstanceBuilder outputBuilder) {
		this.outputBuilder = outputBuilder;
	}

	public Class<?> getInputClass() {
		return upToDateInputClass.get();
	}

	public Class<?> getOutputClass() {
		return upToDateOutputClass.get();
	}

	public List<Step> getPrecedingSteps(Step step) {
		return getPrecedingSteps(step, steps);
	}

	public Object getFocusedStepOrTransition() {
		return focusedStepOrTransition;
	}

	public void setFocusedStepOrTransition(Object focusedStepOrTransition) {
		this.focusedStepOrTransition = focusedStepOrTransition;
	}

	public List<Object> getFocusedStepOrTransitionSurroundings() {
		List<Object> result = new ArrayList<Object>();
		if (focusedStepOrTransition != null) {
			if (focusedStepOrTransition instanceof Step) {
				Step step = (Step) focusedStepOrTransition;
				result.addAll(transitions.stream().filter(transition -> transition.getEndStep() == step)
						.collect(Collectors.toList()));
				result.add(step);
				result.addAll(transitions.stream().filter(transition -> transition.getStartStep() == step)
						.collect(Collectors.toList()));
			} else if (focusedStepOrTransition instanceof Transition) {
				Transition transition = (Transition) focusedStepOrTransition;
				result.add(transition.getStartStep());
				result.add(transition);
				result.add(transition.getEndStep());
			} else {
				throw new UnexpectedError();
			}
		}
		return result;
	}

	private List<Step> getPrecedingSteps(Step step, List<Step> steps) {
		List<Step> result = new ArrayList<Step>();
		for (Step candidate : steps) {
			if (isPrecedingStep(candidate, step)) {
				result.add(candidate);
			}
		}
		return result;
	}

	private boolean isPrecedingStep(Step s1, Step s2) {
		for (Transition t : transitions) {
			if ((t.getStartStep() == s1) && (t.getEndStep() == s2)) {
				return true;
			}
			if ((t.getStartStep() == s1) && isPrecedingStep(t.getEndStep(), s2)) {
				return true;
			}
		}
		return false;
	}

	private List<Step> findFirstSteps(List<Step> steps) {
		List<Step> result = new ArrayList<Step>(steps);
		for (Transition t : transitions) {
			result.remove(t.getEndStep());
		}
		return result;
	}

	public Object execute(Object input) throws ExecutionError {
		return execute(input, new ExecutionInspector() {

			@Override
			public void beforeActivityCreation(StepCrossing stepCrossing) {
			}

			@Override
			public void afterActivityExecution(StepCrossing stepCrossing) {
			}

			@Override
			public boolean isExecutionInterrupted() {
				return false;
			}
		});
	}

	public Object execute(final Object input, ExecutionInspector executionInspector) throws ExecutionError {
		try {
			ExecutionContext context = new ExecutionContext(this);
			Class<?> inputClass = upToDateInputClass.get();
			if (inputClass != null) {
				if (input != null) {
					if (!inputClass.isInstance(input)) {
						throw new UnexpectedError();
					}
				}
				context.getVariables().add(new Variable() {

					@Override
					public Object getValue() {
						return input;
					}

					@Override
					public String getName() {
						return INPUT_VARIABLE_NAME;
					}
				});
			}
			execute(steps.stream().filter(step -> (step.getParent() == null)).collect(Collectors.toList()), context,
					executionInspector);
			return outputBuilder.build(
					new EvaluationContext(context.getVariables(), null, context.getCompilationContextProvider()));
		} catch (Throwable t) {
			throw new ExecutionError("Failed to execute plan (" + Reference.get(this).getPath() + ")", t);
		}
	}

	public void execute(List<Step> steps, ExecutionContext context, ExecutionInspector executionInspector)
			throws ExecutionError {
		if (executionInspector.isExecutionInterrupted()) {
			return;
		}
		if (steps.size() == 0) {
			return;
		}
		List<Step> firstSteps = findFirstSteps(steps);
		if (firstSteps.size() == 0) {
			throw new ExecutionError(new PlanificationError("Could not find any initial step"));
		}
		for (Step firstStep : firstSteps) {
			continueExecution(firstStep, true, context, executionInspector);
		}
	}

	private void continueExecution(Step currentStep, boolean executionBranchValid, ExecutionContext context,
			ExecutionInspector executionInspector) throws ExecutionError {
		List<Transition> currentStepTransitions = transitions.stream()
				.filter(transition -> (transition.getStartStep() == currentStep)).collect(Collectors.toList());
		if (executionBranchValid) {
			StepCrossing stepCrossing = new StepCrossing(currentStep, this);
			ExecutionError executionError;
			try {
				execute(stepCrossing, context, executionInspector);
				executionError = null;
			} catch (ExecutionError e) {
				executionError = e;
				stepCrossing.setActivityError(e.getCause());
			} finally {
				context.getVariables().add(stepCrossing);
				stepCrossing.capturePostVariables(context.getVariables());
			}
			if (currentStepTransitions.size() == 0) {
				if (executionError != null) {
					throw executionError;
				} else {
					return;
				}
			} else {
				List<Transition> validTransitions = filterValidTranstions(currentStepTransitions, executionError,
						context);
				stepCrossing.setValidTransitions(validTransitions);
				if (validTransitions.size() == 0) {
					if (executionError != null) {
						throw executionError;
					} else {
						throw new ExecutionError(new PlanificationError(
								"Could not find any valid transition from step '" + currentStep + "'"));
					}
				}
			}
		} else {
			StepSkipping stepSkipping = new StepSkipping(currentStep, this);
			context.getVariables().add(stepSkipping);
			stepSkipping.capturePostVariables(context.getVariables());
		}
		final int TRANSITION_NOT_REACHED = 0;
		final int TRANSITION_REACHED_THROUGH_VALID_BRANCH = 1;
		final int TRANSITION_REACHED_THROUGH_INVALID_BRANCH = 2;
		for (Transition transition : currentStepTransitions) {
			boolean endStepAlreadyExecuted = context.getVariables().stream()
					.anyMatch(variable -> variable.getName().equals(transition.getEndStep().getName()));
			if (endStepAlreadyExecuted) {
				continue;
			}
			List<Transition> convergentTransitions = transitions.stream()
					.filter(candidateConvergentTransition -> (candidateConvergentTransition.getEndStep() == transition
							.getEndStep()))
					.collect(Collectors.toList());
			Map<Transition, Integer> statusByConvergentTransition = new HashMap<Transition, Integer>();
			Map<Transition, StepOccurrence> startStepOccurrenceByConvergentTransition = new HashMap<Transition, StepOccurrence>();
			for (Transition convergentTransition : convergentTransitions) {
				int convergentTransitionStatus = TRANSITION_NOT_REACHED;
				StepOccurrence startStepOccurrence = null;
				for (Variable variable : context.getVariables()) {
					if ((variable instanceof StepOccurrence)
							&& (((StepOccurrence) variable).getStep() == convergentTransition.getStartStep())) {
						startStepOccurrence = (StepOccurrence) variable;
						if (variable instanceof StepCrossing) {
							convergentTransitionStatus = TRANSITION_REACHED_THROUGH_VALID_BRANCH;
						} else if (variable instanceof StepSkipping) {
							convergentTransitionStatus = TRANSITION_REACHED_THROUGH_INVALID_BRANCH;
						} else {
							throw new UnexpectedError();
						}
						break;
					}
				}
				startStepOccurrenceByConvergentTransition.put(convergentTransition, startStepOccurrence);
				statusByConvergentTransition.put(convergentTransition, convergentTransitionStatus);
			}
			if (!statusByConvergentTransition.containsValue(TRANSITION_NOT_REACHED)) {
				List<Variable> convergentTransitionsMergedVariables = new ArrayList<Variable>();
				{
					startStepOccurrenceByConvergentTransition.entrySet().stream().forEach(entry -> {
						StepOccurrence startStepOccurrence = entry.getValue();
						if (startStepOccurrence == null) {
							throw new UnexpectedError();
						}
						for (Variable variable : startStepOccurrence.getPostVariablesSnapshot()) {
							if (!convergentTransitionsMergedVariables.contains(variable)) {
								convergentTransitionsMergedVariables.add(variable);
							}
						}
					});
				}
				context.getVariables().clear();
				context.getVariables().addAll(convergentTransitionsMergedVariables);
				boolean futureExecutionBranchValid = statusByConvergentTransition.entrySet().stream()
						.anyMatch(entry -> {
							Transition convergentTransition = entry.getKey();
							int convergentTransitionStatus = entry.getValue();
							if (convergentTransitionStatus != TRANSITION_REACHED_THROUGH_VALID_BRANCH) {
								return false;
							}
							StepCrossing convergentTransitionStartStepCrossing = (StepCrossing) startStepOccurrenceByConvergentTransition
									.get(convergentTransition);
							return convergentTransitionStartStepCrossing.getValidTransitions()
									.contains(convergentTransition);
						});
				continueExecution(transition.getEndStep(), futureExecutionBranchValid, context, executionInspector);
			}
		}
	}

	private List<Transition> filterValidTranstions(List<Transition> transitions, ExecutionError executionError,
			ExecutionContext context) throws ExecutionError {
		List<Transition> result = new ArrayList<Transition>();
		List<Transition> elseTransitions = new ArrayList<Transition>();
		for (Transition transition : transitions) {
			if (transition.getCondition() != null) {
				if (transition.getCondition() instanceof IfCondition) {
					if (executionError == null) {
						try {
							if (((IfCondition) transition.getCondition()).isFulfilled(
									getTransitionContextVariableDeclarations(transition), context.getVariables())) {
								result.add(transition);
							}
						} catch (FunctionCallError e) {
							throw new ExecutionError(e);
						}
					}
				} else if (transition.getCondition() instanceof ElseCondition) {
					if (executionError == null) {
						elseTransitions.add(transition);
					}
				} else if (transition.getCondition() instanceof ExceptionCondition) {
					if (executionError != null) {
						if (((ExceptionCondition) transition.getCondition()).isFullfilled(executionError.getCause())) {
							result.add(transition);
						}
					}
				} else {
					throw new UnexpectedError();
				}
			} else {
				if (executionError == null) {
					result.add(transition);
				}
			}
		}
		if (elseTransitions.size() > 0) {
			if (result.stream().noneMatch(transition -> (transition.getCondition() instanceof IfCondition))) {
				result.addAll(elseTransitions);
			}
		}
		return result;
	}

	public List<VariableDeclaration> getTransitionContextVariableDeclarations(Transition transition) {
		List<VariableDeclaration> result = getValidationContext(transition.getStartStep()).getVariableDeclarations();
		VariableDeclaration startStepVariableDeclaration = getResultVariableDeclaration(transition.getStartStep());
		if (startStepVariableDeclaration != null) {
			result = new ArrayList<VariableDeclaration>(result);
			result.add(startStepVariableDeclaration);
		}
		return result;
	}

	private void execute(StepCrossing stepCrossing, ExecutionContext context,
			ExecutionInspector executionInspector) throws ExecutionError {
		Step step = stepCrossing.getStep();
		context.setCutrrentStep(step);
		executionInspector.beforeActivityCreation(stepCrossing);
		ActivityBuilder activityBuilder = step.getActivityBuilder();
		try {
			Activity activity = activityBuilder.build(context, executionInspector);
			stepCrossing.setActivity(activity);
			stepCrossing.setActivityResult(activity.execute());
		} catch (Throwable t) {
			throw new ExecutionError("An error occured at step '" + step.getName() + "'", t);
		} finally {
			executionInspector.afterActivityExecution(stepCrossing);
		}
		context.setCutrrentStep(null);
	}

	public ValidationContext getValidationContext(Step currentStep) {
		ValidationContext result;
		if ((currentStep != null) && (currentStep.getParent() != null)) {
			result = getValidationContext(currentStep.getParent());
			for (VariableDeclaration declaration : currentStep.getParent().getContextualVariableDeclarations()) {
				result = new ValidationContext(result, declaration);
			}
		} else {
			result = new ValidationContext(this, currentStep);
			Class<?> inputClass = upToDateInputClass.get();
			if (inputClass != null) {
				result.getVariableDeclarations().add(new VariableDeclaration() {

					@Override
					public String getVariableName() {
						return INPUT_VARIABLE_NAME;
					}

					@Override
					public Class<?> getVariableType() {
						return inputClass;
					}
				});
			}
		}
		List<Step> precedingSteps = (currentStep != null) ? getPrecedingSteps(currentStep) : steps;
		for (Step step : precedingSteps) {
			VariableDeclaration stepVariableDeclaration = getResultVariableDeclaration(step);
			if (stepVariableDeclaration != null) {
				result.getVariableDeclarations().add(stepVariableDeclaration);
			}
			if (step instanceof CompositeStep) {
				for (Step descendantStep : MiscUtils.getDescendants((CompositeStep) step, this)) {
					if (descendantStep.getActivityBuilder().getActivityResultClass(this, descendantStep) != null) {
						result.getVariableDeclarations().add(new StepEventuality(descendantStep, this));
					}
				}
			}
		}
		return result;
	}

	public VariableDeclaration getResultVariableDeclaration(Step step) {
		if (step.getActivityBuilder().getActivityResultClass(this, step) != null) {
			return new StepEventuality(step, this);
		} else {
			return null;
		}
	}

	public static class ValidationContext {

		private Plan plan;
		private Step step;
		private List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();

		public ValidationContext(Plan plan, Step step) {
			this.plan = plan;
			this.step = step;
		}

		public ValidationContext(ValidationContext parentContext, VariableDeclaration newDeclaration) {
			plan = parentContext.getPlan();
			step = parentContext.getStep();
			variableDeclarations.addAll(parentContext.getVariableDeclarations());
			variableDeclarations.add(newDeclaration);
		}

		public Plan getPlan() {
			return plan;
		}

		public Step getStep() {
			return step;
		}

		public List<VariableDeclaration> getVariableDeclarations() {
			return variableDeclarations;
		}

	}

	public static class ExecutionContext {

		private Plan plan;
		private Step currentStep;
		private List<Variable> variables = new ArrayList<Variable>();

		public ExecutionContext(Plan plan) {
			this.plan = plan;
		}

		public Plan getPlan() {
			return plan;
		}

		public Step getCurrentStep() {
			return currentStep;
		}

		public void setCutrrentStep(Step step) {
			this.currentStep = step;
		}

		public List<Variable> getVariables() {
			return variables;
		}

		public Function<InstantiationFunction, InstantiationFunctionCompilationContext> getCompilationContextProvider() {
			return new Function<InstantiationFunction, InstantiationFunctionCompilationContext>() {

				@Override
				public InstantiationFunctionCompilationContext apply(InstantiationFunction function) {
					return (currentStep != null)
							? currentStep.getActivityBuilder().findFunctionCompilationContext(function, currentStep,
									plan)
							: plan.getOutputBuilder().getFacade().findFunctionCompilationContext(function,
									plan.getValidationContext(currentStep).getVariableDeclarations());
				}
			};
		}

	}

	public interface ExecutionInspector {

		void beforeActivityCreation(StepCrossing stepCrossing);

		boolean isExecutionInterrupted();

		void afterActivityExecution(StepCrossing stepCrossing);

	}

	public static class PlanificationError extends StandardError {

		private static final long serialVersionUID = 1L;

		public PlanificationError(String message) {
			super(message);
		}

	}

	public static class ExecutionError extends StandardError {

		private static final long serialVersionUID = 1L;

		public ExecutionError(Throwable cause) {
			this("A problem occured", cause);
		}

		private ExecutionError(String message, Throwable cause) {
			super((cause instanceof ExecutionError) ? (message + ":\n" + cause.getMessage()) : message,
					(cause instanceof ExecutionError) ? cause.getCause() : cause);
		}

		@Override
		public String getMessage() {
			String result = super.getMessage();
			if (super.getCause() instanceof FunctionCallError) {
				result += "\n" + ((FunctionCallError) super.getCause()).describeSource();
			}
			return result;
		}

		@Override
		public synchronized Throwable getCause() {
			Throwable result = super.getCause();
			if (result instanceof FunctionCallError) {
				result = ((FunctionCallError) result).getCause();
			}
			return result;
		}

	}

}
