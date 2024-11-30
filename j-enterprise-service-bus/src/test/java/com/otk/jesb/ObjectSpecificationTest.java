package com.otk.jesb;

import com.otk.jesb.ObjectSpecification.ObjectSpecificationFacade;

import xy.reflect.ui.CustomizedUI;
import xy.reflect.ui.control.swing.customizer.SwingCustomizer;

public class ObjectSpecificationTest {

	public static void main(String[] args) {
		new SwingCustomizer(new CustomizedUI(),
				"src/test/resources/" + ObjectSpecificationTest.class.getSimpleName() + ".icu").openObjectDialog(null,
						new ObjectSpecificationFacade(null,
								new ObjectSpecification(Tree.class.getName())));
	}

	public static class Tree {

		private String value;
		private Tree child;

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public Tree getChild() {
			return child;
		}

		public void setChild(Tree child) {
			this.child = child;
		}

	}

}
