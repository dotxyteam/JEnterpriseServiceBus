package com.otk.jesb;

import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.Structure.ClassicStructure;
import com.otk.jesb.activity.Activity;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.instantiation.EvaluationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.MiscUtils;

public class Plan extends Asset {

	private static final String INPUT_VARIABLE_NAME = "_planInput_";

	public Plan() {
		this(Plan.class.getSimpleName() + MiscUtils.getDigitalUniqueIdentifier());
	}

	public Plan(String name) {
		super(name);
	}

	private List<Step> steps = new ArrayList<Step>();
	private List<Transition> transitions = new ArrayList<Transition>();
	private ClassicStructure inputStructure;
	private ClassicStructure outputStructure;
	private Class<?> inputClass;
	private Class<?> outputClass;
	private RootInstanceBuilder outputBuilder = new RootInstanceBuilder(Plan.class.getSimpleName() + "Output",
			new Accessor<String>() {
				@Override
				public String get() {
					if (outputClass == null) {
						return null;
					}
					return outputClass.getName();
				}
			});

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
		updateInputClass();
	}

	public ClassicStructure getOutputStructure() {
		return outputStructure;
	}

	public void setOutputStructure(ClassicStructure outputStructure) {
		this.outputStructure = outputStructure;
		updateOutputClass();
	}

	public RootInstanceBuilder getOutputBuilder() {
		return outputBuilder;
	}

	public void setOutputBuilder(RootInstanceBuilder outputBuilder) {
		this.outputBuilder = outputBuilder;
	}

	private void updateInputClass() {
		if (inputStructure == null) {
			inputClass = null;
		} else {
			try {
				String className = Plan.class.getPackage().getName() + "." + Plan.class.getSimpleName() + "Input"
						+ MiscUtils.getDigitalUniqueIdentifier();
				inputClass = MiscUtils.IN_MEMORY_JAVA_COMPILER.compile(className,
						inputStructure.generateJavaTypeSourceCode(className));
			} catch (CompilationError e) {
				throw new AssertionError(e);
			}
		}
	}

	private void updateOutputClass() {
		if (outputStructure == null) {
			outputClass = null;
		} else {
			try {
				String className = Plan.class.getPackage().getName() + "." + Plan.class.getSimpleName() + "Output"
						+ MiscUtils.getDigitalUniqueIdentifier();
				outputClass = MiscUtils.IN_MEMORY_JAVA_COMPILER.compile(className,
						outputStructure.generateJavaTypeSourceCode(className));
			} catch (CompilationError e) {
				throw new AssertionError(e);
			}
		}
	}

	public Class<?> getInputClass() {
		return inputClass;
	}

	public Class<?> getOutputClass() {
		return outputClass;
	}

	public List<Step> getPreviousSteps(Step step) {
		return getPreviousSteps(step, steps);
	}

	private List<Step> getPreviousSteps(Step step, List<Step> steps) {
		List<Step> result = new ArrayList<Step>();
		for (Step candidate : steps) {
			if (isPreviousStep(candidate, step)) {
				result.add(candidate);
			}
		}
		return result;
	}

	private boolean isPreviousStep(Step s1, Step s2) {
		for (Transition t : transitions) {
			if ((t.getStartStep() == s1) && (t.getEndStep() == s2)) {
				return true;
			}
			if ((t.getStartStep() == s1) && isPreviousStep(t.getEndStep(), s2)) {
				return true;
			}
		}
		return false;
	}

	public Object execute(Object input) throws Throwable {
		return execute(input, new ExecutionInspector() {

			@Override
			public void beforeActivityCreation(StepOccurrence stepOccurrence) {
			}

			@Override
			public void afterActivityExecution(StepOccurrence stepOccurrence) {
			}

			@Override
			public boolean isExecutionInterrupted() {
				return false;
			}
		});
	}

	public Object execute(final Object input, ExecutionInspector executionInspector) throws Throwable {
		ExecutionContext context = new ExecutionContext(this);
		if (inputClass != null) {
			context.getVariables().add(new ExecutionContext.Variable() {

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
		execute(steps, context, executionInspector);
		return outputBuilder.build(new EvaluationContext(context, null));
	}

	private void execute(List<Step> steps, ExecutionContext context, ExecutionInspector executionInspector)
			throws Throwable {
		if (executionInspector.isExecutionInterrupted()) {
			return;
		}
		if (steps.size() == 0) {
			return;
		}
		Step step = steps.get(0);
		List<Step> previousSteps = getPreviousSteps(step, steps);
		if (previousSteps.size() > 0) {
			execute(previousSteps, context, executionInspector);
		}
		execute(step, context, executionInspector);
		List<Step> followingSteps = new ArrayList<Step>();
		{
			followingSteps.addAll(steps);
			followingSteps.removeAll(previousSteps);
			followingSteps.remove(step);
		}
		if (followingSteps.size() > 0) {
			execute(followingSteps, context, executionInspector);
		}
	}

	private void execute(Step step, ExecutionContext context, ExecutionInspector executionInspector) throws Throwable {
		context.setCutrrentStep(step);
		StepOccurrence stepOccurrence = new StepOccurrence(step);
		executionInspector.beforeActivityCreation(stepOccurrence);
		try {
			Activity activity = step.getActivityBuilder().build(context);
			stepOccurrence.setActivity(activity);
			Object activityResult = activity.execute();
			stepOccurrence.setActivityResult(activityResult);
			if (activityResult != null) {
				context.getVariables().add(stepOccurrence);
			}
		} catch (Throwable t) {
			stepOccurrence.setActivityError(t);
			throw t;
		}
		executionInspector.afterActivityExecution(stepOccurrence);
		context.setCutrrentStep(null);
	}

	public ValidationContext getValidationContext(Step currentStep) {
		ValidationContext result = new ValidationContext(this);
		if (inputClass != null) {
			result.getVariableDeclarations().add(new ValidationContext.VariableDeclaration() {

				@Override
				public String getVariableName() {
					return INPUT_VARIABLE_NAME;
				}

				@Override
				public Class<?> getVariableClass() {
					return inputClass;
				}
			});
		}
		List<Step> previousSteps = getPreviousSteps(currentStep);
		for (Step step : previousSteps) {
			if (step.getActivityBuilder().getActivityResultClass() != null) {
				result.getVariableDeclarations().add(new StepEventuality(step));
			}
		}
		return result;
	}

	public static class ExecutionContext {

		private Plan plan;
		private Step currentStep;
		private List<Variable> variables = new ArrayList<Variable>();

		public ExecutionContext(Plan plan) {
			this.plan = plan;
		}

		public ExecutionContext(Plan plan, Step currentStep, List<Variable> variables) {
			this.plan = plan;
			this.currentStep = currentStep;
			this.variables = variables;
		}

		public ExecutionContext(ExecutionContext parentContext, Variable newVariable) {
			this.plan = parentContext.getPlan();
			this.currentStep = parentContext.getCurrentStep();
			variables.addAll(parentContext.getVariables());
			variables.add(newVariable);
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

		public interface Variable {

			Object getValue();

			String getName();

		}

	}

	public static class ValidationContext {

		private Plan plan;
		private List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();

		public ValidationContext(Plan plan) {
			this.plan = plan;
		}

		public ValidationContext(Plan plan, List<VariableDeclaration> variableDeclarations) {
			this.plan = plan;
			this.variableDeclarations = variableDeclarations;
		}

		public ValidationContext(Plan plan, ValidationContext parentContext, VariableDeclaration newDeclaration) {
			this.plan = plan;
			variableDeclarations.addAll(parentContext.getVariableDeclarations());
			variableDeclarations.add(newDeclaration);
		}

		public Plan getPlan() {
			return plan;
		}

		public List<VariableDeclaration> getVariableDeclarations() {
			return variableDeclarations;
		}

		public interface VariableDeclaration {

			Class<?> getVariableClass();

			String getVariableName();

		}

	}

	public interface ExecutionInspector {

		void beforeActivityCreation(StepOccurrence stepOccurrence);

		boolean isExecutionInterrupted();

		void afterActivityExecution(StepOccurrence stepOccurrence);

	}

}
