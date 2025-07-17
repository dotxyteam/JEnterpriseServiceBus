package com.otk.jesb;

import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormat;

import com.otk.jesb.util.MiscUtils;

public class Console {

	public static final Console INSTANCE = new Console();
	static {
		System.setOut(new PrintStream(
				MiscUtils.unifyOutputStreams(System.out, INSTANCE.getStream("DEBUG", "#009999", "#00FFFF"))));
		System.setErr(new PrintStream(
				MiscUtils.unifyOutputStreams(System.err, INSTANCE.getStream("DEBUG", "#009999", "#00FFFF"))));
	}

	private StringBuilder buffer = new StringBuilder();
	private int size = 100000;

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
		String date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(MiscUtils.now());
		String formattedMessage = String.format("<font color=\"%s\">- %s - %s [%s] </font> <font color=\"%s\">%s</font>",
				prefixColor, date, levelName, Thread.currentThread().getName(), messageColor, message);
		buffer.append(formattedMessage + "<BR>" + System.lineSeparator());
		if ((buffer.length() - size) > 0) {
			int endOfFirstLine = buffer.indexOf(System.lineSeparator());
			if(endOfFirstLine == -1) {
				endOfFirstLine = buffer.length();
			}
			buffer.delete(0, endOfFirstLine);
		}
	}

	public String read() {
		return buffer.toString();
	}

	public void clear() {
		buffer.delete(0, buffer.length());
	}

	public OutputStream getStream(final String levelName, final String prefixColor, final String messageColor) {
		return new OutputStream() {
			private String line = "";
			private char lastC = 0;
			private char currentC = 0;

			public void write(int b) {
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
			protected void finalize() throws Throwable {
				if (line.length() > 0) {
					flush();
				}
			}

		};
	}
}