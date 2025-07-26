package com.otk.jesb;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.function.Supplier;

import com.otk.jesb.util.MiscUtils;

public class Console {

	public static final Console INSTANCE = new Console();
	static {
		System.setOut(new PrintStream(MiscUtils.unifyOutputStreams(System.out,
				INSTANCE.getStream("DEBUG", "#009999", "#00FFFF", () -> Preferences.INSTANCE.isLogVerbose()))));
		System.setErr(new PrintStream(MiscUtils.unifyOutputStreams(System.err,
				INSTANCE.getStream("DEBUG", "#009999", "#00FFFF", () -> Preferences.INSTANCE.isLogVerbose()))));
	}

	private StringBuilder buffer = new StringBuilder();
	private int size = 100000;
	private final Object bufferMutex = new Object();

	private Console() {
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		if ((size < 1000) || (size > 1000000)) {
			throw new IllegalArgumentException("Invalid console size: " + size);
		}
		this.size = size;
	}

	public void log(String message, String levelName, String prefixColor, String messageColor) {
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

	public OutputStream getStream(final String levelName, final String prefixColor, final String messageColor,
			final Supplier<Boolean> enablementStatusSupplier) {
		return new OutputStream() {
			private String line = "";
			private char lastC = 0;
			private char currentC = 0;

			public void write(int b) {
				if (!enablementStatusSupplier.get()) {
					return;
				}
				currentC = (char) (b & 0xff);
				char standardC = MiscUtils.standardizeNewLineSequences(lastC, currentC);
				try {
					if (standardC == 0) {
						return;
					}
					if (standardC == '\n') {
						pushLine();
					} else {
						line = line + standardC;
					}
				} finally {
					lastC = currentC;
				}
			}

			private void pushLine() {
				log(line, levelName, prefixColor, messageColor);
				line = "";
			}

			@Override
			public void close() throws IOException {
				if (line.length() > 0) {
					pushLine();
				}
				super.close();
			}

		};
	}
}