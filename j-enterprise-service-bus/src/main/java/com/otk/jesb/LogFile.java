package com.otk.jesb;

import java.io.File;
import java.io.PrintStream;
import java.util.function.Supplier;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.Message;

import com.otk.jesb.util.MiscUtils;

import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.*;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;

public class LogFile extends Log {

	public static final String ERROR_LEVEL_NAME = "ERROR";
	public static final String WARNING_LEVEL_NAME = "WARNING";
	public static final String INFORMATION_LEVEL_NAME = "INFORMATION";
	public static final String VERBOSE_LEVEL_NAME = "VERBOSE";

	private static final int DEFAULT_LOGGING_HISTORY_DAYS = Integer
			.valueOf(System.getProperty(LogFile.class.getName() + ".defaultHistoryDays", "7"));
	private static final int DEFAULT_MAX_LOG_FILE_SIZE_MB = Integer
			.valueOf(System.getProperty(LogFile.class.getName() + ".defaultMaxLogFileSizeMB", "10"));

	private RollingFileAppender appender;

	public LogFile(File file) {
		this(file, DEFAULT_MAX_LOG_FILE_SIZE_MB, DEFAULT_LOGGING_HISTORY_DAYS);
	}

	public LogFile(File file, int maxLogFileSizeMB, int historyDays) {
		NullConfiguration config = new NullConfiguration();
		PatternLayout layout = PatternLayout.newBuilder().withPattern("%d [%t] %-5level %msg%n")
				.withConfiguration(config).build();
		SizeBasedTriggeringPolicy policy = SizeBasedTriggeringPolicy.createPolicy(maxLogFileSizeMB + "MB");
		DefaultRolloverStrategy strategy = DefaultRolloverStrategy.newBuilder().withMax(Integer.toString(historyDays))
				.withConfig(config).build();
		appender = RollingFileAppender.newBuilder().setName("StandaloneRolling").withFileName(file.getPath())
				.withFilePattern(file.getPath() + "-%d{yyyy-MM-dd}-%i.zip").withPolicy(policy).withStrategy(strategy)
				.setLayout(layout).setConfiguration(config).build();
		appender.start();
	}

	@Override
	protected PrintStream createErrorStream() {
		return getPrintStreamData(System.err, LogFile.ERROR_LEVEL_NAME, () -> true);
	}

	@Override
	protected PrintStream createWarningStream() {
		return getPrintStreamData(System.err, LogFile.WARNING_LEVEL_NAME, () -> true);
	}

	@Override
	protected PrintStream createInformationStream() {
		return getPrintStreamData(System.out, LogFile.INFORMATION_LEVEL_NAME, () -> true);
	}

	public PrintStream getPrintStreamData(PrintStream parallelPrintStream, String levelName,
			Supplier<Boolean> enablementStatusSupplier) {
		return MiscUtils.createBufferedPrintStream((line, lineTerminated) -> {
			if (lineTerminated) {
				parallelPrintStream.println(line);
			} else {
				parallelPrintStream.print(line);
			}
			log(line, levelName);
		}, enablementStatusSupplier);
	};

	protected void log(String message, String levelName) {
		Level level;
		if (LogFile.INFORMATION_LEVEL_NAME.equals(levelName)) {
			level = Level.INFO;
		} else if (LogFile.WARNING_LEVEL_NAME.equals(levelName)) {
			level = Level.WARN;
		} else if (LogFile.ERROR_LEVEL_NAME.equals(levelName)) {
			level = Level.ERROR;
		} else if (LogFile.VERBOSE_LEVEL_NAME.equals(levelName)) {
			level = Level.DEBUG;
		} else {
			throw new UnexpectedError();
		}
		LogEvent event = Log4jLogEvent.newBuilder().setLoggerName("MyPrivateLogger").setLevel(level)
				.setMessage(new Message() {

					private static final long serialVersionUID = 1L;

					@Override
					public Throwable getThrowable() {
						return null;
					}

					@Override
					public Object[] getParameters() {
						return null;
					}

					@Override
					public String getFormattedMessage() {
						return message;
					}
				}).setTimeMillis(System.currentTimeMillis()).build();

		appender.append(event);
	}
}
