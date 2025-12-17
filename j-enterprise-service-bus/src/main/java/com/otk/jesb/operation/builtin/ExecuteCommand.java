package com.otk.jesb.operation.builtin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.otk.jesb.ValidationError;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.solution.Step;
import com.otk.jesb.util.CommandExecutor;
import xy.reflect.ui.info.ResourcePath;

public class ExecuteCommand implements Operation {

	private String executable;
	private String[] arguments = new String[0];
	private String workingDirectoryPath = ".";
	private int timeoutMilliseconds = 0;
	private boolean runAsynchronously = false;

	public ExecuteCommand(String executable) {
		this.executable = executable;
	}

	public String getExecutable() {
		return executable;
	}

	public String[] getArguments() {
		return arguments;
	}

	public void setArguments(String[] arguments) {
		this.arguments = arguments;
	}

	public String getWorkingDirectoryPath() {
		return workingDirectoryPath;
	}

	public void setWorkingDirectoryPath(String workingDirectoryPath) {
		this.workingDirectoryPath = workingDirectoryPath;
	}

	public int getTimeoutMilliseconds() {
		return timeoutMilliseconds;
	}

	public void setTimeoutMilliseconds(int timeoutMilliseconds) {
		this.timeoutMilliseconds = timeoutMilliseconds;
	}

	public boolean isRunAsynchronously() {
		return runAsynchronously;
	}

	private void setRunAsynchronously(boolean runAsynchronously) {
		this.runAsynchronously = runAsynchronously;
	}

	@Override
	public Object execute(Solution solutionInstance) throws Throwable {
		ByteArrayOutputStream outReceiver = runAsynchronously ? null : new ByteArrayOutputStream();
		ByteArrayOutputStream errReceiver = runAsynchronously ? null : new ByteArrayOutputStream();
		String commandLine = CommandExecutor.quoteArgument(executable) + " "
				+ Arrays.stream(arguments).map(CommandExecutor::quoteArgument).collect(Collectors.joining(" "));
		Process process;
		try {
			process = CommandExecutor.run(commandLine, !runAsynchronously, outReceiver, errReceiver,
					new File(workingDirectoryPath), timeoutMilliseconds, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			process = null;
		}
		boolean timedOut = (process == null);
		return runAsynchronously ? null
				: new CommandResult(timedOut ? null : process.exitValue(), outReceiver.toString(),
						errReceiver.toString(), timedOut);
	}

	public static class CommandResult {
		private Integer exitCode;
		private String output;
		private String error;
		private boolean timedOut;

		public CommandResult(Integer exitCode, String output, String error, boolean timedOut) {
			this.exitCode = exitCode;
			this.output = output;
			this.error = error;
			this.timedOut = timedOut;
		}

		public Integer getExitCode() {
			return exitCode;
		}

		public String getOutput() {
			return output;
		}

		public String getError() {
			return error;
		}

		public boolean isTimedOut() {
			return timedOut;
		}

	}

	public static class Builder implements OperationBuilder<ExecuteCommand> {

		private RootInstanceBuilder instanceBuilder = new RootInstanceBuilder("CommandInput",
				ExecuteCommand.class.getName());
		private boolean runAsynchronously = false;

		public RootInstanceBuilder getInstanceBuilder() {
			return instanceBuilder;
		}

		public void setInstanceBuilder(RootInstanceBuilder instanceBuilder) {
			this.instanceBuilder = instanceBuilder;
		}

		public boolean isRunAsynchronously() {
			return runAsynchronously;
		}

		public void setRunAsynchronously(boolean runAsynchronously) {
			this.runAsynchronously = runAsynchronously;
		}

		@Override
		public ExecuteCommand build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			Solution solutionInstance = context.getSession().getSolutionInstance();
			ExecuteCommand result = (ExecuteCommand) instanceBuilder.build(new InstantiationContext(
					context.getVariables(), context.getPlan()
							.getValidationContext(context.getCurrentStep(), solutionInstance).getVariableDeclarations(),
					solutionInstance));
			result.setRunAsynchronously(runAsynchronously);
			return result;
		}

		@Override
		public Class<?> getOperationResultClass(Solution solutionInstance, Plan currentPlan, Step currentStep) {
			return runAsynchronously ? null : CommandResult.class;
		}

		@Override
		public void validate(boolean recursively, Solution solutionInstance, Plan plan, Step step)
				throws ValidationError {
			if (recursively) {
				if (instanceBuilder != null) {
					try {
						instanceBuilder.getFacade(solutionInstance).validate(recursively,
								plan.getValidationContext(step, solutionInstance).getVariableDeclarations());
					} catch (ValidationError e) {
						throw new ValidationError("Failed to validate the input builder", e);
					}
				}
			}
		}

	}

	public static class Metadata implements OperationMetadata<ExecuteCommand> {

		@Override
		public String getOperationTypeName() {
			return "Execute Command";
		}

		@Override
		public String getCategoryName() {
			return "General";
		}

		@Override
		public Class<? extends OperationBuilder<ExecuteCommand>> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(ExecuteCommand.class.getName().replace(".", "/") + ".png"));
		}
	}

}
