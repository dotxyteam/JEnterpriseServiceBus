package com.otk.jesb.activation;

public class Schedule extends com.otk.jesb.activation.Activator {
	private com.otk.jesb.Variant<com.otk.jesb.meta.DateTime> startVariant = new com.otk.jesb.Variant<com.otk.jesb.meta.DateTime>(
			com.otk.jesb.meta.DateTime.class, com.otk.jesb.meta.DateTime.NOW);
	private com.otk.jesb.Variant<java.lang.Boolean> repeatingVariant = new com.otk.jesb.Variant<java.lang.Boolean>(
			java.lang.Boolean.class, false);
	private RepetitionSettingsStructure repetitionSettings = new RepetitionSettingsStructure();
	private com.otk.jesb.Reference<com.otk.jesb.resource.builtin.SharedStructureModel> referenceReference = new com.otk.jesb.Reference<com.otk.jesb.resource.builtin.SharedStructureModel>(
			com.otk.jesb.resource.builtin.SharedStructureModel.class);
	private com.otk.jesb.activation.ActivationHandler activationHandler = null;

	public Schedule() {

	}

	public com.otk.jesb.Variant<com.otk.jesb.meta.DateTime> getStartVariant() {
		return startVariant;
	}

	public void setStartVariant(com.otk.jesb.Variant<com.otk.jesb.meta.DateTime> startVariant) {
		this.startVariant = startVariant;
	}

	public com.otk.jesb.Variant<java.lang.Boolean> getRepeatingVariant() {
		return repeatingVariant;
	}

	public void setRepeatingVariant(com.otk.jesb.Variant<java.lang.Boolean> repeatingVariant) {
		this.repeatingVariant = repeatingVariant;
	}

	public RepetitionSettingsStructure getRepetitionSettings() {
		return repetitionSettings;
	}

	public void setRepetitionSettings(RepetitionSettingsStructure repetitionSettings) {
		this.repetitionSettings = repetitionSettings;
	}

	public com.otk.jesb.Reference<com.otk.jesb.resource.builtin.SharedStructureModel> getReferenceReference() {
		return referenceReference;
	}

	public void setReferenceReference(
			com.otk.jesb.Reference<com.otk.jesb.resource.builtin.SharedStructureModel> referenceReference) {
		this.referenceReference = referenceReference;
	}

	@Override
	public String toString() {
		return "Schedule [startVariant=" + startVariant + ", repeatingVariant=" + repeatingVariant
				+ ", repetitionSettings=" + repetitionSettings + ", referenceReference=" + referenceReference
				+ ", activationHandler=" + activationHandler + "]";
	}

	@Override
	public Class<?> getInputClass() {
		return InputClassStructure.class;
	}

	@Override
	public Class<?> getOutputClass() {
		return null;
	}

	@Override
	public boolean isAutomaticallyTriggerable() {
		return true;
	}

	@Override
	public void initializeAutomaticTrigger(com.otk.jesb.activation.ActivationHandler activationHandler)
			throws Exception {
		new java.util.Timer().schedule(new java.util.TimerTask() {
			@Override
			public void run() {
				activationHandler.trigger(new InputClassStructure(com.otk.jesb.meta.DateTime.NOW));
			}
		}, startVariant.getValue().toJavaUtilDate());

		this.activationHandler = activationHandler;
	}

	@Override
	public void finalizeAutomaticTrigger() throws Exception {
		this.activationHandler = null;
	}

	@Override
	public boolean isAutomaticTriggerReady() {
		return activationHandler != null;
	}

	public static void customizeUI(xy.reflect.ui.info.custom.InfoCustomizations infoCustomizations) {
		/* com.otk.jesb.activation.Schedule form customization */
		{
			/* field control positions */
			xy.reflect.ui.info.custom.InfoCustomizations
					.getTypeCustomization(infoCustomizations, com.otk.jesb.activation.Schedule.class.getName())
					.setCustomFieldsOrder(java.util.Arrays.asList("startVariant", "repeatingVariant",
							"repetitionSettings", "referenceReference"));
			/* startVariant control customization */
			{
				xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
						com.otk.jesb.activation.Schedule.class.getName(), "startVariant")
						.setCustomFieldCaption("Start At");
				xy.reflect.ui.info.custom.InfoCustomizations
						.getTypeCustomization(xy.reflect.ui.info.custom.InfoCustomizations
								.getFieldCustomization(infoCustomizations,
										com.otk.jesb.ui.GUI.VariantCustomizations.getAdapterTypeName(
												com.otk.jesb.activation.Schedule.class.getName(), "startVariant"),
										com.otk.jesb.ui.GUI.VariantCustomizations
												.getConstantValueFieldName("startVariant"))
								.getSpecificTypeCustomizations(), com.otk.jesb.meta.DateTime.class.getName())
						.setSpecificProperties(new java.util.HashMap<String, Object>() {
							private static final long serialVersionUID = 1L;
							{
								xy.reflect.ui.util.ReflectionUIUtils.setFieldControlPluginIdentifier(this,
										"com.otk.jesb.ui.GUI$JESDDateTimePickerPlugin");
								xy.reflect.ui.util.ReflectionUIUtils.setFieldControlPluginConfiguration(this,
										"com.otk.jesb.ui.GUI$JESDDateTimePickerPlugin",
										(java.io.Serializable) com.otk.jesb.PluginBuilder
												.readControlPluginConfiguration(
														"<xy.reflect.ui.control.swing.plugin.DateTimePickerPlugin_-DateTimePickerConfiguration><dateFormat>yyyy-MM-dd</dateFormat><timeFormat>HH:mm:ss</timeFormat></xy.reflect.ui.control.swing.plugin.DateTimePickerPlugin_-DateTimePickerConfiguration>"));
							}
						});
			}
			/* repeatingVariant control customization */
			{
				xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
						com.otk.jesb.activation.Schedule.class.getName(), "repeatingVariant")
						.setCustomFieldCaption("Repeat");
			}
			/* repetitionSettings control customization */
			{
				xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
						com.otk.jesb.activation.Schedule.class.getName(), "repetitionSettings")
						.setCustomFieldCaption("Repetition Settings");
				xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
						com.otk.jesb.activation.Schedule.class.getName(), "repetitionSettings")
						.setFormControlEmbeddingForced(true);
				RepetitionSettingsStructure.customizeUI(infoCustomizations);
			}
			/* referenceReference control customization */
			{
				xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
						com.otk.jesb.activation.Schedule.class.getName(), "referenceReference")
						.setCustomFieldCaption("Referennce");
				xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
						com.otk.jesb.activation.Schedule.class.getName(), "referenceReference")
						.setFormControlEmbeddingForced(true);
			}
			/* hide UI customization method */
			xy.reflect.ui.info.custom.InfoCustomizations
					.getMethodCustomization(infoCustomizations, com.otk.jesb.activation.Schedule.class.getName(),
							xy.reflect.ui.util.ReflectionUIUtils.buildMethodSignature("void", "customizeUI",
									java.util.Arrays
											.asList(xy.reflect.ui.info.custom.InfoCustomizations.class.getName())))
					.setHidden(true);
		}
	}

	@Override
	public void validate(boolean recursively, com.otk.jesb.solution.Plan plan) {
	}

	static public class InputClassStructure {
		public final com.otk.jesb.meta.DateTime activationMoment;

		public InputClassStructure(com.otk.jesb.meta.DateTime activationMoment) {
			this.activationMoment = activationMoment;
		}

		@Override
		public String toString() {
			return "InputClassStructure [activationMoment=" + activationMoment + "]";
		}

	}

	public static class Metadata implements com.otk.jesb.activation.ActivatorMetadata {
		@Override
		public String getActivatorName() {
			return "Schedule";
		}

		@Override
		public Class<? extends com.otk.jesb.activation.Activator> getActivatorClass() {
			return Schedule.class;
		}

		@Override
		public xy.reflect.ui.info.ResourcePath getActivatorIconImagePath() {
			return new xy.reflect.ui.info.ResourcePath(xy.reflect.ui.info.ResourcePath
					.specifyClassPathResourceLocation(Schedule.class.getName().replace(".", "/") + ".png"));
		}
	}

	static public class RepetitionSettingsStructure implements com.otk.jesb.activation.ActivatorStructure {
		private com.otk.jesb.Variant<java.util.concurrent.TimeUnit> periodUnitVariant = new com.otk.jesb.Variant<java.util.concurrent.TimeUnit>(
				java.util.concurrent.TimeUnit.class, java.util.concurrent.TimeUnit.DAYS);
		private com.otk.jesb.Variant<java.lang.Long> periodLengthVariant = new com.otk.jesb.Variant<java.lang.Long>(
				java.lang.Long.class, 1l);
		private com.otk.jesb.Variant<com.otk.jesb.meta.DateTime> endVariant = new com.otk.jesb.Variant<com.otk.jesb.meta.DateTime>(
				com.otk.jesb.meta.DateTime.class, null);
		private com.otk.jesb.activation.ActivationHandler activationHandler = null;

		public RepetitionSettingsStructure() {

		}

		public com.otk.jesb.Variant<java.util.concurrent.TimeUnit> getPeriodUnitVariant() {
			return periodUnitVariant;
		}

		public void setPeriodUnitVariant(com.otk.jesb.Variant<java.util.concurrent.TimeUnit> periodUnitVariant) {
			this.periodUnitVariant = periodUnitVariant;
		}

		public com.otk.jesb.Variant<java.lang.Long> getPeriodLengthVariant() {
			return periodLengthVariant;
		}

		public void setPeriodLengthVariant(com.otk.jesb.Variant<java.lang.Long> periodLengthVariant) {
			this.periodLengthVariant = periodLengthVariant;
		}

		public com.otk.jesb.Variant<com.otk.jesb.meta.DateTime> getEndVariant() {
			return endVariant;
		}

		public void setEndVariant(com.otk.jesb.Variant<com.otk.jesb.meta.DateTime> endVariant) {
			this.endVariant = endVariant;
		}

		@Override
		public String toString() {
			return "RepetitionSettingsStructure [periodUnitVariant=" + periodUnitVariant + ", periodLengthVariant="
					+ periodLengthVariant + ", endVariant=" + endVariant + ", activationHandler=" + activationHandler
					+ "]";
		}

		public static void customizeUI(xy.reflect.ui.info.custom.InfoCustomizations infoCustomizations) {
			/* RepetitionSettingsStructure form customization */
			{
				/* field control positions */
				xy.reflect.ui.info.custom.InfoCustomizations
						.getTypeCustomization(infoCustomizations, RepetitionSettingsStructure.class.getName())
						.setCustomFieldsOrder(
								java.util.Arrays.asList("periodUnitVariant", "periodLengthVariant", "endVariant"));
				/* periodUnitVariant control customization */
				{
					xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
							RepetitionSettingsStructure.class.getName(), "periodUnitVariant")
							.setCustomFieldCaption("Period Unit");
				}
				/* periodLengthVariant control customization */
				{
					xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
							RepetitionSettingsStructure.class.getName(), "periodLengthVariant")
							.setCustomFieldCaption("Period Length");
				}
				/* endVariant control customization */
				{
					xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
							RepetitionSettingsStructure.class.getName(), "endVariant").setCustomFieldCaption("End At");
					xy.reflect.ui.info.custom.InfoCustomizations
							.getFieldCustomization(infoCustomizations,
									com.otk.jesb.ui.GUI.VariantCustomizations.getAdapterTypeName(
											RepetitionSettingsStructure.class.getName(), "endVariant"),
									com.otk.jesb.ui.GUI.VariantCustomizations.getConstantValueFieldName("endVariant"))
							.setNullValueDistinctForced(true);
					xy.reflect.ui.info.custom.InfoCustomizations
							.getTypeCustomization(
									xy.reflect.ui.info.custom.InfoCustomizations
											.getFieldCustomization(infoCustomizations,
													com.otk.jesb.ui.GUI.VariantCustomizations.getAdapterTypeName(
															RepetitionSettingsStructure.class.getName(), "endVariant"),
													com.otk.jesb.ui.GUI.VariantCustomizations
															.getConstantValueFieldName("endVariant"))
											.getSpecificTypeCustomizations(),
									com.otk.jesb.meta.DateTime.class.getName())
							.setSpecificProperties(new java.util.HashMap<String, Object>() {
								private static final long serialVersionUID = 1L;
								{
									xy.reflect.ui.util.ReflectionUIUtils.setFieldControlPluginIdentifier(this,
											"com.otk.jesb.ui.GUI$JESDDateTimePickerPlugin");
									xy.reflect.ui.util.ReflectionUIUtils.setFieldControlPluginConfiguration(this,
											"com.otk.jesb.ui.GUI$JESDDateTimePickerPlugin",
											(java.io.Serializable) com.otk.jesb.PluginBuilder
													.readControlPluginConfiguration(
															"<xy.reflect.ui.control.swing.plugin.DateTimePickerPlugin_-DateTimePickerConfiguration><dateFormat>yyyy-MM-dd</dateFormat><timeFormat>HH:mm:ss</timeFormat></xy.reflect.ui.control.swing.plugin.DateTimePickerPlugin_-DateTimePickerConfiguration>"));
								}
							});
				}
				/* hide UI customization method */
				xy.reflect.ui.info.custom.InfoCustomizations
						.getMethodCustomization(infoCustomizations, RepetitionSettingsStructure.class.getName(),
								xy.reflect.ui.util.ReflectionUIUtils.buildMethodSignature("void", "customizeUI",
										java.util.Arrays
												.asList(xy.reflect.ui.info.custom.InfoCustomizations.class.getName())))
						.setHidden(true);
			}
		}

		@Override
		public void validate(boolean recursively, com.otk.jesb.solution.Plan plan) {
		}

	}
}