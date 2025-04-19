package com.otk.jesb.compiler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.Plan;
import com.otk.jesb.ValidationContext;
import com.otk.jesb.util.MiscUtils;

public class CompiledFunction {

	private Class<?> functionClass;
	private String functionClassSource;

	private CompiledFunction(Class<?> functionClass, String functionClassSource) {
		this.functionClass = functionClass;
		this.functionClassSource = functionClassSource;
	}

	public static CompiledFunction get(String functionBody, ValidationContext context, Class<?> returnType)
			throws CompilationError {
		String functionClassName = CompiledFunction.class.getPackage().getName() + "."
				+ CompiledFunction.class.getSimpleName() + MiscUtils.getDigitalUniqueIdentifier();
		String preBody = "";
		preBody += "package " + MiscUtils.extractPackageNameFromClassName(functionClassName) + ";" + "\n";
		preBody += "public class " + MiscUtils.extractSimpleNameFromClassName(functionClassName) + "{" + "\n";
		preBody += "public static " + MiscUtils.adaptClassNameToSourceCode(returnType.getName()) + " execute(";
		List<String> declrartionStrings = new ArrayList<String>();
		for (ValidationContext.VariableDeclaration declaration : context.getVariableDeclarations()) {
			declrartionStrings.add(MiscUtils.adaptClassNameToSourceCode(declaration.getVariableType().getName()) + " "
					+ declaration.getVariableName());
		}
		preBody += MiscUtils.stringJoin(declrartionStrings, ", ");
		preBody += ") throws " + Throwable.class.getName() + "{" + "\n";
		String postBody = "\n";
		postBody += "}" + "\n";
		postBody += "}";
		String functionClassSource = preBody + functionBody + postBody;
		Class<?> functionClass;
		try {
			functionClass = MiscUtils.IN_MEMORY_JAVA_COMPILER.compile(functionClassName, functionClassSource);
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
		return new CompiledFunction(functionClass, functionClassSource);
	}

	public Object execute(Plan.ExecutionContext context) throws Exception {
		Object[] functionParameterValues = new Object[functionClass.getMethods()[0].getParameterCount()];
		int i = 0;
		for (Parameter param : functionClass.getMethods()[0].getParameters()) {
			for (Plan.ExecutionContext.Variable variable : context.getVariables()) {
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
			throw new AssertionError(e);
		} catch (IllegalArgumentException e) {
			throw new AssertionError(e);
		} catch (SecurityException e) {
			throw new AssertionError(e);
		} catch (Throwable t) {
			if (t instanceof InvocationTargetException) {
				t = ((InvocationTargetException) t).getTargetException();
			}
			String errorSourceDescription;
			if ((t.getStackTrace().length > 0) && t.getStackTrace()[0].getClassName().equals(functionClass.getName())
					&& (t.getStackTrace()[0].getLineNumber() > 0)) {
				errorSourceDescription = "/* Failure statement */\n"
						+ functionClassSource.split("\n")[t.getStackTrace()[0].getLineNumber() - 1];
			} else {
				errorSourceDescription = "/* Function class source code (" + functionClass.getSimpleName()
						+ ".java) */\n" + functionClassSource;
			}
			throw new Exception("Function error: " + t.toString() + ":\n" + errorSourceDescription, t);
		}
	}

}
