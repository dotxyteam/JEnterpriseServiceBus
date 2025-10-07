package com.otk.jesb;

/**
 * Allows to associate an identifier (name) with a value.
 * 
 * @author olitank
 *
 */
public interface Variable {

	public static final Object UNDEFINED_VALUE = new Object() {

		@Override
		public String toString() {
			return Variable.class.getName() + ".UNDEFINED_VALUE";
		}
	};

	Object getValue();

	String getName();

}