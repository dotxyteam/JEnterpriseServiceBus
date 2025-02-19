package com.otk.jesb.compiler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.Plan;
import com.otk.jesb.util.MiscUtils;

public class CompiledFunction {

	private Class<?> functionClass;

	private CompiledFunction(Class<?> functionClass) {
		this.functionClass = functionClass;
	}

	public static CompiledFunction get(String functionBody, Plan.ValidationContext context, Class<?> returnType)
			throws CompilationError {
		String functionClassName = CompiledFunction.class.getPackage().getName() + "."
				+ CompiledFunction.class.getSimpleName() + MiscUtils.getDigitalUniqueIdentifier();
		String preBody = "";
		preBody += "package " + MiscUtils.extractPackageNameFromClassName(functionClassName) + ";" + "\n";
		preBody += "public class " + MiscUtils.extractSimpleNameFromClassName(functionClassName) + "{" + "\n";
		preBody += "public static " + MiscUtils.adaptClassNameToSourceCode(returnType.getName()) + " execute(";
		List<String> declrartionStrings = new ArrayList<String>();
		for (Plan.ValidationContext.VariableDeclaration declaration : context.getVariableDeclarations()) {
			declrartionStrings.add(MiscUtils.adaptClassNameToSourceCode(declaration.getVariableClass().getName()) + " "
					+ declaration.getVariableName());
		}
		preBody += MiscUtils.stringJoin(declrartionStrings, ", ");
		preBody += ") throws " + Throwable.class.getName() + "{" + "\n";
		String postBody = "";
		postBody += "}";
		postBody += "}";
		Class<?> functionClass;
		try {
			functionClass = MiscUtils.IN_MEMORY_JAVA_COMPILER.compile(functionClassName,
					preBody + functionBody + postBody);
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
			throw new CompilationError(startPosition, endPosition, e.getMessage());
		}
		return new CompiledFunction(functionClass);
	}

	public Object execute(Plan.ExecutionContext context) throws Throwable {
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
		} catch (InvocationTargetException e) {
			throw e.getTargetException();
		} catch (SecurityException e) {
			throw new AssertionError(e);
		}
	}

}
