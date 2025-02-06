package com.otk.jesb.compiler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

import com.otk.jesb.meta.CompositeClassLoader;
import com.otk.jesb.util.MiscUtils;

public class InMemoryJavaCompiler {

	private final Map<ClassIdentifier, byte[]> classes = new HashMap<>();
	private final Map<String, List<JavaFileObject>> packages = new HashMap<>();
	private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
	private UID currentCompilationIdentifier;
	private final JavaFileManager manager = new ForwardingJavaFileManager<JavaFileManager>(
			compiler.getStandardFileManager(null, null, null)) {
		@Override
		public JavaFileObject getJavaFileForOutput(Location loc, String className, Kind kind, FileObject obj) {
			if (currentCompilationIdentifier == null) {
				throw new AssertionError();
			}
			return outputFile(new ClassIdentifier(currentCompilationIdentifier, className), kind);
		}

		@Override
		public Iterable<JavaFileObject> list(Location loc, String pkg, Set<Kind> kinds, boolean rec)
				throws IOException {
			List<JavaFileObject> result = new ArrayList<JavaFileObject>();
			Iterable<JavaFileObject> files;
			files = packages.get(pkg);
			if (files != null)
				for (JavaFileObject file : files)
					result.add(file);
			files = super.list(loc, pkg, kinds, rec);
			if (files != null)
				for (JavaFileObject file : files)
					result.add(file);
			return result;
		}

		@Override
		public String inferBinaryName(Location loc, JavaFileObject file) {
			if (file instanceof NamedJavaFileObject)
				return ((NamedJavaFileObject) file).getClassIdentifier().getClassName();
			else
				return super.inferBinaryName(loc, file);
		}
	};
	private Iterable<String> options;
	private final CompositeClassLoader compositeClassLoader = new CompositeClassLoader();
	private final Object compilationMutex = new Object();
	private final Object classStoreMutex = new Object();
	private final Object packagesMutex = new Object();

	public ClassLoader getClassLoader() {
		return compositeClassLoader;
	}

	public Iterable<String> getOptions() {
		return options;
	}

	public void setOptions(Iterable<String> options) {
		this.options = options;
	}

	public List<Class<?>> compile(File sourceDirectory) throws CompilationError {
		UID compilationIdentifier = new UID();
		List<JavaFileObject> files = collectSourceFiles(compilationIdentifier, sourceDirectory, null);
		compile(compilationIdentifier, files.toArray(new JavaFileObject[files.size()]));
		return files.stream().map(f -> new MemoryClassLoader(((NamedJavaFileObject) f).getClassIdentifier()))
				.collect(Collectors.toList()).stream().map(l -> {
					try {
						return l.loadClass(l.getMainClassIdentifier().className);
					} catch (ClassNotFoundException e) {
						throw new AssertionError(e);
					}
				}).collect(Collectors.toList());
	}

	public Class<?> compile(String className, String source) throws CompilationError {
		ClassIdentifier classIdentifier = new ClassIdentifier(new UID(), className);
		compile(classIdentifier.getCompilationIdentifier(), sourceFile(classIdentifier, source));
		try {
			return new MemoryClassLoader(classIdentifier).loadClass(className);
		} catch (ClassNotFoundException e) {
			throw new AssertionError(e);
		}
	}

	private void compile(UID compilationIdentifier, JavaFileObject... files) throws CompilationError {
		compile(compilationIdentifier, Arrays.asList(files));
	}

	private void compile(UID compilationIdentifier, List<JavaFileObject> files) throws CompilationError {
		if (files.isEmpty())
			throw new RuntimeException("No input files");
		DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
		CompilationTask task = compiler.getTask(null, manager, collector, options, null, files);
		boolean success;
		synchronized (compilationMutex) {
			currentCompilationIdentifier = compilationIdentifier;
			try {
				success = task.call();
			} finally {
				currentCompilationIdentifier = null;
			}
		}
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

	private List<JavaFileObject> collectSourceFiles(UID compilationIdentifier, File sourceDirectory,
			String currentPackageName) {
		List<JavaFileObject> result = new ArrayList<JavaFileObject>();
		for (File fileOrDirectory : sourceDirectory.listFiles()) {
			if (fileOrDirectory.isFile()) {
				if (fileOrDirectory.getName().endsWith(".java")) {
					String className = fileOrDirectory.getName().substring(0,
							fileOrDirectory.getName().length() - ".java".length());
					if (currentPackageName != null) {
						className = currentPackageName + "." + className;
					}
					String source;
					try (FileInputStream in = new FileInputStream(fileOrDirectory)) {
						source = MiscUtils.read(in);
					} catch (Exception e) {
						throw new AssertionError(e);
					}
					result.add(sourceFile(new ClassIdentifier(compilationIdentifier, className), source));
				}
			} else if (fileOrDirectory.isDirectory()) {
				String subPackageName = ((currentPackageName != null) ? (currentPackageName + ".") : "")
						+ fileOrDirectory.getName();
				result.addAll(collectSourceFiles(compilationIdentifier, fileOrDirectory, subPackageName));
			} else {
				throw new AssertionError();
			}
		}
		return result;
	}

	private static JavaFileObject sourceFile(ClassIdentifier classIdentifier, String source) {
		return inputFile(classIdentifier, Kind.SOURCE, source);
	}

	private static JavaFileObject inputFile(ClassIdentifier classIdentifier, Kind kind, String content) {
		return new NamedJavaFileObject(classIdentifier, kind) {
			@Override
			public CharSequence getCharContent(boolean b) {
				return content;
			}
		};
	}

	private static NamedJavaFileObject inputFile(ClassIdentifier classIdentifier, Kind kind, byte[] content) {
		return new NamedJavaFileObject(classIdentifier, kind) {
			@Override
			public InputStream openInputStream() {
				return new ByteArrayInputStream(content);
			}
		};
	}

	private JavaFileObject outputFile(ClassIdentifier classIdentifier, Kind kind) {
		return new NamedJavaFileObject(classIdentifier, kind) {
			@Override
			public OutputStream openOutputStream() {
				return outputStream(getClassIdentifier());
			}
		};
	}

	private OutputStream outputStream(ClassIdentifier classIdentifier) {
		return new ByteArrayOutputStream() {
			@Override
			public void close() {
				storeClass(classIdentifier, toByteArray());
			}
		};
	}

	private void storeClass(ClassIdentifier classIdentifier, byte[] bytes) {
		synchronized (classStoreMutex) {
			if (classes.containsKey(classIdentifier)) {
				throw new AssertionError();
			}
			classes.put(classIdentifier, bytes);
			NamedJavaFileObject file = inputFile(classIdentifier, Kind.CLASS, bytes);
			int dot = classIdentifier.getClassName().lastIndexOf('.');
			String pkg = dot == -1 ? "" : classIdentifier.getClassName().substring(0, dot);
			packages.computeIfAbsent(pkg, k -> new ArrayList<>()).add(0, file);
		}
	}

	private void unstoreClass(ClassIdentifier classIdentifier) {
		synchronized (classStoreMutex) {
			if (!classes.containsKey(classIdentifier)) {
				throw new AssertionError();
			}
			classes.remove(classIdentifier);
			int dot = classIdentifier.getClassName().lastIndexOf('.');
			String pkg = dot == -1 ? "" : classIdentifier.getClassName().substring(0, dot);
			packages.get(pkg)
					.removeIf(file -> ((NamedJavaFileObject) file).getClassIdentifier().equals(classIdentifier));
			if (packages.get(pkg).size() == 0) {
				packages.remove(pkg);
			}
		}
	}

	private static class ClassIdentifier {
		private final UID compilationIdentifier;
		private final String className;

		public ClassIdentifier(UID compilationIdentifier, String className) {
			this.compilationIdentifier = compilationIdentifier;
			this.className = className;
		}

		public UID getCompilationIdentifier() {
			return compilationIdentifier;
		}

		public String getClassName() {
			return className;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((className == null) ? 0 : className.hashCode());
			result = prime * result + ((compilationIdentifier == null) ? 0 : compilationIdentifier.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ClassIdentifier other = (ClassIdentifier) obj;
			if (className == null) {
				if (other.className != null)
					return false;
			} else if (!className.equals(other.className))
				return false;
			if (compilationIdentifier == null) {
				if (other.compilationIdentifier != null)
					return false;
			} else if (!compilationIdentifier.equals(other.compilationIdentifier))
				return false;
			return true;
		}

	}

	private static class NamedJavaFileObject extends SimpleJavaFileObject {
		private final ClassIdentifier classIdentifier;

		protected NamedJavaFileObject(ClassIdentifier classIdentifier, Kind kind) {
			super(URI.create(classIdentifier.getClassName().replace('.', '/') + kind.extension), kind);
			this.classIdentifier = classIdentifier;
		}

		public ClassIdentifier getClassIdentifier() {
			return classIdentifier;
		}
	}

	private class MemoryClassLoader extends ClassLoader {
		private final List<String> definedClassNames = new ArrayList<String>();
		private final ClassIdentifier mainClassIdentifier;

		public MemoryClassLoader(ClassIdentifier classIdentifier) {
			this.mainClassIdentifier = classIdentifier;
			compositeClassLoader.add(this);
		}

		public ClassIdentifier getMainClassIdentifier() {
			return mainClassIdentifier;
		}

		@Override
		protected Package getPackage(String packageName) {
			synchronized (InMemoryJavaCompiler.this.packagesMutex) {
				if (shouldLoadThePackageFromMemory(packageName)) {
					MemoryPackageLoader responsiblePackageLoader = (MemoryPackageLoader) compositeClassLoader
							.getClassLoaders().stream()
							.filter(l -> (l instanceof MemoryPackageLoader)
									&& ((MemoryPackageLoader) l).isResponsibleFor(packageName))
							.findFirst().orElse(null);
					if (responsiblePackageLoader == null) {
						responsiblePackageLoader = new MemoryPackageLoader(packageName);
					}
					return responsiblePackageLoader.getPackage(packageName);
				}
			}
			return super.getPackage(packageName);
		}

		private boolean shouldLoadThePackageFromMemory(String packageName) {
			return compositeClassLoader
					.getClassLoaders().stream().filter(
							l -> (l instanceof MemoryClassLoader) && !(l instanceof MemoryPackageLoader)
									&& packageName.equals(MiscUtils.extractPackageNameFromClassName(
											((MemoryClassLoader) l).getMainClassIdentifier().getClassName())))
					.count() > 0;
		}

		@Override
		protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
			synchronized (getClassLoadingLock(className)) {
				if (isResponsibleFor(className)) {
					// First, check if the class has already been loaded
					Class<?> c = findLoadedClass(className);
					if (c == null) {
						c = findClass(className);
					}
					if (resolve) {
						resolveClass(c);
					}
					return c;
				} else {
					ClassLoader responsibleClassLoader = compositeClassLoader.getClassLoaders().stream()
							.filter(l -> (l instanceof MemoryClassLoader)
									&& ((MemoryClassLoader) l).isResponsibleFor(className))
							.findFirst().orElse(null);
					Class<?> c;
					if (responsibleClassLoader != null) {
						c = responsibleClassLoader.loadClass(className);
					} else {
						c = getParent().loadClass(className);
					}
					if (resolve) {
						resolveClass(c);
					}
					return c;
				}
			}
		}

		protected boolean isResponsibleFor(String className) {
			return className.equals(mainClassIdentifier.getClassName())
					|| className.startsWith(mainClassIdentifier.getClassName() + "$");
		}

		@Override
		protected Class<?> findClass(String className) throws ClassNotFoundException {
			byte[] bytes = classes.get(new ClassIdentifier(mainClassIdentifier.getCompilationIdentifier(), className));
			if (bytes == null)
				throw new ClassNotFoundException(className);
			Class<?> result = super.defineClass(className, bytes, 0, bytes.length);
			definedClassNames.add(className);
			return result;
		}

		@Override
		protected void finalize() throws Throwable {
			for (String className : definedClassNames) {
				unstoreClass(new ClassIdentifier(mainClassIdentifier.getCompilationIdentifier(), className));
			}
			super.finalize();
		}

	}

	private class MemoryPackageLoader extends MemoryClassLoader {

		private final String mainPackageName;
		private Package thePackage;
		private final Object packageCreationMutex = new Object();
		private boolean definingThePackage = false;

		public MemoryPackageLoader(String packageName) {
			super(null);
			this.mainPackageName = packageName;
		}

		@Override
		protected boolean isResponsibleFor(String packageName) {
			return false;
		}

		@Override
		protected Package getPackage(String packageName) {
			if (this.mainPackageName.equals(packageName)) {
				if (definingThePackage) {
					return null;
				}
				synchronized (packageCreationMutex) {
					if (thePackage == null) {
						definingThePackage = true;
						try {
							thePackage = definePackage(packageName, null, null, null, null, null, null, null);
						} finally {
							definingThePackage = false;
						}
					}
					return thePackage;
				}
			}
			return super.getPackage(packageName);
		}
	}

}