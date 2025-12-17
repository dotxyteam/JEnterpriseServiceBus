package com.otk.jesb.compiler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.otk.jesb.StandardError;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.Variable;
import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.util.MiscUtils;

public class CompiledFunction<T> {

	private Class<?> functionClass;
	private String functionClassSource;
	private Solution solutionInstance;

	private CompiledFunction(Class<?> functionClass, String functionClassSource, Solution solutionInstance) {
		this.functionClass = functionClass;
		this.functionClassSource = functionClassSource;
		this.solutionInstance = solutionInstance;
	}

	public Class<?> getFunctionClass() {
		return functionClass;
	}

	public String getFunctionClassSource() {
		return functionClassSource;
	}

	public Solution getSolutionInstance() {
		return solutionInstance;
	}

	public static <T> CompiledFunction<T> get(String functionBody, List<VariableDeclaration> variableDeclarations,
			Class<T> returnType, Solution solutionInstance) throws CompilationError {
		String functionClassName = CompiledFunction.class.getPackage().getName() + "."
				+ CompiledFunction.class.getSimpleName() + MiscUtils.getDigitalUniqueIdentifier();
		String preBody = "";
		preBody += "package " + MiscUtils.extractPackageNameFromClassName(functionClassName) + ";" + "\n";
		preBody += "public class " + MiscUtils.extractSimpleNameFromClassName(functionClassName) + "{" + "\n";
		preBody += "public static " + MiscUtils.adaptClassNameToSourceCode(returnType.getName()) + " execute(";
		List<String> declrartionStrings = new ArrayList<String>();
		for (VariableDeclaration declaration : variableDeclarations) {
			declrartionStrings.add(MiscUtils.adaptClassNameToSourceCode(declaration.getVariableType().getName()) + " "
					+ declaration.getVariableName());
		}
		if (declrartionStrings.size() > 0) {
			preBody += "\n    " + MiscUtils.stringJoin(declrartionStrings, ",\n    ") + "\n";
		}
		preBody += ") throws " + Throwable.class.getName() + "{" + "\n";
		String postBody = "\n";
		postBody += "}" + "\n";
		postBody += "}";
		String functionClassSource = preBody + functionBody + postBody;
		Class<?> functionClass;
		try {
			functionClass = solutionInstance.getRuntime().getInMemoryCompiler().compile(functionClassName,
					functionClassSource);
		} catch (CompilationError e) {
			int startPosition = e.getStartPosition() - preBody.length();
			if (startPosition < 0) {
				startPosition = 0;
			}
			if (startPosition >= functionBody.length()) {
				startPosition = 0;
			}
			int endPosition = e.getEndPosition() - preBody.length();
			if (endPosition < 0) {
				endPosition = functionBody.length() - 1;
			}
			if (endPosition >= functionBody.length()) {
				endPosition = functionBody.length() - 1;
			}
			throw new CompilationError(startPosition, endPosition, e.getMessage(), null, functionBody, e);
		}
		return new CompiledFunction<T>(functionClass, functionClassSource, solutionInstance);
	}

	@SuppressWarnings("unchecked")
	public T call(Variable... variables) throws FunctionCallError {
		return (T) call(Arrays.asList(variables));
	}

	public Object call(List<Variable> variables) throws FunctionCallError {
		Object[] functionParameterValues = new Object[functionClass.getMethods()[0].getParameterCount()];
		int i = 0;
		for (Parameter param : functionClass.getMethods()[0].getParameters()) {
			for (Variable variable : MiscUtils.getReverse(variables)) {
				if (param.getName().equals(variable.getName())) {
					functionParameterValues[i] = variable.getValue();
					break;
				}
			}
			i++;
		}
		try {
			return functionClass.getMethods()[0].invoke(null, functionParameterValues);
		} catch (IllegalAccessException e) {
			throw new UnexpectedError(e);
		} catch (IllegalArgumentException e) {
			throw new UnexpectedError(e);
		} catch (SecurityException e) {
			throw new UnexpectedError(e);
		} catch (Throwable t) {
			if (t instanceof InvocationTargetException) {
				t = ((InvocationTargetException) t).getTargetException();
			}
			throw new FunctionCallError(this, t);
		}
	}

	public static class FunctionCallError extends StandardError {

		private static final long serialVersionUID = 1L;

		private CompiledFunction<?> compiledFunction;

		public FunctionCallError(CompiledFunction<?> compiledFunction, Throwable cause) {
			super(cause);
			this.compiledFunction = compiledFunction;
		}

		@Override
		public String getMessage() {
			return getCause().toString() + "\n" + describeSource();
		}

		public String describeSource() {
			String result;
			Throwable t = getCause();
			if ((t.getStackTrace().length > 0)
					&& t.getStackTrace()[0].getClassName().equals(compiledFunction.getFunctionClass().getName())
					&& (t.getStackTrace()[0].getLineNumber() > 0)) {
				result = "/* The instruction that raised the exception */\n" + compiledFunction.getFunctionClassSource()
						.split("\n")[t.getStackTrace()[0].getLineNumber() - 1];
			} else {
				result = "/* The source code of the function that crashed ("
						+ compiledFunction.getFunctionClass().getSimpleName() + ".java) */\n"
						+ compiledFunction.getFunctionClassSource();
			}
			return result;
		}

	}

}
