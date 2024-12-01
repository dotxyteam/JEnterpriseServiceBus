package com.otk.jesb;

import java.util.List;

import xy.reflect.ui.CustomizedUI;
import xy.reflect.ui.control.swing.customizer.SwingCustomizer;

public class ObjectSpecificationTest {

	public static void main(String[] args) {
		new SwingCustomizer(new CustomizedUI(),
				"src/test/resources/" + ObjectSpecificationTest.class.getSimpleName() + ".icu").openObjectDialog(null,
						new ObjectSpecification(Tree.class.getName()));
	}

	public static class Tree {

		public enum EnumValue {
			ENUM_ITEM1, ENUM_ITEM2, ENUM_ITEM3
		};

		private int intValue;
		private String stringValue;
		private EnumValue enumValue;
		private List<String> stringList;
		private Tree firstChild;
		private Tree[] otherChildrenArray;

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

	}

}
