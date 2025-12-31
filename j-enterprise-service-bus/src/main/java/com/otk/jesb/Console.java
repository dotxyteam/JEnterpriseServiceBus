package com.otk.jesb;

import java.io.PrintStream;
import java.text.DateFormat;
import java.util.function.Consumer;
import java.util.function.Supplier;
import com.otk.jesb.util.MiscUtils;

public class Console extends Log {

	public static final Console DEFAULT = new Console();

	private StringBuilder buffer = new StringBuilder();
	private int size = 100000;
	private final Object bufferMutex = new Object();
	private String pendingInputLine;
	private final Consumer<String> defaultPendingInputLineConsumer = line -> log(line, true, null, null, null);
	private Consumer<String> pendingInputLineConsumer = defaultPendingInputLineConsumer;

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		if ((size < 1000) || (size > 1000000)) {
			throw new IllegalArgumentException("Invalid console size: " + size);
		}
		this.size = size;
	}

	public String getPendingInputLine() {
		return pendingInputLine;
	}

	public void setPendingInputLine(String pendingInputLine) {
		this.pendingInputLine = pendingInputLine;
	}

	public Consumer<String> getPendingInputLineConsumer() {
		return pendingInputLineConsumer;
	}

	public void setPendingInputLineConsumer(Consumer<String> pendingInputLineConsumer) {
		this.pendingInputLineConsumer = pendingInputLineConsumer;
	}

	public Consumer<String> getDefaultPendingInputLineConsumer() {
		return defaultPendingInputLineConsumer;
	}

	@Override
	public PrintStream createErrorStream() {
		return getPrintStream(System.err, LogFile.ERROR_LEVEL_NAME, "#FFFFFF", "#FF6E40", () -> true);
	}

	@Override
	public PrintStream createWarningStream() {
		return getPrintStream(System.err, LogFile.WARNING_LEVEL_NAME, "#FFFFFF", "#FFC13B", () -> true);
	}

	@Override
	public PrintStream createInformationStream() {
		return getPrintStream(System.out, LogFile.INFORMATION_LEVEL_NAME, "#FFFFFF", "#AAAAAA", () -> true);
	}

	public void log(String message, Boolean lineTerminated, String levelName, String prefixColor, String messageColor) {
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
			if (buffer.length() == 0) {
				buffer.append("<pre>");
			}
			buffer.append(formattedMessage);
			if (lineTerminated) {
				buffer.append("</pre>\n<pre>");
			}
			if ((buffer.length() - size) > 0) {
				int endOfFirstLine = buffer.indexOf("\n");
				if (endOfFirstLine == -1) {
					endOfFirstLine = buffer.length();
				}
				buffer.delete(0, endOfFirstLine + "\n".length());
			}
		}
	}

	public void log(String message) {
		log(message, true, null, null, null);
	}

	public String read() {
		return buffer.toString() + "<font color=\"blue\">_</font>";
	}

	public void submitPendingInputLine() {
		String s = pendingInputLine;
		if (s == null) {
			s = "";
		}
		pendingInputLineConsumer.accept(s);
		pendingInputLine = null;
	}

	public void clear() {
		buffer.delete(0, buffer.length());
	}

	public PrintStream getPrintStream(PrintStream parallelPrintStream, String levelName, final String prefixColor,
			final String messageColor, final Supplier<Boolean> enablementStatusSupplier) {
		return MiscUtils.createBufferedPrintStream((line, lineTerminated) -> {
			if (lineTerminated) {
				parallelPrintStream.println(line);
			} else {
				parallelPrintStream.print(line);
			}
			log(line, lineTerminated, levelName, prefixColor, messageColor);
		}, enablementStatusSupplier);
	}

	public PrintStream getPrintStream(PrintStream parallelPrintStream,
			final Supplier<Boolean> enablementStatusSupplier) {
		return getPrintStream(parallelPrintStream, null, null, null, enablementStatusSupplier);
	}
}