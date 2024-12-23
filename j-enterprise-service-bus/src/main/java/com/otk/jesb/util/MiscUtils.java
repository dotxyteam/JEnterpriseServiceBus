package com.otk.jesb.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.otk.jesb.ActivityMetadata;
import com.otk.jesb.Folder;
import com.otk.jesb.GUI;
import com.otk.jesb.InstanceSpecification;
import com.otk.jesb.PathExplorer;
import com.otk.jesb.Plan;
import com.otk.jesb.InstanceSpecification.DynamicValue;
import com.otk.jesb.InstanceSpecification.ValueMode;
import com.otk.jesb.Plan.ExecutionContext;
import com.otk.jesb.Resource;
import com.otk.jesb.Solution;
import com.otk.jesb.Step;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
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
		for (ClassLoader additionalClassLoader : PathExplorer.ClassProvider.getAdditionalClassLoaders()) {
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
			return ValueMode.OBJECT_SPECIFICATION;
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
		for (ActivityMetadata activityMetadata : GUI.Reflecter.ACTIVITY_METADATAS) {
			if (activityMetadata.getActivityBuilderClass().equals(step.getActivityBuilder().getClass())) {
				return activityMetadata.getActivityIconImagePath();
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Resource> List<T> findResources(Solution solution, Class<T> resourceClass) {
		List<T> result = new ArrayList<T>();
		for (Resource resource : solution.getContents()) {
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
	public static  <T extends Resource> List<T>  findDescendantResources(Folder folder, Class<T> resourceClass) {
		List<T> result = new ArrayList<T>();
		for (Resource resource : folder.getContents()) {
			if (resource.getClass().equals(resourceClass)) {
				result.add((T) resource);
			}
			if (resource instanceof Folder) {
				result.addAll(findDescendantResources((Folder) resource, resourceClass));
			}
		}
		return result;
	}

}
