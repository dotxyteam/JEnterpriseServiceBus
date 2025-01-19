package com.otk.jesb.compiler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.Plan;
import com.otk.jesb.meta.TypeInfoProvider;
import com.otk.jesb.util.MiscUtils;

public class CompiledFunction {

	private Class<?> functionClass;

	private CompiledFunction(Class<?> functionClass) {
		this.functionClass = functionClass;
	}

	public static CompiledFunction get(String functionBody, Plan.ValidationContext context) throws CompilationError {
		String functionClassName = "Function" + MiscUtils.getDigitalUniqueIdentifier();
		String preBody = "";
		preBody += "public class " + functionClassName + "{" + "\n";
		preBody += "public static Object execute(";
		List<String> declrartionStrings = new ArrayList<String>();
		for (Plan.ValidationContext.Declaration declaration : context.getDeclarations()) {
			declrartionStrings.add(
					declaration.getPropertyClass().getName().replace("$", ".") + " " + declaration.getPropertyName());
		}
		preBody += MiscUtils.stringJoin(declrartionStrings, ", ");
		preBody += ") throws " + Throwable.class.getName() + "{" + "\n";
		String postBody = "";
		postBody += "}";
		postBody += "}";
		Class<?> functionClass;
		try {
			functionClass = MiscUtils.IN_MEMORY_JAVA_COMPILER.compile(functionClassName,
					preBody + functionBody + postBody, TypeInfoProvider.getClassLoader());
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
			for (Plan.ExecutionContext.Property property : context.getProperties()) {
				if (param.getName().equals(property.getName())) {
					functionParameterValues[i] = property.getValue();
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
