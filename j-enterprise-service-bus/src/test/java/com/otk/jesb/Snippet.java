package com.otk.jesb;

import java.lang.reflect.Array;
import java.util.Objects;
import java.util.function.Predicate;

public class Snippet {

	public static void main(String[] args) {
		//String[] x = new String[] {};
		newArray(String[].class, 1);
		Predicate<?> p =Objects::nonNull;
		System.out.println(p); 
	}

	public static <T> T[] newArray(Class<T[]> type, int size) {
		return type.cast(Array.newInstance(type.getComponentType(), size));
	}

}
