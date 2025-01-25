package com.otk.jesb.util;

import java.awt.Point;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.otk.jesb.Folder;
import com.otk.jesb.GUI;
import com.otk.jesb.InstanceBuilder;
import com.otk.jesb.Plan;
import com.otk.jesb.InstanceBuilder.Function;
import com.otk.jesb.InstanceBuilder.InstanceBuilderFacade;
import com.otk.jesb.InstanceBuilder.EnumerationItemSelector;
import com.otk.jesb.InstanceBuilder.Facade;
import com.otk.jesb.InstanceBuilder.ValueMode;
import com.otk.jesb.InstanceBuilder.VerificationContext;
import com.otk.jesb.Plan.ExecutionContext;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.compiler.CompiledFunction;
import com.otk.jesb.compiler.InMemoryJavaCompiler;
import com.otk.jesb.meta.TypeInfoProvider;
import com.otk.jesb.Asset;
import com.otk.jesb.Solution;
import com.otk.jesb.Step;
import com.otk.jesb.Structure.Structured;

import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.type.DefaultTypeInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.enumeration.IEnumerationTypeInfo;
import xy.reflect.ui.info.type.iterable.map.IMapEntryTypeInfo;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;
import xy.reflect.ui.util.ClassUtils;
import xy.reflect.ui.util.ReflectionUIUtils;

public class MiscUtils {

	private static final String PARENT_STRUCTURE_TYPE_NAME_SYMBOL = "${..}";

	public static InMemoryJavaCompiler IN_MEMORY_JAVA_COMPILER = new InMemoryJavaCompiler();
	static {
		MiscUtils.IN_MEMORY_JAVA_COMPILER.setOptions(Arrays.asList("-parameters"));
	}

	public static Object executeFunction(Function function, InstanceBuilder.EvaluationContext evaluationContext) {
		ExecutionContext executionContext = evaluationContext.getExecutionContext();
		Plan currentPlan = executionContext.getPlan();
		Step currentStep = executionContext.getCurrentStep();
		Plan.ValidationContext validationContext = currentPlan.getValidationContext(currentStep);
		VerificationContext verificationContext = currentStep.getActivityBuilder()
				.findFunctionVerificationContext(function, validationContext);
		if (((verificationContext.getCurrentFacade() == null) ? null
				: verificationContext.getCurrentFacade()
						.getUnderlying()) != ((evaluationContext.getCurrentFacade() == null) ? null
								: evaluationContext.getCurrentFacade().getUnderlying())) {
			throw new AssertionError();
		}
		CompiledFunction compiledFunction;
		try {
			compiledFunction = CompiledFunction.get(
					makeTypeNamesAbsolute(function.getFunctionBody(),
							getAncestorStructureInstanceBuilders(verificationContext.getCurrentFacade())),
					validationContext);
		} catch (CompilationError e) {
			throw new AssertionError(e);
		}
		try {
			return compiledFunction.execute(executionContext);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public static void validateFunction(String functionBody, VerificationContext context) throws CompilationError {
		CompiledFunction.get(
				makeTypeNamesAbsolute(functionBody, getAncestorStructureInstanceBuilders(context.getCurrentFacade())),
				context.getValidationContext());
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

	public static ValueMode getValueMode(Object value) {
		if (value instanceof Function) {
			return ValueMode.FUNCTION;
		} else {
			return ValueMode.PLAIN;
		}
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

	public static boolean isConditionFullfilled(Function condition, InstanceBuilder.EvaluationContext context)
			throws Exception {
		if (condition == null) {
			return true;
		}
		Object conditionResult = MiscUtils.interpretValue(condition,
				TypeInfoProvider.getTypeInfo(Boolean.class.getName()), context);
		if (!(conditionResult instanceof Boolean)) {
			throw new AssertionError("Condition evaluation result is not boolean: '" + conditionResult + "'");
		}
		return !((Boolean) conditionResult);
	}

	public static Object interpretValue(Object value, ITypeInfo type, InstanceBuilder.EvaluationContext context)
			throws Exception {
		if (value instanceof Function) {
			Object result = MiscUtils.executeFunction(((Function) value), context);
			if (!type.supports(result)) {
				throw new Exception(
						"Invalid function result '" + result + "': Expected value of type <" + type.getName() + ">");
			}
			return result;
		} else if (value instanceof InstanceBuilder) {
			Object result = ((InstanceBuilder) value).build(context);
			if (!type.supports(result)) {
				throw new Exception("Invalid instance builder result '" + result + "': Expected value of type <"
						+ type.getName() + ">");
			}
			return result;
		} else if (value instanceof EnumerationItemSelector) {
			for (Object item : ((IEnumerationTypeInfo) type).getValues()) {
				if (((IEnumerationTypeInfo) type).getValueInfo(item).getName()
						.equals(((EnumerationItemSelector) value).getSelectedItemName())) {
					return item;
				}
			}
			throw new AssertionError();
		} else {
			return value;
		}
	}

	public static Object getDefaultInterpretableValue(ITypeInfo type, Facade currentFacade) {
		return getDefaultInterpretableValue(type, ValueMode.PLAIN, currentFacade);
	}

	public static Object getDefaultInterpretableValue(ITypeInfo type, ValueMode valueMode, Facade currentFacade) {
		if (type == null) {
			return null;
		} else if (valueMode == ValueMode.FUNCTION) {
			String functionBody;
			if (!MiscUtils.isComplexType(type)) {
				Object defaultValue = ReflectionUIUtils.createDefaultInstance(type);
				if (defaultValue.getClass().isEnum()) {
					functionBody = "return "
							+ makeTypeNamesRelative(type.getName(), getAncestorStructureInstanceBuilders(currentFacade))
							+ "." + defaultValue.toString() + ";";
				} else if (defaultValue instanceof String) {
					functionBody = "return \"" + defaultValue + "\";";
				} else {
					functionBody = "return " + String.valueOf(defaultValue) + ";";
				}
			} else {
				functionBody = "return null;";
			}
			return new Function(functionBody);
		} else if (valueMode == ValueMode.PLAIN) {
			if (!MiscUtils.isComplexType(type)) {
				if (type instanceof IEnumerationTypeInfo) {
					EnumerationItemSelector result = new EnumerationItemSelector();
					result.configure((IEnumerationTypeInfo) type);
					if (result.getItemNames().size() > 0) {
						result.setSelectedItemName(result.getItemNames().get(0));
					}
					return result;
				} else {
					return ReflectionUIUtils.createDefaultInstance(type);
				}
			} else {
				if (type instanceof IMapEntryTypeInfo) {
					return new InstanceBuilder.MapEntryBuilder();
				} else {
					return new InstanceBuilder(
							makeTypeNamesRelative(type.getName(), getAncestorStructureInstanceBuilders(currentFacade)));
				}
			}
		} else {
			return null;
		}
	}

	public static Object maintainInterpretableValue(Object value, ITypeInfo type) {
		if (value instanceof EnumerationItemSelector) {
			if (type instanceof IEnumerationTypeInfo) {
				((EnumerationItemSelector) value).configure((IEnumerationTypeInfo) type);
			}
		}
		return value;
	}

	public static String makeTypeNamesRelative(String text, List<InstanceBuilder> ancestorStructureInstanceBuilders) {
		if ((ancestorStructureInstanceBuilders == null) || (ancestorStructureInstanceBuilders.size() == 0)) {
			return text;
		}
		InstanceBuilder parentInstanceBuilder = ancestorStructureInstanceBuilders.get(0);
		String absoluteParentTypeName = parentInstanceBuilder.computeActualTypeName(
				ancestorStructureInstanceBuilders.subList(1, ancestorStructureInstanceBuilders.size()));
		return text.replace(absoluteParentTypeName, PARENT_STRUCTURE_TYPE_NAME_SYMBOL);
	}

	public static String makeTypeNamesAbsolute(String text, List<InstanceBuilder> ancestorStructureInstanceBuilders) {
		if ((ancestorStructureInstanceBuilders == null) || (ancestorStructureInstanceBuilders.size() == 0)) {
			return text;
		}
		InstanceBuilder parentInstanceBuilder = ancestorStructureInstanceBuilders.get(0);
		String absoluteParentTypeName = parentInstanceBuilder.computeActualTypeName(
				ancestorStructureInstanceBuilders.subList(1, ancestorStructureInstanceBuilders.size()));
		return text.replace(PARENT_STRUCTURE_TYPE_NAME_SYMBOL, absoluteParentTypeName);
	}

	public static String getDigitalUniqueIdentifier() {
		return String.format("%040d", new BigInteger(UUID.randomUUID().toString().replace("-", ""), 16));
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
		int minX = rectangleCenterX - Math.round(rectangleWidth / 2f);
		int minY = rectangleCenterY - Math.round(rectangleHeight / 2f);
		int maxX = rectangleCenterX + Math.round(rectangleWidth / 2f);
		int maxY = rectangleCenterY + Math.round(rectangleHeight / 2f);
		if ((minX < x && x < maxX) && (minY < y && y < maxY)) {
			return null;
		}
		float midX = (minX + maxX) / 2f;
		float midY = (minY + maxY) / 2f;
		// if (midX - x == 0) -> m == ±Inf -> minYx/maxYx == x (because value / ±Inf =
		// ±0)
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

	public static <T> String stringJoin(T[] array, String separator) {
		return stringJoin(Arrays.asList(array), separator);
	}

	public static String stringJoin(List<?> list, String separator) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			Object item = list.get(i);
			if (i > 0) {
				result.append(separator);
			}
			if (item == null) {
				result.append("null");
			} else {
				result.append(item.toString());
			}
		}
		return result.toString();
	}

	public static <BASE, C extends BASE> List<BASE> convertCollection(Collection<C> ts) {
		List<BASE> result = new ArrayList<BASE>();
		for (C t : ts) {
			result.add((BASE) t);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public static <BASE, C extends BASE> List<C> convertCollectionUnsafely(Collection<BASE> bs) {
		List<C> result = new ArrayList<C>();
		for (BASE b : bs) {
			result.add((C) b);
		}
		return result;
	}

	public static String adaptClassNameToSourceCode(String className) {
		return className.replace("$", ".");
	}

	public static List<InstanceBuilder> getAncestorStructureInstanceBuilders(InstanceBuilder.Facade facade) {
		if (facade == null) {
			return null;
		}
		return InstanceBuilder.getAncestorFacades(facade).stream()
				.filter(f -> (f instanceof InstanceBuilderFacade) && Structured.class
						.isAssignableFrom(((DefaultTypeInfo) ((InstanceBuilderFacade) f).getTypeInfo()).getJavaType()))
				.map(f -> ((InstanceBuilderFacade) f).getUnderlying()).collect(Collectors.toList());
	}

}
