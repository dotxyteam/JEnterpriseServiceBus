package com.otk.jesb.util;

import java.awt.Point;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.syntax.SyntaxException;

import com.otk.jesb.Folder;
import com.otk.jesb.GUI;
import com.otk.jesb.InstanceSpecification;
import com.otk.jesb.Plan;
import com.otk.jesb.InstanceSpecification.DynamicValue;
import com.otk.jesb.InstanceSpecification.ValueMode;
import com.otk.jesb.Plan.ExecutionContext;
import com.otk.jesb.Plan.ValidationContext;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;
import com.otk.jesb.meta.ClassProvider;
import com.otk.jesb.meta.CompositeClassLoader;
import com.otk.jesb.Asset;
import com.otk.jesb.Solution;
import com.otk.jesb.Step;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.transform.TypeChecked;
import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.enumeration.IEnumerationTypeInfo;
import xy.reflect.ui.info.type.iterable.map.IMapEntryTypeInfo;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;
import xy.reflect.ui.util.ClassUtils;
import xy.reflect.ui.util.ReflectionUIUtils;

public class MiscUtils {

	public static Object executeScript(String script, Plan.ExecutionContext context) {
		CompositeClassLoader compositeClassLoader = new CompositeClassLoader();
		for (ClassLoader additionalClassLoader : ClassProvider.getAdditionalClassLoaders()) {
			compositeClassLoader.add(additionalClassLoader);
		}
		Binding binding = new Binding();
		GroovyShell shell = new GroovyShell(compositeClassLoader, binding);
		for (Plan.ExecutionContext.Property property : context.getProperties()) {
			Object value = property.getValue();
			binding.setVariable(property.getName(), value);
		}
		return shell.evaluate(script);
	}

	public static void validateScript(String expression, ValidationContext context) {
		CompositeClassLoader compositeClassLoader = new CompositeClassLoader();
		for (ClassLoader additionalClassLoader : ClassProvider.getAdditionalClassLoaders()) {
			compositeClassLoader.add(additionalClassLoader);
		}
		CompilerConfiguration config = new CompilerConfiguration();
		config.addCompilationCustomizers(new ASTTransformationCustomizer(TypeChecked.class));
		GroovyShell shell = new GroovyShell(compositeClassLoader, config);
		String preExpression = "";
		for (Plan.ValidationContext.Declaration declaration : context.getDeclarations()) {
			preExpression = declaration.getPropertyClass().getName() + " " + declaration.getPropertyName() + ";\n"
					+ preExpression;
		}
		try {
			shell.parse(preExpression + expression);
		} catch (CompilationFailedException e) {
			if (e instanceof MultipleCompilationErrorsException) {
				MultipleCompilationErrorsException me = (MultipleCompilationErrorsException) e;
				SyntaxException se = me.getErrorCollector().getSyntaxError(0);
				if (se != null) {
					throw new ScriptValidationError(
							convertPositionToIndex(preExpression + expression, se.getStartLine(), se.getStartColumn())
									- preExpression.length(),
							convertPositionToIndex(preExpression + expression, se.getEndLine(), se.getEndColumn())
									- preExpression.length(),
							se.getOriginalMessage());
				}
			}
			throw new ScriptValidationError(0, expression.length(), e.getMessageWithoutLocationText());
		}
	}

	public static boolean isComplexType(ITypeInfo type) {
		Class<?> clazz = ((JavaTypeInfoSource) type.getSource()).getJavaType();
		if (ClassUtils.isPrimitiveClassOrWrapperOrString(clazz)) {
			return false;
		}
		if (type instanceof IEnumerationTypeInfo) {
			return false;
		}
		return true;
	}

	public static Object interpretValue(Object value, ExecutionContext context) throws Exception {
		if (value instanceof DynamicValue) {
			return MiscUtils.executeScript(((DynamicValue) value).getScript(), context);
		} else if (value instanceof InstanceSpecification) {
			return ((InstanceSpecification) value).build(context);
		} else {
			return value;
		}
	}

	public static ValueMode getValueMode(Object value) {
		if (value instanceof DynamicValue) {
			return ValueMode.DYNAMIC_VALUE;
		} else if (value instanceof InstanceSpecification) {
			return ValueMode.INSTANCE_SPECIFICATION;
		} else {
			return ValueMode.STATIC_VALUE;
		}
	}

	public static boolean isConditionFullfilled(DynamicValue condition, ExecutionContext context) throws Exception {
		if (condition == null) {
			return true;
		}
		Object conditionResult = MiscUtils.interpretValue(condition.getScript(), context);
		if (!(conditionResult instanceof Boolean)) {
			throw new AssertionError("Condition evaluation result is not boolean: '" + conditionResult + "'");
		}
		return !((Boolean) conditionResult);
	}

	public static IMethodInfo getConstructorInfo(ITypeInfo typeInfo, String selectedConstructorSignature) {
		if (selectedConstructorSignature == null) {
			if (typeInfo.getConstructors().size() == 0) {
				return null;
			} else {
				return typeInfo.getConstructors().get(0);
			}
		} else {
			return ReflectionUIUtils.findMethodBySignature(typeInfo.getConstructors(), selectedConstructorSignature);
		}

	}

	public static Object getDefaultInterpretableValue(ITypeInfo type) {
		if (type == null) {
			return null;
		} else if (!MiscUtils.isComplexType(type)) {
			return ReflectionUIUtils.createDefaultInstance(type);
		} else {
			if (type instanceof IMapEntryTypeInfo) {
				IMapEntryTypeInfo mapEntryType = (IMapEntryTypeInfo) type;
				return new InstanceSpecification.MapEntrySpecification(mapEntryType.getKeyField().getType().getName(),
						mapEntryType.getValueField().getType().getName());
			} else {
				return new InstanceSpecification(type.getName());
			}
		}
	}

	public static String getDigitalUniqueIdentifier() {
		return String.format("%040d", new BigInteger(UUID.randomUUID().toString().replace("-", ""), 16));
	}

	public static Class<?> createClass(String className, String javaSource, ClassLoader parentClassLoader) {
		try {
			com.otk.jesb.compiler.Compiler compiler = new com.otk.jesb.compiler.Compiler();
			compiler.setClassLoader(parentClassLoader);
			compiler.setOptions("-parameters");
			return compiler.compile(javaSource, className);
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	public static <E> Iterable<E> secureIterable(Iterable<E> iterable) {
		ArrayList<E> list = new ArrayList<E>();
		for (E item : iterable) {
			list.add(item);
		}
		return list;
	}

	public static boolean negate(boolean b) {
		return !b;
	}

	public static ResourcePath getIconImagePath(Step step) {
		ActivityBuilder activityBuilder = step.getActivityBuilder();
		if (activityBuilder == null) {
			return null;
		}
		for (ActivityMetadata activityMetadata : GUI.JESBReflectionUI.ACTIVITY_METADATAS) {
			if (activityMetadata.getActivityBuilderClass().equals(step.getActivityBuilder().getClass())) {
				return activityMetadata.getActivityIconImagePath();
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Asset> List<T> findResources(Solution solution, Class<T> resourceClass) {
		List<T> result = new ArrayList<T>();
		for (Asset resource : solution.getContents()) {
			if (resource.getClass().equals(resourceClass)) {
				result.add((T) resource);
			}
			if (resource instanceof Folder) {
				result.addAll(findDescendantResources((Folder) resource, resourceClass));
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Asset> List<T> findDescendantResources(Folder folder, Class<T> resourceClass) {
		List<T> result = new ArrayList<T>();
		for (Asset resource : folder.getContents()) {
			if (resource.getClass().equals(resourceClass)) {
				result.add((T) resource);
			}
			if (resource instanceof Folder) {
				result.addAll(findDescendantResources((Folder) resource, resourceClass));
			}
		}
		return result;
	}

	public static int convertPositionToIndex(String text, int line, int column) {
		if (line < 1 || column < 1) {
			throw new IllegalArgumentException("Line and column numbers must be >= 1");
		}
		int index = 0;
		int currentLine = 1;
		int currentColumn = 1;
		for (char c : text.toCharArray()) {
			if (currentLine == line && currentColumn == column) {
				return index;
			}

			index++;

			if (c == '\n') {
				currentLine++;
				currentColumn = 1;
			} else {
				currentColumn++;
			}
		}
		if (currentLine == line && currentColumn == column) {
			return index;
		} else {
			throw new IllegalArgumentException("Line or column out of range");
		}
	}

	public static Point getRectangleBorderContactOfLineToExternalPoint(int rectangleCenterX, int rectangleCenterY,
			int rectangleWidth, int rectangleHeight, int externalPointX, int externalPointY) {
			int x = externalPointX;
			int y = externalPointY;
			int minX = rectangleCenterX - Math.round(rectangleWidth/2f);
			int minY = rectangleCenterY - Math.round(rectangleHeight/2f);
			int maxX = rectangleCenterX + Math.round(rectangleWidth/2f);
			int maxY = rectangleCenterY + Math.round(rectangleHeight/2f);
			if ((minX < x && x < maxX) && (minY < y && y < maxY)){
				return null;
			}
			float midX = (minX + maxX) / 2f;
			float midY = (minY + maxY) / 2f;
			// if (midX - x == 0) -> m == ±Inf -> minYx/maxYx == x (because value / ±Inf = ±0)
			float m = (midY - y) / (midX - x);
			if (x <= midX) { // check "left" side
				float minXy = m * (minX - x) + y;
				if (minY <= minXy && minXy <= maxY) {
					return new Point(Math.round(minX), Math.round(minXy));
				}
			}
			if (x >= midX) { // check "right" side
				float maxXy = m * (maxX - x) + y;
				if (minY <= maxXy && maxXy <= maxY) {
					return new Point(Math.round(maxX), Math.round(maxXy));
				}
			}
			if (y <= midY) { // check "top" side
				float minYx = (minY - y) / m + x;
				if (minX <= minYx && minYx <= maxX) {
					return new Point(Math.round(minYx), Math.round(minY));
				}
			}
			if (y >= midY) { // check "bottom" side
				float maxYx = (maxY - y) / m + x;
				if (minX <= maxYx && maxYx <= maxX) {
					return new Point(Math.round(maxYx), Math.round(maxY));
				}
			}
			// edge case when finding midpoint intersection: m = 0/0 = NaN
			if (x == midX && y == midY) {  
				return new Point(x, y);
			}
			// Should never happen :) If it does, please tell me!
			throw new AssertionError();
		}

}
