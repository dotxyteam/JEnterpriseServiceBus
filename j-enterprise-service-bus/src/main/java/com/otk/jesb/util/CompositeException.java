package com.otk.jesb.util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

public class CompositeException extends Exception {

	private static final long serialVersionUID = 1L;

	private List<Entry> entries = new ArrayList<Entry>();
	private String mainMessage;

	public List<Entry> getEntries() {
		return entries;
	}

	@Override
	public String getMessage() {
		return mainMessage;
	}

	public void tryCactch(Tryable... tryables) {
		for (Tryable tryable : tryables) {
			try {
				tryable.tryIt();
			} catch (Throwable t) {
				entries.add(new Entry(new Date(), t));
			}
		}
	}

	public <T> T tryReturnCactch(ReturningTryable<T> tryable, T defaultValue) {
		try {
			return tryable.tryIt();
		} catch (Throwable t) {
			entries.add(new Entry(new Date(), t));
			return defaultValue;
		}
	}

	public <T> T tryReturnCactch(ReturningTryable<T> tryable) {
		return tryReturnCactch(tryable, null);
	}

	public void printStackTraces(PrintStream errorStream) {
		for (Entry entry : entries) {
			errorStream.append(entry.getTimestamp().toString() + ": ");
			entry.getException().printStackTrace(errorStream);
		}
	}

	public void rethrow(String mainMessage) throws CompositeException {
		if (entries.size() > 0) {
			this.mainMessage = mainMessage;
			throw this;
		}
	}

	public static void willRethrow(Consumer<CompositeException> workWithCompositeException, boolean printStackTraces,
			String mainMessage, PrintStream errorStream) throws CompositeException {
		CompositeException compositeException = new CompositeException();
		workWithCompositeException.accept(compositeException);
		if (printStackTraces) {
			compositeException.printStackTraces(errorStream);
		}
		compositeException.rethrow(mainMessage);
	}

	@FunctionalInterface
	public interface Tryable {
		void tryIt() throws Throwable;
	}

	@FunctionalInterface
	public interface ReturningTryable<T> {
		T tryIt() throws Throwable;
	}

	public static class Entry {

		private Date timestamp;
		private Throwable exception;

		public Entry(Date timestamp, Throwable exception) {
			this.timestamp = timestamp;
			this.exception = exception;
		}

		public Date getTimestamp() {
			return timestamp;
		}

		public Throwable getException() {
			return exception;
		}

	}

}
