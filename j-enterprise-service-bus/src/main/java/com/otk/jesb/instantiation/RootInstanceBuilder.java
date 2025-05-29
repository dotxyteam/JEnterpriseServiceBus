package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.UnexpectedError;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.meta.TypeInfoProvider;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.InstantiationUtils;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.UpToDate;
import com.otk.jesb.util.UpToDate.VersionAccessException;

public class RootInstanceBuilder extends InstanceBuilder {

	private String rootInstanceName;
	private Accessor<String> rootInstanceDynamicTypeNameAccessor;
	private String rootInstanceTypeName;

	private Accessor<String> rootInstanceWrapperDynamicTypeNameAccessor = new RootInstanceWrapperDynamicTypeNameAccessor();
	private UpToDate<Class<?>> upToDateRootInstanceClass = new UpToDateRootInstanceClass();

	public RootInstanceBuilder() {
		super.setDynamicTypeNameAccessor(rootInstanceWrapperDynamicTypeNameAccessor);
		initialize();
	}

	public RootInstanceBuilder(String rootInstanceName, Accessor<String> rootInstanceDynamicTypeNameAccessor) {
		super.setDynamicTypeNameAccessor(rootInstanceWrapperDynamicTypeNameAccessor);
		this.rootInstanceName = rootInstanceName;
		this.rootInstanceDynamicTypeNameAccessor = rootInstanceDynamicTypeNameAccessor;
		initialize();
	}

	public RootInstanceBuilder(String rootInstanceName, String rootInstanceTypeName) {
		super.setDynamicTypeNameAccessor(rootInstanceWrapperDynamicTypeNameAccessor);
		this.rootInstanceName = rootInstanceName;
		this.rootInstanceTypeName = (rootInstanceTypeName == null) ? NullInstance.class.getName()
				: rootInstanceTypeName;
		initialize();
	}

	private void initialize() {
		getFacade().getChildren().get(0).setConcrete(true);
	}

	@Override
	public void setTypeName(String typeName) {
	}

	@Override
	public void setDynamicTypeNameAccessor(Accessor<String> dynamicTypeNameAccessor) {
	}

	public String getRootInstanceName() {
		return rootInstanceName;
	}

	public void setRootInstanceName(String rootInstanceName) {
		this.rootInstanceName = rootInstanceName;
	}

	public Accessor<String> getRootInstanceDynamicTypeNameAccessor() {
		return rootInstanceDynamicTypeNameAccessor;
	}

	public void setRootInstanceDynamicTypeNameAccessor(Accessor<String> rootInstanceDynamicTypeNameAccessor) {
		this.rootInstanceDynamicTypeNameAccessor = rootInstanceDynamicTypeNameAccessor;
	}

	public String getRootInstanceTypeName() {
		return rootInstanceTypeName;
	}

	public void setRootInstanceTypeName(String rootInstanceTypeName) {
		this.rootInstanceTypeName = rootInstanceTypeName;
	}

	public RootInstanceBuilderFacade getFacade() {
		return (RootInstanceBuilderFacade) Facade.get(this, null);
	}

	public List<FacadeOutline> getFacadeOutlineChildren() {
		return new FacadeOutline(getFacade()).getChildren();
	}

	public Object getRootInitializer() {
		List<Facade> children = getFacade().getChildren();
		if (children.size() == 0) {
			return null;
		}
		return children.get(0).getUnderlying();
	}

	public void setRootInitializer(Object initializer) {
		List<ParameterInitializer> newParameterInitializers = new ArrayList<ParameterInitializer>();
		List<InitializationSwitch> newInitializationSwitches = new ArrayList<InitializationSwitch>();
		if (initializer instanceof ParameterInitializer) {
			newParameterInitializers.add((ParameterInitializer) initializer);
		} else if (initializer instanceof InitializationSwitch) {
			newInitializationSwitches.add((InitializationSwitch) initializer);
		} else if (initializer != null) {
			throw new UnexpectedError();
		}
		setParameterInitializers(newParameterInitializers);
		setInitializationSwitches(newInitializationSwitches);
	}

	@Override
	public Object build(InstantiationContext context) throws Exception {
		RootInstanceWrapper wrapper = ((RootInstanceWrapper) super.build(context));
		if (wrapper == null) {
			return null;
		}
		return wrapper.getRootInstance();
	}

	private class UpToDateRootInstanceClass extends UpToDate<Class<?>> {
		@Override
		protected Object retrieveLastVersionIdentifier() {
			String actualRootInstanceTypeName = (rootInstanceDynamicTypeNameAccessor != null)
					? rootInstanceDynamicTypeNameAccessor.get()
					: rootInstanceTypeName;
			return (actualRootInstanceTypeName == null) ? null : TypeInfoProvider.getClass(actualRootInstanceTypeName);
		}

		@Override
		protected Class<?> obtainLatest(Object versionIdentifier) {
			String actualRootInstanceTypeName = (rootInstanceDynamicTypeNameAccessor != null)
					? rootInstanceDynamicTypeNameAccessor.get()
					: rootInstanceTypeName;
			if (actualRootInstanceTypeName == null) {
				actualRootInstanceTypeName = NullInstance.class.getName();
			}
			if (!InstantiationUtils.isComplexType(TypeInfoProvider.getTypeInfo(actualRootInstanceTypeName))) {
				throw new UnexpectedError();
			}
			Object finalRootInstanceName;
			if (rootInstanceName != null) {
				finalRootInstanceName = rootInstanceName;
			} else {
				finalRootInstanceName = "none";
			}
			String rootInstanceWrapperClassName = RootInstanceBuilder.class.getPackage().getName() + "."
					+ finalRootInstanceName + "." + actualRootInstanceTypeName + "Wrapper";
			StringBuilder rootInstanceWrapperClassSourceBuilder = new StringBuilder();
			{
				rootInstanceWrapperClassSourceBuilder.append(
						"package " + MiscUtils.extractPackageNameFromClassName(rootInstanceWrapperClassName) + ";\n");
				rootInstanceWrapperClassSourceBuilder.append("public class "
						+ MiscUtils.extractSimpleNameFromClassName(rootInstanceWrapperClassName) + " implements "
						+ MiscUtils.adaptClassNameToSourceCode(RootInstanceWrapper.class.getName()) + "{\n");
				rootInstanceWrapperClassSourceBuilder.append("	private "
						+ MiscUtils.adaptClassNameToSourceCode(actualRootInstanceTypeName) + " rootInstance;\n");
				rootInstanceWrapperClassSourceBuilder
						.append("	public " + MiscUtils.extractSimpleNameFromClassName(rootInstanceWrapperClassName)
								+ "(" + MiscUtils.adaptClassNameToSourceCode(actualRootInstanceTypeName) + " "
								+ finalRootInstanceName + ") {\n");
				rootInstanceWrapperClassSourceBuilder
						.append("		this.rootInstance = " + finalRootInstanceName + ";\n");
				rootInstanceWrapperClassSourceBuilder.append("	}\n");
				rootInstanceWrapperClassSourceBuilder.append("	@Override\n");
				rootInstanceWrapperClassSourceBuilder.append("	public "
						+ MiscUtils.adaptClassNameToSourceCode(actualRootInstanceTypeName) + " getRootInstance() {\n");
				rootInstanceWrapperClassSourceBuilder.append("		return rootInstance;\n");
				rootInstanceWrapperClassSourceBuilder.append("	}\n");
				rootInstanceWrapperClassSourceBuilder.append("}\n");
			}
			try {
				return MiscUtils.IN_MEMORY_COMPILER.compile(rootInstanceWrapperClassName,
						rootInstanceWrapperClassSourceBuilder.toString());
			} catch (CompilationError ce) {
				throw new UnexpectedError(ce);
			}
		}
	};

	private class RootInstanceWrapperDynamicTypeNameAccessor extends Accessor<String> {
		@Override
		public String get() {
			try {
				return upToDateRootInstanceClass.get().getName();
			} catch (VersionAccessException e) {
				throw new UnexpectedError(e);
			}
		}
	}

}