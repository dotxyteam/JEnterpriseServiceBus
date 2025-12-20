package com.otk.jesb.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.otk.jesb.PotentialError;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.compiler.CompiledFunction;
import com.otk.jesb.compiler.CompiledFunction.FunctionCallError;
import com.otk.jesb.instantiation.InstantiationFunctionCompilationContext;
import com.otk.jesb.instantiation.ListItemInitializerFacade;
import com.otk.jesb.instantiation.EnumerationItemSelector;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.Facade;
import com.otk.jesb.instantiation.FieldInitializerFacade;
import com.otk.jesb.instantiation.InitializerFacade;
import com.otk.jesb.instantiation.InstanceBuilder;
import com.otk.jesb.instantiation.InstanceBuilderFacade;
import com.otk.jesb.instantiation.InstantiationFunction;
import com.otk.jesb.instantiation.MapEntryBuilder;
import com.otk.jesb.instantiation.ParameterInitializerFacade;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.instantiation.ValueMode;
import com.otk.jesb.meta.TypeInfoProvider;
import com.otk.jesb.resource.builtin.SharedStructureModel;
import com.otk.jesb.solution.Solution;

import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.method.AbstractConstructorInfo;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.type.DefaultTypeInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.enumeration.IEnumerationTypeInfo;
import xy.reflect.ui.info.type.iterable.map.IMapEntryTypeInfo;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;
import xy.reflect.ui.util.ClassUtils;
import xy.reflect.ui.util.Pair;
import xy.reflect.ui.util.ReflectionUIUtils;

public class InstantiationUtils {

	public static final String RELATIVE_TYPE_NAME_VARIABLE_PART_REFRENCE = "${_}";
	private static final String DYNAMIC_TYPE_NAME_VARIABLE_PART_START = "a7374617274";
	private static final String DYNAMIC_TYPE_NAME_VARIABLE_PART_END = "z656e64";
	private static final Pattern DYNAMIC_TYPE_NAME_PATTERN = Pattern.compile(
			".*(" + DYNAMIC_TYPE_NAME_VARIABLE_PART_START + ".+" + DYNAMIC_TYPE_NAME_VARIABLE_PART_END + ").*");

	public static Object cloneInitializer(Object initializer, Solution solutionInstance) {
		Function<Pair<ITypeInfo, IFieldInfo>, Function<Object, Object>> customCopierByContext = context -> {
			ITypeInfo objectType = context.getFirst();
			IFieldInfo field = context.getSecond();
			Class<?> objectClass = solutionInstance.getRuntime().getJESBClass(objectType.getName());
			if (InstanceBuilder.class.isAssignableFrom(objectClass)) {
				if (field.getName().equals("dynamicTypeNameAccessor")) {
					return Function.identity();
				}
			}
			if (RootInstanceBuilder.class.isAssignableFrom(objectClass)) {
				if (field.getName().equals("rootInstanceDynamicTypeNameAccessor")) {
					return Function.identity();
				}
			}
			return null;
		};
		return ReflectionUIUtils.copyAccordingInfos(TypeInfoProvider.INTROSPECTOR, initializer, customCopierByContext);
	}

	public static Object executeFunction(InstantiationFunction function, InstantiationContext instantiationContext)
			throws FunctionCallError {
		InstantiationFunctionCompilationContext compilationContext = instantiationContext
				.getFunctionCompilationContex(function);
		if (!MiscUtils.equalsOrBothNull(compilationContext.getParentFacade(), instantiationContext.getParentFacade())) {
			throw new UnexpectedError();
		}
		List<VariableDeclaration> expectedVariableDeclarations = compilationContext.getVariableDeclarations(function);
		MiscUtils.checkVariables(expectedVariableDeclarations, instantiationContext.getVariables());
		CompiledFunction<?> compiledFunction;
		try {
			compiledFunction = function.getCompiledVersion(compilationContext.getPrecompiler(),
					expectedVariableDeclarations, compilationContext.getFunctionReturnType(function),
					instantiationContext.getSolutionInstance());
		} catch (CompilationError e) {
			throw new PotentialError(e);
		}
		return compiledFunction.call(instantiationContext.getVariables());
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

	public static Object getDefaultValue(ITypeInfo type) {
		Class<?> clazz = ((JavaTypeInfoSource) type.getSource()).getJavaType();
		if (ClassUtils.isPrimitiveWrapperClass(clazz)) {
			clazz = ClassUtils.wrapperToPrimitiveClass(clazz);
		}
		if (clazz.isPrimitive()) {
			return ClassUtils.getDefaultPrimitiveValue(clazz);
		}
		if (clazz == String.class) {
			return "string";
		}
		return ReflectionUIUtils.createDefaultInstance(type);
	}

	public static ValueMode getValueMode(Object value) {
		if (value instanceof InstantiationFunction) {
			return ValueMode.FUNCTION;
		} else {
			return ValueMode.PLAIN;
		}
	}

	public static AbstractConstructorInfo getConstructorInfo(ITypeInfo typeInfo, String selectedConstructorSignature) {
		List<IMethodInfo> options = listSortedConstructors(typeInfo);
		if (selectedConstructorSignature == null) {
			if (options.size() == 0) {
				return null;
			} else {
				return (AbstractConstructorInfo) options.get(0);
			}
		} else {
			return (AbstractConstructorInfo) ReflectionUIUtils.findMethodBySignature(typeInfo.getConstructors(),
					selectedConstructorSignature);
		}

	}

	public static boolean isConditionFullfilled(InstantiationFunction condition, InstantiationContext context)
			throws Exception {
		if (condition == null) {
			return true;
		}
		Object conditionResult = interpretValue(condition,
				TypeInfoProvider.getTypeInfo(Boolean.class.getName(), context.getSolutionInstance()), context);
		if (!(conditionResult instanceof Boolean)) {
			throw new PotentialError("Condition evaluation result is not boolean: '" + conditionResult + "'");
		}
		return (Boolean) conditionResult;
	}

	public static String express(Object value) {
		if (value instanceof InstantiationFunction) {
			return ((InstantiationFunction) value).getFunctionBody();
		} else if (value instanceof InstanceBuilder) {
			return null;
		} else if (value instanceof EnumerationItemSelector) {
			return ((EnumerationItemSelector) value).getSelectedItemName();
		} else {
			if (value == null) {
				return null;
			} else if (value instanceof String) {
				return "\"" + MiscUtils.escapeJavaString((String) value) + "\"";
			} else {
				return value.toString();
			}
		}
	}

	public static void validateValue(Object value, ITypeInfo type, Facade parentFacade, String valueName,
			boolean recursively, List<VariableDeclaration> variableDeclarations, Solution solutionInstance)
			throws ValidationError {
		if (value instanceof InstantiationFunction) {
			InstantiationFunction function = (InstantiationFunction) value;
			InstantiationFunctionCompilationContext compilationContext = new InstantiationFunctionCompilationContext(
					variableDeclarations, parentFacade);
			Class<?> functionReturnType = compilationContext.getFunctionReturnType(function);
			if (((JavaTypeInfoSource) type.getSource()).getJavaType() != functionReturnType) {
				throw new UnexpectedError();
			}
			try {
				function.getCompiledVersion(compilationContext.getPrecompiler(),
						compilationContext.getVariableDeclarations(function), functionReturnType, solutionInstance);
			} catch (CompilationError e) {
				throw new ValidationError("Failed to compile the " + valueName + " function", e);
			}
		} else if (value instanceof InstanceBuilder) {
			try {
				Class<?> instanceBuilderJavaType;
				try {
					instanceBuilderJavaType = solutionInstance.getRuntime().getJESBClass(((InstanceBuilder) value)
							.computeActualTypeName(getAncestorInstanceBuilders(parentFacade), solutionInstance));
				} catch (Throwable t) {
					instanceBuilderJavaType = null;
				}
				if (instanceBuilderJavaType != null) {
					Class<?> declaredJavaType = ((JavaTypeInfoSource) type.getSource()).getJavaType();
					if (!declaredJavaType.isAssignableFrom(instanceBuilderJavaType)) {
						throw new ValidationError("The instance type <" + instanceBuilderJavaType.getName()
								+ "> is not compatible with the declared type <" + declaredJavaType.getName() + ">");
					}
				}
				new InstanceBuilderFacade(parentFacade, (InstanceBuilder) value, solutionInstance).validate(recursively,
						variableDeclarations);
			} catch (ValidationError e) {
				throw new ValidationError("Failed to validate the " + valueName + " instance builder", e);
			}
		} else if (value instanceof EnumerationItemSelector) {
			EnumerationItemSelector enumItemSelector = (EnumerationItemSelector) value;
			IEnumerationTypeInfo enumType = (IEnumerationTypeInfo) type;
			List<String> validItemNames = Arrays.asList(enumType.getValues()).stream()
					.map(item -> enumType.getValueInfo(item).getName()).collect(Collectors.toList());
			if (!validItemNames.contains(enumItemSelector.getSelectedItemName())) {
				throw new ValidationError("Failed to validate the " + valueName + " enumeration item: Unexpected name '"
						+ enumItemSelector.getSelectedItemName() + "', expected "
						+ MiscUtils.stringJoin(validItemNames.stream().map(name -> "'" + name + "'").toArray(), "|"));
			}
		} else {
			if (!type.supports(value)) {
				throw new ValidationError("Failed to validate the " + valueName + ": Invalid value '" + value
						+ "': Expected value of type <" + type.getName() + ">");
			}
		}
	}

	public static Object interpretValue(Object value, ITypeInfo type, InstantiationContext context) throws Exception {
		if (value instanceof InstantiationFunction) {
			Object result = executeFunction(((InstantiationFunction) value), context);
			if (!type.supports(result)) {
				throw new InstantiationError(
						"Invalid function result '" + result + "': Expected value of type <" + type.getName() + ">");
			}
			return result;
		} else if (value instanceof InstanceBuilder) {
			Object result = ((InstanceBuilder) value).build(context);
			if (!type.supports(result)) {
				throw new InstantiationError("Invalid instance builder result '" + result
						+ "': Expected value of type <" + type.getName() + ">");
			}
			return result;
		} else if (value instanceof EnumerationItemSelector) {
			for (Object item : ((IEnumerationTypeInfo) type).getValues()) {
				if (((IEnumerationTypeInfo) type).getValueInfo(item).getName()
						.equals(((EnumerationItemSelector) value).getSelectedItemName())) {
					return item;
				}
			}
			throw new UnexpectedError();
		} else {
			if (!type.supports(value)) {
				throw new InstantiationError(
						"Invalid value '" + value + "': Expected value of type <" + type.getName() + ">");
			}
			return value;
		}
	}

	public static Object getDefaultInterpretableValue(ITypeInfo type, Facade currentFacade, Solution solutionInstance) {
		return getDefaultInterpretableValue(type, ValueMode.PLAIN, currentFacade, solutionInstance);
	}

	public static Object getDefaultInterpretableValue(ITypeInfo type, ValueMode valueMode, Facade currentFacade,
			Solution solutionInstance) {
		if ((type == null) || type.getName().equals(Object.class.getName())) {
			return null;
		} else if (valueMode == ValueMode.FUNCTION) {
			String functionBody;
			if (!isComplexType(type)
					&& !ClassUtils.isPrimitiveWrapperClass(((JavaTypeInfoSource) type.getSource()).getJavaType())) {
				Object defaultValue = getDefaultValue(type);
				if (defaultValue.getClass().isEnum()) {
					functionBody = "return "
							+ makeTypeNamesRelative(MiscUtils.adaptClassNameToSourceCode(type.getName()),
									getAncestorInstanceBuilders(currentFacade), solutionInstance)
							+ "." + defaultValue.toString() + ";";
				} else if (defaultValue instanceof String) {
					functionBody = "return \"" + defaultValue + "\";";
				} else {
					functionBody = "return " + String.valueOf(defaultValue) + ";";
				}
			} else {
				functionBody = "return null;";
			}
			return new InstantiationFunction(functionBody);
		} else if (valueMode == ValueMode.PLAIN) {
			if (!isComplexType(type)) {
				if (type instanceof IEnumerationTypeInfo) {
					EnumerationItemSelector result = new EnumerationItemSelector();
					result.configure((IEnumerationTypeInfo) type);
					if (result.getItemNames().size() > 0) {
						result.setSelectedItemName(result.getItemNames().get(0));
					}
					return result;
				} else {
					return getDefaultValue(type);
				}
			} else {
				RootInstanceBuilder rootInstanceBuilder = RootInstanceBuilder
						.getFromRootInstanceInitializerFacade(currentFacade);
				if (rootInstanceBuilder != null) {
					InstanceBuilder result = new InstanceBuilder();
					result.setTypeName(rootInstanceBuilder.getRootInstanceTypeName());
					result.setDynamicTypeNameAccessor(rootInstanceBuilder.getRootInstanceDynamicTypeNameAccessor());
					if (!type.getName().equals(result.computeActualTypeName(
							InstantiationUtils.getAncestorInstanceBuilders(currentFacade), solutionInstance))) {
						throw new UnexpectedError();
					}
					return result;
				} else {
					Class<?> javaType = ((DefaultTypeInfo) type).getJavaType();
					if (SharedStructureModel.isStructuredClass(javaType)) {
						SharedStructureModel model = SharedStructureModel.getFromStructuredClass(javaType);
						return new InstanceBuilder(model.getStructuredClassNameAccessor(solutionInstance));
					} else {
						if (type instanceof IMapEntryTypeInfo) {
							return new MapEntryBuilder();
						} else {
							/*
							 * Automatically replace the type with a standard alternative when we detect
							 * that it will need to be modified because it cannot be instantiated.
							 * Generally, we replace standard interfaces with their default implementation
							 * here.
							 */
							if (type.getName().equals(List.class.getName())) {
								type = TypeInfoProvider.getTypeInfo(ArrayList.class);
							} else if (type.getName().equals(Set.class.getName())) {
								type = TypeInfoProvider.getTypeInfo(HashSet.class);
							} else if (type.getName().equals(Map.class.getName())) {
								type = TypeInfoProvider.getTypeInfo(HashMap.class);
							}
							return new InstanceBuilder(makeTypeNamesRelative(type.getName(),
									getAncestorInstanceBuilders(currentFacade), solutionInstance));
						}
					}
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

	public static String makeTypeNamesRelative(String text, List<InstanceBuilder> ancestorInstanceBuilders,
			Solution solutionInstance) {
		if ((ancestorInstanceBuilders == null) || (ancestorInstanceBuilders.size() == 0)) {
			return text;
		}
		String dynamicTypeNamePart = findDynamicTypeNameVariablePart(ancestorInstanceBuilders, solutionInstance);
		if (dynamicTypeNamePart != null) {
			text = makeTypeNamesRelative(text, dynamicTypeNamePart);
		}
		return text;
	}

	public static String makeTypeNamesAbsolute(String text, List<InstanceBuilder> ancestorInstanceBuilders,
			Solution solutionInstance) {
		if ((ancestorInstanceBuilders == null) || (ancestorInstanceBuilders.size() == 0)) {
			return text;
		}
		String dynamicTypeNamePart = findDynamicTypeNameVariablePart(ancestorInstanceBuilders, solutionInstance);
		if (dynamicTypeNamePart != null) {
			text = makeTypeNamesAbsolute(text, dynamicTypeNamePart);
		}
		return text;
	}

	public static String makeTypeNamesRelative(String text, String dynamicTypeNamePart) {
		return text.replace(dynamicTypeNamePart, RELATIVE_TYPE_NAME_VARIABLE_PART_REFRENCE);
	}

	public static String makeTypeNamesAbsolute(String text, String dynamicTypeNamePart) {
		return text.replace(RELATIVE_TYPE_NAME_VARIABLE_PART_REFRENCE, dynamicTypeNamePart);
	}

	public static String toRelativeTypeNameVariablePart(String baseClassNamePart) {
		return DYNAMIC_TYPE_NAME_VARIABLE_PART_START + baseClassNamePart + DYNAMIC_TYPE_NAME_VARIABLE_PART_END;
	}

	public static String findDynamicTypeNameVariablePart(List<InstanceBuilder> ancestorInstanceBuilders,
			Solution solutionInstance) {
		for (int i = 0; i < ancestorInstanceBuilders.size(); i++) {
			InstanceBuilder ancestorInstanceBuilder = ancestorInstanceBuilders.get(i);
			String absoluteAncestorTypeName = ancestorInstanceBuilder.computeActualTypeName(
					ancestorInstanceBuilders.subList(i + 1, ancestorInstanceBuilders.size()), solutionInstance);
			String result = extractDynamicTypeNameVariablePart(absoluteAncestorTypeName);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	public static String extractDynamicTypeNameVariablePart(String typeName) {
		Matcher matcher = DYNAMIC_TYPE_NAME_PATTERN.matcher(typeName);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return null;
	}

	public static int positionBeforeTypeNamesMadeAbsolute(int positionAfter, String text,
			List<InstanceBuilder> ancestorInstanceBuilders, Solution solutionInstance) {
		if ((ancestorInstanceBuilders == null) || (ancestorInstanceBuilders.size() == 0)) {
			return positionAfter;
		}
		String dynamicTypeNamePart = findDynamicTypeNameVariablePart(ancestorInstanceBuilders, solutionInstance);
		if (dynamicTypeNamePart != null) {
			return MiscUtils.positionAfterReplacement(positionAfter, text, dynamicTypeNamePart,
					RELATIVE_TYPE_NAME_VARIABLE_PART_REFRENCE);
		}
		return positionAfter;
	}

	public static List<InstanceBuilder> getAncestorInstanceBuilders(Facade facade) {
		if (facade == null) {
			return null;
		}
		List<InstanceBuilder> result = new ArrayList<InstanceBuilder>();
		for (Facade ancestorFacade : Facade.getAncestors(facade)) {
			if (!(ancestorFacade instanceof InstanceBuilderFacade)) {
				continue;
			}
			result.add(((InstanceBuilderFacade) ancestorFacade).getUnderlying());
		}
		return result;
	}

	public InstantiationUtils() {
		super();
	}

	public static List<IMethodInfo> listSortedConstructors(ITypeInfo typeInfo) {
		List<IMethodInfo> result = typeInfo.getConstructors();
		result = new ArrayList<IMethodInfo>(result);
		Collections.sort(result, new Comparator<IMethodInfo>() {
			@Override
			public int compare(IMethodInfo o1, IMethodInfo o2) {
				return Integer.valueOf(o1.getParameters().size()).compareTo(Integer.valueOf(o2.getParameters().size()));
			}
		});
		return result;
	}

	public static void makeConcreteRecursively(Facade facade, int maximumDepth) {
		makeConcreteRecursively(facade, eachFacade -> Facade.getAncestors(eachFacade).size() >= maximumDepth);
	}

	public static void makeConcreteRecursively(Facade facade, Function<Facade, Boolean> until) {
		if (until.apply(facade)) {
			return;
		}
		facade.setConcrete(true);
		facade.getChildren().forEach(childFacade -> makeConcreteRecursively(childFacade, until));

	}

	public static Object getChildInitializerValue(InstanceBuilder instanceBuilder, String childIdentifier,
			Solution solutionInstance) {
		InitializerFacade initializerFacade = (InitializerFacade) new InstanceBuilderFacade(null, instanceBuilder,
				solutionInstance).getChildren().stream().filter(facade -> facade.toString().equals(childIdentifier))
						.findFirst().get();
		return getInitializerFacadeValue(initializerFacade);
	}

	public static void setChildInitializerValue(InstanceBuilder instanceBuilder, String childIdentifier, Object value,
			Solution solutionInstance) {
		InitializerFacade initializerFacade = (InitializerFacade) new InstanceBuilderFacade(null, instanceBuilder,
				solutionInstance).getChildren().stream().filter(facade -> facade.toString().equals(childIdentifier))
						.findFirst().get();
		setInitializerFacadeValue(initializerFacade, value);
	}

	private static Object getInitializerFacadeValue(InitializerFacade initializerFacade) {
		if (initializerFacade instanceof ParameterInitializerFacade) {
			return ((ParameterInitializerFacade) initializerFacade).getParameterValue();
		} else if (initializerFacade instanceof FieldInitializerFacade) {
			return ((FieldInitializerFacade) initializerFacade).getFieldValue();
		} else if (initializerFacade instanceof ListItemInitializerFacade) {
			return ((ListItemInitializerFacade) initializerFacade).getItemValue();
		} else {
			throw new UnexpectedError();
		}
	}

	private static void setInitializerFacadeValue(InitializerFacade initializerFacade, Object value) {
		if (initializerFacade instanceof ParameterInitializerFacade) {
			((ParameterInitializerFacade) initializerFacade).setParameterValue(value);
		} else if (initializerFacade instanceof FieldInitializerFacade) {
			((FieldInitializerFacade) initializerFacade).setFieldValue(value);
		} else if (initializerFacade instanceof ListItemInitializerFacade) {
			((ListItemInitializerFacade) initializerFacade).setItemValue(value);
		} else {
			throw new UnexpectedError();
		}
	}

}