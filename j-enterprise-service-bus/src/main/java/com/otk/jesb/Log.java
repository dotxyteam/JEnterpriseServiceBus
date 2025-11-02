package com.otk.jesb;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.function.Supplier;

import com.otk.jesb.solution.Solution;
import com.otk.jesb.util.DuplicatedInputStreamSource;

/**
 * This class is the base for building the list of execution message records for
 * a {@link Solution}.
 * 
 * @author olitank
 *
 */
public abstract class Log {

	public static final DuplicatedInputStreamSource STANDARD_INPUT_SOURCE = new DuplicatedInputStreamSource(System.in);

	private static Log instance = new Log() {
		protected PrintStream createErrorStream() {
			return System.err;
		}

		protected PrintStream createWarningStream() {
			return System.err;
		}

		protected PrintStream createInformationStream() {
			return System.out;
		}

		@Override
		protected InputStream createInputStream() {
			return STANDARD_INPUT_SOURCE.newInputStream();
		}
	};
	private static Supplier<Boolean> verbosityStatusSupplier = () -> false;

	public static Log get() {
		return instance;
	}

	public static void set(Log instance) {
		Log.instance = instance;
	}

	public static Supplier<Boolean> getVerbosityStatusSupplier() {
		return verbosityStatusSupplier;
	}

	public static void setVerbosityStatusSupplier(Supplier<Boolean> verbosityStatusSupplier) {
		Log.verbosityStatusSupplier = verbosityStatusSupplier;
	}

	public static boolean isVerbose() {
		return verbosityStatusSupplier.get();
	}

	private PrintStream informationStream = createInformationStream();
	private PrintStream warningStream = createWarningStream();
	private PrintStream errorStream = createErrorStream();
	private InputStream inputStream = createInputStream();;

	protected abstract PrintStream createErrorStream();

	protected abstract PrintStream createWarningStream();

	protected abstract PrintStream createInformationStream();

	protected abstract InputStream createInputStream();

	protected Log() {
	}

	public PrintStream getInformationStream() {
		return informationStream;
	}

	public PrintStream getWarningStream() {
		return warningStream;
	}

	public PrintStream getErrorStream() {
		return errorStream;
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public void information(String message) {
		informationStream.println(message);
	}

	public void warning(String message) {
		warningStream.println(message);
	}

	public void error(String message) {
		errorStream.println(message);
	}

	public void error(Throwable t) {
		t.printStackTrace(errorStream);
	}

}
