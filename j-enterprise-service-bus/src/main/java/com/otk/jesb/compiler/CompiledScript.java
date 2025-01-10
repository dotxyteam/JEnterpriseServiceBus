package com.otk.jesb.compiler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.Plan;
import com.otk.jesb.meta.TypeInfoProvider;
import com.otk.jesb.util.MiscUtils;

public class CompiledScript {

	private Class<?> scriptClass;

	private CompiledScript(Class<?> scriptClass) {
		this.scriptClass = scriptClass;
	}

	public static CompiledScript get(String script, Plan.ValidationContext context) throws CompilationError {
		String scriptClassName = "Script" + MiscUtils.getDigitalUniqueIdentifier();
		String preScript = "";
		preScript += "public class " + scriptClassName + "{" + "\n";
		preScript += "public static Object execute(";
		List<String> declrartionStrings = new ArrayList<String>();
		for (Plan.ValidationContext.Declaration declaration : context.getDeclarations()) {
			declrartionStrings.add(
					declaration.getPropertyClass().getName().replace("$", ".") + " " + declaration.getPropertyName());
		}
		preScript += MiscUtils.stringJoin(declrartionStrings, ", ");
		preScript += "){" + "\n";
		String postScript = "";
		postScript += "}";
		postScript += "}";
		Class<?> scriptClass;
		try {
			scriptClass = MiscUtils.compile(scriptClassName, preScript + script + postScript,
					TypeInfoProvider.getClassLoader());
		} catch (CompilationError e) {
			int startPosition = e.getStartPosition() - preScript.length();
			if (startPosition < 0) {
				startPosition = 0;
			}
			if (startPosition >= script.length()) {
				startPosition = 0;
			}
			int endPosition = e.getEndPosition() - preScript.length();
			if (endPosition < 0) {
				endPosition = script.length() - 1;
			}
			if (endPosition >= script.length()) {
				endPosition = script.length() - 1;
			}
			throw new CompilationError(startPosition, endPosition, e.getMessage());
		}
		return new CompiledScript(scriptClass);
	}

	public Object execute(Plan.ExecutionContext context) throws Throwable {
		Object[] scriptParameterValues = new Object[scriptClass.getMethods()[0].getParameterCount()];
		int i=0;
		for (Parameter param : scriptClass.getMethods()[0].getParameters()) {
			for (Plan.ExecutionContext.Property property : context.getProperties()) {
				if (param.getName().equals(property.getName())) {
					scriptParameterValues[i] = property.getValue();
					break;
				}
			}
			i++;
		}
		try {
			return scriptClass.getMethods()[0].invoke(null, scriptParameterValues);
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
