package com.otk.jesb.activation.builtin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;

import com.otk.jesb.JESB;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.Variant;
import com.otk.jesb.activation.ActivationHandler;
import com.otk.jesb.activation.Activator;
import com.otk.jesb.activation.ActivatorMetadata;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.ui.GUI.JESBSubCustomizedUI;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.UpToDate.VersionAccessException;

import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.info.custom.InfoCustomizations;
import xy.reflect.ui.info.type.factory.IInfoProxyFactory;
import xy.reflect.ui.info.type.factory.InfoCustomizationsFactory;
import xy.reflect.ui.util.ReflectionUIUtils;

public class ReadCommandLine extends Activator {

	private Variant<String> promptVariant = new Variant<String>(String.class, "jesb> ");

	private ActivationHandler activationHandler;
	private BufferedReader standardInput;
	private Thread thread;

	public ReadCommandLine() {
	}

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
	public Class<?> getInputClass() {
		return InputClassStructure.class;
	}

	@Override
	public Class<?> getOutputClass() {
		return OutputClassStructure.class;
	}

	@Override
	public boolean isAutomaticallyTriggerable() {
		return true;
	}

	@Override
	public void initializeAutomaticTrigger(ActivationHandler activationHandler) throws Exception {
		this.activationHandler = activationHandler;
		standardInput = new BufferedReader(new InputStreamReader(JESB.getStandardInputSource().newInputStream()));
		thread = new Thread("CommandLineReader-" + hashCode()) {
			{
				setDaemon(true);
			}

			@Override
			public void run() {
				String prompt = promptVariant.getValue();
				try {
					while (true) {
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
						OutputClassStructure ouput = (OutputClassStructure) activationHandler.trigger(input);
						if (ouput.result != null) {
							JESB.getStandardOutput().println(ouput.result);
						}
					}
				} catch (InterruptedIOException e) {
					return;
				} catch (IOException e) {
					throw new UnexpectedError(e);
				}
			}
		};
		thread.start();
	}

	@Override
	public void finalizeAutomaticTrigger() throws Exception {
		while (thread.isAlive()) {
			thread.interrupt();
			MiscUtils.relieveCPU();
		}
		thread = null;
		standardInput.close();
		standardInput = null;

		this.activationHandler = null;
	}

	@Override
	public boolean isAutomaticTriggerReady() {
		return activationHandler != null;
	}

	private static void customizeUI(InfoCustomizations infoCustomizations) {
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

	public static void getUICustomizationsFactory(JESBSubCustomizedUI customizedUI) {
		return new InfoCustomizationsFactory(customizedUI) {

			InfoCustomizations infoCustomizations = new InfoCustomizations();
			{
				customizeUI(infoCustomizations);
			}

			@Override
			public String getIdentifier() {
				return "MethodBasedSubInfoCustomizationsFactory [of=" + ReadCommandLine.class.getName() + "]";
			}

			@Override
			protected IInfoProxyFactory getInfoCustomizationsSetupFactory() {
				return IInfoProxyFactory.NULL_INFO_PROXY_FACTORY;
			}

			@Override
			public InfoCustomizations accessInfoCustomizations() {
				return infoCustomizations;
			}
		};
	}

	@Override
	public void validate(boolean recursively, Plan plan) throws ValidationError {
		super.validate(recursively, plan);
		if (recursively) {
			try {
				promptVariant.validate();
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
