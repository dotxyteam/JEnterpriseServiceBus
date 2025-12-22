package com.otk.jesb.util;

import java.util.HashMap;
import java.util.Map;

public abstract class UpToDate<C, T> {

	private static final Object UNDEFINED = new Object() {

		@Override
		public String toString() {
			return UpToDate.class.getName() + ".UNDEFINED";
		}

	};

	private transient Map<C, Version> lastVersionByContext;

	protected abstract T obtainLatest(C context, Object versionIdentifier) throws VersionAccessException;

	protected abstract Object retrieveLastVersionIdentifier(C context);

	public T get(C context) throws VersionAccessException {
		Version lastVersion = accessLastVersion(context);
		synchronized (lastVersion) {
			try {
				Object versionIdentifier = retrieveLastVersionIdentifier(context);
				if (!MiscUtils.equalsOrBothNull(versionIdentifier, lastVersion.getIdentifier())) {
					lastVersion.setValue(obtainLatest(context, versionIdentifier));
					lastVersion.setIdentifier(versionIdentifier);
				}
				return lastVersion.getValue();
			} catch (VersionAccessException e) {
				throw e;
			} catch (Throwable t) {
				throw new VersionAccessException(t);
			}
		}
	}

	private Version accessLastVersion(C context) {
		synchronized (this) {
			if (lastVersionByContext == null) {
				lastVersionByContext = new HashMap<C, Version>();
			}
			Version result = lastVersionByContext.get(context);
			if (result == null) {
				result = new Version();
				lastVersionByContext.put(context, result);
			}
			return result;
		}
	}

	public class Version {
		private Object identifier = UNDEFINED;
		private T value;

		public Object getIdentifier() {
			return identifier;
		}

		public void setIdentifier(Object identifier) {
			this.identifier = identifier;
		}

		public T getValue() {
			return value;
		}

		public void setValue(T value) {
			this.value = value;
		}
	}

	public static class VersionAccessException extends Exception {

		private static final long serialVersionUID = 1L;

		public VersionAccessException(Throwable cause) {
			super(cause);
		}

	}

	public static abstract class GloballyUpToDate<T> extends UpToDate<Object, T> {

		protected abstract T obtainLatest(Object versionIdentifier) throws VersionAccessException;

		protected abstract Object retrieveLastVersionIdentifier();

		@Override
		protected T obtainLatest(Object context, Object versionIdentifier) throws VersionAccessException {
			return obtainLatest(versionIdentifier);
		}

		@Override
		protected Object retrieveLastVersionIdentifier(Object context) {
			return retrieveLastVersionIdentifier();
		}

		public T get() throws VersionAccessException {
			return get(UNDEFINED);
		}

	}
}
