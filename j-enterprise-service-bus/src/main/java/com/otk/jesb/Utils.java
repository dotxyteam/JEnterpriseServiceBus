package com.otk.jesb;

import com.otk.jesb.ObjectSpecification.DynamicValue;
import com.otk.jesb.ObjectSpecification.ValueMode;
import com.otk.jesb.Plan.ExecutionContext;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.enumeration.IEnumerationTypeInfo;
import xy.reflect.ui.util.ClassUtils;

public class Utils {

	public static Object executeScript(String code, Plan.ExecutionContext context) {
		Binding binding = new Binding();
		for (Plan.ExecutionContext.Property property : context.getProperties()) {
			binding.setVariable(property.getName(), property.getValue());
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

	public static Object interpretValue(Object value, ExecutionContext context) throws Exception {
		if (value instanceof DynamicValue) {
			return Utils.executeScript(((DynamicValue) value).getScript(), context);
		} else if (value instanceof ObjectSpecification) {
			return ((ObjectSpecification) value).build(context);
		} else {
			return value;
		}
	}

	public static ValueMode getValueMode(Object value) {
		if (value instanceof DynamicValue) {
			return ValueMode.DYNAMIC_VALUE;
		} else if (value instanceof ObjectSpecification) {
			return ValueMode.OBJECT_SPECIFICATION;
		} else {
			return ValueMode.STATIC_VALUE;
		}
	}

}
