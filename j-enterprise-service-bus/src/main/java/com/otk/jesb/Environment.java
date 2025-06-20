package com.otk.jesb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.otk.jesb.Structure.ClassicStructure;
import com.otk.jesb.Structure.Element;
import com.otk.jesb.Structure.SimpleElement;
import com.otk.jesb.Structure.StructuredElement;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.Pair;
import com.otk.jesb.util.UpToDate;
import com.otk.jesb.util.UpToDate.VersionAccessException;

public class Environment {

	private List<EnvironmentVariableTreeElement> environmentVariableTreeElements = new ArrayList<Environment.EnvironmentVariableTreeElement>();

	private UpToDate<Class<?>> upToDateVariablesRootClass = new UpToDate<Class<?>>() {

		@Override
		protected Object retrieveLastVersionIdentifier() {
			return MiscUtils.serialize(getVariablesRootStructure());
		}

		@Override
		protected Class<?> obtainLatest(Object versionIdentifier) throws VersionAccessException {
			String className = Environment.class.getName() + MiscUtils.toDigitalUniqueIdentifier(Environment.this);
			try {
				return MiscUtils.IN_MEMORY_COMPILER.compile(className,
						getVariablesRootStructure().generateJavaTypeSourceCode(className));
			} catch (CompilationError e) {
				throw new UnexpectedError(e);
			}
		}

	};
	private RootInstanceBuilder variablesRootBuilder = new RootInstanceBuilder("VariablesRoot",
			new VariablesRootClassNameAccessor());
	private UpToDate<Object> upToDateVariablesRoot = new UpToDate<Object>() {

		@Override
		protected Object retrieveLastVersionIdentifier() {
			try {
				return new Pair<Class<?>, String>(upToDateVariablesRootClass.get(),
						MiscUtils.serialize(variablesRootBuilder));
			} catch (VersionAccessException e) {
				throw new UnexpectedError(e);
			}
		}

		@Override
		protected Object obtainLatest(Object versionIdentifier) throws VersionAccessException {
			try {
				return variablesRootBuilder
						.build(new InstantiationContext(Collections.emptyList(), Collections.emptyList()));
			} catch (Exception e) {
				throw new UnexpectedError(e);
			}
		}

	};

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
			throw new UnexpectedError(e);
		}
	}

	public Class<?> getVariablesRootClass() {
		try {
			return upToDateVariablesRootClass.get();
		} catch (VersionAccessException e) {
			throw new UnexpectedError(e);
		}
	}

	private ClassicStructure getVariablesRootStructure() {
		ClassicStructure result = new ClassicStructure();
		for (EnvironmentVariableTreeElement element : environmentVariableTreeElements) {
			result.getElements().add(toStructure(element));
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
			for (EnvironmentVariableTreeElement childElement : ((EnvironmentVariableGroup) element).getElements()) {
				result.getSubElements().add(toStructure(childElement));
			}
			result.setStructure(structure);
			return result;
		} else {
			throw new UnexpectedError();
		}
	}

	public static class EnvironmentVariableTreeElement {
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	public static class EnvironmentVariable extends EnvironmentVariableTreeElement {
		private String valueString;

		public String getValueString() {
			return valueString;
		}

		public void setValueString(String valueString) {
			this.valueString = valueString;
		}

	}

	public static class EnvironmentVariableGroup extends EnvironmentVariableTreeElement {
		private List<EnvironmentVariableTreeElement> elements = new ArrayList<Environment.EnvironmentVariableTreeElement>();

		public List<EnvironmentVariableTreeElement> getElements() {
			return elements;
		}

		public void setElements(List<EnvironmentVariableTreeElement> elements) {
			this.elements = elements;
		}

	}

	public class VariablesRootClassNameAccessor extends Accessor<String> {

		@Override
		public String get() {
			try {
				return upToDateVariablesRootClass.get().getName();
			} catch (VersionAccessException e) {
				throw new UnexpectedError(e);
			}
		}

	}

}
