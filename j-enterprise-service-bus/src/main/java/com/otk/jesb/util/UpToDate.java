package com.otk.jesb.util;

public abstract class UpToDate<T> {

	private static final Object UNDEFINED = new Object() {

		@Override
		public String toString() {
			return UpToDate.class.getName() + ".UNDEFINED";
		}

	};

	private transient Object lastVersionIdentifier = UNDEFINED;
	private transient T latest;
	private transient Object customValue;

	protected abstract Object retrieveLastVersionIdentifier();

	protected abstract T obtainLatest(Object versionIdentifier) throws VersionAccessException;

	public Object getCustomValue() {
		return customValue;
	}

	public void setCustomValue(Object customValue) {
		this.customValue = customValue;
	}

	public synchronized T get() throws VersionAccessException {
		Object versionIdentifier = retrieveLastVersionIdentifier();
		if (!MiscUtils.equalsOrBothNull(versionIdentifier, lastVersionIdentifier)) {
			latest = obtainLatest(versionIdentifier);
			lastVersionIdentifier = versionIdentifier;
		}
		return latest;
	}

	public static class VersionAccessException extends Exception {

		private static final long serialVersionUID = 1L;

		public VersionAccessException(Throwable cause) {
			super(cause);
		}

	}

}
