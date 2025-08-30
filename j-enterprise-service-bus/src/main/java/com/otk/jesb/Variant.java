package com.otk.jesb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import com.otk.jesb.PathExplorer.PathNode;
import com.otk.jesb.meta.Date;
import com.otk.jesb.meta.DateTime;

import xy.reflect.ui.util.ClassUtils;
import xy.reflect.ui.util.ReflectionUIUtils;

public class Variant<T> {

	private Class<T> valueClass;
	private Object constantValue;
	private Expression<String> expression;
	private boolean variable = false;

	public Variant(Class<T> valueClass, T value) {
		this.valueClass = valueClass;
		this.constantValue = value;
	}

	public Variant(Class<T> valueClass) {
		this(valueClass, null);
	}

	public Variant() {
		this(null);
	}

	public Class<T> getValueClass() {
		return valueClass;
	}

	public void setValueClass(Class<T> valueClass) {
		this.valueClass = valueClass;
	}

	public Object getConstantValue() {
		return constantValue;
	}

	public void setConstantValue(Object constantValue) {
		this.constantValue = constantValue;
	}

	public Expression<String> getVariableReferenceExpression() {
		return expression;
	}

	public void setVariableReferenceExpression(Expression<String> expression) {
		this.expression = expression;
	}

	public boolean isVariable() {
		return variable;
	}

	public void setVariable(boolean variable) {
		this.variable = variable;
	}

	public T getValue() {
		if (variable) {
			requireExpression();
			String valueString = expression.evaluate(
					Collections.singletonList(EnvironmentSettings.ENVIRONMENT_VARIABLES_ROOT_DECLARATION),
					Collections.singletonList(EnvironmentSettings.ENVIRONMENT_VARIABLES_ROOT));
			if (valueString == null) {
				if (valueClass.isPrimitive()) {
					throw new PotentialError("Unexpected <null> environment variable value. <" + valueClass.getName()
							+ "> value expected from: " + expression.get());
				}
				return null;
			}
			if (ClassUtils.isPrimitiveClassOrWrapper(valueClass)) {
				return valueClass.cast(ReflectionUIUtils.primitiveFromString(valueString, valueClass));
			} else if (valueClass == Date.class) {
				return valueClass.cast(new Date(valueString));
			} else if (valueClass == DateTime.class) {
				return valueClass.cast(new DateTime(valueString));
			} else if (valueClass == String.class) {
				return valueClass.cast(valueString);
			} else if (valueClass.isEnum()) {
				T result = Arrays.stream(valueClass.getEnumConstants())
						.filter(constant -> constant.toString().equals(valueString)).findFirst().orElse(null);
				if (result == null) {
					throw new NoSuchElementException(valueClass.getName() + "." + valueString);
				}
				return result;
			} else {
				throw new UnexpectedError();
			}
		} else {
			return valueClass.cast(constantValue);
		}
	}

	public List<Expression<String>> getVariableReferenceExpressionOptions() {
		List<Expression<String>> result = new ArrayList<Expression<String>>();
		PathOptionsProvider pathOptionsProvider = new PathOptionsProvider(
				Collections.singletonList(EnvironmentSettings.ENVIRONMENT_VARIABLES_ROOT_DECLARATION));
		result.addAll(getVariableReferenceExpressionOptions(pathOptionsProvider.getRootPathNodes()));
		return result;
	}

	private Collection<? extends Expression<String>> getVariableReferenceExpressionOptions(List<PathNode> pathNodes) {
		List<Expression<String>> result = new ArrayList<Expression<String>>();
		for (PathNode pathNode : pathNodes) {
			if (pathNode.getExpressionType().getName().equals(String.class.getName())) {
				Expression<String> expression = new Expression<String>(String.class);
				expression.set(pathNode.getTypicalExpression());
				result.add(expression);
			}
			result.addAll(getVariableReferenceExpressionOptions(pathNode.getChildren()));
		}
		return result;
	}

	private void requireExpression() {
		if (expression == null) {
			throw new PotentialError("Environment variable reference not provided");
		}
	}

	public void validate() throws ValidationError {
		if (variable) {
			try {
				requireExpression();
			} catch (PotentialError e) {
				throw new ValidationError(e.getMessage());
			}
		}
	}

	@Override
	public String toString() {
		if (variable) {
			return "(" + expression + ")";
		} else {
			return Objects.toString(constantValue);
		}
	}

}
