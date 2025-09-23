package com.otk.jesb.meta;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

public class DelegatingClassLoader extends ClassLoader {

	private volatile ClassLoader delegate;

	public DelegatingClassLoader(ClassLoader initial) {
		super(null);
		this.delegate = initial;
	}

	public ClassLoader getDelegate() {
		return delegate;
	}

	public void setDelegate(ClassLoader newDelegate) {
		this.delegate = newDelegate;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		return delegate.loadClass(name);
	}

	@Override
	public URL getResource(String name) {
		return delegate.getResource(name);
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		return delegate.getResources(name);
	}
}
