package com.otk.jesb;

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