package com.otk.jesb.solution;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

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
import com.otk.jesb.util.Accessor;

/**
 * This class allows to model a set of successive actions (instances of
 * {@link Operation}) and their trigger ({@link Activator}) in the form of
 * directed graphs (nodes are instances of {@link Step}).
 * 
 * @author olitank
 *
 */
public class Plan extends Asset {

	private String inputVariableName = "PLAN_INPUT";
	private Activator activator = new LaunchAtStartup();
	private List<Step> steps = new ArrayList<Step>();
	private List<Transition> transitions = new ArrayList<Transition>();
	private transient Set<PlanElement> selectedElements = new HashSet<PlanElement>();
	private transient PlanElement focusedElementSelectedSurrounding;
	private RootInstanceBuilder outputBuilder = new RootInstanceBuilder(Plan.class.getSimpleName() + "Output",
			new OutputClassNameAccessor());

	public Plan() {
		super();
	}

	public Plan(String name) {
		super(name);
	}

	public String getInputVariableName() {
		return inputVariableName;
	}

	public void setInputVariableName(String inputVariableName) {
		this.inputVariableName = inputVariableName;
	}

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

	public boolean isInputEnabled(Solution solutionInstance) {
		return activator.getInputClass(solutionInstance) != null;
	}

	public boolean isOutputEnabled(Solution solutionInstance) {
		return activator.getOutputClass(solutionInstance) != null;
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
		if (getSelectedElements().size() == 0) {
			result.addAll(steps);
			result.addAll(transitions);
		} else {
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
					result.addAll(transitions.stream()
							.filter(anyTransition -> (anyTransition.getStartStep() == transition.getStartStep())
									&& (anyTransition.getEndStep() == transition.getEndStep()))
							.collect(Collectors.toList()));
					result.add(transition.getEndStep());
				} else {
					throw new UnexpectedError();
				}
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

	private List<Step> findLastSteps(List<Step> steps) {
		List<Step> result = new ArrayList<Step>(steps);
		for (Transition t : transitions) {
			result.remove(t.getStartStep());
		}
		return result;
	}

	public Object execute(final Object input, ExecutionInspector executionInspector, ExecutionContext context)
			throws ExecutionError {
		Solution solutionInstance = context.getSession().getSolutionInstance();
		try {
			if (!solutionInstance.getEnvironmentSettings().getEnvironmentVariableTreeElements().isEmpty()) {
				context.getVariables().add(solutionInstance.getEnvironmentSettings().getRootVariable(solutionInstance));
			}
			Class<?> inputClass = activator.getInputClass(solutionInstance);
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
						return inputVariableName;
					}
				});
			}
			execute(steps.stream().filter(step -> (step.getParent() == null)).collect(Collectors.toList()), context,
					executionInspector);
			if (executionInspector.isExecutionInterrupted()) {
				return null;
			}
			return outputBuilder.build(new InstantiationContext(context.getVariables(),
					getValidationContext(null, solutionInstance).getVariableDeclarations(), solutionInstance));
		} catch (Throwable t) {
			Reference<Plan> reference = Reference.get(this, solutionInstance);
			throw new ExecutionError(
					"Failed to execute plan (" + ((reference != null) ? reference.getPath() : this) + ")", t);
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
		List<Variable> initialVariables = new ArrayList<Variable>(context.getVariables());
		List<Variable> allVariables = new ArrayList<Variable>(initialVariables);
		for (Step firstStep : firstSteps) {
			context.getVariables().clear();
			context.getVariables().addAll(initialVariables);
			continueExecution(firstStep, true, context, allVariables, executionInspector);
		}
		if (executionInspector.isExecutionInterrupted()) {
			return;
		}
		List<Step> lastSteps = findLastSteps(steps);
		List<StepOccurrence> lastStepOccurrences = lastSteps.stream()
				.map(lastStep -> findStepOccurrence(lastStep, allVariables)).collect(Collectors.toList());
		if (lastStepOccurrences.contains(null)) {
			throw new UnexpectedError();
		}
		List<Variable> finalVariables = mergeStepOccurrenceVariables(lastStepOccurrences);
		context.getVariables().clear();
		context.getVariables().addAll(finalVariables);
	}

	private void continueExecution(Step fromStep, boolean executionBranchValid, ExecutionContext context,
			List<Variable> allTakenExecutionPathVariables, ExecutionInspector executionInspector)
			throws ExecutionError {
		if (executionInspector.isExecutionInterrupted()) {
			return;
		}
		List<Transition> outgoingTransitions = transitions.stream()
				.filter(transition -> (transition.getStartStep() == fromStep)).collect(Collectors.toList());
		StepOccurrence currentStepOccurrence = execute(fromStep, outgoingTransitions, executionBranchValid, context,
				executionInspector);
		allTakenExecutionPathVariables.add(currentStepOccurrence);
		final int TRANSITION_NOT_REACHED = 0;
		final int TRANSITION_REACHED_THROUGH_VALID_BRANCH = 1;
		final int TRANSITION_REACHED_THROUGH_INVALID_BRANCH = 2;
		for (Transition outgoingTransition : outgoingTransitions) {
			boolean endStepAlreadyExecuted = allTakenExecutionPathVariables.stream()
					.anyMatch(variable -> variable.getName().equals(outgoingTransition.getEndStep().getName()));
			if (endStepAlreadyExecuted) {
				/*
				 * It occurs when the end step is also in a path starting from a preceding
				 * sibling outgoing transition.
				 */
				continue;
			}
			List<Transition> convergentTransitions = transitions.stream()
					.filter(candidateConvergentTransition -> (candidateConvergentTransition
							.getEndStep() == outgoingTransition.getEndStep()))
					.collect(Collectors.toList());
			Map<Transition, Integer> statusByConvergentTransition = new HashMap<Transition, Integer>();
			for (Transition convergentTransition : convergentTransitions) {
				int convergentTransitionStatus = TRANSITION_NOT_REACHED;
				for (Variable variable : allTakenExecutionPathVariables) {
					if ((variable instanceof StepOccurrence)
							&& (((StepOccurrence) variable).getStep() == convergentTransition.getStartStep())) {
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
				statusByConvergentTransition.put(convergentTransition, convergentTransitionStatus);
			}
			boolean readyForNextStep = !statusByConvergentTransition.containsValue(TRANSITION_NOT_REACHED);
			if (readyForNextStep) {
				List<StepOccurrence> startStepOccurrences = convergentTransitions.stream()
						.map(convergentTransition -> findStepOccurrence(convergentTransition.getStartStep(),
								allTakenExecutionPathVariables))
						.collect(Collectors.toList());
				if (startStepOccurrences.contains(null)) {
					throw new UnexpectedError();
				}
				List<Variable> convergentTransitionsMergedVariables = mergeStepOccurrenceVariables(
						startStepOccurrences);
				context.getVariables().clear();
				context.getVariables().addAll(convergentTransitionsMergedVariables);
				if (outgoingTransition.getCondition() instanceof Transition.ExceptionCondition) {
					Transition.ExceptionCondition exceptionCondition = (Transition.ExceptionCondition) outgoingTransition
							.getCondition();
					Throwable exception = null;
					if (currentStepOccurrence instanceof StepCrossing) {
						StepCrossing currentStepCrossing = (StepCrossing) currentStepOccurrence;
						if (currentStepCrossing.getValidTransitions().contains(outgoingTransition)) {
							exception = currentStepCrossing.getOperationError();
						}
					}
					Variable exceptionVariable = exceptionCondition.getExceptionVariable(exception);
					if (exceptionVariable != null) {
						context.getVariables().add(exceptionVariable);
					}
				}
				boolean futureExecutionBranchValid = statusByConvergentTransition.entrySet().stream()
						.anyMatch(entry -> {
							Transition convergentTransition = entry.getKey();
							int convergentTransitionStatus = entry.getValue();
							if (convergentTransitionStatus != TRANSITION_REACHED_THROUGH_VALID_BRANCH) {
								return false;
							}
							StepCrossing convergentTransitionStartStepCrossing = (StepCrossing) startStepOccurrences
									.stream().filter(startStepOccurrence -> startStepOccurrence
											.getStep() == convergentTransition.getStartStep())
									.findFirst().get();
							return convergentTransitionStartStepCrossing.getValidTransitions()
									.contains(convergentTransition);
						});
				continueExecution(outgoingTransition.getEndStep(), futureExecutionBranchValid, context,
						allTakenExecutionPathVariables, executionInspector);
			}
			context.getVariables().forEach(variable -> {
				if (!allTakenExecutionPathVariables.contains(variable)) {
					allTakenExecutionPathVariables.add(variable);
				}
			});
		}
	}

	private List<Variable> mergeStepOccurrenceVariables(List<StepOccurrence> stepOccurrences) {
		List<Variable> result = new ArrayList<Variable>();
		stepOccurrences.stream().forEach(stepOccurrence -> {
			for (Variable variable : stepOccurrence.getPostVariablesSnapshot()) {
				if (!result.contains(variable)) {
					result.add(variable);
				}
			}
		});
		return result;
	}

	private StepOccurrence findStepOccurrence(Step step, List<Variable> variables) {
		for (Variable variable : variables) {
			if ((variable instanceof StepOccurrence) && (((StepOccurrence) variable).getStep() == step)) {
				return (StepOccurrence) variable;
			}
		}
		return null;
	}

	private StepOccurrence execute(Step step, List<Transition> outgoingTransitions, boolean executionBranchValid,
			ExecutionContext context, ExecutionInspector executionInspector) throws ExecutionError {
		Solution solutionInstance = context.getSession().getSolutionInstance();
		if (executionBranchValid) {
			StepCrossing stepCrossing = new StepCrossing(step, this, solutionInstance);
			ExecutionError executionError;
			try {
				try {
					execute(stepCrossing, context, executionInspector);
					executionError = null;
				} catch (ExecutionError e) {
					executionError = e;
				} finally {
					context.getVariables().add(stepCrossing);
				}
				if (executionInspector.isExecutionInterrupted()) {
					return stepCrossing;
				}
				if (outgoingTransitions.size() == 0) {
					if (executionError != null) {
						throw executionError;
					}
				} else {
					List<Transition> validTransitions;
					try {
						validTransitions = Transition.computeValidTranstions(outgoingTransitions, executionError,
								context);
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
									"Could not find any valid transition from step '" + step + "'");
							stepCrossing.setOperationError(planificationError);
							throw new ExecutionError(planificationError);
						}
					}
				}
			} finally {
				stepCrossing.capturePostVariables(context.getVariables());
			}
			return stepCrossing;
		} else {
			StepSkipping stepSkipping = new StepSkipping(step, this, solutionInstance);
			context.getVariables().add(stepSkipping);
			stepSkipping.capturePostVariables(context.getVariables());
			return stepSkipping;
		}
	}

	public List<VariableDeclaration> getTransitionContextVariableDeclarations(Transition transition,
			Solution solutionInstance) {
		List<VariableDeclaration> result = getValidationContext(transition.getStartStep(), solutionInstance)
				.getVariableDeclarations();
		VariableDeclaration startStepVariableDeclaration = getResultVariableDeclaration(transition.getStartStep(),
				solutionInstance);
		if (startStepVariableDeclaration != null) {
			result = new ArrayList<VariableDeclaration>(result);
			result.add(startStepVariableDeclaration);
		}
		return result;
	}

	private void execute(StepCrossing stepCrossing, ExecutionContext context, ExecutionInspector executionInspector)
			throws ExecutionError {
		Solution solutionInstance = context.getSession().getSolutionInstance();
		Step step = stepCrossing.getStep();
		context.setCutrrentStep(step);
		executionInspector.beforeOperation(stepCrossing);
		try {
			OperationBuilder<?> operationBuilder = step.getOperationBuilder();
			Operation operation = operationBuilder.build(context, executionInspector);
			stepCrossing.setOperation(operation);
			stepCrossing.setOperationResult(operation.execute(solutionInstance));
		} catch (Throwable t) {
			stepCrossing.setOperationError(t);
			throw new ExecutionError("An error occured at step '" + step.getName() + "'", t);
		} finally {
			executionInspector.afterOperation(stepCrossing);
		}
		context.setCutrrentStep(null);
	}

	public ValidationContext getValidationContext(Step currentStep, Solution solutionInstance) {
		ValidationContext result;
		if ((currentStep != null) && (currentStep.getParent() != null)) {
			result = getValidationContext(currentStep.getParent(), solutionInstance);
			for (VariableDeclaration declaration : currentStep.getParent().getContextualVariableDeclarations()) {
				result = new ValidationContext(result, declaration);
			}
		} else {
			result = new ValidationContext(this, currentStep);
			if (!solutionInstance.getEnvironmentSettings().getEnvironmentVariableTreeElements().isEmpty()) {
				result.getVariableDeclarations()
						.add(solutionInstance.getEnvironmentSettings().getRootVariableDeclaration(solutionInstance));
			}
			Class<?> inputClass = activator.getInputClass(solutionInstance);
			if (inputClass != null) {
				result.getVariableDeclarations().add(new VariableDeclaration() {

					@Override
					public String getVariableName() {
						return inputVariableName;
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
			VariableDeclaration stepVariableDeclaration = getResultVariableDeclaration(step, solutionInstance);
			result.getVariableDeclarations().addAll(getIncomingTransitionVariableDeclarations(step, solutionInstance));
			if (stepVariableDeclaration != null) {
				result.getVariableDeclarations().add(stepVariableDeclaration);
			}
		}
		result.getVariableDeclarations()
				.addAll(getIncomingTransitionVariableDeclarations(currentStep, solutionInstance));
		return result;
	}

	private List<VariableDeclaration> getIncomingTransitionVariableDeclarations(Step step, Solution solutionInstance) {
		List<VariableDeclaration> result = new ArrayList<VariableDeclaration>();
		List<Transition> incomingTransitions = transitions.stream()
				.filter(transition -> transition.getEndStep() == step).collect(Collectors.toList());
		incomingTransitions.forEach(transition -> result.addAll(transition.getVariableDeclarations(solutionInstance)));
		return result;
	}

	public VariableDeclaration getResultVariableDeclaration(Step step, Solution solutionInstance) {
		if (step.getOperationBuilder().getOperationResultClass(solutionInstance, this, step) != null) {
			return new StepEventuality(step, this, solutionInstance);
		} else {
			return null;
		}
	}

	@Override
	public void validate(boolean recursively, Solution solutionInstance) throws ValidationError {
		super.validate(recursively, solutionInstance);
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
					step.validate(recursively, solutionInstance, this);
				} catch (ValidationError e) {
					throw new ValidationError("Failed to validate step '" + step.getName() + "'", e);
				}
			}
			for (Transition transition : transitions) {
				try {
					transition.validate(recursively, solutionInstance, this);
				} catch (ValidationError e) {
					throw new ValidationError("Failed to validate transition '" + transition.getSummary() + "'", e);
				}
			}
			activator.validate(recursively, solutionInstance, this);
			if (isOutputEnabled(solutionInstance)) {
				outputBuilder.getFacade(solutionInstance).validate(recursively,
						getValidationContext(null, solutionInstance).getVariableDeclarations());
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
		private List<Variable> variables = new CopyOnWriteArrayList<Variable>();

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

		public static final ExecutionInspector DEFAULT = new Plan.ExecutionInspector() {

			@Override
			public boolean isExecutionInterrupted() {
				return false;
			}

			@Override
			public void beforeOperation(StepCrossing stepCrossing) {
			}

			@Override
			public void afterOperation(StepCrossing stepCrossing) {
			}
		};

		void beforeOperation(StepCrossing stepCrossing);

		boolean isExecutionInterrupted();

		void afterOperation(StepCrossing stepCrossing);

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

	private class OutputClassNameAccessor extends Accessor<Solution, String> {
		@Override
		public String get(Solution solutionInstance) {
			Class<?> outputClass = activator.getOutputClass(solutionInstance);
			if (outputClass == null) {
				return null;
			}
			return outputClass.getName();
		}
	}

}
