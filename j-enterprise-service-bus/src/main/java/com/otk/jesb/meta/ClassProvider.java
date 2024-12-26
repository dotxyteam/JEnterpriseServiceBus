package com.otk.jesb.meta;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class ClassProvider {

	private static Set<ClassLoader> additionalClassLoaders = Collections
			.newSetFromMap(new WeakHashMap<ClassLoader, Boolean>());

	public static Class<?> getClass(String typeName) {
		try {
			return Class.forName(typeName);
		} catch (ClassNotFoundException e) {
			for (ClassLoader classLoader : additionalClassLoaders) {
				try {
					return Class.forName(typeName, false, classLoader);
				} catch (ClassNotFoundException ignore) {
				}
			}
		}
		throw new AssertionError(new ClassNotFoundException(typeName));
	}

	public static void register(ClassLoader classLoader) {
		additionalClassLoaders.add(classLoader);
	}

	public static void unregister(ClassLoader classLoader) {
		additionalClassLoaders.remove(classLoader);
	}

	public static Set<ClassLoader> getAdditionalClassLoaders() {
		return additionalClassLoaders;
	}
}