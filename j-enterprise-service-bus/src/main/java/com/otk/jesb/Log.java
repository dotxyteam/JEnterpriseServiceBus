package com.otk.jesb;

import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Supplier;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Log {

	public static final Log INSTANCE = new Log();

	private Logger logger = createLogger();
	private PrintStream infoStream = getLoggingPrintStream(Console.INFORMATION_LEVEL_NAME, () -> true, System.out);
	private PrintStream warningStream = getLoggingPrintStream(Console.WARNING_LEVEL_NAME, () -> true, System.err);
	private PrintStream errorStream = getLoggingPrintStream(Console.ERROR_LEVEL_NAME, () -> true, System.out);

	private Log() {
	}

	public void info(String message) {
		infoStream.println(message);
	}

	public void warn(String message) {
		warningStream.println(message);
	}

	public void error(String message) {
		errorStream.println(message);
	}

	protected Logger createLogger() {
		Logger logger = Logger.getLogger(JESB.class.getName());
		logger.setUseParentHandlers(false);
		String logFileNamePattern = JESB.class.getSimpleName() + "-%g.log";
		FileHandler logFileHandler;
		try {
			logFileHandler = new FileHandler(logFileNamePattern, 1_000_000, 5, true);
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
			System.exit(-1);
			throw new UnexpectedError();
		}
		logFileHandler.setFormatter(new Formatter() {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			@Override
			public String format(LogRecord record) {
				String date = sdf.format(new Date(record.getMillis()));
				String level = record.getLevel().getName();
				String message = formatMessage(record);
				return String.format("[%s][%s] %s%n", date, level, message);
			}
		});
		logger.addHandler(logFileHandler);
		logger.setLevel(Level.FINE);
		return logger;
	}

	public PrintStream getLoggingPrintStream(String levelName, Supplier<Boolean> enablementStatusSupplier,
			PrintStream rawMessagePrintStream) {
		return new Console() {

			@Override
			public void log(String message, String levelName, String prefixColor, String messageColor) {
				if (Console.INFORMATION_LEVEL_NAME.equals(levelName)) {
					logger.info(message);
				} else if (Console.WARNING_LEVEL_NAME.equals(levelName)) {
					logger.warning(message);
				} else if (Console.ERROR_LEVEL_NAME.equals(levelName)) {
					logger.severe(message);
				} else if (Console.VERBOSE_LEVEL_NAME.equals(levelName)) {
					logger.fine(message);
				} else {
					throw new UnexpectedError();
				}
			}

		}.getPrintStream(levelName, null, null, enablementStatusSupplier, rawMessagePrintStream);
	};

}
