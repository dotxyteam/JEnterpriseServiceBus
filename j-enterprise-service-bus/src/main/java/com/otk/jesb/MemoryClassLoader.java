package com.otk.jesb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class MemoryClassLoader extends ClassLoader {

	public MemoryClassLoader(ClassLoader parent) {
		super(parent);
	}

	public Class<?> compileAndLoad(String className, String javaSource) throws Exception {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		StringWriter errorWriter = new StringWriter();
		ByteArrayOutputStream compiledBytesOutputStream = new ByteArrayOutputStream();

		SimpleJavaFileObject sourceFile = new SimpleJavaFileObject(
				URI.create("file:///" + className.replace('.', '/') + ".java"), Kind.SOURCE) {
			@Override
			public CharSequence getCharContent(boolean ignoreEncErrors) {
				return javaSource;
			}
		};

		SimpleJavaFileObject classFile = new SimpleJavaFileObject(
				URI.create("file:///" + className.replace('.', '/') + ".class"), Kind.CLASS) {
			@Override
			public OutputStream openOutputStream() throws IOException {
				return compiledBytesOutputStream;
			}
		};

		ForwardingJavaFileManager fileManager = new ForwardingJavaFileManager(
				compiler.getStandardFileManager(null, null, null)) {
			@Override
			public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className,
					JavaFileObject.Kind kind, FileObject sibling) throws IOException {
				return classFile;
			}
		};

		// compile class
		if (!compiler.getTask(errorWriter, fileManager, null, null, null, Arrays.asList(sourceFile)).call()) {
			throw new Exception(errorWriter.toString());
		}

		// load class
		byte[] bytes = compiledBytesOutputStream.toByteArray();
		return super.defineClass(className, bytes, 0, bytes.length);
	}
}