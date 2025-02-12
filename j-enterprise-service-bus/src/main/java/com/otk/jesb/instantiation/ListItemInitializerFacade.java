package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.iterable.IListTypeInfo;

public class ListItemInitializerFacade implements Facade {

	private Facade parent;
	private int index;
	private Object itemValue;

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
	public Facade getParent() {
		return parent;
	}

	public int getIndex() {
		return index;
	}

	public InstanceBuilderFacade getCurrentInstanceBuilderFacade() {
		return (InstanceBuilderFacade) Facade.getAncestors(this).stream()
				.filter(f -> (f instanceof InstanceBuilderFacade)).findFirst().get();
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

	public Function getCondition() {
		ListItemInitializer listItemInitializer = getUnderlying();
		if (listItemInitializer == null) {
			return null;
		}
		return listItemInitializer.getCondition();
	}

	public void setCondition(Function condition) {
		setConcrete(true);
		ListItemInitializer listItemInitializer = getUnderlying();
		if ((condition != null) && (condition.getFunctionBody() == null)) {
			condition = new Function("return true;");
		}
		listItemInitializer.setCondition(condition);
	}

	public Object getItemValue() {
		ListItemInitializer listItemInitializer = getUnderlying();
		if (listItemInitializer == null) {
			return null;
		}
		return MiscUtils.maintainInterpretableValue(listItemInitializer.getItemValue(), getItemType());
	}

	public void setItemValue(Object value) {
		setConcrete(true);
		ListItemInitializer listItemInitializer = getUnderlying();
		ITypeInfo itemType = getItemType();
		if ((value == null) && (itemType != null) && (itemType.isPrimitive())) {
			throw new AssertionError("Cannot add null item to primitive item list");
		}
		listItemInitializer.setItemValue(value);
	}

	public ValueMode getItemValueMode() {
		ListItemInitializer listItemInitializer = getUnderlying();
		if (listItemInitializer == null) {
			return null;
		}
		return MiscUtils.getValueMode(listItemInitializer.getItemValue());
	}

	public void setItemValueMode(ValueMode valueMode) {
		setConcrete(true);
		ListItemInitializer listItemInitializer = getUnderlying();
		if (valueMode == getItemValueMode()) {
			return;
		}
		ITypeInfo itemType = getItemType();
		itemValue = MiscUtils.getDefaultInterpretableValue(itemType, valueMode, this);
		listItemInitializer.setItemValue(itemValue);
	}

	private Object createDefaultItemValue() {
		ITypeInfo itemType = getItemType();
		return MiscUtils.getDefaultInterpretableValue(itemType, this);
	}

	public ITypeInfo getItemType() {
		ITypeInfo parentTypeInfo = getCurrentInstanceBuilderFacade().getTypeInfo();
		return ((IListTypeInfo) parentTypeInfo).getItemType();
	}

	public String getItemTypeName() {
		ITypeInfo itemType = getItemType();
		String result = (itemType == null) ? Object.class.getName() : itemType.getName();
		result = MiscUtils.makeTypeNamesRelative(result, MiscUtils.getAncestorStructureInstanceBuilders(this));
		return result;
	}

	@Override
	public ListItemInitializer getUnderlying() {
		if (index >= ((InitializationCase) parent.getUnderlying()).getListItemInitializers().size()) {
			return null;
		}
		return ((InitializationCase) parent.getUnderlying()).getListItemInitializers().get(index);
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
			((InitializationCase) parent.getUnderlying()).getListItemInitializers().add(index,
					new ListItemInitializer(itemValue));
		} else {
			((InitializationCase) parent.getUnderlying()).getListItemInitializers().remove(index);
			itemValue = createDefaultItemValue();
		}
	}

	@Override
	public List<Facade> getChildren() {
		List<Facade> result = new ArrayList<Facade>();
		if (itemValue instanceof MapEntryBuilder) {
			result.add(new MapEntryBuilderFacade(this, (MapEntryBuilder) itemValue));
		} else if (itemValue instanceof InstanceBuilder) {
			result.add(new InstanceBuilderFacade(this, (InstanceBuilder) itemValue));
		}
		return result;
	}

	public void duplicate() {
		setConcrete(true);
		((InitializationCase) parent.getUnderlying()).getListItemInitializers().add(MiscUtils.copy(getUnderlying()));
	}

	@Override
	public String toString() {
		return "[" + index + "]" + ((getItemReplicationFacade() != null) ? "*" : "")
				+ ((getCondition() != null) ? "?" : "");
	}

}