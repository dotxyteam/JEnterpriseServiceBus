package com.otk.jesb.compiler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;

public class MemoryJavaFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
	private final Map<String, MemoryFileObject> classFilesMap;

	protected MemoryJavaFileManager(final StandardJavaFileManager fileManager) {
		super(fileManager);
		classFilesMap = new HashMap<String, MemoryFileObject>();
	}

	public Map<String, MemoryFileObject> getClassFileObjectsMap() {
		return classFilesMap;
	}

	@Override
	public JavaFileObject getJavaFileForOutput(final Location location, final String className, final Kind kind,
			final FileObject sibling) throws IOException {
		MemoryFileObject classFile = new MemoryFileObject(className);
		classFilesMap.put(className, classFile);
		return classFile;
	}

}
