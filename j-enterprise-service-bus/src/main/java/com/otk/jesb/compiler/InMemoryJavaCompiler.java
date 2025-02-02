package com.otk.jesb.compiler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.otk.jesb.util.MiscUtils;

public class InMemoryJavaCompiler {

	private final Map<String, byte[]> classes = new HashMap<>();
	private final Map<String, List<JavaFileObject>> packages = new HashMap<>();
	private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
	private final JavaFileManager manager = new ForwardingJavaFileManager<JavaFileManager>(
			compiler.getStandardFileManager(null, null, null)) {
		@Override
		public JavaFileObject getJavaFileForOutput(Location loc, String name, Kind kind, FileObject obj) {
			return outputFile(name, kind);
		}

		@Override
		public Iterable<JavaFileObject> list(Location loc, String pkg, Set<Kind> kinds, boolean rec)
				throws IOException {
			List<JavaFileObject> files = packages.get(pkg);
			if (files != null)
				return files;
			else
				return super.list(loc, pkg, kinds, rec);
		}

		@Override
		public String inferBinaryName(Location loc, JavaFileObject file) {
			if (file instanceof NamedJavaFileObject)
				return ((NamedJavaFileObject) file).className;
			else
				return super.inferBinaryName(loc, file);
		}
	};
	private Iterable<String> options;

	public Iterable<String> getOptions() {
		return options;
	}

	public void setOptions(Iterable<String> options) {
		this.options = options;
	}

	public List<Class<?>> compile(File sourceDirectory, ClassLoader parentClassLoader) throws CompilationError {
		List<JavaFileObject> files = collectSourceFiles(sourceDirectory, null);
		compile(files.toArray(new JavaFileObject[files.size()]));
		List<Class<?>> result = new ArrayList<Class<?>>();
		for (JavaFileObject file : files) {
			String name = ((NamedJavaFileObject) file).className;
			try {
				result.add(new MemoryClassLoader(parentClassLoader, name).loadClass(name));
			} catch (ClassNotFoundException e) {
				throw new AssertionError(e);
			}
		}
		return result;
	}

	public Class<?> compile(String className, String source, ClassLoader parentClassLoader) throws CompilationError {
		compile(sourceFile(className, source));
		try {
			return new MemoryClassLoader(parentClassLoader, className).loadClass(className);
		} catch (ClassNotFoundException e) {
			throw new AssertionError(e);
		}
	}

	private void compile(JavaFileObject... files) throws CompilationError {
		compile(Arrays.asList(files));
	}

	private void compile(List<JavaFileObject> files) throws CompilationError {
		if (files.isEmpty())
			throw new RuntimeException("No input files");
		DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
		CompilationTask task = compiler.getTask(null, manager, collector, options, null, files);
		boolean success = task.call();
		check(success, collector);
	}

	private void check(boolean success, DiagnosticCollector<?> collector) throws CompilationError {
		for (Diagnostic<?> diagnostic : collector.getDiagnostics()) {
			if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
				throw new CompilationError((int) diagnostic.getStartPosition(), (int) diagnostic.getEndPosition(),
						diagnostic.getMessage(null));
			}
		}
		if (!success)
			throw new CompilationError(-1, -1, "Unknown error");
	}

	private List<JavaFileObject> collectSourceFiles(File sourceDirectory, String currentPackageName) {
		List<JavaFileObject> result = new ArrayList<JavaFileObject>();
		for (File fileOrDirectory : sourceDirectory.listFiles()) {
			if (fileOrDirectory.isFile()) {
				if (fileOrDirectory.getName().endsWith(".java")) {
					String className = fileOrDirectory.getName().substring(0, fileOrDirectory.getName().length() - ".java".length());
					if (currentPackageName != null) {
						className = currentPackageName + "." + className;
					}
					String source;
					try (FileInputStream in = new FileInputStream(fileOrDirectory)) {
						source = MiscUtils.read(in);
					} catch (Exception e) {
						throw new AssertionError(e);
					}
					result.add(sourceFile(className, source));
				}
			} else if (fileOrDirectory.isDirectory()) {
				String subPackageName = ((currentPackageName != null) ? (currentPackageName + ".") : "") + fileOrDirectory.getName();
				result.addAll(collectSourceFiles(fileOrDirectory, subPackageName));
			} else {
				throw new AssertionError();
			}
		}
		return result;
	}

	private static JavaFileObject sourceFile(String name, String source) {
		return inputFile(name, Kind.SOURCE, source);
	}

	private static JavaFileObject inputFile(String name, Kind kind, String content) {
		return new NamedJavaFileObject(name, kind) {
			@Override
			public CharSequence getCharContent(boolean b) {
				return content;
			}
		};
	}

	private static NamedJavaFileObject inputFile(String name, Kind kind, byte[] content) {
		return new NamedJavaFileObject(name, kind) {
			@Override
			public InputStream openInputStream() {
				return new ByteArrayInputStream(content);
			}
		};
	}

	private JavaFileObject outputFile(String name, Kind kind) {
		return new NamedJavaFileObject(name, kind) {
			@Override
			public OutputStream openOutputStream() {
				return outputStream(className);
			}
		};
	}

	private OutputStream outputStream(String name) {
		return new ByteArrayOutputStream() {
			@Override
			public void close() {
				storeClass(name, toByteArray());
			}
		};
	}

	private void storeClass(String name, byte[] bytes) {
		classes.put(name, bytes);
		NamedJavaFileObject file = inputFile(name, Kind.CLASS, bytes);
		int dot = name.lastIndexOf('.');
		String pkg = dot == -1 ? "" : name.substring(0, dot);
		packages.computeIfAbsent(pkg, k -> new ArrayList<>()).add(file);
	}

	private void unstoreClass(String name) {
		classes.remove(name);
		int dot = name.lastIndexOf('.');
		String pkg = dot == -1 ? "" : name.substring(0, dot);
		packages.get(pkg).removeIf((file) -> ((NamedJavaFileObject) file).className.equals(name));
		if (packages.get(pkg).size() == 0) {
			packages.remove(pkg);
		}
	}

	private static class NamedJavaFileObject extends SimpleJavaFileObject {
		final String className;

		protected NamedJavaFileObject(String name, Kind kind) {
			super(URI.create(name.replace('.', '/') + kind.extension), kind);
			this.className = name;
		}
	}

	private class MemoryClassLoader extends ClassLoader {
		private List<String> definedClassNames = new ArrayList<String>();
		private String mainClassName;

		public MemoryClassLoader(ClassLoader parentClassLoader, String mainClassName) {
			super(parentClassLoader);
			this.mainClassName = mainClassName;
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			synchronized (getClassLoadingLock(name)) {
				if (name.equals(mainClassName) || name.startsWith(mainClassName + "$")) {
					// First, check if the class has already been loaded
					Class<?> c = findLoadedClass(name);
					if (c == null) {
						c = findClass(name);
					}
					if (resolve) {
						resolveClass(c);
					}
					return c;
				} else {
					Class<?> c = getParent().loadClass(name);
					if (resolve) {
						resolveClass(c);
					}
					return c;
				}
			}
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			byte[] bytes = classes.get(name);
			if (bytes == null)
				throw new ClassNotFoundException(name);
			definedClassNames.add(name);
			return super.defineClass(name, bytes, 0, bytes.length);
		}

		@Override
		protected void finalize() throws Throwable {
			for (String name : definedClassNames) {
				unstoreClass(name);
			}
			super.finalize();
		}

	}
}