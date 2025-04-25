package com.otk.jesb;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.otk.jesb.Structure.ClassicStructure;
import com.otk.jesb.activity.Activity;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.instantiation.EvaluationContext;
import com.otk.jesb.instantiation.InstantiationFunction;
import com.otk.jesb.instantiation.InstantiationFunctionCompilationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
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
					throw new AssertionError(e);
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
					throw new AssertionError(e);
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
		Class<?> inputClass = upToDateInputClass.get();
		if (inputClass != null) {
			if (input != null) {
				if (!inputClass.isInstance(input)) {
					throw new AssertionError();
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
		return outputBuilder
				.build(new EvaluationContext(context.getVariables(), null, context.getComilationContextProvider()));
	}

	public void execute(List<Step> steps, ExecutionContext context, ExecutionInspector executionInspector)
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
			Activity activity = step.getActivityBuilder().build(context, executionInspector);
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
		List<Step> previousSteps = (currentStep != null) ? getPreviousSteps(currentStep) : steps;
		for (Step step : previousSteps) {
			if (step.getActivityBuilder().getActivityResultClass(this, step) != null) {
				result.getVariableDeclarations().add(new StepEventuality(step, this));
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

		public Function<InstantiationFunction, InstantiationFunctionCompilationContext> getComilationContextProvider() {
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

		void beforeActivityCreation(StepOccurrence stepOccurrence);

		boolean isExecutionInterrupted();

		void afterActivityExecution(StepOccurrence stepOccurrence);

	}

}
