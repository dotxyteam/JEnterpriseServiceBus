/*
 * Copyright (C) 2004, 2005 Joe Walnes.
 * Copyright (C) 2006, 2007, 2011, 2013, 2018 XStream Committers.
 * All rights reserved.
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 * 
 * Created on 16. November 2004 by Joe Walnes
 */
package com.otk.jesb.meta;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ClassLoader composed of other classloaders. Each loader will be used to load
 * the required class or resource until one of them succeeds. The
 * {@link #firstClassLoader} and the {@link #firstClassLoader} will be used
 * first and last respectively if provided. <b>Note:</b> The loaders will always
 * be called in the REVERSE order in which they were added.
 *
 * <p>
 * The added classloaders are kept with weak references to allow an application
 * container to reload classes.
 * </p>
 */
@SuppressWarnings("all")
public class CompositeClassLoader extends ClassLoader {

	private ClassLoader firstClassLoader;
	private ClassLoader lastClassLoader;
	private final List<WeakReference> classLoaders = new CopyOnWriteArrayList<WeakReference>();
	private final ReferenceQueue queue = new ReferenceQueue();

	public ClassLoader getFirstClassLoader() {
		return firstClassLoader;
	}

	public void setFirstClassLoader(ClassLoader firstClassLoader) {
		this.firstClassLoader = firstClassLoader;
	}

	public ClassLoader getLastClassLoader() {
		return lastClassLoader;
	}

	public void setLastClassLoader(ClassLoader lastClassLoader) {
		this.lastClassLoader = lastClassLoader;
	}

	public List<ClassLoader> getClassLoaders() {
		return classLoaders.stream().map(ref -> (ClassLoader) ((WeakReference) ref).get()).collect(Collectors.toList());
	}

	/**
	 * Add a loader to this
	 * 
	 * @param classLoader A loader
	 */
	public synchronized void add(ClassLoader classLoader) {
		cleanup();
		if (classLoader != null) {
			addInternal(classLoader);
		}
	}

	private void addInternal(ClassLoader classLoader) {
		WeakReference refClassLoader = null;
		for (Iterator iterator = classLoaders.iterator(); iterator.hasNext();) {
			WeakReference ref = (WeakReference) iterator.next();
			ClassLoader cl = (ClassLoader) ref.get();
			if (cl == null) {
				classLoaders.remove(ref);
			} else if (cl == classLoader) {
				classLoaders.remove(ref);
				refClassLoader = ref;
			}
		}
		classLoaders.add(0, refClassLoader != null ? refClassLoader : new WeakReference(classLoader, queue));
	}

	@Override
	public URL getResource(String name) {
		return applyCompositively(getResourceLoadingFunction(name));
	}

	@Override
	public Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
		Class result = applyCompositively(getClassLoadingFunction(name, resolve));
		if (result == null) {
			throw new ClassNotFoundException(name);
		}
		return result;
	}

	protected Function<ClassLoader, URL> getResourceLoadingFunction(String resourceName) {
		return classLoader -> classLoader.getResource(resourceName);
	}

	protected Function<ClassLoader, Class> getClassLoadingFunction(String className, boolean resolve) {
		return classLoader -> {
			try {
				Class<?> clazz = classLoader.loadClass(className);
				if (resolve) {
					resolveClass(clazz);
				}
				return clazz;
			} catch (ClassNotFoundException notFound) {
				return null;
			}
		};
	}

	private <T> T applyCompositively(Function<ClassLoader, T> accessor) {
		List copy = new ArrayList(classLoaders.size()) {

			public boolean addAll(Collection c) {
				boolean result = false;
				for (Iterator iter = c.iterator(); iter.hasNext();) {
					result |= add(iter.next());
				}
				return result;
			}

			public boolean add(Object ref) {
				Object classLoader = ((WeakReference) ref).get();
				if (classLoader != null) {
					return super.add(classLoader);
				}
				return false;
			}

		};
		synchronized (this) {
			cleanup();
			copy.addAll(classLoaders);
		}
		if (firstClassLoader != null) {
			T result = accessor.apply(firstClassLoader);
			if (result != null) {
				return result;
			}
		}
		for (Iterator iterator = copy.iterator(); iterator.hasNext();) {
			ClassLoader classLoader = (ClassLoader) iterator.next();
			T result = accessor.apply(classLoader);
			if (result != null) {
				return result;
			}
		}
		if (lastClassLoader != null) {
			T result = accessor.apply(lastClassLoader);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	private void cleanup() {
		WeakReference ref;
		while ((ref = (WeakReference) queue.poll()) != null) {
			classLoaders.remove(ref);
		}
	}
}
