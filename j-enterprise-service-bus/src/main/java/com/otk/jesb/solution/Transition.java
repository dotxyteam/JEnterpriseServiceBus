package com.otk.jesb.solution;

import java.util.List;

import com.otk.jesb.Function;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.Variable;
import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.compiler.CompiledFunction;
import com.otk.jesb.compiler.CompiledFunction.FunctionCallError;
import com.otk.jesb.meta.TypeInfoProvider;

public class Transition {

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
	public String toString() {
		return ((label != null) && (label.length() > 0)) ? label : ((condition != null) ? condition.toString() : "");
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
				compiledFunction = CompiledFunction.get(getFunctionBody(), variableDeclarations, boolean.class);
			} catch (CompilationError e) {
				throw new UnexpectedError(e);
			}
			return (boolean) compiledFunction.call(variables);
		}

		@Override
		public void validate(List<VariableDeclaration> variableDeclarations) throws ValidationError {
			try {
				CompiledFunction.get(getFunctionBody(), variableDeclarations, boolean.class);
			} catch (CompilationError e) {
				throw new ValidationError(e.toString(), e);
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
				exceptionClass = TypeInfoProvider.getClass(exceptionTypeName);
			} catch (Throwable t) {
				throw new UnexpectedError(t);
			}
			return exceptionClass.isInstance(thrown);
		}

		@Override
		public void validate(List<VariableDeclaration> variableDeclarations) throws ValidationError {
			TypeInfoProvider.getClass(exceptionTypeName);
		}

		@Override
		public String toString() {
			return exceptionTypeName;
		}

	}

}
