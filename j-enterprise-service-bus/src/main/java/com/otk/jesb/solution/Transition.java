package com.otk.jesb.solution;

import java.util.List;

import com.otk.jesb.Function;
import com.otk.jesb.PotentialError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.Variable;
import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.compiler.CompiledFunction;
import com.otk.jesb.compiler.CompiledFunction.FunctionCallError;
import com.otk.jesb.util.MiscUtils;

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

	@Override
	public String toString() {
		return (label != null) ? label : ((condition != null) ? "?" : "");
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

		public String getExceptionTypeName() {
			return exceptionTypeName;
		}

		public void setExceptionTypeName(String exceptionTypeName) {
			this.exceptionTypeName = exceptionTypeName;
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
