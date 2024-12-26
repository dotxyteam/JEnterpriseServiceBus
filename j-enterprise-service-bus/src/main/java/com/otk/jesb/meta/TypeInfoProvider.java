package com.otk.jesb.meta;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import xy.reflect.ui.ReflectionUI;
import xy.reflect.ui.info.IInfo;
import xy.reflect.ui.info.field.GetterFieldInfo;
import xy.reflect.ui.info.field.PublicFieldInfo;
import xy.reflect.ui.info.method.DefaultMethodInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;

public class TypeInfoProvider {

	public static ITypeInfo getTypeInfo(String typeName) {
		return getTypeInfo(typeName, null);
	}

	public static ITypeInfo getTypeInfo(String typeName, IInfo typeOwner) {
		Class<?> objectClass = ClassProvider.getClass(typeName);
		ReflectionUI reflectionUI = ReflectionUI.getDefault();
		JavaTypeInfoSource javaTypeInfoSource;
		if (typeOwner != null) {
			if (typeOwner instanceof GetterFieldInfo) {
				Method javaTypeOwner = ((GetterFieldInfo) typeOwner).getJavaGetterMethod();
				javaTypeInfoSource = new JavaTypeInfoSource(reflectionUI, objectClass, javaTypeOwner, -1, null);
			} else if (typeOwner instanceof PublicFieldInfo) {
				Field javaTypeOwner = ((PublicFieldInfo) typeOwner).getJavaField();
				javaTypeInfoSource = new JavaTypeInfoSource(reflectionUI, objectClass, javaTypeOwner, -1, null);
			} else if (typeOwner instanceof DefaultMethodInfo) {
				Method javaTypeOwner = ((DefaultMethodInfo) typeOwner).getJavaMethod();
				javaTypeInfoSource = new JavaTypeInfoSource(reflectionUI, objectClass, javaTypeOwner, -1, null);
			} else {
				throw new AssertionError();
			}
		} else {
			javaTypeInfoSource = new JavaTypeInfoSource(reflectionUI, objectClass, null);
		}
		return reflectionUI.getTypeInfo(javaTypeInfoSource);
	}

}