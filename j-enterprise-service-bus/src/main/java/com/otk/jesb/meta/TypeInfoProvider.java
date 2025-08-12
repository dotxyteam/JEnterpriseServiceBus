package com.otk.jesb.meta;

import java.lang.reflect.Array;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.ReflectionUI;
import xy.reflect.ui.info.IInfo;
import xy.reflect.ui.info.field.GetterFieldInfo;
import xy.reflect.ui.info.field.PublicFieldInfo;
import xy.reflect.ui.info.method.DefaultConstructorInfo;
import xy.reflect.ui.info.method.DefaultMethodInfo;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.type.DefaultTypeInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;
import xy.reflect.ui.util.ClassUtils;

public class TypeInfoProvider {

	public static final ReflectionUI INTROSPECTOR = ReflectionUI.getDefault();

	public static ITypeInfo getTypeInfo(String typeName) {
		return getTypeInfo(MiscUtils.getJESBClass(typeName));
	}

	public static ITypeInfo getTypeInfo(String typeName, IInfo typeOwner) {
		return getTypeInfo(MiscUtils.getJESBClass(typeName), typeOwner);
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
		Class<?> objectClass = MiscUtils.getJESBClass(parameterTypeName);
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

	public static ITypeInfo getInfoFromResolvedType(ResolvedType resolvedType) {
		if (resolvedType.isPrimitive()) {
			return getTypeInfo(ClassUtils
					.wrapperToPrimitiveClass(MiscUtils.getJESBClass(resolvedType.asPrimitive().getBoxTypeQName())));
		} else if (resolvedType.isReferenceType()) {
			ResolvedReferenceType referenceType = resolvedType.asReferenceType();
			String qualifiedName = referenceType.getQualifiedName();
			Class<?> javaType = MiscUtils.getJESBClassFromCanonicalName(qualifiedName);
			List<ResolvedType> typeParameters = referenceType.typeParametersValues();
			if (typeParameters.size() > 0) {
				List<Class<?>> typeParameterClasses = new ArrayList<Class<?>>();
				for (ResolvedType resolvedTypeParameter : typeParameters) {
					ITypeInfo typeParameterInfo = getInfoFromResolvedType(resolvedTypeParameter);
					if (typeParameterInfo == null) {
						return getTypeInfo(javaType);
					}
					typeParameterClasses.add(((DefaultTypeInfo) typeParameterInfo).getJavaType());
				}
				return getTypeInfo(javaType,
						typeParameterClasses.toArray(new Class<?>[typeParameterClasses.size()]));
			} else {
				return getTypeInfo(javaType);
			}
		} else if (resolvedType.isArray()) {
			Class<?> componentClass = ((DefaultTypeInfo) getInfoFromResolvedType(
					resolvedType.asArrayType().getComponentType())).getJavaType();
			return getTypeInfo(Array.newInstance(componentClass, 0).getClass());
		} else if (resolvedType.isWildcard() && resolvedType.asWildcard().isBounded()) {
			return getInfoFromResolvedType(resolvedType.asWildcard().getBoundedType());
		} else {
			return null;
		}
	}

}