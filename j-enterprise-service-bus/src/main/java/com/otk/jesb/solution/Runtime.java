package com.otk.jesb.solution;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import com.otk.jesb.IPluginInfo;
import com.otk.jesb.JESB;
import com.otk.jesb.PotentialError;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.compiler.InMemoryCompiler;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.util.ClassUtils;

/**
 * Encapsulates dynamic resources required to execute a {@link Solution}
 * instance.
 * 
 * @author olitank
 *
 */
public class Runtime {

	public static final Attributes.Name PLUGIN_INFO_CLASS_MANIFEST_KEY = new Attributes.Name("Plugin-Info-Class");
	public static final String STATIC_PLUGIN_INFO_CLASS_NAMES_PROPERTY_KEY = Runtime.class.getName()
			+ ".pluginInfoClassNames";
	private static final String DEFAULT_APPLICATION_PLUGINS_DIRECTORY_PATH = "plugins";
	private static final String APPLICATION_PLUGINS_DIRECTORY_PATH_PROPERTY_KEY = Runtime.class.getName()
			+ ".pluginsDirectory";

	private final InMemoryCompiler inMemoryCompiler = new InMemoryCompiler();

	private final List<IPluginInfo> pluginInfos = new ArrayList<IPluginInfo>();

	public Runtime() {
		configureDependencies(Collections.emptyList());
	}

	public InMemoryCompiler getInMemoryCompiler() {
		return inMemoryCompiler;
	}

	public List<IPluginInfo> getPluginInfos() {
		return pluginInfos;
	}

	public void configureDependencies(List<JAR> solutionJARs) {
		pluginInfos.clear();
		URLClassLoader jarsClassLoader = buildAllJARsClassLoader(solutionJARs);
		inMemoryCompiler.setFirstClassLoader(jarsClassLoader);
		pluginInfos.addAll(getDeclaredPlugins());
		pluginInfos.addAll(discoverJARPlugins(jarsClassLoader.getURLs()));
	}

	private List<IPluginInfo> discoverJARPlugins(URL[] jarFileURLs) {
		List<IPluginInfo> result = new ArrayList<IPluginInfo>();
		for (URL jarFileURL : jarFileURLs) {
			JarURLConnection connection;
			try {
				connection = (JarURLConnection) new URL("jar:" + jarFileURL.toString() + "!/").openConnection();
			} catch (IOException e) {
				throw new UnexpectedError(e);
			}
			Manifest manifest;
			try {
				manifest = connection.getManifest();
			} catch (IOException e) {
				continue;
			}
			Attributes attributes = manifest.getMainAttributes();
			String pluginInfoClassName = attributes.getValue(PLUGIN_INFO_CLASS_MANIFEST_KEY);
			if (pluginInfoClassName != null) {
				try {
					result.add((IPluginInfo) getJESBClass(pluginInfoClassName).newInstance());
				} catch (InstantiationException | IllegalAccessException e) {
					throw new UnexpectedError(e);
				}
			}
		}
		return result;
	}

	private List<IPluginInfo> getDeclaredPlugins() {
		List<IPluginInfo> result = new ArrayList<IPluginInfo>();
		for (String pluginInfoClassName : System.getProperty(STATIC_PLUGIN_INFO_CLASS_NAMES_PROPERTY_KEY, "")
				.split(",")) {
			pluginInfoClassName = pluginInfoClassName.trim();
			if (pluginInfoClassName.isEmpty()) {
				continue;
			}
			try {
				result.add((IPluginInfo) getJESBClass(pluginInfoClassName).newInstance());
			} catch (InstantiationException | IllegalAccessException e) {
				throw new UnexpectedError(e);
			}
		}
		return result;
	}

	private URLClassLoader buildAllJARsClassLoader(List<JAR> solutionJARs) {
		List<URL> allJarFileURLs = new ArrayList<URL>();
		File applicationPluginsDirectory = new File(System.getProperty(APPLICATION_PLUGINS_DIRECTORY_PATH_PROPERTY_KEY,
				DEFAULT_APPLICATION_PLUGINS_DIRECTORY_PATH));
		if (applicationPluginsDirectory.isDirectory()) {
			Arrays.stream(applicationPluginsDirectory.listFiles(file -> file.getName().endsWith(".jar")))
					.forEach(file -> {
						try {
							allJarFileURLs.add(file.toURI().toURL());
						} catch (MalformedURLException e) {
							throw new UnexpectedError(e);
						}
					});
		}
		solutionJARs.stream().forEach(jar -> {
			allJarFileURLs.add(jar.getURL());
		});
		return new URLClassLoader(allJarFileURLs.toArray(new URL[allJarFileURLs.size()]), JESB.class.getClassLoader());
	}

	public ClassLoader getJESBResourceLoader() {
		return inMemoryCompiler.getCompiledClassesLoader();
	}

	public Class<?> getJESBClass(String typeName) throws PotentialError {
		String arrayComponentTypeName = MiscUtils.getArrayComponentTypeName(typeName);
		if (arrayComponentTypeName != null) {
			return MiscUtils.getArrayType(getJESBClass(arrayComponentTypeName));
		}
		if (ClassUtils.PRIMITIVE_CLASS_BY_NAME.containsKey(typeName)) {
			return ClassUtils.PRIMITIVE_CLASS_BY_NAME.get(typeName);
		}
		try {
			return inMemoryCompiler.loadClassThroughCache(typeName);
		} catch (ClassNotFoundException e1) {
			throw new PotentialError(e1);
		}
	}

	public Class<?> getJESBClassFromCanonicalName(String canonicalName) {
		try {
			return getJESBClass(canonicalName);
		} catch (PotentialError e1) {
			try {
				int lastDotIndex = canonicalName.lastIndexOf('.');
				if (lastDotIndex == -1) {
					throw new PotentialError(new UnexpectedError());
				}
				return getJESBClassFromCanonicalName(
						canonicalName.substring(0, lastDotIndex) + "$" + canonicalName.substring(lastDotIndex + 1));
			} catch (PotentialError e2) {
				throw new PotentialError(new ClassNotFoundException("Canonical name: " + canonicalName));
			}
		}
	}

}