package com.otk.jesb.activation.builtin;

import com.otk.jesb.activation.Activator;
import com.otk.jesb.activation.ActivatorMetadata;
import com.otk.jesb.activation.ActivationHandler;
import com.otk.jesb.Variant;
import xy.reflect.ui.info.custom.InfoCustomizations;
import xy.reflect.ui.util.ReflectionUIUtils;
import com.otk.jesb.ValidationError;
import xy.reflect.ui.info.ResourcePath;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Solution;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.JESB;
import com.otk.jesb.Log;
import com.otk.jesb.UnexpectedError;

public class ReadCommandLine extends Activator {

	private Variant<String> promptVariant = new Variant<String>(String.class, "jesb> ");

	private ActivationHandler activationHandler;
	private BufferedReader standardInput;
	private Thread thread;

	public Variant<String> getPromptVariant() {
		return promptVariant;
	}

	public void setPromptVariant(Variant<String> promptVariant) {
		this.promptVariant = promptVariant;
	}

	@Override
	public String toString() {
		return "ReadCommandLine [promptVariant=" + promptVariant + "]";
	}

	@Override
	public Class<?> getInputClass(Solution solutionInstance) {
		return InputClassStructure.class;
	}

	@Override
	public Class<?> getOutputClass(Solution solutionInstance) {
		return OutputClassStructure.class;
	}

	@Override
	public boolean isAutomaticallyTriggerable() {
		return true;
	}

	@Override
	public void initializeAutomaticTrigger(ActivationHandler activationHandler, Solution solutionIOnstance)
			throws Exception {
		this.activationHandler = activationHandler;
		standardInput = new BufferedReader(new InputStreamReader(JESB.getStandardInputSource().newInputStream()));
		thread = new Thread(ReadCommandLine.class.getSimpleName() + "Worker-" + hashCode()) {

			@Override
			public void run() {
				String prompt = promptVariant.getValue(solutionIOnstance);
				while (true) {
					try {
						if (isInterrupted()) {
							break;
						}
						if (prompt != null) {
							JESB.getStandardOutput().print(prompt);
							JESB.getStandardOutput().flush();
						}
						String line = standardInput.readLine();
						if (line == null) {
							break;
						}
						InputClassStructure input = new InputClassStructure(line);
						OutputClassStructure output = (OutputClassStructure) activationHandler.trigger(input);
						if (output.result != null) {
							JESB.getStandardOutput().println(output.result);
						}
					} catch (Throwable t) {
						if (MiscUtils.isInterruptionException(t)) {
							break;
						} else {
							Log.get().error(t);
							throw new UnexpectedError(t);
						}
					}
				}
			}
		};
		thread.start();
	}

	@Override
	public void finalizeAutomaticTrigger(Solution solutionIOnstance) throws Exception {
		MiscUtils.willRethrowCommonly((compositeException) -> {
			compositeException.tryCactch(() -> {
				while (thread.isAlive()) {
					thread.interrupt();
					MiscUtils.relieveCPU();
				}
			});
			thread = null;
			compositeException.tryCactch(() -> {
				standardInput.close();
			});
			standardInput = null;
			this.activationHandler = null;
		});
	}

	@Override
	public boolean isAutomaticTriggerReady() {
		return activationHandler != null;
	}

	public static void customizeUI(InfoCustomizations infoCustomizations) {
		// ReadCommandLine form customization
		{
			// field control positions
			InfoCustomizations.getTypeCustomization(infoCustomizations, ReadCommandLine.class.getName())
					.setCustomFieldsOrder(java.util.Arrays.asList("promptVariant"));
			// promptVariant control customization
			{
				InfoCustomizations
						.getFieldCustomization(infoCustomizations, ReadCommandLine.class.getName(), "promptVariant")
						.setCustomFieldCaption("Prompt");
				InfoCustomizations
						.getFieldCustomization(infoCustomizations,
								com.otk.jesb.ui.GUI.VariantCustomizations
										.getAdapterTypeName(ReadCommandLine.class.getName(), "promptVariant"),
								com.otk.jesb.ui.GUI.VariantCustomizations.getConstantValueFieldName("promptVariant"))
						.setCustomFieldCaption("Prompt");
				InfoCustomizations
						.getFieldCustomization(infoCustomizations,
								com.otk.jesb.ui.GUI.VariantCustomizations
										.getAdapterTypeName(ReadCommandLine.class.getName(), "promptVariant"),
								com.otk.jesb.ui.GUI.VariantCustomizations.getConstantValueFieldName("promptVariant"))
						.setNullValueDistinctForced(true);
			}
			// hide UI customization method
			InfoCustomizations.getMethodCustomization(infoCustomizations, ReadCommandLine.class.getName(),
					ReflectionUIUtils.buildMethodSignature("void", "customizeUI",
							java.util.Arrays.asList(InfoCustomizations.class.getName())))
					.setHidden(true);
		}
	}

	@Override
	public void validate(boolean recursively, Solution solutionInstance, Plan plan) throws ValidationError {
		super.validate(recursively, solutionInstance, plan);
		if (recursively) {
			try {
				promptVariant.validate(solutionInstance);
			} catch (ValidationError e) {
				throw new ValidationError("Failed to validate 'Prompt'", e);
			}
		}
	}

	static public class InputClassStructure {

		public final String inputLine;

		public InputClassStructure(String inputLine) {
			this.inputLine = inputLine;
		}

		@Override
		public String toString() {
			return "InputClassStructure [inputLine=" + inputLine + "]";
		}

	}

	static public class OutputClassStructure {

		public String result;

		public OutputClassStructure() {
		}

		@Override
		public String toString() {
			return "OutputClassStructure [result=" + result + "]";
		}

	}

	public static class Metadata implements ActivatorMetadata {

		@Override
		public String getActivatorName() {
			return "Read Command Line";
		}

		@Override
		public Class<? extends Activator> getActivatorClass() {
			return ReadCommandLine.class;
		}

		@Override
		public ResourcePath getActivatorIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(ReadCommandLine.class.getName().replace(".", "/") + ".png"));
		}

	}

}
