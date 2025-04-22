package com.otk.jesb.instantiation;

public class ListItemInitializer {

	private int index;
	private Object itemValue;
	private ListItemReplication itemReplication;
	private InstantiationFunction condition;

	public ListItemInitializer() {
	}

	public ListItemInitializer(int index, Object itemValue) {
		this.index = index;
		this.itemValue = itemValue;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public Object getItemValue() {
		return itemValue;
	}

	public void setItemValue(Object itemValue) {
		this.itemValue = itemValue;
	}

	public InstantiationFunction getCondition() {
		return condition;
	}

	public void setCondition(InstantiationFunction condition) {
		this.condition = condition;
	}

	public ListItemReplication getItemReplication() {
		return itemReplication;
	}

	public void setItemReplication(ListItemReplication itemReplication) {
		this.itemReplication = itemReplication;
	}

}