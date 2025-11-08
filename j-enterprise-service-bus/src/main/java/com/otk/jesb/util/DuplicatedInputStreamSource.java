package com.otk.jesb.util;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

import com.otk.jesb.UnexpectedError;

/**
 * Duplicates an InputStream (e.g. System.in) so that multiple readers can
 * independently read the same input data.
 *
 * The duplicator thread starts automatically when the first reader subscribes
 * (via newInputStream()) and stops once no readers remain.
 */
public class DuplicatedInputStreamSource implements Closeable {

	// --- Example usage ---
	public static void main(String[] args) throws Exception {
		@SuppressWarnings("resource")
		DuplicatedInputStreamSource source = new DuplicatedInputStreamSource(System.in);
		for (int i = 0; i < 2; i++) {
			final int idx = i;
			new Thread(() -> {
				try (BufferedReader br = new BufferedReader(new InputStreamReader(source.newInputStream()))) {
					String line;
					while ((line = br.readLine()) != null)
						System.out.println(Thread.currentThread().getName() + "> " + line);
				} catch (IOException e) {
					System.err.println(Thread.currentThread().getName() + " ended: " + e);
				}
			}, "Thread-" + idx).start();
		}
	}

	private final BufferedReader sourceReader;
	private final List<PipedOutputStream> outputs = new ArrayList<>();
	private Thread duplicatorThread;
	private volatile boolean closed = false;
	private volatile boolean running = false;

	public DuplicatedInputStreamSource(InputStream source) {
		this.sourceReader = new BufferedReader(
				new InputStreamReader(Objects.requireNonNull(source, "source must not be null")));
	}

	public boolean isClosed() {
		return closed;
	}

	/**
	 * Creates and returns a new InputStream that receives the same data as the
	 * source. Starts the duplicator thread if necessary.
	 */
	public synchronized InputStream newInputStream() {
		if (closed)
			throw new IllegalStateException("Source already closed");

		try {
			PipedInputStream in = new PipedInputStream(8192) {
				/*
				 * Fix for this issue: https://bugs.java.com/bugdatabase/view_bug?bug_id=4028322
				 */
				@Override
				public synchronized int read() throws IOException {
					try {
						return super.read();
					} catch (IOException e) {
						if ("Pipe broken".equals(e.getMessage()) || "Write end dead".equals(e.getMessage())) {
							try {
								Field writeSideField = PipedInputStream.class.getDeclaredField("writeSide");
								writeSideField.setAccessible(true);
								writeSideField.set(this, Thread.currentThread());
							} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException
									| SecurityException e1) {
								throw new UnexpectedError(e1);
							}
							return read();
						} else {
							throw e;
						}
					}
				}

			};
			PipedOutputStream out = new PipedOutputStream(in);
			outputs.add(out);

			// Start duplicator thread if not already running
			if (!running) {
				running = true;
				duplicatorThread = new Thread(this::duplicateLoop, "DuplicatedInputStreamSource-duplicator");
				duplicatorThread.setDaemon(true);
				duplicatorThread.start();
			}

			return in;
		} catch (IOException e) {
			throw new UncheckedIOException("Error creating duplicated stream", e);
		}
	}

	/**
	 * Reads line by line from the source and broadcasts each line to all active
	 * readers. The loop exits automatically when no readers remain or when the
	 * source closes.
	 */
	private void duplicateLoop() {
		try {
			String line;
			while (!closed) {
				synchronized (this) {
					if (outputs.isEmpty()) {
						// No active readers → pause duplicator
						running = false;
						return;
					}
				}

				line = sourceReader.readLine();
				if (line == null)
					break;
				pushLine(line);
			}
		} catch (IOException e) {
			if (!closed)
				throw new UnexpectedError("Duplicator thread error: " + e, e);
		} finally {
			synchronized (this) {
				for (PipedOutputStream out : outputs) {
					try {
						out.close();
					} catch (IOException ignored) {
					}
				}
				outputs.clear();
				running = false;
			}
		}
	}

	public void pushLine(String line) {
		byte[] data = (line + System.lineSeparator()).getBytes();
		synchronized (this) {
			Iterator<PipedOutputStream> it = outputs.iterator();
			while (it.hasNext()) {
				PipedOutputStream out = it.next();
				try {
					out.write(data);
					out.flush();
				} catch (IOException e) {
					try {
						out.close();
					} catch (IOException ignored) {
					}
					it.remove();
				}
			}
		}
	}

	@Override
	public synchronized void close() throws IOException {
		closed = true;
		sourceReader.close();
		for (PipedOutputStream out : outputs) {
			try {
				out.close();
			} catch (IOException ignored) {
			}
		}
		outputs.clear();
		running = false;
	}

}
