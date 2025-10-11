package com.otk.jesb;

import java.io.PrintStream;
import java.util.function.Supplier;

import com.otk.jesb.solution.Solution;

/**
 * This class is the base for building the list of execution message records
 * for a {@link Solution}.
 * 
 * @author olitank
 *
 */
public abstract class Log {

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

	protected abstract PrintStream createErrorStream();

	protected abstract PrintStream createWarningStream();

	protected abstract PrintStream createInformationStream();

	protected Log() {
	}

	public void info(String message) {
		informationStream.println(message);
	}

	public void warn(String message) {
		warningStream.println(message);
	}

	public void err(String message) {
		errorStream.println(message);
	}

	public void err(Throwable t) {
		t.printStackTrace(errorStream);
	}

}
