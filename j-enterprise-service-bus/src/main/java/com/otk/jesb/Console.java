package com.otk.jesb;

import java.io.PrintStream;
import java.text.DateFormat;
import java.util.function.Supplier;
import com.otk.jesb.util.MiscUtils;

public class Console extends Log {

	public static final Console DEFAULT = new Console();

	private StringBuilder buffer = new StringBuilder();
	private int size = 100000;
	private final Object bufferMutex = new Object();

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		if ((size < 1000) || (size > 1000000)) {
			throw new IllegalArgumentException("Invalid console size: " + size);
		}
		this.size = size;
	}

	@Override
	public PrintStream createErrorStream() {
		return interceptPrintStreamData(System.err, LogFile.ERROR_LEVEL_NAME, "#FFFFFF", "#FF6E40", () -> true);
	}

	@Override
	public PrintStream createWarningStream() {
		return interceptPrintStreamData(System.err, LogFile.WARNING_LEVEL_NAME, "#FFFFFF", "#FFC13B", () -> true);
	}

	@Override
	public PrintStream createInformationStream() {
		return interceptPrintStreamData(System.out, LogFile.INFORMATION_LEVEL_NAME, "#FFFFFF", "#AAAAAA", () -> true);
	}

	protected void log(String message, String levelName, String prefixColor, String messageColor) {
		synchronized (bufferMutex) {
			String date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(MiscUtils.now());
			int MAX_MESSAGE_LENGTH = 5000;
			if (message.length() > MAX_MESSAGE_LENGTH) {
				message = message.substring(0, MAX_MESSAGE_LENGTH) + "...";
			}
			String formattedMessage = message;
			formattedMessage = "<b>" + MiscUtils.escapeHTML(formattedMessage, false) + "</b>";
			if (messageColor != null) {
				formattedMessage = "<font color=\"" + messageColor + "\">" + formattedMessage + "</font>";
			}
			if ((levelName != null) && (prefixColor != null)) {
				formattedMessage = "<font color=\"" + prefixColor + "\">" + "- " + date + " - " + levelName + " ["
						+ Thread.currentThread().getName() + "] " + "</font>" + formattedMessage;
			}
			formattedMessage = "<pre>" + formattedMessage + "</pre>";
			buffer.append(formattedMessage + "\n");
			if ((buffer.length() - size) > 0) {
				int endOfFirstLine = buffer.indexOf("\n");
				if (endOfFirstLine == -1) {
					endOfFirstLine = buffer.length();
				}
				buffer.delete(0, endOfFirstLine + "\n".length());
			}
		}
	}

	public String read() {
		return buffer.toString();
	}

	public void submitInputLine(String s) {
		if (s == null) {
			s = "";
		}
		buffer.append("<pre>" + s + "</pre>" + "\n");
		JESB.getStandardInputSource().pushLine(s);
	}

	public void clear() {
		buffer.delete(0, buffer.length());
	}

	public PrintStream interceptPrintStreamData(PrintStream basePrintStream, String levelName, final String prefixColor,
			final String messageColor, final Supplier<Boolean> enablementStatusSupplier) {
		return MiscUtils.interceptPrintStreamData(line -> {
			basePrintStream.println(line);
			log(line, levelName, prefixColor, messageColor);
		}, enablementStatusSupplier);
	}
}