package com.otk.jesb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

public class EnvironmentSettings {

	public static final VariableDeclaration ENVIRONMENT_VARIABLES_ROOT_DECLARATION = new VariableDeclaration() {
		@Override
		public String getVariableName() {
			return "ENVIRONMENT";
		}

		@Override
		public Class<?> getVariableType() {
			return Solution.INSTANCE.getEnvironmentSettings().getVariablesRootClass();
		}

	};

	public static final Variable ENVIRONMENT_VARIABLES_ROOT = new Variable() {

		@Override
		public String getName() {
			return ENVIRONMENT_VARIABLES_ROOT_DECLARATION.getVariableName();
		}

		@Override
		public Object getValue() {
			return Solution.INSTANCE.getEnvironmentSettings().getVariablesRoot();
		}

	};

	private List<EnvironmentVariableTreeElement> environmentVariableTreeElements = new ArrayList<EnvironmentSettings.EnvironmentVariableTreeElement>();

	private UpToDate<Class<?>> upToDateVariablesRootClass = new UpToDateVariablesRootClass();

	private UpToDate<Object> upToDateVariablesRoot = new UpToDateVariablesRoot();

	public List<EnvironmentVariableTreeElement> getEnvironmentVariableTreeElements() {
		return environmentVariableTreeElements;
	}

	public void setEnvironmentVariableTreeElements(
			List<EnvironmentVariableTreeElement> environmentVariableTreeElements) {
		this.environmentVariableTreeElements = environmentVariableTreeElements;
	}

	public Object getVariablesRoot() {
		try {
			return upToDateVariablesRoot.get();
		} catch (VersionAccessException e) {
			throw new PotentialError(e);
		}
	}

	public Class<?> getVariablesRootClass() {
		try {
			return upToDateVariablesRootClass.get();
		} catch (VersionAccessException e) {
			throw new PotentialError(e);
		}
	}

	private ClassicStructure getVariablesRootStructure() {
		ClassicStructure result = new ClassicStructure();
		for (EnvironmentVariableTreeElement element : environmentVariableTreeElements) {
			result.getElements().add(toStructure(element));
		}
		return result;
	}

	private RootInstanceBuilder getVariablesRootBuilder() {
		RootInstanceBuilder result = new RootInstanceBuilder("VariablesRoot", new VariableRootClassNameAccessor());
		;
		ParameterInitializerFacade rootInitializerFacade = (ParameterInitializerFacade) result.getFacade().getChildren()
				.get(0);
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
			result.setTypeName(String.class.getName());
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

	public void validate() throws ValidationError {
		try {
			upToDateVariablesRoot.get();
		} catch (VersionAccessException e) {
			throw new ValidationError("Failed to validate environment variables", e);
		}
	}

	private class VariableRootClassNameAccessor extends Accessor<String> {

		@Override
		public String get() {
			try {
				return upToDateVariablesRootClass.get().getName();
			} catch (VersionAccessException e) {
				throw new PotentialError(e);
			}
		}

	}

	private class UpToDateVariablesRootClass extends UpToDate<Class<?>> {

		@Override
		protected Object retrieveLastVersionIdentifier() {
			return MiscUtils.serialize(getVariablesRootStructure());
		}

		@Override
		protected Class<?> obtainLatest(Object versionIdentifier) throws VersionAccessException {
			String className = EnvironmentSettings.class.getName() + InstantiationUtils
					.toRelativeTypeNameVariablePart(MiscUtils.toDigitalUniqueIdentifier(EnvironmentSettings.this));
			try {
				return MiscUtils.IN_MEMORY_COMPILER.compile(className,
						getVariablesRootStructure().generateJavaTypeSourceCode(className));
			} catch (CompilationError e) {
				throw new VersionAccessException(e);
			}
		}

	};

	private class UpToDateVariablesRoot extends UpToDate<Object> {

		@Override
		protected Object retrieveLastVersionIdentifier() {
			try {
				return new Pair<Class<?>, String>(upToDateVariablesRootClass.get(),
						MiscUtils.serialize(environmentVariableTreeElements));
			} catch (VersionAccessException e) {
				throw new PotentialError(e);
			}
		}

		@Override
		protected Object obtainLatest(Object versionIdentifier) throws VersionAccessException {
			try {
				return getVariablesRootBuilder()
						.build(new InstantiationContext(Collections.emptyList(), Collections.emptyList()));
			} catch (Exception e) {
				throw new PotentialError(e);
			}
		}

	};

	public abstract static class EnvironmentVariableTreeElement {
		private String name;

		public abstract String getValueSummary();

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	public static class EnvironmentVariable extends EnvironmentVariableTreeElement {
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
	}

}
