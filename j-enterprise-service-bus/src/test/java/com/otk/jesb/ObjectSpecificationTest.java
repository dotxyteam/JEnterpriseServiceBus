package com.otk.jesb;

import com.otk.jesb.ObjectSpecification.ObjectSpecificationFacade;

import xy.reflect.ui.CustomizedUI;
import xy.reflect.ui.control.swing.customizer.SwingCustomizer;

public class ObjectSpecificationTest {

	public static void main(String[] args) {
		new SwingCustomizer(new CustomizedUI(),
				"src/test/resources/" + ObjectSpecificationTest.class.getSimpleName() + ".icu").openObjectDialog(null,
						new ObjectSpecificationFacade(null, new ObjectSpecification(Tree.class.getName())));
	}

	public static class Tree {

		public enum EnumValue {
			ENUM_ITEM1, ENUM_ITEM2, ENUM_ITEM3
		};

		private String stringValue;
		private EnumValue enumValue;
		private Tree child;

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

		public Tree getChild() {
			return child;
		}

		public void setChild(Tree child) {
			this.child = child;
		}

	}

}
