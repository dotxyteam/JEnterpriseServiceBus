package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.meta.TypeInfoProvider;
import com.otk.jesb.util.InstantiationUtils;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.info.type.DefaultTypeInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.iterable.IListTypeInfo;

public class ListItemInitializerFacade extends InitializerFacade {

	private int index;
	private ITypeInfo itemTypeInfo;

	public ListItemInitializerFacade(Facade parent, int index) {
		super(parent);
		this.index = index;
	}

	@Override
	public List<VariableDeclaration> getAdditionalVariableDeclarations(InstantiationFunction function,
			List<VariableDeclaration> baseVariableDeclarations) {
		List<VariableDeclaration> baseResult = getParent().getAdditionalVariableDeclarations(null,
				baseVariableDeclarations);
		List<VariableDeclaration> result;
		if (getItemReplicationFacade() == null) {
			result = baseResult;
		} else {
			result = new ArrayList<VariableDeclaration>(baseResult);
			result.add(new VariableDeclaration() {

				@Override
				public String getVariableName() {
					return getItemReplicationFacade().getIterationVariableName();
				}

				@Override
				public Class<?> getVariableType() {
					ListItemReplicationFacade itemReplicationFacade = getItemReplicationFacade();
					ITypeInfo declaredIterationVariableType = itemReplicationFacade
							.getDeclaredIterationVariableTypeInfo();
					if (declaredIterationVariableType != null) {
						return ((DefaultTypeInfo) declaredIterationVariableType).getJavaType();
					}
					ITypeInfo guessedIterationVariableType = itemReplicationFacade
							.guessIterationVariableTypeInfo(baseVariableDeclarations);
					if (guessedIterationVariableType != null) {
						return ((DefaultTypeInfo) guessedIterationVariableType).getJavaType();
					}
					return Object.class;
				}
			});
		}
		if (function == null) {
			return result;
		}
		if (getCondition() == function) {
			return baseResult;
		}
		if (getItemReplicationFacade() != null) {
			if (getItemReplicationFacade().getIterationListValue() == function) {
				return baseResult;
			}
		}
		if (getItemValue() == function) {
			return result;
		}
		throw new UnexpectedError();
	}

	@Override
	public Class<?> getFunctionReturnType(InstantiationFunction function,
			List<VariableDeclaration> baseVariableDeclarations) {
		if (getCondition() == function) {
			return boolean.class;
		}
		if (getItemReplicationFacade() != null) {
			if (getItemReplicationFacade().getIterationListValue() == function) {
				return Object.class;
			}
		}
		if (getItemValue() == function) {
			return ((DefaultTypeInfo) getItemTypeInfo()).getJavaType();
		}
		throw new UnexpectedError();
	}

	@Override
	public void validate(boolean recursively, List<VariableDeclaration> variableDeclarations) throws ValidationError {
		if (!isConcrete()) {
			return;
		}
		if (getCondition() != null) {
			InstantiationUtils.validateValue(getCondition(), TypeInfoProvider.getTypeInfo(boolean.class), this,
					"condition", true, variableDeclarations);
		}
		if (getItemReplicationFacade() != null) {
			getItemReplicationFacade().validate(variableDeclarations);
		}
		InstantiationUtils.validateValue(getUnderlying().getItemValue(), getItemTypeInfo(), this, "item value",
				recursively, variableDeclarations);
	}

	@Override
	public String express() {
		Object value = getItemValue();
		if (value instanceof InstanceBuilderFacade) {
			value = ((InstanceBuilderFacade) value).getUnderlying();
		}
		String result = InstantiationUtils.express(value);
		if (getItemReplicationFacade() != null) {
			result = getItemReplicationFacade().preprendExpression(result);
		}
		if (getCondition() != null) {
			result = "IF " + InstantiationUtils.express(getCondition()) + ((result != null) ? (" THEN " + result) : "");
		}
		return result;
	}

	public int getIndex() {
		return index;
	}

	public ListItemReplicationFacade getItemReplicationFacade() {
		ListItemInitializer listItemInitializer = getUnderlying();
		if (listItemInitializer == null) {
			return null;
		}
		return (listItemInitializer.getItemReplication() == null) ? null
				: new ListItemReplicationFacade(this, listItemInitializer.getItemReplication());
	}

	public void setItemReplicationFacade(ListItemReplicationFacade itemReplicationFacade) {
		setConcrete(true);
		ListItemInitializer listItemInitializer = getUnderlying();
		listItemInitializer
				.setItemReplication((itemReplicationFacade == null) ? null : itemReplicationFacade.getUnderlying());
	}

	public InstantiationFunction getCondition() {
		ListItemInitializer listItemInitializer = getUnderlying();
		if (listItemInitializer == null) {
			return null;
		}
		return listItemInitializer.getCondition();
	}

	public void setCondition(InstantiationFunction condition) {
		setConcrete(true);
		ListItemInitializer listItemInitializer = getUnderlying();
		if ((condition != null) && (condition.getFunctionBody() == null)) {
			condition = new InstantiationFunction("return true;");
		}
		listItemInitializer.setCondition(condition);
	}

	public Object getItemValue() {
		return super.getValue();
	}

	public void setItemValue(Object value) {
		super.setValue(value);
	}

	public ValueMode getItemValueMode() {
		return super.getValueMode();
	}

	public void setItemValueMode(ValueMode valueMode) {
		super.setValueMode(valueMode);
	}

	public Object createDefaultItemValue() {
		return super.createDefaultValue();
	}

	public ITypeInfo getItemTypeInfo() {
		if (itemTypeInfo == null) {
			ITypeInfo parentTypeInfo = getCurrentInstanceBuilderFacade().getTypeInfo();
			ITypeInfo result = ((IListTypeInfo) parentTypeInfo).getItemType();
			if (result == null) {
				result = TypeInfoProvider.getTypeInfo(Object.class.getName());
			}
			itemTypeInfo = result;
		}
		return itemTypeInfo;
	}

	public String getItemTypeName() {
		return super.getValueTypeName();
	}

	@Override
	public ListItemInitializer getUnderlying() {
		return ((InitializationCase) getParent().getUnderlying()).getListItemInitializer(index);
	}

	@Override
	protected Object retrieveInitializerValue(Object initializer) {
		return ((ListItemInitializer) initializer).getItemValue();
	}

	@Override
	protected void updateInitializerValue(Object initializer, Object newValue) {
		((ListItemInitializer) initializer).setItemValue(newValue);
	}

	@Override
	protected ITypeInfo getValueType() {
		return getItemTypeInfo();
	}

	@Override
	protected void createUnderlying(Object value) {
		((InitializationCase) getParent().getUnderlying()).getListItemInitializers()
				.add(new ListItemInitializer(index, value));
	}

	@Override
	protected void deleteUnderlying() {
		((InitializationCase) getParent().getUnderlying()).removeListItemInitializer(index);
	}

	@Override
	public void setConcrete(boolean b) {
		if (b) {
			if (getUnderlying() == null) {
				makeIndexAvailable(index);
			}
		} else {
			if (getUnderlying() != null) {
				makeIndexUnavailable(index);
			}
		}
		super.setConcrete(b);
	}

	protected void makeIndexAvailable(int index) {
		if (isIndexAvailable(index)) {
			return;
		}
		for (ListItemInitializer listItemInitializer : collectAllListItemInitializers()) {
			if (listItemInitializer.getIndex() >= index) {
				listItemInitializer.setIndex(listItemInitializer.getIndex() + 1);
			}
		}
	}

	protected void makeIndexUnavailable(int index) {
		if (!isIndexAvailable(index)) {
			return;
		}
		for (ListItemInitializer listItemInitializer : collectAllListItemInitializers()) {
			if (listItemInitializer.getIndex() >= index) {
				listItemInitializer.setIndex(listItemInitializer.getIndex() - 1);
			}
		}
	}

	protected boolean isIndexAvailable(int index) {
		for (ListItemInitializer listItemInitializer : collectAllListItemInitializers()) {
			if (listItemInitializer.getIndex() == index) {
				return false;
			}
		}
		return true;
	}

	protected List<ListItemInitializer> collectAllListItemInitializers() {
		return collectAllConcreteListItemInitializerFacades(getCurrentInstanceBuilderFacade().getChildren()).stream()
				.map(facade -> facade.getUnderlying()).collect(Collectors.toList());
	}

	protected List<ListItemInitializerFacade> collectAllConcreteListItemInitializerFacades(List<Facade> facades) {
		List<ListItemInitializerFacade> result = new ArrayList<ListItemInitializerFacade>();
		for (Facade facade : facades) {
			if (!facade.isConcrete()) {
				continue;
			}
			if (facade instanceof ListItemInitializerFacade) {
				result.add((ListItemInitializerFacade) facade);
			}
			if (facade instanceof InitializationSwitchFacade) {
				for (InitializationCaseFacade caseFacade : ((InitializationSwitchFacade) facade).getChildren()) {
					result.addAll(collectAllConcreteListItemInitializerFacades(caseFacade.getChildren()));
				}
			}
		}
		return result;
	}

	public void duplicate() {
		setConcrete(true);
		ListItemInitializer underlyingCopy = MiscUtils.copy(getUnderlying());
		int underlyingCopyIndex = getUnderlying().getIndex() + 1;
		makeIndexAvailable(underlyingCopyIndex);
		underlyingCopy.setIndex(underlyingCopyIndex);
		((InitializationCase) getParent().getUnderlying()).getListItemInitializers().add(underlyingCopy);
	}

	@Override
	public String toString() {
		return "[" + index + "]" + ((getItemReplicationFacade() != null) ? "*" : "")
				+ ((getCondition() != null) ? "?" : "");
	}

}