package com.otk.jesb.instantiation;

public class ListItemInitializer {

	private Object itemValue;
	private ListItemReplication itemReplication;
	private Function condition;

	public ListItemInitializer() {
	}

	public ListItemInitializer(Object itemValue) {
		this.itemValue = itemValue;
	}

	public Object getItemValue() {
		return itemValue;
	}

	public void setItemValue(Object itemValue) {
		this.itemValue = itemValue;
	}

	public Function getCondition() {
		return condition;
	}

	public void setCondition(Function condition) {
		this.condition = condition;
	}

	public ListItemReplication getItemReplication() {
		return itemReplication;
	}

	public void setItemReplication(ListItemReplication itemReplication) {
		this.itemReplication = itemReplication;
	}

}