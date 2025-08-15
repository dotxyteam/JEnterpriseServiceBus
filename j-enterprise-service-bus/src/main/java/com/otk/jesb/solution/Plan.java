package com.otk.jesb.solution;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.otk.jesb.EnvironmentSettings;
import com.otk.jesb.Reference;
import com.otk.jesb.Session;
import com.otk.jesb.StandardError;
import com.otk.jesb.Variable;
import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.activation.Activator;
import com.otk.jesb.activation.builtin.LaunchAtStartup;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.compiler.CompiledFunction.FunctionCallError;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.solution.Transition.ElseCondition;
import com.otk.jesb.solution.Transition.ExceptionCondition;
import com.otk.jesb.solution.Transition.IfCondition;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.MiscUtils;

public class Plan extends Asset {

	private static final String INPUT_VARIABLE_NAME = "PLAN_INPUT";

	public Plan() {
		this(Plan.class.getSimpleName() + MiscUtils.getDigitalUniqueIdentifier());
	}

	public Plan(String name) {
		super(name);
	}

	private Activator activator = new LaunchAtStartup();
	private List<Step> steps = new ArrayList<Step>();
	private List<Transition> transitions = new ArrayList<Transition>();
	private transient Set<PlanElement> selectedElements = new HashSet<PlanElement>();
	private transient PlanElement focusedElementSelectedSurrounding;
	private RootInstanceBuilder outputBuilder = new RootInstanceBuilder(Plan.class.getSimpleName() + "Output",
			new OutputClassNameAccessor());

	public Activator getActivator() {
		return activator;
	}

	public void setActivator(Activator activator) {
		this.activator = activator;
	}

	public RootInstanceBuilder getOutputBuilder() {
		return outputBuilder;
	}

	public void setOutputBuilder(RootInstanceBuilder outputBuilder) {
		this.outputBuilder = outputBuilder;
	}

	public boolean isOutputEnabled() {
		return activator.getOutputClass() != null;
	}

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

	@Transient
	public Set<PlanElement> getSelectedElements() {
		return selectedElements.stream().filter(element -> steps.contains(element) || transitions.contains(element))
				.collect(Collectors.toSet());
	}

	public void setSelectedElements(Set<PlanElement> selectedElements) {
		this.selectedElements = selectedElements;
	}

	@Transient
	public PlanElement getFocusedElementSelectedSurrounding() {
		return getFocusedElementSurroundings().contains(focusedElementSelectedSurrounding)
				? focusedElementSelectedSurrounding
				: null;
	}

	public void setFocusedElementSelectedSurrounding(PlanElement focusedElementSelectedSurrounding) {
		this.focusedElementSelectedSurrounding = focusedElementSelectedSurrounding;
	}

	@Transient
	public PlanElement getFocusedElement() {
		Set<PlanElement> selectedElements = getSelectedElements();
		return (selectedElements.size() == 1) ? selectedElements.iterator().next() : null;
	}

	public void setFocusedElement(PlanElement focusedElement) {
		setSelectedElements((focusedElement != null) ? Collections.singleton(focusedElement) : Collections.emptySet());
	}

	public List<PlanElement> getFocusedElementSurroundings() {
		List<PlanElement> result = new ArrayList<PlanElement>();
		PlanElement focusedElement = getFocusedElement();
		if (focusedElement != null) {
			if (focusedElement instanceof Step) {
				Step step = (Step) focusedElement;
				result.addAll(transitions.stream().filter(transition -> transition.getEndStep() == step)
						.collect(Collectors.toList()));
				result.add(step);
				result.addAll(transitions.stream().filter(transition -> transition.getStartStep() == step)
						.collect(Collectors.toList()));
			} else if (focusedElement instanceof Transition) {
				Transition transition = (Transition) focusedElement;
				result.add(transition.getStartStep());
				result.add(transition);
				result.add(transition.getEndStep());
			} else {
				throw new UnexpectedError();
			}
		}
		return result;
	}

	private List<Step> getPrecedingSteps(Step step) {
		Map<Step, List<Step>> reversedGraph = new HashMap<>();
		for (Transition t : transitions) {
			if (t.getStartStep().getParent() != step.getParent()) {
				continue;
			}
			if (t.getEndStep().getParent() != step.getParent()) {
				continue;
			}
			reversedGraph.computeIfAbsent(t.getEndStep(), k -> new ArrayList<>()).add(t.getStartStep());
		}
		Set<Step> visited = new HashSet<>();
		List<Step> ordered = new ArrayList<>();
		collectPrecedingStepsTopologically(step, reversedGraph, visited, ordered);
		return ordered;
	}

	private void collectPrecedingStepsTopologically(Step current, Map<Step, List<Step>> graph, Set<Step> visited,
			List<Step> result) {
		for (Step pred : graph.getOrDefault(current, Collections.emptyList())) {
			if (visited.add(pred)) {
				collectPrecedingStepsTopologically(pred, graph, visited, result);
				result.add(pred);
			}
		}
	}

	public boolean isPreceding(Step step1, Step step2) {
		return getPrecedingSteps(step2).contains(step1);
	}

	private List<Step> findFirstSteps(List<Step> steps) {
		List<Step> result = new ArrayList<Step>(steps);
		for (Transition t : transitions) {
			result.remove(t.getEndStep());
		}
		return result;
	}

	public Object execute(final Object input, ExecutionInspector executionInspector, ExecutionContext context)
			throws ExecutionError {
		try {
			if (!Solution.INSTANCE.getEnvironmentSettings().getEnvironmentVariableTreeElements().isEmpty()) {
				context.getVariables().add(EnvironmentSettings.ENVIRONMENT_VARIABLES_ROOT);
			}
			Class<?> inputClass = activator.getInputClass();
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
			return outputBuilder.build(new InstantiationContext(context.getVariables(),
					getValidationContext(null).getVariableDeclarations()));
		} catch (Throwable t) {
			throw new ExecutionError("Failed to execute plan (" + Reference.get(this).getPath() + ")", t);
		}
	}

	public void execute(List<Step> steps, ExecutionContext context, ExecutionInspector executionInspector)
			throws ExecutionError {
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
		if (executionInspector.isExecutionInterrupted()) {
			return;
		}
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
				List<Transition> validTransitions;
				try {
					validTransitions = filterValidTranstions(currentStepTransitions, executionError, context);
				} catch (Throwable t) {
					stepCrossing.setOperationError(t);
					throw new ExecutionError(t);
				}
				stepCrossing.setValidTransitions(validTransitions);
				if (validTransitions.size() == 0) {
					if (executionError != null) {
						throw executionError;
					} else {
						PlanificationError planificationError = new PlanificationError(
								"Could not find any valid transition from step '" + currentStep + "'");
						stepCrossing.setOperationError(planificationError);
						throw new ExecutionError(planificationError);
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
			ExecutionContext context) throws FunctionCallError {
		List<Transition> result = new ArrayList<Transition>();
		List<Transition> elseTransitions = new ArrayList<Transition>();
		for (Transition transition : transitions) {
			if (transition.getCondition() != null) {
				if (transition.getCondition() instanceof IfCondition) {
					if (executionError == null) {
						if (((IfCondition) transition.getCondition()).isFulfilled(
								getTransitionContextVariableDeclarations(transition), context.getVariables())) {
							result.add(transition);
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

	private void execute(StepCrossing stepCrossing, ExecutionContext context, ExecutionInspector executionInspector)
			throws ExecutionError {
		Step step = stepCrossing.getStep();
		context.setCutrrentStep(step);
		executionInspector.beforeOperation(stepCrossing);
		try {
			OperationBuilder<?> operationBuilder = step.getOperationBuilder();
			Operation operation = operationBuilder.build(context, executionInspector);
			stepCrossing.setOperation(operation);
			stepCrossing.setOperationResult(operation.execute());
		} catch (Throwable t) {
			stepCrossing.setOperationError(t);
			throw new ExecutionError("An error occured at step '" + step.getName() + "'", t);
		} finally {
			executionInspector.afterOperation(stepCrossing);
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
			if (!Solution.INSTANCE.getEnvironmentSettings().getEnvironmentVariableTreeElements().isEmpty()) {
				result.getVariableDeclarations().add(EnvironmentSettings.ENVIRONMENT_VARIABLES_ROOT_DECLARATION);
			}
			Class<?> inputClass = activator.getInputClass();
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
		List<Step> precedingSteps = (currentStep != null) ? getPrecedingSteps(currentStep)
				: steps.stream().filter(step -> step.getParent() == null).collect(Collectors.toList());
		for (Step step : precedingSteps) {
			VariableDeclaration stepVariableDeclaration = getResultVariableDeclaration(step);
			if (stepVariableDeclaration != null) {
				result.getVariableDeclarations().add(stepVariableDeclaration);
			}
			if (step instanceof CompositeStep) {
				for (Step descendantStep : MiscUtils.getDescendants((CompositeStep<?>) step, this)) {
					if (descendantStep.getOperationBuilder().getOperationResultClass(this, descendantStep) != null) {
						result.getVariableDeclarations().add(new StepEventuality(descendantStep, this));
					}
				}
			}
		}
		return result;
	}

	public VariableDeclaration getResultVariableDeclaration(Step step) {
		if (step.getOperationBuilder().getOperationResultClass(this, step) != null) {
			return new StepEventuality(step, this);
		} else {
			return null;
		}
	}

	@Override
	public void validate(boolean recursively) throws ValidationError {
		super.validate(recursively);
		List<String> stepNames = new ArrayList<String>();
		for (Step step : steps) {
			if (stepNames.contains(step.getName())) {
				throw new ValidationError("Duplicate step name detected: '" + step.getName() + "'");
			} else {
				stepNames.add(step.getName());
			}
		}
		if (recursively) {
			for (Step step : steps) {
				try {
					step.validate(recursively, this);
				} catch (ValidationError e) {
					throw new ValidationError("Failed to validate step '" + step.getName() + "'", e);
				}
			}
			for (Transition transition : transitions) {
				try {
					transition.validate(recursively, this);
				} catch (ValidationError e) {
					throw new ValidationError("Failed to validate transition '" + transition.getSummary() + "'", e);
				}
			}
			activator.validate(recursively, this);
			if (isOutputEnabled()) {
				outputBuilder.getFacade().validate(recursively, getValidationContext(null).getVariableDeclarations());
			}
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

		public ValidationContext(ValidationContext parentContext, VariableDeclaration... newDeclarations) {
			plan = parentContext.getPlan();
			step = parentContext.getStep();
			variableDeclarations.addAll(parentContext.getVariableDeclarations());
			for (VariableDeclaration newDeclaration : newDeclarations) {
				variableDeclarations.add(newDeclaration);
			}
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

		private Session session;
		private Plan plan;
		private Step currentStep;
		private List<Variable> variables = new ArrayList<Variable>();

		public ExecutionContext(Session session, Plan plan) {
			this.session = session;
			this.plan = plan;
		}

		public Session getSession() {
			return session;
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

	}

	public interface ExecutionInspector {

		void beforeOperation(StepCrossing stepCrossing);

		boolean isExecutionInterrupted();

		void afterOperation(StepCrossing stepCrossing);

		void logInformation(String message);

		void logError(String message);

		void logWarning(String message);

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

	private class OutputClassNameAccessor extends Accessor<String> {
		@Override
		public String get() {
			Class<?> outputClass = activator.getOutputClass();
			if (outputClass == null) {
				return null;
			}
			return outputClass.getName();
		}
	}

}
