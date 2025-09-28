package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.PotentialError;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.CodeBuilder;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.TreeVisitor;
import com.otk.jesb.util.UpToDate;
import com.otk.jesb.util.UpToDate.VersionAccessException;

public class RootInstanceBuilder extends InstanceBuilder {

	private String rootInstanceName;
	private Accessor<String> rootInstanceDynamicTypeNameAccessor;
	private String rootInstanceTypeName;

	private Accessor<String> rootInstanceWrapperDynamicTypeNameAccessor = new RootInstanceWrapperDynamicTypeNameAccessor();
	private UpToDate<Class<?>> upToDateRootInstanceClass = new UpToDateRootInstanceClass();
	private RootInstanceBuilderFacade facade;

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
		this.rootInstanceTypeName = (rootInstanceTypeName == null) ? NullInstance.class.getName()
				: rootInstanceTypeName;
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

	public synchronized RootInstanceBuilderFacade getFacade() {
		if (facade == null) {
			facade = (RootInstanceBuilderFacade) Facade.get(this, null);
			facade.getChildren().get(0).setConcrete(true);
		}
		return facade;
	}

	public List<FacadeOutline> getFacadeOutlineChildren() {
		return new FacadeOutline(getFacade()).getChildren();
	}

	public Object getRootInstantiationNode() {
		List<Facade> children = getFacade().getChildren();
		if (children.size() == 0) {
			return null;
		}
		return children.get(0).getUnderlying();
	}

	public void setRootInstantiationNode(Object instantiationNode) {
		List<ParameterInitializer> newParameterInitializers = new ArrayList<ParameterInitializer>();
		List<InitializationSwitch> newInitializationSwitches = new ArrayList<InitializationSwitch>();
		if (instantiationNode instanceof ParameterInitializer) {
			newParameterInitializers.add((ParameterInitializer) instantiationNode);
		} else if (instantiationNode instanceof InitializationSwitch) {
			newInitializationSwitches.add((InitializationSwitch) instantiationNode);
		} else if (instantiationNode != null) {
			throw new UnexpectedError();
		}
		setParameterInitializers(newParameterInitializers);
		setInitializationSwitches(newInitializationSwitches);
	}

	public List<InstanceBuilderFacade> getWrappedInstanceBuilderFacades() {
		List<InstanceBuilderFacade> result = new ArrayList<InstanceBuilderFacade>();
		RootInstanceBuilderFacade rootInstanceBuilderFacade = getFacade();
		rootInstanceBuilderFacade.visit(new TreeVisitor<Facade>() {
			@Override
			public VisitStatus visitNode(Facade facade) {
				if (facade == rootInstanceBuilderFacade) {
					return VisitStatus.VISIT_NOT_INTERRUPTED;
				}
				if (RootInstanceBuilder.getFromRootInstanceInitializerFacade(facade) != null) {
					ParameterInitializerFacade initializerFacade = (ParameterInitializerFacade) facade;
					initializerFacade.getChildren();
					Object initializerValue = initializerFacade.getCachedValue();
					if (initializerValue instanceof InstanceBuilder) {
						result.add(new InstanceBuilderFacade(facade, (InstanceBuilder) initializerValue));
					}
					return VisitStatus.SUBTREE_VISIT_INTERRUPTED;
				}
				return VisitStatus.VISIT_NOT_INTERRUPTED;
			}
		});
		return result;
	}

	@Override
	public Object build(InstantiationContext context) throws Exception {
		RootInstanceWrapper wrapper = ((RootInstanceWrapper) super.build(context));
		if (wrapper == null) {
			return null;
		}
		return wrapper.getRootInstance();
	}

	public static RootInstanceBuilder getFromRootInstanceInitializerFacade(Facade facade) {
		if ((facade instanceof ParameterInitializerFacade) && (((ParameterInitializerFacade) facade)
				.getCurrentInstanceBuilderFacade() instanceof RootInstanceBuilderFacade)) {
			return ((RootInstanceBuilderFacade) ((ParameterInitializerFacade) facade).getCurrentInstanceBuilderFacade())
					.getUnderlying();

		} else {
			return null;
		}

	}

	private class UpToDateRootInstanceClass extends UpToDate<Class<?>> {
		@Override
		protected Object retrieveLastVersionIdentifier() {
			String actualRootInstanceTypeName = (rootInstanceDynamicTypeNameAccessor != null)
					? rootInstanceDynamicTypeNameAccessor.get()
					: rootInstanceTypeName;
			return (actualRootInstanceTypeName == null) ? null : MiscUtils.getJESBClass(actualRootInstanceTypeName);
		}

		@Override
		protected Class<?> obtainLatest(Object versionIdentifier) {
			String actualRootInstanceTypeName = (rootInstanceDynamicTypeNameAccessor != null)
					? rootInstanceDynamicTypeNameAccessor.get()
					: rootInstanceTypeName;
			if (actualRootInstanceTypeName == null) {
				actualRootInstanceTypeName = NullInstance.class.getName();
			}
			final String finalActualRootInstanceTypeName = actualRootInstanceTypeName;
			Object finalRootInstanceName;
			if (rootInstanceName != null) {
				finalRootInstanceName = rootInstanceName;
			} else {
				finalRootInstanceName = "none";
			}
			String rootInstanceWrapperClassName;
			{
				String arrayComponentTypeName = MiscUtils.getArrayComponentTypeName(actualRootInstanceTypeName);
				if (arrayComponentTypeName != null) {
					rootInstanceWrapperClassName = RootInstanceBuilder.class.getPackage().getName() + "."
							+ finalRootInstanceName + "." + arrayComponentTypeName + "ArrayWrapper";
				} else {
					rootInstanceWrapperClassName = RootInstanceBuilder.class.getPackage().getName() + "."
							+ finalRootInstanceName + "." + actualRootInstanceTypeName + "Wrapper";
				}
				rootInstanceWrapperClassName = rootInstanceWrapperClassName.replace("$", "_");
			}
			final String finalRootInstanceWrapperClassName = rootInstanceWrapperClassName;
			CodeBuilder rootInstanceWrapperClassSourceBuilder = new CodeBuilder();
			{
				rootInstanceWrapperClassSourceBuilder.append(
						"package " + MiscUtils.extractPackageNameFromClassName(rootInstanceWrapperClassName) + ";\n");
				rootInstanceWrapperClassSourceBuilder.append("public class "
						+ MiscUtils.extractSimpleNameFromClassName(rootInstanceWrapperClassName) + " implements "
						+ MiscUtils.adaptClassNameToSourceCode(RootInstanceWrapper.class.getName()) + "{\n");
				rootInstanceWrapperClassSourceBuilder.indenting(() -> {
					rootInstanceWrapperClassSourceBuilder
							.append("private " + MiscUtils.adaptClassNameToSourceCode(finalActualRootInstanceTypeName)
									+ " rootInstance;\n");
					rootInstanceWrapperClassSourceBuilder.append(
							"public " + MiscUtils.extractSimpleNameFromClassName(finalRootInstanceWrapperClassName)
									+ "(" + MiscUtils.adaptClassNameToSourceCode(finalActualRootInstanceTypeName) + " "
									+ finalRootInstanceName + ") {\n");
					rootInstanceWrapperClassSourceBuilder
							.appendIndented("this.rootInstance = " + finalRootInstanceName + ";\n");
					rootInstanceWrapperClassSourceBuilder.append("}\n");
					rootInstanceWrapperClassSourceBuilder.append("@Override\n");
					rootInstanceWrapperClassSourceBuilder
							.append("public " + MiscUtils.adaptClassNameToSourceCode(finalActualRootInstanceTypeName)
									+ " getRootInstance() {\n");
					rootInstanceWrapperClassSourceBuilder.appendIndented("return rootInstance;\n");
					rootInstanceWrapperClassSourceBuilder.append("}\n");
				});
				rootInstanceWrapperClassSourceBuilder.append("}\n");
			}
			try {
				return MiscUtils.IN_MEMORY_COMPILER.compile(rootInstanceWrapperClassName,
						rootInstanceWrapperClassSourceBuilder.toString());
			} catch (CompilationError ce) {
				throw new PotentialError(ce);
			}
		}
	};

	private class RootInstanceWrapperDynamicTypeNameAccessor extends Accessor<String> {
		@Override
		public String get() {
			try {
				return upToDateRootInstanceClass.get().getName();
			} catch (VersionAccessException e) {
				throw new PotentialError(e);
			}
		}
	}

}