package com.otk.jesb.instantiation;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import xy.reflect.ui.info.type.enumeration.IEnumerationTypeInfo;

public class EnumerationItemSelector {

	private List<String> itemNames;
	private String selectedItemName;

	public EnumerationItemSelector() {
	}

	public EnumerationItemSelector(List<String> itemNames) {
		this.itemNames = itemNames;
		selectedItemName = itemNames.get(0);
	}

	public List<String> getItemNames() {
		return itemNames;
	}

	public String getSelectedItemName() {
		return selectedItemName;
	}

	public void setSelectedItemName(String selectedItemName) {
		this.selectedItemName = selectedItemName;
	}

	public void configure(IEnumerationTypeInfo enumType) {
		itemNames = Arrays.asList(enumType.getValues()).stream().map(item -> enumType.getValueInfo(item).getName())
				.collect(Collectors.toList());
	}

}