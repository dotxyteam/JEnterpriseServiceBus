package com.otk.jesb;

import java.util.ArrayList;
import java.util.List;

public class Plan extends Resource {

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
		ExecutionContext context = new ExecutionContext();
		execute(steps, context);
	}

	private void execute(List<Step> steps, ExecutionContext context) throws Exception {
		if (steps.size() == 0) {
			return;
		}
		Step step = steps.get(0);
		List<Step> previousSteps = getPreviousSteps(step, steps);
		if (previousSteps.size() > 0) {
			execute(previousSteps, context);
		}
		execute(step, context);
		List<Step> followingSteps = new ArrayList<Step>();
		{
			followingSteps.addAll(steps);
			followingSteps.removeAll(previousSteps);
			followingSteps.remove(step);
		}
		if (followingSteps.size() > 0) {
			execute(followingSteps, context);
		}
	}

	private void execute(Step step, ExecutionContext context) throws Exception {
		Activity activity = step.getActivityBuilder().build(context);
		if (step.getActivityBuilder().getActivityResultClass() != null) {
			ActivityResult activityResult = activity.execute();
			context.getProperties().add(new StepOccurrence(step, activityResult));
		}
	}

	public ValidationContext getValidationContext(Step currentStep) {
		ValidationContext result = new ValidationContext();
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

		public ExecutionContext() {
		}

		public ExecutionContext(ExecutionContext parentContext, Property newProperty) {
			properties.addAll(parentContext.getProperties());
			properties.add(newProperty);
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

		public ValidationContext() {
		}

		public ValidationContext(ValidationContext parentContext, Declaration newDeclaration) {
			declarations.addAll(parentContext.getDeclarations());
			declarations.add(newDeclaration);
		}

		public List<Declaration> getDeclarations() {
			return declarations;
		}

		public interface Declaration {

			Class<?> getPropertyClass();

			String getPropertyName();

		}

	}

}
