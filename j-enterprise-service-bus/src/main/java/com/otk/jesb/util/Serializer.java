package com.otk.jesb.util;

import java.beans.PropertyDescriptor;
import java.beans.Transient;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import com.otk.jesb.UnexpectedError;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.converters.javabean.BeanProvider;
import com.thoughtworks.xstream.converters.javabean.JavaBeanConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import com.thoughtworks.xstream.security.AnyTypePermission;

import xy.reflect.ui.control.plugin.AbstractSimpleCustomizableFieldControlPlugin;

/**
 * XML-based persistence manager.
 * 
 * @author olitank
 *
 */
public class Serializer {

	protected static final String SERIALIZATION_CHARSET_NAME = "UTF-8";

	protected final XStream xstream;

	public Serializer() {
		this.xstream = createXstream();
	}

	protected XStream createXstream() {
		XStream xstream = new XStream() {
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
					public boolean shouldSerializeMember(@SuppressWarnings("rawtypes") Class definedIn,
							String fieldName) {
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
		return xstream;
	}

	public void write(Object object, OutputStream output) throws IOException {
		write(new OutputStreamWriter(output, SERIALIZATION_CHARSET_NAME));
	}

	public void write(Object object, Writer writer) throws IOException {
		try {
			xstream.toXML(object, writer);
		} catch (XStreamException e) {
			throw new IOException(e);
		}
	}

	public String write(Object object) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			write(object, output);
		} catch (IOException e) {
			throw new UnexpectedError(e);
		}
		try {
			return output.toString(SERIALIZATION_CHARSET_NAME);
		} catch (UnsupportedEncodingException e) {
			throw new UnexpectedError(e);
		}
	}

	public Object read(InputStream input) throws IOException {
		return read(new InputStreamReader(input, SERIALIZATION_CHARSET_NAME));
	}

	public Object read(Reader reader) throws IOException {
		try {
			return xstream.fromXML(reader);
		} catch (XStreamException e) {
			throw new IOException(e);
		}
	}

	public Object read(String inputString) {
		ByteArrayInputStream input;
		try {
			input = new ByteArrayInputStream(inputString.getBytes(SERIALIZATION_CHARSET_NAME));
		} catch (UnsupportedEncodingException e) {
			throw new UnexpectedError(e);
		}
		try {
			return read(input);
		} catch (IOException e) {
			throw new UnexpectedError(e);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T copy(T object) {
		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			write(object, output);
			ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
			return (T) read(input);
		} catch (IOException e) {
			throw new UnexpectedError(e);
		}
	}

}
