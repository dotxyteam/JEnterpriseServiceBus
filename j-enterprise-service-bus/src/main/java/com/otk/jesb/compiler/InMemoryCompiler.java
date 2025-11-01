package com.otk.jesb.compiler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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

import org.apache.commons.io.input.ReaderInputStream;

import com.otk.jesb.Log;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.meta.CompositeClassLoader;
import com.otk.jesb.meta.DelegatingClassLoader;
import com.otk.jesb.util.MiscUtils;

public class InMemoryCompiler {

	private static final Class<?> CACHED_CLASS_NOT_FOUND = (new Object() {
		@Override
		public String toString() {
			return "CACHED_CLASS_NOT_FOUND";
		}
	}).getClass();

	private final Map<ClassIdentifier, byte[]> classBinaries = new HashMap<>();
	private final Map<String, List<JavaFileObject>> packagingMap = new HashMap<>();
	private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
	private UID currentCompilationIdentifier;
	private Iterable<String> options = Arrays.asList("-parameters");
	private final DelegatingClassLoader firstClassLoaderDelegator = new DelegatingClassLoader(
			new URLClassLoader(new URL[0]));
	private final CompiledClassesLoader compiledClassesLoader = new CompiledClassesLoader();
	private final Map<String, Class<?>> classCache = new HashMap<String, Class<?>>();
	private final Object compilationMutex = new Object();
	private final Object classDataMutex = new Object();
	private final Object classCacheMutex = new Object();

	public byte[] getClassBinary(Class<?> clazz) {
		if (!(clazz.getClassLoader() instanceof MemoryClassLoader)) {
			throw new IllegalArgumentException();
		}
		ClassIdentifier classIdentifier = new ClassIdentifier(
				((MemoryClassLoader) clazz.getClassLoader()).getMainClassIdentifier().getCompilationIdentifier(),
				clazz.getName());
		synchronized (classDataMutex) {
			return classBinaries.get(classIdentifier);
		}
	}

	public ClassLoader getCompiledClassesLoader() {
		return compiledClassesLoader;
	}

	public URLClassLoader getFirstClassLoader() {
		return (URLClassLoader) firstClassLoaderDelegator.getDelegate();
	}

	public void setFirstClassLoader(URLClassLoader classLoader) {
		firstClassLoaderDelegator.setDelegate(classLoader);
		synchronized (classCacheMutex) {
			classCache.clear();
		}
	}

	public Iterable<String> getOptions() {
		return options;
	}

	public void setOptions(Iterable<String> options) {
		this.options = options;
	}

	public Class<?> loadClassThroughCache(String className) throws ClassNotFoundException {
		synchronized (classCacheMutex) {
			Class<?> c = classCache.get(className);
			if (c == null) {
				try {
					c = compiledClassesLoader.loadClass(className);
				} catch (ClassNotFoundException e) {
					c = CACHED_CLASS_NOT_FOUND;
				}
				classCache.put(className, c);
			}
			if (c == CACHED_CLASS_NOT_FOUND) {
				throw new ClassNotFoundException(className);
			}
			return c;
		}
	}

	public List<Class<?>> compile(File sourceDirectory) throws CompilationError {
		synchronized (compilationMutex) {
			UID compilationIdentifier = new UID();
			List<JavaFileObject> files = collectSourceFiles(compilationIdentifier, sourceDirectory, null);
			return compile(compilationIdentifier, files.toArray(new JavaFileObject[files.size()]));
		}
	}

	public Class<?> compile(String className, String source) throws CompilationError {
		synchronized (compilationMutex) {
			ClassIdentifier classIdentifier = new ClassIdentifier(new UID(), className);
			return compile(classIdentifier.getCompilationIdentifier(), sourceFile(classIdentifier, source, null))
					.get(0);
		}
	}

	private List<Class<?>> compile(UID compilationIdentifier, JavaFileObject... files) throws CompilationError {
		return compile(compilationIdentifier, Arrays.asList(files));
	}

	private List<Class<?>> compile(UID compilationIdentifier, List<JavaFileObject> files) throws CompilationError {
		if (Log.isVerbose()) {
			Log.get()
					.info("Compiling " + files.stream()
							.map(fileObject -> ((NamedJavaFileObject) fileObject).getClassIdentifier().getClassName())
							.collect(Collectors.toList()) + "...");
		}
		if (files.isEmpty())
			throw new CompilationError(-1, -1, "No input files", null, null);
		synchronized (classCacheMutex) {
			classCache.entrySet().removeIf(entry -> {
				String className = entry.getKey();
				return files.stream()
						.map(fileObject -> ((NamedJavaFileObject) fileObject).getClassIdentifier().getClassName())
						.anyMatch(newClassName -> className.startsWith(newClassName));
			});
		}
		try (JavaFileManager manager = createJavaFileManager()) {
			DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
			boolean success;
			List<String> finalOptions = new ArrayList<String>();
			{
				if (options != null) {
					options.forEach(finalOptions::add);
				}
				String classpath = System.getProperty("java.class.path");
				if (classpath == null) {
					throw new UnexpectedError();
				}
				URLClassLoader firstClassLoader = getFirstClassLoader();
				if (firstClassLoader != null) {
					String parentClasspapth = Arrays.stream(firstClassLoader.getURLs()).map(url -> {
						try {
							return new File(url.toURI()).getAbsolutePath();
						} catch (URISyntaxException e) {
							throw new UnexpectedError(e);
						}
					}).collect(Collectors.joining(File.pathSeparator));
					classpath += File.pathSeparator + parentClasspapth;
				}
				finalOptions.addAll(Arrays.asList("-classpath", classpath));
			}
			CompilationTask task = compiler.getTask(null, manager, collector, finalOptions, null, files);
			currentCompilationIdentifier = compilationIdentifier;
			try {
				success = task.call();
			} finally {
				currentCompilationIdentifier = null;
			}
			check(success, collector);
		} catch (IOException e) {
			throw new UnexpectedError(e);
		}
		List<Class<?>> result = files.stream()
				.map(f -> new MemoryClassLoader(((NamedJavaFileObject) f).getClassIdentifier()))
				.collect(Collectors.toList()).stream().map(classLoader -> {
					try {
						return classLoader.loadClass(classLoader.getMainClassIdentifier().className);
					} catch (ClassNotFoundException e) {
						throw new UnexpectedError(e);
					}
				}).collect(Collectors.toList());
		synchronized (classCacheMutex) {
			result.stream().forEach(clazz -> classCache.put(clazz.getName(), clazz));
		}
		return result;
	}

	private void check(boolean success, DiagnosticCollector<?> collector) throws CompilationError {
		for (Diagnostic<?> diagnostic : collector.getDiagnostics()) {
			if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
				throw new CompilationError((int) diagnostic.getStartPosition(), (int) diagnostic.getEndPosition(),
						diagnostic.getMessage(null), extractSourceFilePath(diagnostic), extractSourceCode(diagnostic));
			}
		}
		if (!success) {
			throw new CompilationError(-1, -1, "Unknown error", null, null);
		}
	}

	private JavaFileManager createJavaFileManager() {
		return new ForwardingJavaFileManager<JavaFileManager>(
				compiler.getStandardFileManager(null, null, Charset.defaultCharset())) {
			@Override
			public JavaFileObject getJavaFileForOutput(Location loc, String className, Kind kind, FileObject obj) {
				if (currentCompilationIdentifier == null) {
					throw new UnexpectedError();
				}
				return outputFile(new ClassIdentifier(currentCompilationIdentifier, className), kind, null);
			}

			@Override
			public Iterable<JavaFileObject> list(Location loc, String pkg, Set<Kind> kinds, boolean rec)
					throws IOException {
				List<JavaFileObject> result = new ArrayList<JavaFileObject>();
				Iterable<JavaFileObject> files;
				synchronized (classDataMutex) {
					files = packagingMap.get(pkg);
				}
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
	}

	private String extractSourceFilePath(Diagnostic<?> diagnostic) {
		if (!(diagnostic.getSource() instanceof SimpleJavaFileObject)) {
			return null;
		}
		SimpleJavaFileObject javaFileObject = (SimpleJavaFileObject) diagnostic.getSource();
		if (javaFileObject.getKind() != Kind.SOURCE) {
			return null;
		}
		return javaFileObject.getName();
	}

	private String extractSourceCode(Diagnostic<?> diagnostic) {
		if (!(diagnostic.getSource() instanceof SimpleJavaFileObject)) {
			return null;
		}
		SimpleJavaFileObject javaFileObject = (SimpleJavaFileObject) diagnostic.getSource();
		if (javaFileObject.getKind() != Kind.SOURCE) {
			return null;
		}
		try (Reader reader = javaFileObject.openReader(true)) {
			return MiscUtils.read(new ReaderInputStream(reader, Charset.defaultCharset()));
		} catch (IOException e) {
			return null;
		}
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
					} catch (IOException e) {
						throw new UnexpectedError(e);
					}
					result.add(sourceFile(new ClassIdentifier(compilationIdentifier, className), source,
							fileOrDirectory.toURI()));
				}
			} else if (fileOrDirectory.isDirectory()) {
				String subPackageName = ((currentPackageName != null) ? (currentPackageName + ".") : "")
						+ fileOrDirectory.getName();
				result.addAll(collectSourceFiles(compilationIdentifier, fileOrDirectory, subPackageName));
			} else {
				throw new UnexpectedError();
			}
		}
		return result;
	}

	private static JavaFileObject sourceFile(ClassIdentifier classIdentifier, String source, URI uri) {
		return inputFile(classIdentifier, Kind.SOURCE, source, uri);
	}

	private static JavaFileObject inputFile(ClassIdentifier classIdentifier, Kind kind, String content, URI uri) {
		return new NamedJavaFileObject(classIdentifier, kind, uri) {
			@Override
			public CharSequence getCharContent(boolean b) {
				return content;
			}
		};
	}

	private static NamedJavaFileObject inputFile(ClassIdentifier classIdentifier, Kind kind, byte[] content, URI uri) {
		return new NamedJavaFileObject(classIdentifier, kind, uri) {
			@Override
			public InputStream openInputStream() {
				return new ByteArrayInputStream(content);
			}
		};
	}

	private JavaFileObject outputFile(ClassIdentifier classIdentifier, Kind kind, URI uri) {
		return new NamedJavaFileObject(classIdentifier, kind, uri) {
			@Override
			public OutputStream openOutputStream() {
				return outputStream(getClassIdentifier());
			}
		};
	}

	private OutputStream outputStream(ClassIdentifier classIdentifier) {
		return new ByteArrayOutputStream() {
			boolean closed = false;

			@Override
			public void close() {
				if (closed) {
					return;
				}
				storeClass(classIdentifier, toByteArray());
				closed = true;
			}
		};
	}

	private void storeClass(ClassIdentifier classIdentifier, byte[] bytes) {
		synchronized (classDataMutex) {
			if (classBinaries.containsKey(classIdentifier)) {
				throw new UnexpectedError();
			}
			classBinaries.put(classIdentifier, bytes);
			NamedJavaFileObject file = inputFile(classIdentifier, Kind.CLASS, bytes, null);
			int dot = classIdentifier.getClassName().lastIndexOf('.');
			String pkg = dot == -1 ? "" : classIdentifier.getClassName().substring(0, dot);
			packagingMap.computeIfAbsent(pkg, k -> new ArrayList<>()).add(0, file);
		}
	}

	private void unstoreClass(ClassIdentifier classIdentifier) {
		synchronized (classDataMutex) {
			if (!classBinaries.containsKey(classIdentifier)) {
				throw new UnexpectedError();
			}
			classBinaries.remove(classIdentifier);
			int dot = classIdentifier.getClassName().lastIndexOf('.');
			String pkg = dot == -1 ? "" : classIdentifier.getClassName().substring(0, dot);
			packagingMap.get(pkg)
					.removeIf(file -> ((NamedJavaFileObject) file).getClassIdentifier().equals(classIdentifier));
			if (packagingMap.get(pkg).size() == 0) {
				packagingMap.remove(pkg);
			}
		}
	}

	private boolean shouldLoadThePackageFromMemory(String packageName) {
		return compiledClassesLoader.getClassLoaders().stream()
				.filter(l -> (l instanceof MemoryClassLoader) && !(l instanceof MemoryPackageLoader)
						&& packageName.equals(MiscUtils.extractPackageNameFromClassName(
								((MemoryClassLoader) l).getMainClassIdentifier().getClassName())))
				.count() > 0;
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

		protected NamedJavaFileObject(ClassIdentifier classIdentifier, Kind kind, URI uri) {
			super((uri != null) ? uri : URI.create(classIdentifier.getClassName().replace('.', '/') + kind.extension),
					kind);
			this.classIdentifier = classIdentifier;
		}

		public ClassIdentifier getClassIdentifier() {
			return classIdentifier;
		}
	}

	private class CompiledClassesLoader extends CompositeClassLoader {
		public CompiledClassesLoader() {
			setFirstClassLoader(firstClassLoaderDelegator);
		}

		@SuppressWarnings("rawtypes")
		@Override
		protected Function<ClassLoader, Class> getClassLoadingFunction(String className, boolean resolve) {
			Function<ClassLoader, Class> baseFunction = super.getClassLoadingFunction(className, resolve);
			return classLoader -> {
				if (classLoader instanceof MemoryClassLoader) {
					if (!((MemoryClassLoader) classLoader).isResponsibleForClass(className)) {
						return null;
					}
				}
				return baseFunction.apply(classLoader);
			};
		}

		@Override
		protected Function<ClassLoader, URL> getResourceLoadingFunction(String name) {
			Function<ClassLoader, URL> baseFunction = super.getResourceLoadingFunction(name);
			return classLoader -> {
				if (classLoader instanceof MemoryClassLoader) {
					return null;
				}
				return baseFunction.apply(classLoader);
			};
		}

	}

	private class MemoryClassLoader extends ClassLoader {
		private final List<String> definedClassNames = new ArrayList<String>();
		private final ClassIdentifier mainClassIdentifier;

		public MemoryClassLoader(ClassIdentifier classIdentifier) {
			super(compiledClassesLoader);
			this.mainClassIdentifier = classIdentifier;
			compiledClassesLoader.add(this);
		}

		public ClassIdentifier getMainClassIdentifier() {
			return mainClassIdentifier;
		}

		@Override
		protected Package getPackage(String packageName) {
			if (shouldLoadThePackageFromMemory(packageName)) {
				MemoryPackageLoader responsiblePackageLoader = (MemoryPackageLoader) compiledClassesLoader
						.getClassLoaders().stream()
						.filter(l -> (l instanceof MemoryPackageLoader)
								&& ((MemoryPackageLoader) l).isResponsibleForPackage(packageName))
						.findFirst().orElse(null);
				if (responsiblePackageLoader == null) {
					responsiblePackageLoader = new MemoryPackageLoader(packageName);
				}
				return responsiblePackageLoader.getPackage(packageName);
			}
			return super.getPackage(packageName);
		}

		@Override
		protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
			synchronized (getClassLoadingLock(className)) {
				if (isResponsibleForClass(className)) {
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
					Class<?> c = compiledClassesLoader.loadClass(className);
					if (resolve) {
						resolveClass(c);
					}
					return c;
				}
			}
		}

		protected boolean isResponsibleForClass(String className) {
			return className.equals(mainClassIdentifier.getClassName())
					|| className.startsWith(mainClassIdentifier.getClassName() + "$");
		}

		@Override
		protected Class<?> findClass(String className) throws ClassNotFoundException {
			byte[] bytes;
			synchronized (classDataMutex) {
				bytes = classBinaries
						.get(new ClassIdentifier(mainClassIdentifier.getCompilationIdentifier(), className));
			}
			if (bytes == null)
				throw new ClassNotFoundException(className);
			try {
				if (firstClassLoaderDelegator.loadClass(className) != null) {
					throw new UnexpectedError(
							"Cannot define a class that is already defined by the system class loader: " + className);
				}
			} catch (ClassNotFoundException e) {
			}
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

		private final String thePackageName;
		private Package thePackage;
		private final Object packageCreationMutex = new Object();
		private boolean definingThePackage = false;

		public MemoryPackageLoader(String thePackageName) {
			super(null);
			this.thePackageName = thePackageName;
		}

		public boolean isResponsibleForPackage(String packageName) {
			return thePackageName.equals(packageName);
		}

		@Override
		protected boolean isResponsibleForClass(String className) {
			return false;
		}

		@Override
		protected Package getPackage(String packageName) {
			if (thePackageName.equals(packageName)) {
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