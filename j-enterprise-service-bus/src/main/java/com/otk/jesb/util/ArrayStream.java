package com.otk.jesb.util;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ArrayStream {

	public static <T> Stream<T> get(T[] array) {
		return Arrays.stream(array);
	}

	public static Stream<Byte> get(byte[] array) {
		return IntStream.range(0, array.length).mapToObj(i -> array[i]);
	}

	public static Stream<Short> get(short[] array) {
		return IntStream.range(0, array.length).mapToObj(i -> array[i]);
	}

	public static Stream<Integer> get(int[] array) {
		return IntStream.range(0, array.length).mapToObj(i -> array[i]);
	}

	public static Stream<Long> get(long[] array) {
		return IntStream.range(0, array.length).mapToObj(i -> array[i]);
	}

	public static Stream<Float> get(float[] array) {
		return IntStream.range(0, array.length).mapToObj(i -> array[i]);
	}

	public static Stream<Double> get(double[] array) {
		return IntStream.range(0, array.length).mapToObj(i -> array[i]);
	}

	public static Stream<Character> get(char[] array) {
		return IntStream.range(0, array.length).mapToObj(i -> array[i]);
	}

	public static Stream<Boolean> get(boolean[] array) {
		return IntStream.range(0, array.length).mapToObj(i -> array[i]);
	}
}
