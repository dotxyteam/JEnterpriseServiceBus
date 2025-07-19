package com.otk.jesb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import com.otk.jesb.PathExplorer.PathNode;

import xy.reflect.ui.util.ClassUtils;
import xy.reflect.ui.util.ReflectionUIUtils;

public class Variant<T> {

	private Class<T> valueClass;
	private Object value;

	public Variant(Class<T> valueClass, T value) {
		this.valueClass = valueClass;
		this.value = value;
	}

	public Variant(Class<T> valueClass) {
		this(valueClass, null);
	}

	public Class<T> getValueClass() {
		return valueClass;
	}

	@SuppressWarnings("unchecked")
	public T getValue() {
		if (value instanceof Expression) {
			String valueString = ((Expression<String>) value).evaluate(
					Collections.singletonList(EnvironmentSettings.ENVIRONMENT_VARIABLES_ROOT_DECLARATION),
					Collections.singletonList(EnvironmentSettings.ENVIRONMENT_VARIABLES_ROOT));
			if (ClassUtils.isPrimitiveClassOrWrapper(valueClass)) {
				return valueClass.cast(ReflectionUIUtils.primitiveFromString(valueString, valueClass));
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
		}
		return valueClass.cast(value);
	}

	public void setValue(T t) {
		this.value = t;
	}

	@SuppressWarnings("unchecked")
	public Expression<String> getVariableReferenceExpression() {
		if (!(value instanceof Expression)) {
			return null;
		}
		return (Expression<String>) value;
	}

	public void setVariableReferenceExpression(Expression<String> expression) {
		value = expression;
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

	public boolean isVariable() {
		return value instanceof Expression;
	}

	public void setVariable(boolean b) {
		value = b ? new Expression<String>(String.class) : null;
	}
}
