package com.otk.jesb.compiler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

class NamedJavaFileObject extends SimpleJavaFileObject {
	final String name;

	protected NamedJavaFileObject(String name, Kind kind) {
		super(URI.create(name.replace('.', '/') + kind.extension), kind);
		this.name = name;
	}
}

public class InMemoryJavaCompiler {

	public static JavaFileObject sourceFile(String name, String source) {
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

	private static JavaFileObject inputFile(String name, Kind kind, byte[] content) {
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
				return outputStream(name);
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
		JavaFileObject file = inputFile(name, Kind.CLASS, bytes);
		int dot = name.lastIndexOf('.');
		String pkg = dot == -1 ? "" : name.substring(0, dot);
		packages.computeIfAbsent(pkg, k -> new ArrayList<>()).add(file);
	}

	private final Map<String, byte[]> classes = new HashMap<>();
	private final Map<String, List<JavaFileObject>> packages = new HashMap<>();

	private final ClassLoader loader = new ClassLoader() {
		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			byte[] bytes = classes.get(name);
			if (bytes == null)
				throw new ClassNotFoundException(name);
			return super.defineClass(name, bytes, 0, bytes.length);
		}
	};

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
				return ((NamedJavaFileObject) file).name;
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

	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return loader.loadClass(name);
	}

	public Class<?> compile(String name, String source) throws CompilationError {
		compile(sourceFile(name, source));
		try {
			return loadClass(name);
		} catch (ClassNotFoundException e) {
			throw new AssertionError(e);
		}
	}

	public void compile(JavaFileObject... files) throws CompilationError {
		compile(Arrays.asList(files));
	}

	public void compile(List<JavaFileObject> files) throws CompilationError {
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
}