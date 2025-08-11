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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.otk.jesb.PotentialError;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.Variable;
import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.compiler.CompiledFunction;
import com.otk.jesb.compiler.CompiledFunction.FunctionCallError;
import com.otk.jesb.instantiation.InstantiationFunctionCompilationContext;
import com.otk.jesb.instantiation.EnumerationItemSelector;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.Facade;
import com.otk.jesb.instantiation.InstanceBuilder;
import com.otk.jesb.instantiation.InstanceBuilderFacade;
import com.otk.jesb.instantiation.InstantiationFunction;
import com.otk.jesb.instantiation.MapEntryBuilder;
import com.otk.jesb.instantiation.ParameterInitializerFacade;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.instantiation.RootInstanceBuilderFacade;
import com.otk.jesb.instantiation.ValueMode;
import com.otk.jesb.meta.TypeInfoProvider;
import com.otk.jesb.resource.builtin.SharedStructureModel;

import xy.reflect.ui.ReflectionUI;
import xy.reflect.ui.info.method.AbstractConstructorInfo;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.type.DefaultTypeInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.enumeration.IEnumerationTypeInfo;
import xy.reflect.ui.info.type.factory.InfoProxyFactory;
import xy.reflect.ui.info.type.iterable.map.IMapEntryTypeInfo;
import xy.reflect.ui.info.type.source.ITypeInfoSource;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;
import xy.reflect.ui.util.ClassUtils;
import xy.reflect.ui.util.ReflectionUIUtils;

public class InstantiationUtils {

	public static final String RELATIVE_TYPE_NAME_VARIABLE_PART_REFRENCE = "${_}";
	private static final String RELATIVE_TYPE_NAME_VARIABLE_PART_START = "a7374617274";
	private static final String RELATIVE_TYPE_NAME_VARIABLE_PART_END = "z656e64";
	private static final Pattern RELATIVE_TYPE_NAME_PATTERN = Pattern.compile(
			".*(" + RELATIVE_TYPE_NAME_VARIABLE_PART_START + ".+" + RELATIVE_TYPE_NAME_VARIABLE_PART_END + ").*");

	public static Object cloneInitializer(Object initializer) {
		ReflectionUI cloningReflection = new ReflectionUI() {
			@Override
			public ITypeInfo getTypeInfo(ITypeInfoSource typeInfoSource) {
				return new InfoProxyFactory() {
					@Override
					protected boolean isImmutable(ITypeInfo type) {
						Class<?> objectClass = TypeInfoProvider.getClass(type.getName());
						if (Accessor.class.isAssignableFrom(objectClass)) {
							return true;
						}
						return super.isImmutable(type);
					}
				}.wrapTypeInfo(super.getTypeInfo(typeInfoSource));
			}
		};
		return ReflectionUIUtils.copyAccordingInfos(cloningReflection, initializer);
	}

	public static Object executeFunction(InstantiationFunction function, InstantiationContext instantiationContext)
			throws FunctionCallError {
		InstantiationFunctionCompilationContext compilationContext = instantiationContext
				.getFunctionCompilationContex(function);
		if (!MiscUtils.equalsOrBothNull(compilationContext.getParentFacade(), instantiationContext.getParentFacade())) {
			throw new UnexpectedError();
		}
		List<VariableDeclaration> expectedVariableDeclarations = compilationContext.getVariableDeclarations(function);
		Set<String> actualVariableNames = instantiationContext.getVariables().stream()
				.filter(variable -> variable.getValue() != Variable.UNDEFINED_VALUE).map(variable -> variable.getName())
				.collect(Collectors.toSet());
		Set<String> expectedVariableNames = expectedVariableDeclarations.stream()
				.map(variableDeclaration -> variableDeclaration.getVariableName()).collect(Collectors.toSet());
		if (!actualVariableNames.equals(expectedVariableNames)) {
			throw new UnexpectedError();
		}
		CompiledFunction compiledFunction;
		try {
			compiledFunction = function.getCompiledVersion(compilationContext.getPrecompiler(),
					expectedVariableDeclarations, compilationContext.getFunctionReturnType(function));
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
		Object conditionResult = interpretValue(condition, TypeInfoProvider.getTypeInfo(Boolean.class.getName()),
				context);
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
			boolean recursively, List<VariableDeclaration> variableDeclarations) throws ValidationError {
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
						compilationContext.getVariableDeclarations(function), functionReturnType);
			} catch (CompilationError e) {
				throw new ValidationError("Failed to compile the " + valueName + " function", e);
			}
		} else if (value instanceof InstanceBuilder) {
			try {
				Class<?> instanceBuilderJavaType;
				try {
					instanceBuilderJavaType = TypeInfoProvider.getClass(
							((InstanceBuilder) value).computeActualTypeName(getAncestorInstanceBuilders(parentFacade)));
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
				new InstanceBuilderFacade(parentFacade, (InstanceBuilder) value).validate(recursively,
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

	public static Object getDefaultInterpretableValue(ITypeInfo type, Facade currentFacade) {
		return getDefaultInterpretableValue(type, ValueMode.PLAIN, currentFacade);
	}

	public static Object getDefaultInterpretableValue(ITypeInfo type, ValueMode valueMode, Facade currentFacade) {
		if ((type == null) || type.getName().equals(Object.class.getName())) {
			return null;
		} else if (valueMode == ValueMode.FUNCTION) {
			String functionBody;
			if (!isComplexType(type)
					&& !ClassUtils.isPrimitiveWrapperClass(((JavaTypeInfoSource) type.getSource()).getJavaType())) {
				Object defaultValue = ReflectionUIUtils.createDefaultInstance(type);
				if (defaultValue.getClass().isEnum()) {
					functionBody = "return "
							+ makeTypeNamesRelative(type.getName(), getAncestorInstanceBuilders(currentFacade)) + "."
							+ defaultValue.toString() + ";";
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
					return ReflectionUIUtils.createDefaultInstance(type);
				}
			} else {
				if (RootInstanceBuilderFacade.isRootInitializerFacade(currentFacade)) {
					RootInstanceBuilder rootInstanceBuilder = ((RootInstanceBuilderFacade) ((ParameterInitializerFacade) currentFacade)
							.getCurrentInstanceBuilderFacade()).getUnderlying();
					InstanceBuilder result = new InstanceBuilder();
					result.setTypeName(rootInstanceBuilder.getRootInstanceTypeName());
					result.setDynamicTypeNameAccessor(rootInstanceBuilder.getRootInstanceDynamicTypeNameAccessor());
					if (!type.getName().equals(result
							.computeActualTypeName(InstantiationUtils.getAncestorInstanceBuilders(currentFacade)))) {
						throw new UnexpectedError();
					}
					return result;
				} else {
					Class<?> javaType = ((DefaultTypeInfo) type).getJavaType();
					if (SharedStructureModel.isStructuredClass(javaType)) {
						SharedStructureModel model = SharedStructureModel.getFromStructuredClass(javaType);
						return new InstanceBuilder(model.getStructuredClassNameAccessor());
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
							return new InstanceBuilder(
									makeTypeNamesRelative(type.getName(), getAncestorInstanceBuilders(currentFacade)));
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

	public static String makeTypeNamesRelative(String text, List<InstanceBuilder> ancestorInstanceBuilders) {
		if ((ancestorInstanceBuilders == null) || (ancestorInstanceBuilders.size() == 0)) {
			return text;
		}
		String dynamicTypeNamePart = extractRelativeTypeNameVariablePart(ancestorInstanceBuilders);
		if (dynamicTypeNamePart != null) {
			text = text.replace(dynamicTypeNamePart, RELATIVE_TYPE_NAME_VARIABLE_PART_REFRENCE);
		}
		return text;
	}

	public static String makeTypeNamesAbsolute(String text, List<InstanceBuilder> ancestorInstanceBuilders) {
		if ((ancestorInstanceBuilders == null) || (ancestorInstanceBuilders.size() == 0)) {
			return text;
		}
		String dynamicTypeNamePart = extractRelativeTypeNameVariablePart(ancestorInstanceBuilders);
		if (dynamicTypeNamePart != null) {
			text = text.replace(RELATIVE_TYPE_NAME_VARIABLE_PART_REFRENCE, dynamicTypeNamePart);
		}
		return text;
	}

	public static String toRelativeTypeNameVariablePart(String baseClassNamePart) {
		return RELATIVE_TYPE_NAME_VARIABLE_PART_START + baseClassNamePart + RELATIVE_TYPE_NAME_VARIABLE_PART_END;
	}

	private static String extractRelativeTypeNameVariablePart(List<InstanceBuilder> ancestorInstanceBuilders) {
		for (int i = 0; i < ancestorInstanceBuilders.size(); i++) {
			InstanceBuilder ancestorInstanceBuilder = ancestorInstanceBuilders.get(i);
			String absoluteAncestorTypeName = ancestorInstanceBuilder
					.computeActualTypeName(ancestorInstanceBuilders.subList(i + 1, ancestorInstanceBuilders.size()));
			Matcher matcher = RELATIVE_TYPE_NAME_PATTERN.matcher(absoluteAncestorTypeName);
			if (matcher.find()) {
				return matcher.group(1);
			}
		}
		return null;
	}

	public static int positionBeforeTypeNamesMadeAbsolute(int positionAfter, String text,
			List<InstanceBuilder> ancestorInstanceBuilders) {
		if ((ancestorInstanceBuilders == null) || (ancestorInstanceBuilders.size() == 0)) {
			return positionAfter;
		}
		String dynamicTypeNamePart = extractRelativeTypeNameVariablePart(ancestorInstanceBuilders);
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

}