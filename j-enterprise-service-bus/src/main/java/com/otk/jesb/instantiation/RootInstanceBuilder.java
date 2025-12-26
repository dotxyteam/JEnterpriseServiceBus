package com.otk.jesb.instantiation;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.PotentialError;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.CodeBuilder;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.TreeVisitor;
import com.otk.jesb.util.UpToDate;
import com.otk.jesb.util.UpToDate.VersionAccessException;

/**
 * This class allows to specify a root value that can be obtained by using, or
 * even combining, the different instantiation structures
 * ({@link InstanceBuilder}, {@link InstantiationFunction},
 * {@link InitializationSwitch}, ...).
 * 
 * To provide this flexibility, a dynamically generated class is internally used
 * as a wrapper for the root value which is then named according to the provided
 * {@link #rootInstanceName}.
 * 
 * The root value type name is computed with the provided
 * {@link #rootInstanceTypeName} or
 * {@link #rootInstanceDynamicTypeNameAccessor}.
 * 
 * @author olitank
 *
 */
public class RootInstanceBuilder extends InstanceBuilder {

	public static final String ROOT_INSTANCE_TYPE_NAME_REFERENCE = RootInstanceBuilder.class.getName()
			+ ".ROOT_INSTANCE_TYPE_NAME_REFERENCE";
	public static final boolean ROOT_INSTANCE_TYPE_NAME_REFERENCE_MODE_PREFFERED = System
			.getProperty(RootInstanceBuilder.class.getName() + ".ROOT_INSTANCE_TYPE_NAME_REFERENCE_MODE_PREFFERED", "true")
			.equals("true");

	private String rootInstanceName;
	private Accessor<Solution, String> rootInstanceDynamicTypeNameAccessor;
	private String rootInstanceTypeName;

	private Accessor<Solution, String> rootInstanceWrapperDynamicTypeNameAccessor = new RootInstanceWrapperDynamicTypeNameAccessor();
	private UpToDate<Solution, Class<?>> upToDateRootInstanceClass = new UpToDateRootInstanceClass();
	private RootInstanceBuilderFacade facade;

	public RootInstanceBuilder() {
		super.setDynamicTypeNameAccessor(rootInstanceWrapperDynamicTypeNameAccessor);
	}

	public RootInstanceBuilder(String rootInstanceName,
			Accessor<Solution, String> rootInstanceDynamicTypeNameAccessor) {
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
	public void setDynamicTypeNameAccessor(Accessor<Solution, String> dynamicTypeNameAccessor) {
	}

	public String getRootInstanceName() {
		return rootInstanceName;
	}

	public void setRootInstanceName(String rootInstanceName) {
		this.rootInstanceName = rootInstanceName;
	}

	public Accessor<Solution, String> getRootInstanceDynamicTypeNameAccessor() {
		return rootInstanceDynamicTypeNameAccessor;
	}

	public void setRootInstanceDynamicTypeNameAccessor(Accessor<Solution, String> rootInstanceDynamicTypeNameAccessor) {
		this.rootInstanceDynamicTypeNameAccessor = rootInstanceDynamicTypeNameAccessor;
	}

	public String getRootInstanceTypeName() {
		return rootInstanceTypeName;
	}

	public void setRootInstanceTypeName(String rootInstanceTypeName) {
		this.rootInstanceTypeName = rootInstanceTypeName;
	}

	public synchronized RootInstanceBuilderFacade getFacade(Solution solutionInstance) {
		if (facade == null) {
			RootInstanceBuilderFacade tmpFacade = (RootInstanceBuilderFacade) Facade.get(this, null, solutionInstance);
			List<Facade> facadeChildren = tmpFacade.getChildren();
			if (facadeChildren.size() == 0) {
				return tmpFacade;
			}
			facadeChildren.get(0).setConcrete(true);
			facade = tmpFacade;
		}
		return facade;
	}

	public List<FacadeOutline> getFacadeOutlineChildren(Solution solutionInstance) {
		return new FacadeOutline(getFacade(solutionInstance)).getChildren();
	}

	@Transient
	public Object getRootInstantiationNode(Solution solutionInstance) {
		List<Facade> children = getFacade(solutionInstance).getChildren();
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

	public List<InstanceBuilderFacade> getWrappedInstanceBuilderFacades(Solution solutionInstance) {
		List<InstanceBuilderFacade> result = new ArrayList<InstanceBuilderFacade>();
		RootInstanceBuilderFacade rootInstanceBuilderFacade = getFacade(solutionInstance);
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
						result.add(new InstanceBuilderFacade(facade, (InstanceBuilder) initializerValue,
								solutionInstance));
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

	private class UpToDateRootInstanceClass extends UpToDate<Solution, Class<?>> {
		@Override
		protected Object retrieveLastVersionIdentifier(Solution solutionInstance) {
			String actualRootInstanceTypeName = (rootInstanceDynamicTypeNameAccessor != null)
					? rootInstanceDynamicTypeNameAccessor.get(solutionInstance)
					: rootInstanceTypeName;
			return (actualRootInstanceTypeName == null) ? null
					: solutionInstance.getRuntime().getJESBClass(actualRootInstanceTypeName);
		}

		@Override
		protected Class<?> obtainLatest(Solution solutionInstance, Object versionIdentifier) {
			String actualRootInstanceTypeName = (rootInstanceDynamicTypeNameAccessor != null)
					? rootInstanceDynamicTypeNameAccessor.get(solutionInstance)
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
				return solutionInstance.getRuntime().getInMemoryCompiler().compile(rootInstanceWrapperClassName,
						rootInstanceWrapperClassSourceBuilder.toString());
			} catch (CompilationError ce) {
				throw new PotentialError(ce);
			}
		}
	};

	private class RootInstanceWrapperDynamicTypeNameAccessor extends Accessor<Solution, String> {
		@Override
		public String get(Solution solutionInstance) {
			try {
				return upToDateRootInstanceClass.get(solutionInstance).getName();
			} catch (VersionAccessException e) {
				throw new PotentialError(e);
			}
		}
	}

	public static String resolveRootInstanceTypeNameReference(List<InstanceBuilder> ancestorStructureInstanceBuilders,
			Solution solutionInstance) {
		if (ancestorStructureInstanceBuilders.size() == 0) {
			throw new UnexpectedError();
		}
		if (!(ancestorStructureInstanceBuilders.get(0) instanceof RootInstanceBuilder)) {
			throw new UnexpectedError();
		}
		RootInstanceBuilder rootInstanceBuilder = (RootInstanceBuilder) ancestorStructureInstanceBuilders.get(0);
		return ((rootInstanceBuilder.getRootInstanceDynamicTypeNameAccessor() != null)
				? new InstanceBuilder(rootInstanceBuilder.getRootInstanceDynamicTypeNameAccessor())
				: new InstanceBuilder(rootInstanceBuilder.getRootInstanceTypeName())).computeActualTypeName(
						ancestorStructureInstanceBuilders.subList(1, ancestorStructureInstanceBuilders.size()),
						solutionInstance);
	}

}