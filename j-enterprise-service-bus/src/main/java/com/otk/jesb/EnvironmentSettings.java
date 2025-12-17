package com.otk.jesb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import com.otk.jesb.Structure.ClassicStructure;
import com.otk.jesb.Structure.Element;
import com.otk.jesb.Structure.SimpleElement;
import com.otk.jesb.Structure.StructuredElement;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.instantiation.Facade;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.ParameterInitializerFacade;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.InstantiationUtils;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.Pair;
import com.otk.jesb.util.UpToDate;
import com.otk.jesb.util.UpToDate.VersionAccessException;

/**
 * This class represents all the data of a solution, which may vary from one
 * environment to another.
 * 
 * @author olitank
 *
 */
public class EnvironmentSettings {

	private List<EnvironmentVariableTreeElement> environmentVariableTreeElements = new ArrayList<EnvironmentSettings.EnvironmentVariableTreeElement>();

	private UpToDate<Solution, Class<?>> upToDateRootValueClass = new UpToDateRootValueClass();

	private UpToDate<Solution, Object> upToDateRootValue = new UpToDateRootValue();

	public VariableDeclaration getRootVariableDeclaration(Solution solutionInstance) {
		return new VariableDeclaration() {
			@Override
			public String getVariableName() {
				return "ENVIRONMENT";
			}

			@Override
			public Class<?> getVariableType() {
				return getRootValueClass(solutionInstance);
			}

		};
	}

	public Variable getRootVariable(Solution solutionInstance) {
		return new Variable() {
			@Override
			public String getName() {
				return getRootVariableDeclaration(solutionInstance).getVariableName();
			}

			@Override
			public Object getValue() {
				return getRootValue(solutionInstance);
			}

		};
	}

	public List<EnvironmentVariableTreeElement> getEnvironmentVariableTreeElements() {
		return environmentVariableTreeElements;
	}

	public void setEnvironmentVariableTreeElements(
			List<EnvironmentVariableTreeElement> environmentVariableTreeElements) {
		this.environmentVariableTreeElements = environmentVariableTreeElements;
	}

	public Object getRootValue(Solution solutionInstance) {
		try {
			return upToDateRootValue.get(solutionInstance);
		} catch (VersionAccessException e) {
			throw new PotentialError(e);
		}
	}

	public Class<?> getRootValueClass(Solution solutionInstance) {
		try {
			return upToDateRootValueClass.get(solutionInstance);
		} catch (VersionAccessException e) {
			throw new PotentialError(e);
		}
	}

	private ClassicStructure getRootValueStructure() {
		ClassicStructure result = new ClassicStructure();
		for (EnvironmentVariableTreeElement element : environmentVariableTreeElements) {
			result.getElements().add(toStructure(element));
		}
		return result;
	}

	private RootInstanceBuilder getRootValueBuilder(Solution solutionInstance) {
		RootInstanceBuilder result = new RootInstanceBuilder("Root", new VariableRootClassNameAccessor());
		ParameterInitializerFacade rootInitializerFacade = (ParameterInitializerFacade) result
				.getFacade(solutionInstance).getChildren().get(0);
		rootInitializerFacade.setConcrete(true);
		List<Facade> elementInitializerFacades = rootInitializerFacade.getChildren();
		for (EnvironmentVariableTreeElement element : environmentVariableTreeElements) {
			ParameterInitializerFacade elementInitializerFacade = (ParameterInitializerFacade) elementInitializerFacades
					.stream().filter(facade -> ((ParameterInitializerFacade) facade).getParameterName()
							.equals(element.getName()))
					.findFirst().get();
			configureInitializer(elementInitializerFacade, element);
		}
		return result;
	}

	private Element toStructure(EnvironmentVariableTreeElement element) {
		if (element instanceof EnvironmentVariable) {
			SimpleElement result = new SimpleElement();
			result.setName(element.getName());
			result.setTypeNameOrAlias(String.class.getName());
			return result;
		} else if (element instanceof EnvironmentVariableGroup) {
			StructuredElement result = new StructuredElement();
			result.setName(element.getName());
			ClassicStructure structure = new ClassicStructure();
			result.setStructure(structure);
			for (EnvironmentVariableTreeElement childElement : ((EnvironmentVariableGroup) element).getElements()) {
				result.getSubElements().add(toStructure(childElement));
			}
			return result;
		} else {
			throw new UnexpectedError();
		}
	}

	private void configureInitializer(ParameterInitializerFacade elementInitializerFacade,
			EnvironmentVariableTreeElement element) {
		if (element instanceof EnvironmentVariable) {
			elementInitializerFacade.setParameterValue(((EnvironmentVariable) element).getValueString());
		} else if (element instanceof EnvironmentVariableGroup) {
			elementInitializerFacade.setConcrete(true);
			List<Facade> childElementInitializerFacades = elementInitializerFacade.getChildren();
			for (EnvironmentVariableTreeElement childElement : ((EnvironmentVariableGroup) element).getElements()) {
				ParameterInitializerFacade childElementInitializerFacade = (ParameterInitializerFacade) childElementInitializerFacades
						.stream().filter(facade -> ((ParameterInitializerFacade) facade).getParameterName()
								.equals(childElement.getName()))
						.findFirst().get();
				configureInitializer(childElementInitializerFacade, childElement);
			}
		} else {
			throw new UnexpectedError();
		}
	}

	public void importProperties(File file) throws FileNotFoundException, IOException {
		Properties properties = new Properties();
		try (FileInputStream fileInputStream = new FileInputStream(file)) {
			properties.load(fileInputStream);
		}
		for (EnvironmentVariableTreeElement element : environmentVariableTreeElements) {
			element.importProperties(properties, null);
		}
	}

	public void exportProperties(File file) throws FileNotFoundException, IOException {
		Properties properties = new Properties();
		for (EnvironmentVariableTreeElement element : environmentVariableTreeElements) {
			element.exportProperties(properties, null);
		}
		try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
			properties.store(fileOutputStream, null);
		}
	}

	public void mergeProperties(File file) throws FileNotFoundException, IOException {
		Properties oldProperties = new Properties();
		try (FileInputStream fileInputStream = new FileInputStream(file)) {
			oldProperties.load(fileInputStream);
		}
		Properties newProperties = new Properties();
		for (EnvironmentVariableTreeElement element : environmentVariableTreeElements) {
			element.exportProperties(newProperties, null);
		}
		Properties addedProperties = new Properties();
		Properties removedProperties = new Properties();
		Properties mergedProperties = new Properties();
		mergedProperties.putAll(oldProperties);
		for (Entry<Object, Object> entry : oldProperties.entrySet()) {
			if (!newProperties.entrySet().contains(entry)) {
				mergedProperties.remove(entry.getKey());
				removedProperties.put(entry.getKey(), entry.getValue());
			}
		}
		for (Entry<Object, Object> entry : newProperties.entrySet()) {
			if (!oldProperties.entrySet().contains(entry)) {
				mergedProperties.put(entry.getKey(), entry.getValue());
				addedProperties.put(entry.getKey(), entry.getValue());
			}
		}
		StringWriter commentsBuffer = new StringWriter();
		if (!addedProperties.isEmpty()) {
			commentsBuffer.write("\n- Added:\n");
			addedProperties.store(commentsBuffer, null);
		}
		if (!removedProperties.isEmpty()) {
			commentsBuffer.write("\n- Removed:\n");
			removedProperties.store(commentsBuffer, null);
		}
		try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
			String comments;
			if (commentsBuffer.getBuffer().length() > 0) {
				comments = commentsBuffer.toString();
				comments = comments.replaceAll(
						"#[a-zA-Z]{3} [a-zA-Z]{3} [0-9]{2} [0-9]{2}\\:[0-9]{2}\\:[0-9]{2} [^\\s]+ [0-9]{4}\r?\n", "");
			} else {
				comments = "\n(no changes)\n";
			}
			comments = "#############\n" + "MERGE RESULT#\n" + "##############\n" + "" + comments;
			mergedProperties.store(fileOutputStream, comments);
		}
	}

	public void validate(Solution solutionInstance) throws ValidationError {
		try {
			upToDateRootValue.get(solutionInstance);
		} catch (VersionAccessException e) {
			throw new ValidationError("Failed to validate environment variables", e);
		}
	}

	private class VariableRootClassNameAccessor extends Accessor<Solution, String> {

		@Override
		public String get(Solution solutionInstance) {
			try {
				return upToDateRootValueClass.get(solutionInstance).getName();
			} catch (VersionAccessException e) {
				throw new PotentialError(e);
			}
		}

	}

	private class UpToDateRootValueClass extends UpToDate<Solution, Class<?>> {

		@Override
		protected Object retrieveLastVersionIdentifier(Solution solutionInstance) {
			return MiscUtils.serialize(getRootValueStructure(), solutionInstance.getRuntime().getXstream());
		}

		@Override
		protected Class<?> obtainLatest(Solution solutionInstance, Object versionIdentifier)
				throws VersionAccessException {
			String className = EnvironmentSettings.class.getName() + InstantiationUtils
					.toRelativeTypeNameVariablePart(MiscUtils.toDigitalUniqueIdentifier(EnvironmentSettings.this));
			try {
				return solutionInstance.getRuntime().getInMemoryCompiler().compile(className,
						getRootValueStructure().generateJavaTypeSourceCode(className, solutionInstance));
			} catch (CompilationError e) {
				throw new VersionAccessException(e);
			}
		}

	};

	private class UpToDateRootValue extends UpToDate<Solution, Object> {

		@Override
		protected Object retrieveLastVersionIdentifier(Solution solutionInstance) {
			try {
				return new Pair<Class<?>, String>(upToDateRootValueClass.get(solutionInstance), MiscUtils
						.serialize(environmentVariableTreeElements, solutionInstance.getRuntime().getXstream()));
			} catch (VersionAccessException e) {
				throw new PotentialError(e);
			}
		}

		@Override
		protected Object obtainLatest(Solution solutionInstance, Object versionIdentifier)
				throws VersionAccessException {
			try {
				return getRootValueBuilder(solutionInstance).build(
						new InstantiationContext(Collections.emptyList(), Collections.emptyList(), solutionInstance));
			} catch (Exception e) {
				throw new PotentialError(e);
			}
		}

	};

	public abstract static class EnvironmentVariableTreeElement {
		private String name;

		public abstract String getValueSummary();

		protected abstract void importProperties(Properties properties, String propertyNamePrefix);

		protected abstract void exportProperties(Properties properties, String propertyNamePrefix);

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	public static class EnvironmentVariable extends EnvironmentVariableTreeElement {

		private static final String NULL_PROPERTY_VALUE = "<null>";

		private String valueString;

		public EnvironmentVariable() {
			setName("var");
		}

		public String getValueString() {
			return valueString;
		}

		public void setValueString(String valueString) {
			this.valueString = valueString;
		}

		@Override
		public String getValueSummary() {
			return valueString;
		}

		@Override
		protected void importProperties(Properties properties, String propertyNamePrefix) {
			String propertyName = ((propertyNamePrefix != null) ? (propertyNamePrefix + ".") : "") + getName();
			if (!properties.stringPropertyNames().contains(propertyName)) {
				Log.get().information(
						"Variable property not found while importing environment settings: '" + propertyName + "'");
				return;
			}
			String propertyValue = properties.getProperty(propertyName);
			if (NULL_PROPERTY_VALUE.equals(propertyValue)) {
				propertyValue = null;
			}
			valueString = propertyValue;
		}

		@Override
		protected void exportProperties(Properties properties, String propertyNamePrefix) {
			String propertyName = ((propertyNamePrefix != null) ? (propertyNamePrefix + ".") : "") + getName();
			String propertyValue = valueString;
			if (propertyValue == null) {
				propertyValue = NULL_PROPERTY_VALUE;
			}
			properties.setProperty(propertyName, propertyValue);
		}

	}

	public static class EnvironmentVariableGroup extends EnvironmentVariableTreeElement {
		private List<EnvironmentVariableTreeElement> elements = new ArrayList<EnvironmentSettings.EnvironmentVariableTreeElement>();

		public EnvironmentVariableGroup() {
			setName("group");
		}

		public List<EnvironmentVariableTreeElement> getElements() {
			return elements;
		}

		public void setElements(List<EnvironmentVariableTreeElement> elements) {
			this.elements = elements;
		}

		@Override
		public String getValueSummary() {
			return null;
		}

		@Override
		protected void importProperties(Properties properties, String propertyNamePrefix) {
			String childPropertyNamePrefix = ((propertyNamePrefix != null) ? (propertyNamePrefix + ".") : "")
					+ getName();
			for (EnvironmentVariableTreeElement element : elements) {
				element.importProperties(properties, childPropertyNamePrefix);
			}
		}

		@Override
		protected void exportProperties(Properties properties, String propertyNamePrefix) {
			String childPropertyNamePrefix = ((propertyNamePrefix != null) ? (propertyNamePrefix + ".") : "")
					+ getName();
			for (EnvironmentVariableTreeElement element : elements) {
				element.exportProperties(properties, childPropertyNamePrefix);
			}
		}

	}

}
