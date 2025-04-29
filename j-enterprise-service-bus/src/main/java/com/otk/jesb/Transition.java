package com.otk.jesb;

import java.util.List;

import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.compiler.CompiledFunction;
import com.otk.jesb.compiler.CompiledFunction.FunctionCallError;

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

	@Override
	public String toString() {
		return ((label != null) && (label.length() > 0)) ? label : ((condition != null) ? condition.toString() : "");
	}

	public static interface Condition {

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
				throw new AssertionError(e);
			}
			return (boolean) compiledFunction.call(variables);
		}
	}

	public static class ElseCondition implements Condition {
		@Override
		public String toString() {
			return "Else";
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
				exceptionClass = Class.forName(exceptionTypeName);
			} catch (ClassNotFoundException e) {
				throw new AssertionError(e);
			}
			return exceptionClass.isInstance(thrown);
		}

		@Override
		public String toString() {
			return "Try/Catch: " + exceptionTypeName;
		}

	}
}
