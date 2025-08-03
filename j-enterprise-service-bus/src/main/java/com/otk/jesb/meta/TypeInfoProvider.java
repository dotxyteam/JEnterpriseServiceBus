package com.otk.jesb.meta;

import java.lang.reflect.Member;

import com.otk.jesb.PotentialError;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.ReflectionUI;
import xy.reflect.ui.info.IInfo;
import xy.reflect.ui.info.field.GetterFieldInfo;
import xy.reflect.ui.info.field.PublicFieldInfo;
import xy.reflect.ui.info.method.DefaultConstructorInfo;
import xy.reflect.ui.info.method.DefaultMethodInfo;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;
import xy.reflect.ui.util.ClassUtils;

public class TypeInfoProvider {

	public static final ReflectionUI INTROSPECTOR = ReflectionUI.getDefault();

	public static Class<?> getClass(String typeName) {
		String arrayComponentTypeName = MiscUtils.getArrayComponentTypeName(typeName);
		if (arrayComponentTypeName != null) {
			return MiscUtils.getArrayType(getClass(arrayComponentTypeName));
		}
		try {
			return ClassUtils.getCachedClassForName(typeName);
		} catch (ClassNotFoundException e) {
			try {
				return MiscUtils.IN_MEMORY_COMPILER.getClassLoader().loadClass(typeName);
			} catch (ClassNotFoundException e1) {
				throw new PotentialError(e1);
			}
		}
	}

	public static Class<?> getClassFromCanonicalName(String canonicalName) {
		try {
			return getClass(canonicalName);
		} catch (PotentialError e1) {
			try {
				int lastDotIndex = canonicalName.lastIndexOf('.');
				if (lastDotIndex == -1) {
					throw new PotentialError(new UnexpectedError());
				}
				return getClassFromCanonicalName(
						canonicalName.substring(0, lastDotIndex) + "$" + canonicalName.substring(lastDotIndex + 1));
			} catch (PotentialError e2) {
				throw new PotentialError(new ClassNotFoundException("Canonical name: " + canonicalName));
			}
		}
	}

	public static ITypeInfo getTypeInfo(String typeName) {
		return getTypeInfo(getClass(typeName));
	}

	public static ITypeInfo getTypeInfo(String typeName, IInfo typeOwner) {
		return getTypeInfo(getClass(typeName), typeOwner);
	}

	public static ITypeInfo getTypeInfo(Class<?> objectClass, IInfo typeOwner) {
		if (typeOwner != null) {
			Member javaTypeOwner;
			if (typeOwner instanceof GetterFieldInfo) {
				javaTypeOwner = ((GetterFieldInfo) typeOwner).getJavaGetterMethod();
			} else if (typeOwner instanceof PublicFieldInfo) {
				javaTypeOwner = ((PublicFieldInfo) typeOwner).getJavaField();
			} else if (typeOwner instanceof DefaultMethodInfo) {
				javaTypeOwner = ((DefaultMethodInfo) typeOwner).getJavaMethod();
			} else {
				throw new UnexpectedError();
			}
			return getTypeInfo(objectClass, javaTypeOwner);
		} else {
			return getTypeInfo(objectClass);
		}
	}

	public static ITypeInfo getTypeInfo(Class<?> objectClass) {
		return INTROSPECTOR.getTypeInfo(new JavaTypeInfoSource(objectClass, null));
	}

	public static ITypeInfo getTypeInfo(Class<?> objectClass, Member javaTypeOwner) {
		JavaTypeInfoSource javaTypeInfoSource = new JavaTypeInfoSource(objectClass, javaTypeOwner, -1, null);
		return INTROSPECTOR.getTypeInfo(javaTypeInfoSource);
	}

	public static ITypeInfo getTypeInfo(String parameterTypeName, IMethodInfo method, int parameterPosition) {
		Class<?> objectClass = getClass(parameterTypeName);
		JavaTypeInfoSource javaTypeInfoSource;
		if (method instanceof DefaultConstructorInfo) {
			javaTypeInfoSource = new JavaTypeInfoSource(objectClass,
					((DefaultConstructorInfo) method).getJavaConstructor(), parameterPosition, null);
		} else if (method instanceof DefaultMethodInfo) {
			javaTypeInfoSource = new JavaTypeInfoSource(objectClass, ((DefaultMethodInfo) method).getJavaMethod(),
					parameterPosition, null);
		} else {
			throw new UnexpectedError();
		}
		return INTROSPECTOR.getTypeInfo(javaTypeInfoSource);
	}

	public static ITypeInfo getTypeInfo(Class<?> objectClass, Class<?>[] genericTypeParameters) {
		JavaTypeInfoSource javaTypeInfoSource = new JavaTypeInfoSource(objectClass, genericTypeParameters, null);
		return INTROSPECTOR.getTypeInfo(javaTypeInfoSource);
	}

}