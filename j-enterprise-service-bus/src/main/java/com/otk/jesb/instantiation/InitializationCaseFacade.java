package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.otk.jesb.JESB;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.util.InstantiationUtils;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.parameter.IParameterInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.iterable.IListTypeInfo;

public class InitializationCaseFacade extends Facade {

	private InitializationSwitchFacade parent;
	private InstantiationFunction condition;
	private InitializationCase underlying;

	public InitializationCaseFacade(InitializationSwitchFacade parent, InstantiationFunction condition,
			InitializationCase underlying) {
		this.parent = parent;
		this.condition = condition;
		this.underlying = underlying;
	}

	@Override
	public List<VariableDeclaration> getAdditionalVariableDeclarations(InstantiationFunction function,
			List<VariableDeclaration> baseVariableDeclarations) {
		if (function != null) {
			throw new UnexpectedError();
		}
		return parent.getAdditionalVariableDeclarations(null, baseVariableDeclarations);
	}

	@Override
	public Class<?> getFunctionReturnType(InstantiationFunction function,
			List<VariableDeclaration> baseVariableDeclarations) {
		if (getCondition() == function) {
			return boolean.class;
		}
		throw new UnexpectedError();
	}

	public Facade findInstantiationFunctionParentFacade(InstantiationFunction function) {
		for (Facade facade : getChildren()) {
			if (!facade.isConcrete()) {
				continue;
			}
			if (facade instanceof ParameterInitializerFacade) {
				ParameterInitializerFacade currentFacade = (ParameterInitializerFacade) facade;
				if (currentFacade.getParameterValue() == function) {
					return currentFacade;
				}
				if (currentFacade.getParameterValue() instanceof InstanceBuilderFacade) {
					Facade result = ((InstanceBuilderFacade) currentFacade.getParameterValue())
							.findInstantiationFunctionParentFacade(function);
					if (result != null) {
						return result;
					}
				}
			} else if (facade instanceof FieldInitializerFacade) {
				FieldInitializerFacade currentFacade = (FieldInitializerFacade) facade;
				if (currentFacade.getCondition() == function) {
					return currentFacade;
				}
				if (currentFacade.getFieldValue() == function) {
					return currentFacade;
				}
				if (currentFacade.getFieldValue() instanceof InstanceBuilderFacade) {
					Facade result = ((InstanceBuilderFacade) currentFacade.getFieldValue())
							.findInstantiationFunctionParentFacade(function);
					if (result != null) {
						return result;
					}
				}
			} else if (facade instanceof ListItemInitializerFacade) {
				ListItemInitializerFacade currentFacade = (ListItemInitializerFacade) facade;
				if (currentFacade.getCondition() == function) {
					return currentFacade;
				}
				if (currentFacade.getItemReplicationFacade() != null) {
					if (currentFacade.getItemReplicationFacade().getIterationListValue() == function) {
						return currentFacade;
					}
					if (currentFacade.getItemReplicationFacade()
							.getIterationListValue() instanceof InstanceBuilderFacade) {
						Facade result = ((InstanceBuilderFacade) currentFacade.getItemReplicationFacade()
								.getIterationListValue()).findInstantiationFunctionParentFacade(function);
						if (result != null) {
							return result;
						}
					}
				}
				if (currentFacade.getItemValue() == function) {
					return currentFacade;
				}
				if (currentFacade.getItemValue() instanceof InstanceBuilderFacade) {
					Facade result = ((InstanceBuilderFacade) currentFacade.getItemValue())
							.findInstantiationFunctionParentFacade(function);
					if (result != null) {
						return result;
					}
				}
			} else if (facade instanceof InitializationSwitchFacade) {
				InitializationSwitchFacade currentFacade = (InitializationSwitchFacade) facade;
				for (Facade childFacade : currentFacade.getChildren()) {
					InitializationCaseFacade caseFacade = (InitializationCaseFacade) childFacade;
					if (caseFacade.getCondition() == function) {
						return childFacade;
					}
					Facade result = caseFacade.findInstantiationFunctionParentFacade(function);
					if (result != null) {
						return result;
					}
				}
			}
		}
		return null;
	}

	@Override
	public void validate(boolean recursively, List<VariableDeclaration> variableDeclarations) throws ValidationError {
		if (!isConcrete()) {
			return;
		}
		if (recursively) {
			for (Facade facade : getChildren()) {
				try {
					facade.validate(recursively, variableDeclarations);
				} catch (ValidationError e) {
					throw new ValidationError("Failed to validate '" + facade.toString() + "'", e);
				}
			}
		}
	}

	@Override
	public String express() {
		return InstantiationUtils.express(getCondition());
	}

	public InstantiationFunction getCondition() {
		return condition;
	}

	private int getIndex() {
		return parent.getChildren().stream().map(facade -> facade.getUnderlying()).collect(Collectors.toList())
				.indexOf(underlying);
	}

	public void duplicate() {
		InstantiationFunction conditionCopy;
		if (isDefault()) {
			conditionCopy = InitializationCase.createDefaultCondition();
		} else {
			conditionCopy = MiscUtils.copy(condition);
		}
		InitializationCase underlyingCopy = MiscUtils.copy(getUnderlying());
		MiscUtils.add(parent.getUnderlying().getInitializationCaseByCondition(), getIndex(), conditionCopy,
				underlyingCopy);
	}

	public void insertNewSibling() {
		InstantiationFunction newSiblingCondition = InitializationCase.createDefaultCondition();
		InitializationCase newSiblingUnderlying = new InitializationCase();
		MiscUtils.add(parent.getUnderlying().getInitializationCaseByCondition(), getIndex(), newSiblingCondition,
				newSiblingUnderlying);
	}

	public boolean canMoveUp() {
		if (isDefault()) {
			return false;
		}
		return getIndex() > 0;
	}

	public boolean canMoveDown() {
		if (isDefault()) {
			return false;
		}
		return getIndex() < (parent.getUnderlying().getInitializationCaseByCondition().size() - 1);
	}

	public void moveUp() {
		if (!canMoveUp()) {
			return;
		}
		int index = getIndex();
		parent.getUnderlying().getInitializationCaseByCondition().remove(condition);
		MiscUtils.add(parent.getUnderlying().getInitializationCaseByCondition(), index - 1, condition, underlying);
	}

	public void moveDown() {
		if (!canMoveDown()) {
			return;
		}
		int index = getIndex();
		parent.getUnderlying().getInitializationCaseByCondition().remove(condition);
		MiscUtils.add(parent.getUnderlying().getInitializationCaseByCondition(), index + 1, condition, underlying);
	}

	@Override
	public List<Facade> getChildren() {
		List<Facade> result = new ArrayList<Facade>();
		for (InitializationSwitch initializationSwitch : underlying.getInitializationSwitches()) {
			result.add(createInitializationSwitchFacade(initializationSwitch));
		}
		InstanceBuilderFacade instanceBuilderFacade = getCurrentInstanceBuilderFacade();
		ITypeInfo typeInfo;
		try {
			typeInfo = instanceBuilderFacade.getTypeInfo();
		} catch (Throwable t) {
			if (JESB.DEBUG) {
				t.printStackTrace();
			}
			return Collections.emptyList();
		}
		IMethodInfo constructor = InstantiationUtils.getConstructorInfo(typeInfo,
				instanceBuilderFacade.getSelectedConstructorSignature());
		if (constructor != null) {
			for (IParameterInfo parameterInfo : constructor.getParameters()) {
				if (!mustHaveParameterFacadeLocally(parameterInfo)) {
					continue;
				}
				result.add(createParameterInitializerFacade(parameterInfo.getPosition()));
			}
		}
		if (typeInfo instanceof IListTypeInfo) {
			if (mustHaveListItemFacadesLocally()) {
				for (ListItemInitializer initializer : underlying.getListItemInitializers()) {
					result.add(createListItemInitializerFacade(initializer.getIndex()));
				}
				result.add(createListItemInitializerFacade(getGreatestListItemInitializerIndex() + 1));
			}
		} else {
			for (IFieldInfo fieldInfo : typeInfo.getFields()) {
				if (fieldInfo.isGetOnly()) {
					continue;
				}
				if (JESB.DEBUG) {
					if (result.stream().anyMatch(facade -> (facade instanceof ParameterInitializerFacade)
							&& ((ParameterInitializerFacade) facade).getParameterName().equals(fieldInfo.getName()))) {
						System.out.println("Name conflict detected between parameter '" + fieldInfo.getName()
								+ "' and editable field '" + fieldInfo.getName() + "'");
					}
				}
				if (!mustHaveFieldFacadeLocally(fieldInfo)) {
					continue;
				}
				result.add(createFieldInitializerFacade(fieldInfo.getName()));
			}
		}
		Collections.sort(result, new Comparator<Facade>() {
			List<Class<?>> CLASSES_ORDER = Arrays.asList(ParameterInitializerFacade.class, FieldInitializerFacade.class,
					ListItemInitializerFacade.class, InitializationSwitchFacade.class);

			@Override
			public int compare(Facade o1, Facade o2) {
				if (o1 instanceof InitializationSwitchFacade) {
					List<Facade> managedFacades = ((InitializationSwitchFacade) o1).getManagedInitializerFacades();
					if (managedFacades.size() > 0) {
						o1 = managedFacades.get(0);
					}
				}
				if (o2 instanceof InitializationSwitchFacade) {
					List<Facade> managedFacades = ((InitializationSwitchFacade) o2).getManagedInitializerFacades();
					if (managedFacades.size() > 0) {
						o2 = managedFacades.get(0);
					}
				}
				if (!o1.getClass().equals(o2.getClass())) {
					return Integer.valueOf(CLASSES_ORDER.indexOf(o1.getClass()))
							.compareTo(Integer.valueOf(CLASSES_ORDER.indexOf(o2.getClass())));
				}
				if ((o1 instanceof ParameterInitializerFacade) && (o2 instanceof ParameterInitializerFacade)) {
					ParameterInitializerFacade pif1 = (ParameterInitializerFacade) o1;
					ParameterInitializerFacade pif2 = (ParameterInitializerFacade) o2;
					return Integer.valueOf(pif1.getParameterPosition())
							.compareTo(Integer.valueOf(pif2.getParameterPosition()));
				} else if ((o1 instanceof FieldInitializerFacade) && (o2 instanceof FieldInitializerFacade)) {
					FieldInitializerFacade fif1 = (FieldInitializerFacade) o1;
					FieldInitializerFacade fif2 = (FieldInitializerFacade) o2;
					return fif1.getFieldInfo().getName().compareTo(fif2.getFieldInfo().getName());
				} else if ((o1 instanceof ListItemInitializerFacade) && (o2 instanceof ListItemInitializerFacade)) {
					ListItemInitializerFacade liif1 = (ListItemInitializerFacade) o1;
					ListItemInitializerFacade liif2 = (ListItemInitializerFacade) o2;
					return Integer.valueOf(liif1.getIndex()).compareTo(Integer.valueOf(liif2.getIndex()));
				} else if ((o1 instanceof InitializationSwitchFacade) && (o2 instanceof InitializationSwitchFacade)) {
					return 0;
				} else {
					throw new UnexpectedError();
				}

			}
		});
		return result;
	}

	protected int getGreatestListItemInitializerIndex() {
		int result = -1;
		for (ListItemInitializer listItemInitializer : underlying.getListItemInitializers()) {
			result = Math.max(result, listItemInitializer.getIndex());
		}
		for (InitializationSwitch initializationSwitch : underlying.getInitializationSwitches()) {
			InitializationSwitchFacade switchFacade = new InitializationSwitchFacade(this, initializationSwitch);
			for (InitializationCaseFacade caseFacade : switchFacade.getChildren()) {
				result = Math.max(result, caseFacade.getGreatestListItemInitializerIndex());
			}
		}
		return result;
	}

	protected InitializationSwitchFacade createInitializationSwitchFacade(InitializationSwitch initializationSwitch) {
		return new InitializationSwitchFacade(this, initializationSwitch);
	}

	protected FieldInitializerFacade createFieldInitializerFacade(String fieldName) {
		return new FieldInitializerFacade(this, fieldName);
	}

	protected ListItemInitializerFacade createListItemInitializerFacade(int index) {
		return new ListItemInitializerFacade(this, index);
	}

	protected ParameterInitializerFacade createParameterInitializerFacade(int parameterPosition) {
		return new ParameterInitializerFacade(this, parameterPosition);
	}

	protected boolean isFieldInitializedInChildSwitch(IFieldInfo fieldInfo) {
		for (InitializationSwitch initializationSwitch : underlying.getInitializationSwitches()) {
			InitializationSwitchFacade switchFacade = new InitializationSwitchFacade(this, initializationSwitch);
			InitializationCaseFacade defaultCaseFacade = new InitializationCaseFacade(switchFacade, null,
					switchFacade.getUnderlying().getDefaultInitializationCase());
			if (defaultCaseFacade.getUnderlying().getFieldInitializer(fieldInfo.getName()) != null) {
				return true;
			}
			if (defaultCaseFacade.isFieldInitializedInChildSwitch(fieldInfo)) {
				return true;
			}
		}
		return false;
	}

	protected boolean isParameterInitializedInChildSwitch(IParameterInfo parameterInfo) {
		for (InitializationSwitch initializationSwitch : underlying.getInitializationSwitches()) {
			InitializationSwitchFacade switchFacade = new InitializationSwitchFacade(this, initializationSwitch);
			InitializationCaseFacade defaultCaseFacade = new InitializationCaseFacade(switchFacade, null,
					switchFacade.getUnderlying().getDefaultInitializationCase());
			if (defaultCaseFacade.getUnderlying().getParameterInitializer(parameterInfo.getPosition()) != null) {
				return true;
			}
			if (defaultCaseFacade.isParameterInitializedInChildSwitch(parameterInfo)) {
				return true;
			}
		}
		return false;
	}

	protected boolean areListItemsInitializedInChildSwitch() {
		for (InitializationSwitch initializationSwitch : underlying.getInitializationSwitches()) {
			InitializationSwitchFacade switchFacade = new InitializationSwitchFacade(this, initializationSwitch);
			InitializationCaseFacade defaultCaseFacade = new InitializationCaseFacade(switchFacade, null,
					switchFacade.getUnderlying().getDefaultInitializationCase());
			if (defaultCaseFacade.getUnderlying().getListItemInitializers().size() > 0) {
				return true;
			}
			if (defaultCaseFacade.areListItemsInitializedInChildSwitch()) {
				return true;
			}
		}
		return false;
	}

	protected boolean mustHaveParameterFacadeLocally(IParameterInfo parameterInfo) {
		if (isParameterInitializedInChildSwitch(parameterInfo)) {
			return false;
		}
		if (underlying.getParameterInitializer(parameterInfo.getPosition()) != null) {
			return true;
		}
		for (InitializationCaseFacade siblingCaseFacade : parent.getChildren()) {
			if (underlying != siblingCaseFacade.getUnderlying()) {
				if (siblingCaseFacade.isParameterInitializedInChildSwitch(parameterInfo)) {
					return true;
				}
				if (siblingCaseFacade.getUnderlying().getParameterInitializer(parameterInfo.getPosition()) != null) {
					return true;
				}
			}
		}
		return false;
	}

	protected boolean mustHaveFieldFacadeLocally(IFieldInfo fieldInfo) {
		if (isFieldInitializedInChildSwitch(fieldInfo)) {
			return false;
		}
		if (underlying.getFieldInitializer(fieldInfo.getName()) != null) {
			return true;
		}
		for (InitializationCaseFacade siblingCaseFacade : parent.getChildren()) {
			if (underlying != siblingCaseFacade.getUnderlying()) {
				if (siblingCaseFacade.isFieldInitializedInChildSwitch(fieldInfo)) {
					return true;
				}
				if (siblingCaseFacade.getUnderlying().getFieldInitializer(fieldInfo.getName()) != null) {
					return true;
				}
			}
		}
		return false;
	}

	protected boolean mustHaveListItemFacadesLocally() {
		if (areListItemsInitializedInChildSwitch()) {
			return true;
		}
		if (underlying.getListItemInitializers().size() > 0) {
			return true;
		}
		for (InitializationCaseFacade siblingCaseFacade : parent.getChildren()) {
			if (underlying != siblingCaseFacade.getUnderlying()) {
				if (siblingCaseFacade.getUnderlying().getListItemInitializers().size() > 0) {
					return true;
				}
				if (siblingCaseFacade.areListItemsInitializedInChildSwitch()) {
					return true;
				}
			}
		}
		return false;
	}

	public InstanceBuilderFacade getCurrentInstanceBuilderFacade() {
		return (InstanceBuilderFacade) Facade.getAncestors(this).stream()
				.filter(f -> (f instanceof InstanceBuilderFacade)).findFirst().orElse(null);
	}

	public boolean isDefault() {
		return condition == null;
	}

	@Override
	public boolean isConcrete() {
		if (!parent.isConcrete()) {
			return false;
		}
		if (isDefault()) {
			return parent.getUnderlying().getDefaultInitializationCase() == underlying;
		} else {
			return parent.getUnderlying().getInitializationCaseByCondition().get(condition) == underlying;
		}
	}

	@Override
	public void setConcrete(boolean b) {
		if (b == isConcrete()) {
			return;
		}
		if (b) {
			if (!parent.isConcrete()) {
				parent.setConcrete(true);
			}
			if (isDefault()) {
				if (parent.getUnderlying().getDefaultInitializationCase() != underlying) {
					parent.getUnderlying().setDefaultInitializationCase(underlying);
				}
			} else {
				if (!parent.getUnderlying().getInitializationCaseByCondition().containsKey(condition)) {
					parent.getUnderlying().getInitializationCaseByCondition().put(condition, underlying);
				}
			}
		} else {
			if (isDefault()) {
				if (parent.getUnderlying().getDefaultInitializationCase() == underlying) {
					for (Object childUnderlying : getChildren().stream().map(facade -> facade.getUnderlying())
							.filter(Objects::nonNull).toArray()) {
						Facade.get(childUnderlying, this).setConcrete(false);
					}
				}
			} else {
				if (parent.getUnderlying().getInitializationCaseByCondition().containsKey(condition)) {
					for (Object childUnderlying : getChildren().stream().map(facade -> facade.getUnderlying())
							.filter(Objects::nonNull).toArray()) {
						Facade.get(childUnderlying, this).setConcrete(false);
					}
					parent.getUnderlying().getInitializationCaseByCondition().remove(condition);
				}
			}
		}
	}

	@Override
	public InitializationCase getUnderlying() {
		return underlying;
	}

	@Override
	public InitializationSwitchFacade getParent() {
		return parent;
	}

	public List<Facade> collectLiveInitializerFacades(InstantiationContext context) {
		List<Facade> result = new ArrayList<Facade>();
		for (Facade facade : getChildren()) {
			if (!facade.isConcrete()) {
				continue;
			}
			if (facade instanceof ParameterInitializerFacade) {
				result.add(facade);
			} else if (facade instanceof FieldInitializerFacade) {
				result.add(facade);
			} else if (facade instanceof ListItemInitializerFacade) {
				result.add(facade);
			} else if (facade instanceof InitializationSwitchFacade) {
				result.addAll(((InitializationSwitchFacade) facade)
						.collectLiveInitializerFacades(createInstantiationContextForChildren(context)));
			}
		}
		return result;
	}

	protected InstantiationContext createInstantiationContextForChildren(InstantiationContext context) {
		return new InstantiationContext(context, this);
	}

	@Override
	public String toString() {
		if (isDefault()) {
			return "[Default]";
		} else {
			return "[Case]";
		}
	}
}