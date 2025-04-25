package com.otk.jesb.util;

public abstract class UpToDate<T> {

	private static final Object UNDEFINED = new Object() {

		@Override
		public String toString() {
			return UpToDate.class.getName() + ".UNDEFINED";
		}

	};

	private Object lastModificationIdentifier = UNDEFINED;
	private T latest;
	private Object customValue;

	protected abstract Object retrieveLastModificationIdentifier();

	protected abstract T obtainLatest();

	public Object getCustomValue() {
		return customValue;
	}

	public void setCustomValue(Object customValue) {
		this.customValue = customValue;
	}

	public T get() {
		Object identifier = retrieveLastModificationIdentifier();
		if (!MiscUtils.equalsOrBothNull(identifier, lastModificationIdentifier)) {
			latest = obtainLatest();
			lastModificationIdentifier = identifier;
		}
		return latest;
	}

}
