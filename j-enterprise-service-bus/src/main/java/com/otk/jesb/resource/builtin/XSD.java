package com.otk.jesb.resource.builtin;

import java.io.File;
import java.util.List;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import org.xml.sax.SAXParseException;

import com.otk.jesb.UnexpectedError;
import com.otk.jesb.Structure.ClassicStructure;
import com.otk.jesb.Structure.SimpleElement;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.ResourceMetadata;
import com.otk.jesb.util.InstantiationUtils;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.UpToDate.VersionAccessException;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Driver;
import com.sun.tools.xjc.XJCListener;

import xy.reflect.ui.info.ResourcePath;

public class XSD extends XMLBasedDocumentResource {

	public XSD() {
		this(XSD.class.getSimpleName() + MiscUtils.getDigitalUniqueIdentifier());
	}

	public XSD(String name) {
		super(name);
	}

	@Override
	protected void runClassesGenerationTool(File mainFile, File metaSchemaFile, File outputDirectory)
			throws BadCommandLineException {
		String[] xjcArgs = { "-d", outputDirectory.getPath(), "-p",
				InstantiationUtils.toRelativeTypeNameVariablePart(
						XSD.class.getName().toLowerCase() + MiscUtils.toDigitalUniqueIdentifier(XSD.this)),
				mainFile.getPath() };

		final StringBuilder logsBuffer = new StringBuilder();
		int driverStatus;
		Throwable driverException;
		try {
			driverStatus = Driver.run(xjcArgs, new XJCListener() {

				@Override
				public void warning(SAXParseException exception) {
					System.err.println("XJC WARNING: " + exception);
				}

				@Override
				public void info(SAXParseException exception) {
					System.out.println("XJC INFO: " + exception);
				}

				@Override
				public void fatalError(SAXParseException exception) {
					if (logsBuffer.length() > 0) {
						logsBuffer.append("\n");
					}
					logsBuffer.append("FATAL ERROR: " + exception.toString());
				}

				@Override
				public void error(SAXParseException exception) {
					if (logsBuffer.length() > 0) {
						logsBuffer.append("\n");
					}
					logsBuffer.append("ERROR: " + exception.toString());
				}
			});
			driverException = null;
		} catch (Throwable t) {
			driverStatus = -1;
			driverException = t;
		}
		if (driverStatus != 0) {
			throw new RuntimeException("Failed to generate XSD classes"
					+ ((logsBuffer.length() > 0) ? (":\n" + logsBuffer.toString()) : ""), driverException);
		}
	}

	public List<RootElementDescriptor> getRootElements() {
		try {
			return upToDateGeneratedClasses.get().stream()
					.filter(c -> c.getAnnotation(javax.xml.bind.annotation.XmlRootElement.class) != null)
					.map(c -> new RootElementDescriptor(c)).collect(Collectors.toList());
		} catch (VersionAccessException e) {
			throw new UnexpectedError(e);
		}
	}

	public static class RootElementDescriptor {

		private Class<?> elementClass;
		private static WeakHashMap<Class<?>, Class<?>> documentClassByRootElementClass = new WeakHashMap<Class<?>, Class<?>>();

		public RootElementDescriptor(Class<?> elementClass) {
			this.elementClass = elementClass;
		}

		public String getName() {
			javax.xml.bind.annotation.XmlRootElement annotation = elementClass
					.getAnnotation(javax.xml.bind.annotation.XmlRootElement.class);
			return annotation.name();
		}

		public Class<?> retrieveClass() {
			return elementClass;
		}

		public Class<?> getDocumentClass() {
			synchronized (documentClassByRootElementClass) {
				documentClassByRootElementClass.computeIfAbsent(elementClass, elementClass -> {
					ClassicStructure resultStructure = new ClassicStructure();
					{
						SimpleElement rootElement = new SimpleElement();
						rootElement.setName(getName());
						rootElement.setTypeName(elementClass.getName());
						resultStructure.getElements().add(rootElement);
					}
					String className = elementClass.getName() + "DocumentObject";
					try {
						return MiscUtils.IN_MEMORY_COMPILER.compile(className,
								resultStructure.generateJavaTypeSourceCode(className));
					} catch (CompilationError e) {
						throw new UnexpectedError(e);
					}
				});
			}
			return documentClassByRootElementClass.get(elementClass);
		}

	}

	public static class Metadata implements ResourceMetadata {

		@Override
		public ResourcePath getResourceIconImagePath() {
			return new ResourcePath(
					ResourcePath.specifyClassPathResourceLocation(XSD.class.getName().replace(".", "/") + ".png"));
		}

		@Override
		public Class<? extends Resource> getResourceClass() {
			return XSD.class;
		}

		@Override
		public String getResourceTypeName() {
			return "XSD";
		}

	}

}
