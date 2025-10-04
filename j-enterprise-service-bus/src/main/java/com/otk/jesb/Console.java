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
			String formattedMessage = String.format(
					"<pre><font color=\"%s\">- %s - %s [%s] </font><font color=\"%s\"><b>%s</b></font></pre>",
					prefixColor, date, levelName, Thread.currentThread().getName(), messageColor,
					MiscUtils.escapeHTML(message, false));
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

	public void clear() {
		buffer.delete(0, buffer.length());
	}

	public PrintStream interceptPrintStreamData(PrintStream basePrintStream, String levelName, final String prefixColor,
			final String messageColor, final Supplier<Boolean> enablementStatusSupplier) {
		return MiscUtils.interceptPrintStreamData(basePrintStream,
				line -> log(line, levelName, prefixColor, messageColor), enablementStatusSupplier);
	}

}