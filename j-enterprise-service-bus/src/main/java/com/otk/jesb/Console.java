package com.otk.jesb;

import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.function.Supplier;

import org.apache.commons.io.output.WriterOutputStream;

import java.io.Writer;
import java.nio.charset.*;
import com.otk.jesb.util.MiscUtils;

public class Console {

	public static final Console DEFAULT = new Console();

	public static final String ERROR_LEVEL_NAME = "ERROR";
	public static final String WARNING_LEVEL_NAME = "WARNING";
	public static final String INFORMATION_LEVEL_NAME = "INFORMATION";
	public static final String VERBOSE_LEVEL_NAME = "VERBOSE";

	private StringBuilder buffer = new StringBuilder();
	private int size = 100000;
	private final Object bufferMutex = new Object();
	private PrintStream informationStream = interceptPrintStreamData(System.out, INFORMATION_LEVEL_NAME, "#FFFFFF",
			"#AAAAAA", () -> true);
	private PrintStream warningStream = interceptPrintStreamData(System.err, WARNING_LEVEL_NAME, "#FFFFFF", "#FFC13B",
			() -> true);
	private PrintStream errorStream = interceptPrintStreamData(System.err, ERROR_LEVEL_NAME, "#FFFFFF", "#FF6E40",
			() -> true);

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		if ((size < 1000) || (size > 1000000)) {
			throw new IllegalArgumentException("Invalid console size: " + size);
		}
		this.size = size;
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
		return new PrintStream(new WriterOutputStream(new Writer() {
			private final StringBuilder lineBuffer = new StringBuilder();

			@Override
			public synchronized void write(char[] cbuf, int off, int len) throws IOException {
				for (int i = 0; i < len; i++) {
					char c = cbuf[off + i];
					if (c == '\n') {
						flushLine();
					} else if (c == '\r') {
						if ((i + 1 < len) && (cbuf[off + i + 1] == '\n')) {
							i++;
						}
						flushLine();
					} else {
						lineBuffer.append(c);
					}
				}
			}

			@Override
			public synchronized void flush() throws IOException {
				if (lineBuffer.length() > 0) {
					flushLine();
				}
			}

			@Override
			public synchronized void close() throws IOException {
				flush();
			}

			private void flushLine() throws IOException {
				String line = lineBuffer.toString();
				basePrintStream.println(line);
				log(line, levelName, prefixColor, messageColor);
				lineBuffer.setLength(0);
			}
		}, Charset.defaultCharset())) {

			@Override
			public void println() {
				super.println();
				flush();
			}

			@Override
			public void println(boolean x) {
				super.println(x);
				flush();
			}

			@Override
			public void println(char x) {
				super.println(x);
				flush();
			}

			@Override
			public void println(int x) {
				super.println(x);
				flush();
			}

			@Override
			public void println(long x) {
				super.println(x);
				flush();
			}

			@Override
			public void println(float x) {
				super.println(x);
				flush();
			}

			@Override
			public void println(double x) {
				super.println(x);
				flush();
			}

			@Override
			public void println(char[] x) {
				super.println(x);
				flush();
			}

			@Override
			public void println(String x) {
				super.println(x);
				flush();
			}

			@Override
			public void println(Object x) {
				super.println(x);
				flush();
			}

		};
	}

}