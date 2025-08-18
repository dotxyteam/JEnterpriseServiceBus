package com.otk.jesb.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import com.otk.jesb.UnexpectedError;

public class CommandExecutor {

	private String commandLine;
	private OutputStream processOutputRedirectedTo;
	private OutputStream processErrorRedirectedTo;
	private File workingDir;
	private Map<String, String> env;
	private Process process;
	private Thread outputRedirector;
	private Thread errorRedirector;

	public CommandExecutor(String commandLine) {
		this.commandLine = commandLine;
	}

	public CommandExecutor() {
	}

	public String getCommandLine() {
		return commandLine;
	}

	public void setCommandLine(String commandLine) {
		this.commandLine = commandLine;
	}

	public OutputStream getProcessOutputRedirectedTo() {
		return processOutputRedirectedTo;
	}

	public void setProcessOutputRedirectedTo(OutputStream out) {
		this.processOutputRedirectedTo = out;
	}

	public OutputStream getProcessErrorRedirectedTo() {
		return processErrorRedirectedTo;
	}

	public void setProcessErrorRedirectedTo(OutputStream err) {
		this.processErrorRedirectedTo = err;
	}

	public File getWorkingDir() {
		return workingDir;
	}

	public void setWorkingDir(File workingDir) {
		this.workingDir = workingDir;
	}

	public Map<String, String> getEnv() {
		return env;
	}

	public void setEnv(Map<String, String> env) {
		this.env = env;
	}

	public Process getLaunchedProcess() {
		return process;
	}

	public String[] getEnvAsArray() {
		if (env == null) {
			return null;
		}
		List<String> envList = new ArrayList<String>();
		for (Map.Entry<String, String> entry : env.entrySet()) {
			envList.add(entry.getKey() + "=" + entry.getValue());
		}
		return envList.toArray(new String[envList.size()]);
	}

	public void startProcess() throws IOException {
		String[] args = splitArguments(commandLine);
		if (args.length == 0) {
			throw new IllegalArgumentException("Executable file not specified");
		}
		String[] envArray = getEnvAsArray();
		process = Runtime.getRuntime().exec(args, envArray, workingDir);
		destroyOnExit(process);
		outputRedirector = getProcessStreamRedirector(process.getInputStream(), processOutputRedirectedTo, "Output");
		errorRedirector = getProcessStreamRedirector(process.getErrorStream(), processErrorRedirectedTo, "Error");
	}

	protected void destroyOnExit(final Process process) {
		Runtime.getRuntime().addShutdownHook(new Thread(CommandExecutor.this + " shutdown hook") {
			Thread hook = this;
			{
				new Thread(CommandExecutor.this + " shutdown hook remover") {
					@Override
					public void run() {
						try {
							process.waitFor();
						} catch (InterruptedException e) {
							throw new UnexpectedError(e);
						}
						try {
							Runtime.getRuntime().removeShutdownHook(hook);
						} catch (IllegalStateException shutdownInProgress) {
						}
					}
				}.start();
			}

			@Override
			public void run() {
				if (process.isAlive()) {
					process.destroy();
				}
			}
		});
	}

	public boolean isProcessAlive() {
		if (process != null) {
			if (process.isAlive()) {
				return true;
			}
		}
		return false;
	}

	public void waitForProcessEnd(long timeout, TimeUnit unit) throws InterruptedException {
		if (timeout <= 0) {
			process.waitFor();
		} else {
			process.waitFor(timeout, unit);
		}
	}

	public void killProcess() {
		process.destroy();
		if (process.isAlive()) {
			process.destroyForcibly();
		}
	}

	public void disconnectProcess() {
		for (Thread thread : new Thread[] { outputRedirector, errorRedirector }) {
			while (thread.isAlive()) {
				thread.interrupt();
				MiscUtils.relieveCPU();
			}
		}
		try {
			process.getOutputStream().close();
		} catch (IOException ignore) {
		}
		try {
			process.getInputStream().close();
		} catch (IOException ignore) {
		}
		try {
			process.getErrorStream().close();
		} catch (IOException ignore) {
		}
	}

	private Thread getProcessStreamRedirector(InputStream processStream, OutputStream destinationStream,
			String processStreamName) {
		if (destinationStream == null) {
			return new Thread("No redirection");
		} else {
			String reason = processStreamName + " stream consumption for " + getCommandDescription();
			return MiscUtils.redirectStream(processStream, destinationStream, reason);
		}
	}

	public String getCommandDescription() {
		return "Command: " + MiscUtils.truncateNicely(commandLine, 50);
	}

	@Override
	public String toString() {
		return "CommandExecutor [commandLine=" + commandLine + "]";
	}

	public static Process run(final String commandLine, boolean wait, final OutputStream outReceiver,
			final OutputStream errReceiver, File workingDir, long timeout, TimeUnit timeoutUnit) throws IOException {
		CommandExecutor commandExecutor = new CommandExecutor(commandLine);
		commandExecutor.setProcessOutputRedirectedTo(outReceiver);
		commandExecutor.setProcessErrorRedirectedTo(errReceiver);
		commandExecutor.setWorkingDir(workingDir);
		commandExecutor.startProcess();
		if (wait) {
			try {
				commandExecutor.waitForProcessEnd(timeout, timeoutUnit);
			} catch (InterruptedException e) {
				commandExecutor.killProcess();
			}
			commandExecutor.disconnectProcess();
		} else {
			new Thread("ProcessMonitor [of=" + commandExecutor.getCommandDescription() + "]") {
				{
					setDaemon(true);
				}

				@Override
				public void run() {
					try {
						commandExecutor.waitForProcessEnd(timeout, timeoutUnit);
					} catch (InterruptedException e) {
						throw new UnexpectedError(e);
					}
					commandExecutor.disconnectProcess();
				}
			}.start();
		}
		return commandExecutor.getLaunchedProcess();
	}

	public static Process run(final String command, boolean wait, final OutputStream outReceiver,
			final OutputStream errReceiver) throws IOException {
		return run(command, wait, outReceiver, errReceiver, null, 0, null);
	}

	public static String quoteArgument(String argument) {
		String[] argumentSplitByQuotes = argument.split("\"", -1);
		if (argumentSplitByQuotes.length == 1) {
			return "\"" + argument + "\"";
		} else {
			StringBuilder result = new StringBuilder();
			for (int i = 0; i < argumentSplitByQuotes.length; i++) {
				if (i > 0) {
					result.append("'\"'");
				}
				String elementOfArgumentSplitByQuotes = argumentSplitByQuotes[i];
				result.append(quoteArgument(elementOfArgumentSplitByQuotes));
			}
			return result.toString();
		}
	}

	public static String[] splitArguments(String s) {
		if ((s == null) || (s.length() == 0)) {
			return new String[0];
		}
		final int normal = 0;
		final int inQuote = 1;
		final int inDoubleQuote = 2;
		int state = normal;
		StringTokenizer tok = new StringTokenizer(s, "\"\' ", true);
		Vector<String> v = new Vector<String>();
		StringBuilder current = new StringBuilder();

		while (tok.hasMoreTokens()) {
			String nextTok = tok.nextToken();
			switch (state) {
			case inQuote:
				if ("\'".equals(nextTok)) {
					state = normal;
				} else {
					current.append(nextTok);
				}
				break;
			case inDoubleQuote:
				if ("\"".equals(nextTok)) {
					state = normal;
				} else {
					current.append(nextTok);
				}
				break;
			default:
				if ("\'".equals(nextTok)) {
					state = inQuote;
				} else if ("\"".equals(nextTok)) {
					state = inDoubleQuote;
				} else if (" ".equals(nextTok)) {
					if (current.length() != 0) {
						v.addElement(current.toString());
						current.setLength(0);
					}
				} else {
					current.append(nextTok);
				}
				break;
			}
		}

		if (current.length() != 0) {
			v.addElement(current.toString());
		}

		if ((state == inQuote) || (state == inDoubleQuote)) {
			throw new IllegalArgumentException("unbalanced quotes in " + s);
		}

		String[] args = new String[v.size()];
		v.copyInto(args);
		return args;
	}

}
