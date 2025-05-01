package com.otk.jesb.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.otk.jesb.Structure.Structured;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.Variable;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.compiler.CompiledFunction;
import com.otk.jesb.compiler.CompiledFunction.FunctionCallError;
import com.otk.jesb.instantiation.InstantiationFunctionCompilationContext;
import com.otk.jesb.instantiation.EnumerationItemSelector;
import com.otk.jesb.instantiation.EvaluationContext;
import com.otk.jesb.instantiation.Facade;
import com.otk.jesb.instantiation.InstanceBuilder;
import com.otk.jesb.instantiation.InstanceBuilderFacade;
import com.otk.jesb.instantiation.InstantiationFunction;
import com.otk.jesb.instantiation.MapEntryBuilder;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.instantiation.ValueMode;
import com.otk.jesb.meta.TypeInfoProvider;

import xy.reflect.ui.info.method.AbstractConstructorInfo;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.type.DefaultTypeInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.enumeration.IEnumerationTypeInfo;
import xy.reflect.ui.info.type.iterable.map.IMapEntryTypeInfo;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;
import xy.reflect.ui.util.ClassUtils;
import xy.reflect.ui.util.ReflectionUIUtils;

public class InstantiationUtils {

	private static final String PARENT_STRUCTURE_TYPE_NAME_SYMBOL = "${..}";

	public static Object executeFunction(InstantiationFunction function, EvaluationContext evaluationContext)
			throws FunctionCallError {
		InstantiationFunctionCompilationContext compilationContext = evaluationContext
				.getInstantiationFunctionCompilationContextMapper().apply(function);
		if (!MiscUtils.equalsOrBothNull(compilationContext.getParentFacade(), evaluationContext.getParentFacade())) {
			throw new UnexpectedError();
		}
		Set<String> actualVariableNames = evaluationContext.getVariables().stream()
				.filter(variable -> variable.getValue() != Variable.UNDEFINED_VALUE).map(variable -> variable.getName())
				.collect(Collectors.toSet());
		Set<String> expectedVariableNames = compilationContext.getVariableDeclarations().stream()
				.map(variableDeclaration -> variableDeclaration.getVariableName()).collect(Collectors.toSet());
		if (!actualVariableNames.equals(expectedVariableNames)) {
			throw new UnexpectedError();
		}
		CompiledFunction compiledFunction;
		try {
			compiledFunction = CompiledFunction.get(
					compilationContext.getPrecompiler().apply(function.getFunctionBody()),
					compilationContext.getVariableDeclarations(), compilationContext.getFunctionReturnType());
		} catch (CompilationError e) {
			throw new UnexpectedError(e);
		}
		return compiledFunction.call(evaluationContext.getVariables());
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
		if (selectedConstructorSignature == null) {
			if (typeInfo.getConstructors().size() == 0) {
				return null;
			} else {
				return (AbstractConstructorInfo) listSortedConstructors(typeInfo).get(0);
			}
		} else {
			return (AbstractConstructorInfo) ReflectionUIUtils.findMethodBySignature(typeInfo.getConstructors(),
					selectedConstructorSignature);
		}

	}

	public static boolean isConditionFullfilled(InstantiationFunction condition, EvaluationContext context)
			throws Exception {
		if (condition == null) {
			return true;
		}
		Object conditionResult = interpretValue(condition, TypeInfoProvider.getTypeInfo(Boolean.class.getName()),
				context);
		if (!(conditionResult instanceof Boolean)) {
			throw new UnexpectedError("Condition evaluation result is not boolean: '" + conditionResult + "'");
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

	public static Object interpretValue(Object value, ITypeInfo type, EvaluationContext context) throws Exception {
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
			return value;
		}
	}

	public static Object getDefaultInterpretableValue(ITypeInfo type, Facade currentFacade) {
		return getDefaultInterpretableValue(type, ValueMode.PLAIN, currentFacade);
	}

	public static Object getDefaultInterpretableValue(ITypeInfo type, ValueMode valueMode, Facade currentFacade) {
		Object specialDefaultValue = RootInstanceBuilder.getRootInitializerSpecialDefaultInterpretableValue(type,
				valueMode, currentFacade);
		if (specialDefaultValue != null) {
			return specialDefaultValue;
		}
		if (type == null) {
			return null;
		} else if (valueMode == ValueMode.FUNCTION) {
			String functionBody;
			if (!isComplexType(type)
					&& !ClassUtils.isPrimitiveWrapperClass(((JavaTypeInfoSource) type.getSource()).getJavaType())) {
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
				if (type instanceof IMapEntryTypeInfo) {
					return new MapEntryBuilder();
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

	public static List<InstanceBuilder> getAncestorStructureInstanceBuilders(Facade facade) {
		if (facade == null) {
			return null;
		}
		return Facade.getAncestors(facade).stream()
				.filter(f -> (f instanceof InstanceBuilderFacade) && Structured.class
						.isAssignableFrom(((DefaultTypeInfo) ((InstanceBuilderFacade) f).getTypeInfo()).getJavaType()))
				.map(f -> ((InstanceBuilderFacade) f).getUnderlying()).collect(Collectors.toList());
	}

	public InstantiationUtils() {
		super();
	}

	public static List<IMethodInfo> listSortedConstructors(ITypeInfo typeInfo) {
		List<IMethodInfo> ctors = typeInfo.getConstructors();
		ctors = new ArrayList<IMethodInfo>(ctors);
		Collections.sort(ctors, new Comparator<IMethodInfo>() {
			@Override
			public int compare(IMethodInfo o1, IMethodInfo o2) {
				return Integer.valueOf(o1.getParameters().size()).compareTo(Integer.valueOf(o2.getParameters().size()));
			}
		});
		return ctors;
	}

}