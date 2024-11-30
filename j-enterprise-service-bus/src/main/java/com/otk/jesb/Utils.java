package com.otk.jesb;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

public class Utils {

	public static Object executeScript(String code, Plan.ExecutionContext context) {
		Binding binding = new Binding();
		for (StepOccurrence stepOccurrence : context.getStepOccurrences()) {
			binding.setVariable(stepOccurrence.getStep().getName(), stepOccurrence.getActivityResult());
		}
		GroovyShell shell = new GroovyShell(binding);
		return shell.evaluate(code);
	}

}
