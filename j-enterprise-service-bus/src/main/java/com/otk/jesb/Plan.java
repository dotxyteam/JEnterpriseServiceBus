package com.otk.jesb;

import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.activity.Activity;
import com.otk.jesb.activity.ActivityResult;

public class Plan extends Asset {

	public Plan(String name) {
		super(name);
	}

	private List<Step> steps = new ArrayList<Step>();
	private List<Transition> transitions = new ArrayList<Transition>();

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

	public void execute() throws Exception {
		execute(new ExecutionInspector() {

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

	public void execute(ExecutionInspector executionInspector) throws Exception {
		ExecutionContext context = new ExecutionContext(this);
		execute(steps, context, executionInspector);
	}

	private void execute(List<Step> steps, ExecutionContext context, ExecutionInspector executionInspector)
			throws Exception {
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

	private void execute(Step step, ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
		context.setCutrrentStep(step);
		StepOccurrence stepOccurrence = new StepOccurrence(step);
		executionInspector.beforeActivityCreation(stepOccurrence);
		Activity activity = step.getActivityBuilder().build(context);
		stepOccurrence.setActivity(activity);
		try {
			ActivityResult activityResult = activity.execute();
			stepOccurrence.setActivityResult(activityResult);
			if (activityResult != null) {
				context.getProperties().add(stepOccurrence);
			}
		} catch (Exception e) {
			stepOccurrence.setActivityError(e);
			throw e;
		}
		executionInspector.afterActivityExecution(stepOccurrence);
		context.setCutrrentStep(null);		
	}

	public ValidationContext getValidationContext(Step currentStep) {
		ValidationContext result = new ValidationContext(this);
		List<Step> previousSteps = getPreviousSteps(currentStep);
		for (Step step : previousSteps) {
			if (step.getActivityBuilder().getActivityResultClass() != null) {
				result.getDeclarations().add(new StepEventuality(step));
			}
		}
		return result;
	}

	public static class ExecutionContext {

		private List<Property> properties = new ArrayList<Property>();
		private Plan plan;
		private Step currentStep;

		public ExecutionContext(Plan plan) {
			this.plan = plan;
		}

		

		public ExecutionContext(ExecutionContext parentContext, Property newProperty) {
			this.plan = parentContext.getPlan();
			properties.addAll(parentContext.getProperties());
			properties.add(newProperty);
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
		public List<Property> getProperties() {
			return properties;
		}

		public interface Property {

			Object getValue();

			String getName();

		}

	}

	public static class ValidationContext {

		private List<Declaration> declarations = new ArrayList<Declaration>();
		private Plan plan;

		public ValidationContext(Plan plan) {
			this.plan = plan;
		}

		public ValidationContext(Plan plan, ValidationContext parentContext, Declaration newDeclaration) {
			this.plan = plan;
			declarations.addAll(parentContext.getDeclarations());
			declarations.add(newDeclaration);
		}

		public Plan getPlan() {
			return plan;
		}

		public List<Declaration> getDeclarations() {
			return declarations;
		}

		public interface Declaration {

			Class<?> getPropertyClass();

			String getPropertyName();

		}

	}

	public interface ExecutionInspector {

		void beforeActivityCreation(StepOccurrence stepOccurrence);

		boolean isExecutionInterrupted();

		void afterActivityExecution(StepOccurrence stepOccurrence);

	}

}
