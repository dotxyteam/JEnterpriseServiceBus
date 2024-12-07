package com.otk.jesb;

import java.util.List;
import java.util.Map;

import xy.reflect.ui.CustomizedUI;
import xy.reflect.ui.control.swing.customizer.SwingCustomizer;

public class ObjectSpecificationTest {

	public static void main(String[] args) {
		new SwingCustomizer(new CustomizedUI(),
				"src/test/resources/" + ObjectSpecificationTest.class.getSimpleName() + ".icu").openObjectDialog(null,
						new InstanceSpecification(Tree.class.getName()));
	}

	public static class Tree {

		public Tree() {
		}

		public Tree(int intValue, String stringValue, EnumValue enumValue, List<String> stringList) {
			super();
			this.intValue = intValue;
			this.stringValue = stringValue;
			this.enumValue = enumValue;
			this.stringList = stringList;
		}

		public Tree(Tree firstChild, Tree[] otherChildrenArray, Map<String, Tree> childByString,
				Map<Integer, Boolean> booleanByInteger) {
			super();
			this.firstChild = firstChild;
			this.otherChildrenArray = otherChildrenArray;
			this.childByString = childByString;
			this.booleanByInteger = booleanByInteger;
		}

		public enum EnumValue {
			ENUM_ITEM1, ENUM_ITEM2, ENUM_ITEM3
		};

		private int intValue;
		private String stringValue;
		private EnumValue enumValue;
		private List<String> stringList;
		private Tree firstChild;
		private Tree[] otherChildrenArray;
		private Map<String, Tree> childByString;
		private Map<Integer, Boolean> booleanByInteger;

		public int getIntValue() {
			return intValue;
		}

		public void setIntValue(int intValue) {
			this.intValue = intValue;
		}

		public String getStringValue() {
			return stringValue;
		}

		public void setStringValue(String stringValue) {
			this.stringValue = stringValue;
		}

		public EnumValue getEnumValue() {
			return enumValue;
		}

		public void setEnumValue(EnumValue enumValue) {
			this.enumValue = enumValue;
		}

		public List<String> getStringList() {
			return stringList;
		}

		public void setStringList(List<String> stringList) {
			this.stringList = stringList;
		}

		public Tree getFirstChild() {
			return firstChild;
		}

		public void setFirstChild(Tree firstChild) {
			this.firstChild = firstChild;
		}

		public Tree[] getOtherChildrenArray() {
			return otherChildrenArray;
		}

		public void setOtherChildrenArray(Tree[] otherChildrenArray) {
			this.otherChildrenArray = otherChildrenArray;
		}

		public Map<String, Tree> getChildByString() {
			return childByString;
		}

		public void setChildByString(Map<String, Tree> childByString) {
			this.childByString = childByString;
		}

		public Map<Integer, Boolean> getBooleanByInteger() {
			return booleanByInteger;
		}

		public void setBooleanByInteger(Map<Integer, Boolean> booleanByInteger) {
			this.booleanByInteger = booleanByInteger;
		}

	}

}
