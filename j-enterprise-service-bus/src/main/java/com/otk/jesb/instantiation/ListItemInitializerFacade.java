package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.compiler.CompiledFunction;
import com.otk.jesb.meta.TypeInfoProvider;
import com.otk.jesb.util.InstantiationUtils;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.info.type.DefaultTypeInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.iterable.IListTypeInfo;

public class ListItemInitializerFacade extends Facade {

	private Facade parent;
	private int index;
	private Object itemValue;
	private ITypeInfo itemTypeInfo;

	public ListItemInitializerFacade(Facade parent, int index) {
		this.parent = parent;
		this.index = index;
		ListItemInitializer listItemInitializer = getUnderlying();
		if (listItemInitializer == null) {
			this.itemValue = createDefaultItemValue();
		} else {
			this.itemValue = listItemInitializer.getItemValue();
		}
	}

	@Override
	public List<VariableDeclaration> getAdditionalVariableDeclarations() {
		List<VariableDeclaration> result = new ArrayList<VariableDeclaration>(
				parent.getAdditionalVariableDeclarations());
		if (getItemReplicationFacade() != null) {
			result.add(new VariableDeclaration() {

				@Override
				public String getVariableName() {
					return getItemReplicationFacade().getIterationVariableName();
				}

				@Override
				public Class<?> getVariableType() {
					return getItemReplicationFacade().getIterationVariableClass();
				}
			});
		}
		return result;
	}

	@Override
	public Class<?> getFunctionReturnType(InstantiationFunction function) {
		if (getCondition() == function) {
			return boolean.class;
		}
		if (getItemReplicationFacade() != null) {
			if (getItemReplicationFacade().getIterationListValue() == function) {
				return getItemReplicationFacade().getIterationListValueClass();
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
			try {
				CompiledFunction.get(getCondition().getFunctionBody(), variableDeclarations,
						getFunctionReturnType(getCondition()));
			} catch (CompilationError e) {
				throw new ValidationError("Failed to validate the condition", e);
			}
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
			result = "FOR " + getItemReplicationFacade().getIterationVariableName() + " IN "
					+ InstantiationUtils.express(getItemReplicationFacade().getIterationListValue())
					+ ((result != null) ? (" LOOP " + result) : "");
		}
		if (getCondition() != null) {
			result = "IF " + InstantiationUtils.express(getCondition()) + ((result != null) ? (" THEN " + result) : "");
		}
		return result;
	}

	@Override
	public Facade getParent() {
		return parent;
	}

	public int getIndex() {
		return index;
	}

	public InstanceBuilderFacade getCurrentInstanceBuilderFacade() {
		return (InstanceBuilderFacade) Facade.getAncestors(this).stream()
				.filter(f -> (f instanceof InstanceBuilderFacade)).findFirst().orElse(null);
	}

	public ListItemReplicationFacade getItemReplicationFacade() {
		ListItemInitializer listItemInitializer = getUnderlying();
		if (listItemInitializer == null) {
			return null;
		}
		return (listItemInitializer.getItemReplication() == null) ? null
				: new ListItemReplicationFacade(listItemInitializer.getItemReplication());
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
		ListItemInitializer listItemInitializer = getUnderlying();
		if (listItemInitializer == null) {
			return null;
		}
		Object result = InstantiationUtils.maintainInterpretableValue(listItemInitializer.getItemValue(),
				getItemTypeInfo());
		if (result instanceof InstanceBuilder) {
			result = new InstanceBuilderFacade(this, (InstanceBuilder) result);
		}
		return result;
	}

	public void setItemValue(Object value) {
		if (value instanceof InstanceBuilderFacade) {
			value = ((InstanceBuilderFacade) value).getUnderlying();
		}
		setConcrete(true);
		ListItemInitializer listItemInitializer = getUnderlying();
		ITypeInfo itemType = getItemTypeInfo();
		if ((value == null) && (itemType != null) && (itemType.isPrimitive())) {
			throw new UnexpectedError("Cannot add null to a primitive item list");
		}
		listItemInitializer.setItemValue(value);
	}

	public ValueMode getItemValueMode() {
		ListItemInitializer listItemInitializer = getUnderlying();
		if (listItemInitializer == null) {
			return null;
		}
		return InstantiationUtils.getValueMode(listItemInitializer.getItemValue());
	}

	public void setItemValueMode(ValueMode valueMode) {
		setConcrete(true);
		if (valueMode == getItemValueMode()) {
			return;
		}
		ITypeInfo itemType = getItemTypeInfo();
		Object newItemValue = InstantiationUtils.getDefaultInterpretableValue(itemType, valueMode, this);
		if (newItemValue instanceof InstanceBuilder) {
			newItemValue = new InstanceBuilderFacade(this, (InstanceBuilder) newItemValue);
		}
		setItemValue(newItemValue);
	}

	public Object createDefaultItemValue() {
		ITypeInfo itemType = getItemTypeInfo();
		return InstantiationUtils.getDefaultInterpretableValue(itemType, this);
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
		ITypeInfo itemType = getItemTypeInfo();
		String result = (itemType == null) ? Object.class.getName() : itemType.getName();
		result = InstantiationUtils.makeTypeNamesRelative(result,
				InstantiationUtils.getAncestorStructuredInstanceBuilders(this));
		return result;
	}

	@Override
	public ListItemInitializer getUnderlying() {
		return ((InitializationCase) parent.getUnderlying()).getListItemInitializer(index);
	}

	@Override
	public boolean isConcrete() {
		if (!parent.isConcrete()) {
			return false;
		}
		return getUnderlying() != null;
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
			if (getUnderlying() == null) {
				makeIndexAvailable(index);
				((InitializationCase) parent.getUnderlying()).getListItemInitializers()
						.add(new ListItemInitializer(index, itemValue));
			}
		} else {
			if (getUnderlying() != null) {
				((InitializationCase) parent.getUnderlying()).removeListItemInitializer(index);
				itemValue = createDefaultItemValue();
				makeIndexUnavailable(index);
			}
		}
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

	@Override
	public List<Facade> getChildren() {
		List<Facade> result = new ArrayList<Facade>();
		if (itemValue instanceof MapEntryBuilder) {
			result.addAll(new MapEntryBuilderFacade(this, (MapEntryBuilder) itemValue).getChildren());
		} else if (itemValue instanceof InstanceBuilder) {
			result.addAll(new InstanceBuilderFacade(this, (InstanceBuilder) itemValue).getChildren());
		}
		return result;
	}

	public void duplicate() {
		setConcrete(true);
		ListItemInitializer underlyingCopy = MiscUtils.copy(getUnderlying());
		int underlyingCopyIndex = getUnderlying().getIndex() + 1;
		makeIndexAvailable(underlyingCopyIndex);
		underlyingCopy.setIndex(underlyingCopyIndex);
		((InitializationCase) parent.getUnderlying()).getListItemInitializers().add(underlyingCopy);
	}

	@Override
	public String toString() {
		return "[" + index + "]" + ((getItemReplicationFacade() != null) ? "*" : "")
				+ ((getCondition() != null) ? "?" : "");
	}

}