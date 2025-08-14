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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * ClassLoader that is composed of other classloaders. Each loader will be used
 * to try to load each required class, until one of them succeeds. Otherwise the
 * eventual {@link #defaultClassLoader} will be used. <b>Note:</b> The loaders
 * will always be called in the REVERSE order they were added in.
 *
 * <p>
 * The added classloaders are kept with weak references to allow an application
 * container to reload classes.
 * </p>
 */
@SuppressWarnings("all")
public class CompositeClassLoader extends ClassLoader {

	private ClassLoader defaultClassLoader;
	private final List<WeakReference> classLoaders = new CopyOnWriteArrayList<WeakReference>();
	private final ReferenceQueue queue = new ReferenceQueue();

	public CompositeClassLoader(ClassLoader defaultClassLoader) {
		this.defaultClassLoader = defaultClassLoader;
	}

	public ClassLoader getDefaultClassLoader() {
		return defaultClassLoader;
	}

	public void setDefaultClassLoader(ClassLoader defaultClassLoader) {
		this.defaultClassLoader = defaultClassLoader;
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
	public Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
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

		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		for (Iterator iterator = copy.iterator(); iterator.hasNext();) {
			ClassLoader classLoader = (ClassLoader) iterator.next();
			if (classLoader == contextClassLoader) {
				contextClassLoader = null;
			}
			try {
				Class<?> result = classLoader.loadClass(name);
				if (resolve) {
					resolveClass(result);
				}
				return result;
			} catch (ClassNotFoundException notFound) {
				// ok.. try another one
			}
		}

		if (defaultClassLoader != null) {
			Class<?> result = defaultClassLoader.loadClass(name);
			if (resolve) {
				resolveClass(result);
			}
			return result;
		}

		throw new ClassNotFoundException(name);
	}

	private void cleanup() {
		WeakReference ref;
		while ((ref = (WeakReference) queue.poll()) != null) {
			classLoaders.remove(ref);
		}
	}
}
