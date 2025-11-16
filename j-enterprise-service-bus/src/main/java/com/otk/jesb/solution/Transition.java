package com.otk.jesb.solution;

import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.Function;
import com.otk.jesb.PotentialError;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.Variable;
import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.compiler.CompiledFunction;
import com.otk.jesb.compiler.CompiledFunction.FunctionCallError;
import com.otk.jesb.util.MiscUtils;

/**
 * This class represents the arcs that connect {@link Step} instances in a
 * {@link Plan}.
 * 
 * @author olitank
 *
 */
public class Transition extends PlanElement {

	private Step startStep;
	private Step endStep;
	private String label;
	private Condition condition;

	public Step getStartStep() {
		return startStep;
	}

	public void setStartStep(Step startStep) {
		this.startStep = startStep;
	}

	public Step getEndStep() {
		return endStep;
	}

	public void setEndStep(Step endStep) {
		this.endStep = endStep;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public Condition getCondition() {
		return condition;
	}

	public void setCondition(Condition condition) {
		this.condition = condition;
	}

	@Override
	public void validate(boolean recursively, Plan plan) throws ValidationError {
		for (Transition otherTransition : plan.getTransitions()) {
			if (otherTransition == this) {
				continue;
			}
			if ((startStep != null) && (endStep != null)) {
				if ((startStep == otherTransition.startStep) && (endStep == otherTransition.endStep)) {
					throw new ValidationError("Duplicate transition detected");
				}
			}
		}
		if (recursively) {
			if (condition != null) {
				try {
					condition.validate(plan.getTransitionContextVariableDeclarations(this));
				} catch (ValidationError e) {
					throw new ValidationError("Failed to valide condition", e);
				}
			}
		}
	}

	@Override
	public String getSummary() {
		return getStartStep().getName() + " => " + getEndStep().getName();
	}

	public List<VariableDeclaration> getVariableDeclarations() {
		List<VariableDeclaration> result = new ArrayList<VariableDeclaration>();
		if (condition instanceof ExceptionCondition) {
			ExceptionCondition exceptionCondition = (ExceptionCondition) condition;
			VariableDeclaration exceptionVariableDeclaration = exceptionCondition.getExceptionVariableDeclaration();
			if (exceptionVariableDeclaration != null) {
				result.add(exceptionVariableDeclaration);
			}
		}
		return result;
	}

	public static List<Transition> computeValidTranstions(List<Transition> transitions,
			Plan.ExecutionError executionError, Plan.ExecutionContext context) throws FunctionCallError {
		List<Transition> result = new ArrayList<Transition>();
		List<Transition> elseTransitions = new ArrayList<Transition>();
		for (Transition transition : transitions) {
			if (transition.getCondition() != null) {
				if (transition.getCondition() instanceof IfCondition) {
					if (executionError == null) {
						if (((IfCondition) transition.getCondition()).isFulfilled(
								context.getPlan().getTransitionContextVariableDeclarations(transition),
								context.getVariables())) {
							result.add(transition);
						}
					}
				} else if (transition.getCondition() instanceof ElseCondition) {
					if (executionError == null) {
						elseTransitions.add(transition);
					}
				} else if (transition.getCondition() instanceof ExceptionCondition) {
					if (executionError != null) {
						ExceptionCondition exceptionCondition = (ExceptionCondition) transition.getCondition();
						if (exceptionCondition.isFullfilled(executionError.getCause())) {
							result.add(transition);
							Variable exceptionVariable = exceptionCondition
									.getExceptionVariable(executionError.getCause());
							if (exceptionVariable != null) {
								context.getVariables().add(exceptionVariable);
								/*
								 * TODO: PROBLEM: the exception variable is added only to valid execution branch
								 * contexts.
								 */
								/*
								 * TODO: Check if a unique context is abnormally used for all execution
								 * branches.
								 */
							}
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

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		if (label != null) {
			result.append(label);
		} else {
			if (condition instanceof IfCondition) {
				result.append("?");
			} else if (condition instanceof ElseCondition) {
				result.append("x");
			} else if (condition instanceof ExceptionCondition) {
				result.append("!");
			}
		}
		return result.toString();
	}

	public static interface Condition {

		void validate(List<VariableDeclaration> variableDeclarations) throws ValidationError;

	}

	public static class IfCondition extends Function implements Condition {
		public IfCondition() {
			setFunctionBody("return true;");
		}

		@Override
		public String toString() {
			return "If: " + getFunctionBody();
		}

		public boolean isFulfilled(List<VariableDeclaration> variableDeclarations, List<Variable> variables)
				throws FunctionCallError {
			CompiledFunction compiledFunction;
			try {
				compiledFunction = getCompiledVersion(null, variableDeclarations, boolean.class);
			} catch (CompilationError e) {
				throw new PotentialError(e);
			}
			return (boolean) compiledFunction.call(variables);
		}

		@Override
		public void validate(List<VariableDeclaration> variableDeclarations) throws ValidationError {
			try {
				getCompiledVersion(null, variableDeclarations, boolean.class);
			} catch (CompilationError e) {
				throw new ValidationError("Failed to validate the predicate", e);
			}
		}

	}

	public static class ElseCondition implements Condition {
		@Override
		public String toString() {
			return "Else";
		}

		@Override
		public void validate(List<VariableDeclaration> variableDeclarations) throws ValidationError {
		}
	}

	public static class ExceptionCondition implements Condition {
		private String exceptionTypeName = Exception.class.getName();
		private String exceptionVariableName;

		public String getExceptionTypeName() {
			return exceptionTypeName;
		}

		public void setExceptionTypeName(String exceptionTypeName) {
			this.exceptionTypeName = exceptionTypeName;
		}

		public String getExceptionVariableName() {
			return exceptionVariableName;
		}

		public void setExceptionVariableName(String exceptionVariableName) {
			this.exceptionVariableName = exceptionVariableName;
		}

		public VariableDeclaration getExceptionVariableDeclaration() {
			if (exceptionVariableName == null) {
				return null;
			}
			return new VariableDeclaration() {
				@Override
				public Class<?> getVariableType() {
					return MiscUtils.getJESBClass(exceptionTypeName);
				}

				@Override
				public String getVariableName() {
					return exceptionVariableName;
				}
			};
		}

		public Variable getExceptionVariable(Throwable exception) {
			if (exceptionVariableName == null) {
				return null;
			}
			return new Variable() {

				@Override
				public Object getValue() {
					return exception;
				}

				@Override
				public String getName() {
					return exceptionVariableName;
				}
			};
		}

		public boolean isFullfilled(Throwable thrown) {
			Class<?> exceptionClass;
			try {
				exceptionClass = MiscUtils.getJESBClass(exceptionTypeName);
			} catch (Throwable t) {
				throw new PotentialError(t);
			}
			return exceptionClass.isInstance(thrown);
		}

		@Override
		public void validate(List<VariableDeclaration> variableDeclarations) throws ValidationError {
			MiscUtils.getJESBClass(exceptionTypeName);
		}

		@Override
		public String toString() {
			return exceptionTypeName;
		}

	}

}
