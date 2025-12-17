
package com.otk.jesb.util;

/**
 * Simple generic getter/setter abstract class.
 * 
 * @author olitank
 *
 * @param <C> The type of the context.
 * @param <T> The type of the value that is accessed.
 */
public abstract class Accessor<C, T> {

	/**
	 * @param context The context.
	 * @return the value.
	 */
	public abstract T get(C context);

	/**
	 * Updates the value (not supported by default).
	 * 
	 * @param context The context.
	 * @param t       The new value.
	 */
	public void set(C context, T t) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @param <T> The type that is accessed.
	 * @param t   The value.
	 * @return an instance returning the specified value.
	 */
	public static <C, T> Accessor<C, T> returning(final T t) {
		return returning(t, true);
	}

	/**
	 * @param <C>    The type of the context.
	 * @param <T>    The type of the value that is accessed.
	 * @param t      The value.
	 * @param canSet WHether the value can be updated or not.
	 * @return an instance returning and potentially allowing to update the
	 *         specified value.
	 */
	public static <C, T> Accessor<C, T> returning(T t, boolean canSet) {
		return new Accessor<C, T>() {
			BasicAccessor<T> internalAccessor = new BasicAccessor<T>(t, canSet);

			@Override
			public T get(C context) {
				return internalAccessor.get(context);
			}

			@Override
			public void set(C context, T t) {
				internalAccessor.set(context, t);
			}
		};
	}

	/**
	 * Accessor without context.
	 * 
	 * @author olitank
	 *
	 * @param <T> The type of the value that is accessed.
	 */
	public static abstract class GlobalAccessor<T> extends Accessor<Object, T> {
		/**
		 * @return the value.
		 */
		public abstract T get();

		/**
		 * Updates the value (not supported by default).
		 * 
		 * @param t The new value.
		 */
		public void set(T t) {
			throw new UnsupportedOperationException();
		}

		@Override
		public T get(Object context) {
			return get();
		}

		@Override
		public void set(Object context, T t) {
			set(t);
		}

	}

	/**
	 * Basic accessor implementation with embedded value.
	 * 
	 * @author olitank
	 *
	 * @param <TT>
	 */
	protected static class BasicAccessor<TT> extends GlobalAccessor<TT> {

		TT value;
		boolean canSet;

		public BasicAccessor(TT value, boolean canSet) {
			this.value = value;
			this.canSet = canSet;
		}

		@Override
		public TT get() {
			return value;
		}

		@Override
		public void set(TT t) {
			if (canSet) {
				value = t;
			} else {
				throw new UnsupportedOperationException();
			}
		}

	};

}
