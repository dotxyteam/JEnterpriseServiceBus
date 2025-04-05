package com.otk.jesb;

import java.lang.reflect.Array;

public class Snippet {

	public static void main(String[] args) {
		//String[] x = new String[] {};
		newArray(String[].class, 1);
	}

	public static <T> T[] newArray(Class<T[]> type, int size) {
		return type.cast(Array.newInstance(type.getComponentType(), size));
	}

}
