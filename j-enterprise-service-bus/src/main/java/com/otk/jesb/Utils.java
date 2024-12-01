package com.otk.jesb;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.enumeration.IEnumerationTypeInfo;
import xy.reflect.ui.util.ClassUtils;

public class Utils {

	public static Object executeScript(String code, Plan.ExecutionContext context) {
		Binding binding = new Binding();
		for (StepOccurrence stepOccurrence : context.getStepOccurrences()) {
			binding.setVariable(stepOccurrence.getStep().getName(), stepOccurrence.getActivityResult());
		}
		GroovyShell shell = new GroovyShell(binding);
		return shell.evaluate(code);
	}

	public static boolean isComplexType(ITypeInfo type) {
		try {
			if (ClassUtils.isPrimitiveClassOrWrapperOrString(ClassUtils.forNameEvenIfPrimitive(type.getName()))) {
				return false;
			}
			if (type instanceof IEnumerationTypeInfo) {
				return false;
			}
			return true;
		} catch (ClassNotFoundException e) {
			throw new AssertionError(e);
		}
	}

}
