package com.otk.jesb;

import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.otk.jesb.util.Listener;
import com.otk.jesb.util.MiscUtils;

public class Log {

	private static List<Listener<LogEvent>> listeners = new CopyOnWriteArrayList<Listener<LogEvent>>();

	public static List<Listener<LogEvent>> getListeners() {
		return listeners;
	}

	public static void error(Throwable t) {
		error(MiscUtils.getPrintedStackTrace(t));
	}

	public static void error(String s) {
		writeLine(Level.ERROR, Thread.currentThread(), s);
	}

	public static void info(String s) {
		writeLine(Level.INFO, Thread.currentThread(), s);
	}

	private static synchronized void writeLine(Level level, Thread thread, String message) {
		String date = MiscUtils.getDefaultDateFormat().format(MiscUtils.now());
		String formattedMessage = String.format("%1$s %3$s - [%2$s] %4$s", date, thread.getName(), level.toString(),
				message);
		System.out.println(formattedMessage);
		for (Listener<LogEvent> l : listeners) {
			l.handle(new LogEvent(level, message));
		}
	}

	public static OutputStream getStream(final Level level) {
		return new OutputStream() {
			private String line = "";
			private Thread thread = Thread.currentThread();
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
						flush();
					} else {
						line = line + standardC;
					}
				} finally {
					lastC = currentC;
				}
			}

			public void flush() {
				Log.writeLine(level, thread, line);
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

	public enum Level {
		INFO, ERROR
	};

	public static class LogEvent {
		private Level level;
		private String message;

		public LogEvent(Level level, String message) {
			super();
			this.level = level;
			this.message = message;
		}

		public Level getLevel() {
			return level;
		}

		public String getMessage() {
			return message;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((level == null) ? 0 : level.hashCode());
			result = prime * result + ((message == null) ? 0 : message.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			LogEvent other = (LogEvent) obj;
			if (level != other.level)
				return false;
			if (message == null) {
				if (other.message != null)
					return false;
			} else if (!message.equals(other.message))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "LogEvent [level=" + level + ", message=" + message + "]";
		}

	}

}
