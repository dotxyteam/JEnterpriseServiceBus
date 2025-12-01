package com.otk.jesb;

import java.io.PrintStream;
import java.text.DateFormat;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.otk.jesb.solution.Solution;
import com.otk.jesb.util.MiscUtils;

/**
 * This class is the base for building the list of execution message records for
 * a {@link Solution}.
 * 
 * @author olitank
 *
 */
public abstract class Log {

	private static Log instance = new Log() {
		protected PrintStream createErrorStream() {
			return createPrintStream("ERROR", System.err);
		}

		protected PrintStream createWarningStream() {
			return createPrintStream("WARNING", System.err);
		}

		protected PrintStream createInformationStream() {
			return createPrintStream("INFORMATION", System.out);
		}

		private PrintStream createPrintStream(String levelName, PrintStream baseStream) {
			return MiscUtils.createBufferedPrintStream(new BiConsumer<String, Boolean>() {
				private boolean previousLineTerminated = true;

				@Override
				public void accept(String line, Boolean lineTerminated) {
					if (previousLineTerminated) {
						String date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
								.format(MiscUtils.now());
						line = "- " + date + " - " + levelName + " [" + Thread.currentThread().getName() + "] " + line;
					}
					if (lineTerminated) {
						baseStream.println(line);
					} else {
						baseStream.print(line);
					}
					previousLineTerminated = lineTerminated;
				}
			}, () -> true);
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

	public PrintStream getInformationStream() {
		return informationStream;
	}

	public PrintStream getWarningStream() {
		return warningStream;
	}

	public PrintStream getErrorStream() {
		return errorStream;
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
