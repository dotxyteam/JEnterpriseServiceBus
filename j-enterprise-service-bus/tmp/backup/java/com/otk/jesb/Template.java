package com.otk.jesb;

import java.util.Arrays;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.compiler.CompiledFunction.FunctionCallError;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.util.ClassUtils;
import xy.reflect.ui.util.ReflectionUIUtils;

public class Template<T> {

	private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\{([^\\}]+)\\}");

	private Class<T> valueClass;
	private String string;

	public Template(Class<T> valueClass) {
		this.valueClass = valueClass;
	}

	public String get() {
		return string;
	}

	public void set(String string) {
		this.string = string;
	}

	public T getValue() {
		if (string == null) {
			return null;
		}
		String valueString = expand(string);
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

	public void setValue(T t) {
		if (t == null) {
			string = null;
			return;
		}
		if (ClassUtils.isPrimitiveClassOrWrapper(valueClass)) {
			string = ReflectionUIUtils.primitiveToString(t);
		} else if (valueClass == String.class) {
			string = (String) t;
		} else if (valueClass.isEnum()) {
			string = t.toString();
		} else {
			throw new UnexpectedError();
		}
	}

	protected String expand(String string) {
		Matcher matcher = EXPRESSION_PATTERN.matcher(string);
		StringBuilder result = new StringBuilder();
		int lastFindStartPosition = 0;
		while (matcher.find()) {
			result.append(string.substring(lastFindStartPosition, matcher.start()));
			String expression = matcher.group(1);
			Object expressionEvaluationResult;
			try {
				expressionEvaluationResult = MiscUtils
						.compileExpression(expression, Collections.emptyList(), Object.class)
						.call(Collections.emptyList());
			} catch (CompilationError e) {
				String sourceCode = string;
				int startPosition = (e.getStartPosition() != -1) ? (e.getStartPosition() + matcher.start())
						: e.getStartPosition();
				int endPosition = (e.getEndPosition() != -1) ? (e.getEndPosition() + matcher.start())
						: e.getEndPosition();
				throw new UnexpectedError(
						new CompilationError(startPosition, endPosition, e.getMessage(), null, sourceCode));
			} catch (FunctionCallError e) {
				throw new UnexpectedError(e);
			}
			if (expressionEvaluationResult == null) {
				throw new UnexpectedError("Illegal null value returned by the expression: '" + expression + "'");
			}
			result.append(expressionEvaluationResult.toString());
			lastFindStartPosition = matcher.end();
		}
		result.append(string.substring(lastFindStartPosition));
		return result.toString();
	}
}
