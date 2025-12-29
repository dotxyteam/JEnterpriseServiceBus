package com.otk.jesb.solution;

import java.beans.PropertyDescriptor;
import java.beans.Transient;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
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
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.javabean.BeanProvider;
import com.thoughtworks.xstream.converters.javabean.JavaBeanConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import com.thoughtworks.xstream.security.AnyTypePermission;

import xy.reflect.ui.control.plugin.AbstractSimpleCustomizableFieldControlPlugin;
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
	public static final String PLUGIN_INFO_CLASS_NAMES_PROPERTY_KEY = Runtime.class.getName() + ".pluginInfoClassNames";

	private final InMemoryCompiler inMemoryCompiler = new InMemoryCompiler();

	private final List<IPluginInfo> pluginInfos = new ArrayList<IPluginInfo>();

	private final XStream xstream = new XStream() {
		@Override
		protected MapperWrapper wrapMapper(MapperWrapper next) {
			return new MapperWrapper(next) {
				@Override
				public String serializedClass(@SuppressWarnings("rawtypes") Class type) {
					if ((type != null) && type.isAnonymousClass()) {
						throw new UnexpectedError("Cannot serialize instance of class " + type
								+ ": Anonymous class instance serialization is forbidden");
					}
					return super.serializedClass(type);
				}

				@Override
				public boolean shouldSerializeMember(@SuppressWarnings("rawtypes") Class definedIn, String fieldName) {
					if (Throwable.class.isAssignableFrom(definedIn)) {
						if (fieldName.equals("stackTrace")) {
							return false;
						}
						if (fieldName.equals("suppressedExceptions")) {
							return false;
						}
					}
					return super.shouldSerializeMember(definedIn, fieldName);
				}

			};
		}
	};

	public Runtime() {
		configureXstream();
		configureSolutionDependencies(Collections.emptyList());
	}

	public InMemoryCompiler getInMemoryCompiler() {
		return inMemoryCompiler;
	}

	public List<IPluginInfo> getPluginInfos() {
		return pluginInfos;
	}

	public XStream getXstream() {
		return xstream;
	}

	private void configureXstream() {
		xstream.setClassLoader(inMemoryCompiler.getCompiledClassesLoader());
		xstream.registerConverter(new JavaBeanConverter(xstream.getMapper(), new BeanProvider() {
			@Override
			protected boolean canStreamProperty(PropertyDescriptor descriptor) {

				final boolean canStream = super.canStreamProperty(descriptor);
				if (!canStream) {
					return false;
				}

				final boolean readMethodIsTransient = descriptor.getReadMethod() == null
						|| descriptor.getReadMethod().getAnnotation(Transient.class) != null;
				final boolean writeMethodIsTransient = descriptor.getWriteMethod() == null
						|| descriptor.getWriteMethod().getAnnotation(Transient.class) != null;
				final boolean isTransient = readMethodIsTransient || writeMethodIsTransient;
				if (isTransient) {
					return false;
				}

				return true;
			}

			@Override
			public void writeProperty(Object object, String propertyName, Object value) {
				if (!propertyWriteable(propertyName, object.getClass())) {
					return;
				}
				super.writeProperty(object, propertyName, value);
			}
		}), XStream.PRIORITY_VERY_LOW);
		xstream.registerConverter(new ReflectionConverter(xstream.getMapper(), xstream.getReflectionProvider()) {
			@Override
			public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
				if ((type != null) && AbstractSimpleCustomizableFieldControlPlugin.AbstractConfiguration.class
						.isAssignableFrom(type)) {
					return true;
				}
				if ((type != null) && Throwable.class.isAssignableFrom(type)) {
					return true;
				}
				return false;
			}
		}, XStream.PRIORITY_VERY_HIGH);
		xstream.addPermission(AnyTypePermission.ANY);
		xstream.ignoreUnknownElements();
	}

	public void configureSolutionDependencies(List<JAR> jars) {
		pluginInfos.clear();
		for (String pluginInfoClassName : System.getProperty(PLUGIN_INFO_CLASS_NAMES_PROPERTY_KEY, "").split(",")) {
			pluginInfoClassName = pluginInfoClassName.trim();
			if (pluginInfoClassName.isEmpty()) {
				continue;
			}
			try {
				pluginInfos.add((IPluginInfo) getJESBClass(pluginInfoClassName).newInstance());
			} catch (InstantiationException | IllegalAccessException e) {
				throw new UnexpectedError(e);
			}
		}
		URLClassLoader jarsClassLoader = new URLClassLoader(
				jars.stream().map(JAR::getURL).toArray(length -> new URL[length]), JESB.class.getClassLoader());
		{
			inMemoryCompiler.setFirstClassLoader(jarsClassLoader);
			for (JAR jar : jars) {
				JarURLConnection connection;
				try {
					connection = (JarURLConnection) new URL("jar:" + jar.getURL().toString() + "!/").openConnection();
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
						pluginInfos.add((IPluginInfo) getJESBClass(pluginInfoClassName).newInstance());
					} catch (InstantiationException | IllegalAccessException e) {
						throw new UnexpectedError(e);
					}
				}
			}
		}
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