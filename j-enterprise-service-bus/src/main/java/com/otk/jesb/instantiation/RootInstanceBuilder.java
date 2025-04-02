package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.MiscUtils;

public class RootInstanceBuilder extends InstanceBuilder {

	private String rootInstanceName;
	private Accessor<String> rootInstanceDynamicTypeNameAccessor;
	private String rootInstanceTypeName;

	private Accessor<String> rootInstanceWrapperDynamicTypeNameAccessor = new Accessor<String>() {
		@Override
		public String get() {
			String actualRootInstanceTypeName = (rootInstanceDynamicTypeNameAccessor != null)
					? rootInstanceDynamicTypeNameAccessor.get()
					: rootInstanceTypeName;
			if (actualRootInstanceTypeName == null) {
				return NullInstance.class.getName();
			}
			String rootInstanceWrapperClassName = RootInstanceBuilder.class.getPackage().getName() + "."
					+ rootInstanceName + "." + actualRootInstanceTypeName + "Wrapper";
			Class<?> rootInstanceWrapperClass;
			try {
				rootInstanceWrapperClass = MiscUtils.IN_MEMORY_JAVA_COMPILER.getClassLoader()
						.loadClass(rootInstanceWrapperClassName);
			} catch (ClassNotFoundException e) {
				StringBuilder rootInstanceWrapperClassSourceBuilder = new StringBuilder();
				{
					rootInstanceWrapperClassSourceBuilder.append("package "
							+ MiscUtils.extractPackageNameFromClassName(rootInstanceWrapperClassName) + ";\n");
					rootInstanceWrapperClassSourceBuilder.append("public class "
							+ MiscUtils.extractSimpleNameFromClassName(rootInstanceWrapperClassName) + " implements "
							+ MiscUtils.adaptClassNameToSourceCode(RootInstanceWrapper.class.getName()) + "{\n");
					rootInstanceWrapperClassSourceBuilder.append("	private "
							+ MiscUtils.adaptClassNameToSourceCode(actualRootInstanceTypeName) + " rootInstance;\n");
					rootInstanceWrapperClassSourceBuilder.append(
							"	public " + MiscUtils.extractSimpleNameFromClassName(rootInstanceWrapperClassName) + "("
									+ MiscUtils.adaptClassNameToSourceCode(actualRootInstanceTypeName) + " "
									+ rootInstanceName + ") {\n");
					rootInstanceWrapperClassSourceBuilder
							.append("		this.rootInstance = " + rootInstanceName + ";\n");
					rootInstanceWrapperClassSourceBuilder.append("	}\n");
					rootInstanceWrapperClassSourceBuilder.append("	@Override\n");
					rootInstanceWrapperClassSourceBuilder
							.append("	public " + MiscUtils.adaptClassNameToSourceCode(actualRootInstanceTypeName)
									+ " getRootInstance() {\n");
					rootInstanceWrapperClassSourceBuilder.append("		return rootInstance;\n");
					rootInstanceWrapperClassSourceBuilder.append("	}\n");
					rootInstanceWrapperClassSourceBuilder.append("}\n");
				}
				try {
					rootInstanceWrapperClass = MiscUtils.IN_MEMORY_JAVA_COMPILER.compile(rootInstanceWrapperClassName,
							rootInstanceWrapperClassSourceBuilder.toString());
				} catch (CompilationError ce) {
					throw new AssertionError(ce);
				}
			}
			return rootInstanceWrapperClass.getName();
		}
	};

	public RootInstanceBuilder() {
		super.setDynamicTypeNameAccessor(rootInstanceWrapperDynamicTypeNameAccessor);
	}

	public RootInstanceBuilder(String rootInstanceName, Accessor<String> rootInstanceDynamicTypeNameAccessor) {
		super.setDynamicTypeNameAccessor(rootInstanceWrapperDynamicTypeNameAccessor);
		this.rootInstanceName = rootInstanceName;
		this.rootInstanceDynamicTypeNameAccessor = rootInstanceDynamicTypeNameAccessor;
	}

	public RootInstanceBuilder(String rootInstanceName, String rootInstanceTypeName) {
		super.setDynamicTypeNameAccessor(rootInstanceWrapperDynamicTypeNameAccessor);
		this.rootInstanceName = rootInstanceName;
		this.rootInstanceTypeName = rootInstanceTypeName;
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
			throw new AssertionError();
		}
		setParameterInitializers(newParameterInitializers);
		setInitializationSwitches(newInitializationSwitches);
	}

	@Override
	public Object build(EvaluationContext context) throws Exception {
		RootInstanceWrapper wrapper = ((RootInstanceWrapper) super.build(context));
		if (wrapper == null) {
			return null;
		}
		return wrapper.getRootInstance();
	}

}