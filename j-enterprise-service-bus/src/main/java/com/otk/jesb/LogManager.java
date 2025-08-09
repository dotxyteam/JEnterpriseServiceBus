package com.otk.jesb;

import java.io.File;
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

public class LogManager {

	static {
		// More information here:
		// https://stackoverflow.com/questions/5969321/hsqldb-messing-up-with-my-server%C2%B4s-logs
		System.setProperty("hsqldb.reconfig_logging", "false");
	}

	private Logger logger;
	private PrintStream informationStream = interceptPrintStreamData(System.out, Console.INFORMATION_LEVEL_NAME,
			() -> true);
	private PrintStream warningStream = interceptPrintStreamData(System.err, Console.WARNING_LEVEL_NAME, () -> true);
	private PrintStream errorStream = interceptPrintStreamData(System.err, Console.ERROR_LEVEL_NAME, () -> true);

	public LogManager(File file) {
		logger = createLogger(file);
	}

	public void info(String message) {
		informationStream.println(message);
	}

	public void warn(String message) {
		warningStream.println(message);
	}

	public void error(String message) {
		errorStream.println(message);
	}

	protected Logger createLogger(File file) {
		Logger logger = Logger.getLogger(JESB.class.getName());
		logger.setUseParentHandlers(false);
		String logFileNamePattern = file.getPath() + "-%g.log";
		FileHandler logFileHandler;
		try {
			logFileHandler = new FileHandler(logFileNamePattern, 1_000_000, 5, true);
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
			System.exit(-1);
			throw new UnexpectedError();
		}
		logFileHandler.setFormatter(new Formatter() {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			@Override
			public String format(LogRecord record) {
				String date = dateFormat.format(new Date(record.getMillis()));
				String level = record.getLevel().getName();
				String message = formatMessage(record);
				return String.format("[%s][%s] %s%n", date, level, message);
			}
		});
		logger.addHandler(logFileHandler);
		logger.setLevel(Level.FINE);
		return logger;
	}

	public PrintStream interceptPrintStreamData(PrintStream basePrintStream, String levelName,
			Supplier<Boolean> enablementStatusSupplier) {
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

		}.interceptPrintStreamData(basePrintStream, levelName, null, null, enablementStatusSupplier);
	};

}
