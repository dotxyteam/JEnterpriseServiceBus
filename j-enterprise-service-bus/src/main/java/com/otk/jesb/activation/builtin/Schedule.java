package com.otk.jesb.activation.builtin;

import com.otk.jesb.activation.Activator;
import com.otk.jesb.activation.ActivatorStructure;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.activation.ActivatorMetadata;
import com.otk.jesb.activation.ActivationHandler;
import com.otk.jesb.Variant;
import xy.reflect.ui.info.custom.InfoCustomizations;
import xy.reflect.ui.util.ReflectionUIUtils;
import com.otk.jesb.ValidationError;
import xy.reflect.ui.info.ResourcePath;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class Schedule extends Activator {

	private Variant<com.otk.jesb.meta.DateTime> startDateTimeVariant = new Variant<com.otk.jesb.meta.DateTime>(
			com.otk.jesb.meta.DateTime.class,
			com.otk.jesb.meta.DateTime.fromJavaUtilDate(new java.util.Date(System.currentTimeMillis() + 60000)));
	private Variant<Boolean> repeatingVariant = new Variant<Boolean>(Boolean.class, false);
	private RepetitionSettingsStructure repetitionSettings = new RepetitionSettingsStructure();
	private Variant<String> timeZoneIdentifierVariant = new Variant<String>(String.class, null);

	private ActivationHandler activationHandler;
	private ScheduledExecutorService scheduler;

	public Schedule() {
	}

	public Variant<com.otk.jesb.meta.DateTime> getStartDateTimeVariant() {
		return startDateTimeVariant;
	}

	public void setStartDateTimeVariant(Variant<com.otk.jesb.meta.DateTime> startDateTimeVariant) {
		this.startDateTimeVariant = startDateTimeVariant;
	}

	public Variant<Boolean> getRepeatingVariant() {
		return repeatingVariant;
	}

	public void setRepeatingVariant(Variant<Boolean> repeatingVariant) {
		this.repeatingVariant = repeatingVariant;
	}

	public RepetitionSettingsStructure getRepetitionSettings() {
		return repetitionSettings;
	}

	public void setRepetitionSettings(RepetitionSettingsStructure repetitionSettings) {
		this.repetitionSettings = repetitionSettings;
	}

	public Variant<String> getTimeZoneIdentifierVariant() {
		return timeZoneIdentifierVariant;
	}

	public void setTimeZoneIdentifierVariant(Variant<String> timeZoneIdentifierVariant) {
		this.timeZoneIdentifierVariant = timeZoneIdentifierVariant;
	}

	@Override
	public String toString() {
		return "Schedule [startDateTimeVariant=" + startDateTimeVariant + ", repeatingVariant=" + repeatingVariant
				+ ", repetitionSettings=" + repetitionSettings + ", timeZoneIdentifierVariant="
				+ timeZoneIdentifierVariant + "]";
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
	public void initializeAutomaticTrigger(ActivationHandler activationHandler) throws Exception {
		this.activationHandler = activationHandler;
		scheduler = MiscUtils.newScheduler(Schedule.class.getName() + "Worker-" + hashCode(), 1);
		Runnable task = new Runnable() {
			@Override
			public void run() {
				activationHandler.trigger(new InputClassStructure(com.otk.jesb.meta.DateTime.now()));
			}
		};
		ChronoUnit chronUnit = Arrays.stream(ChronoUnit.class.getEnumConstants())
				.filter(constant -> repetitionSettings.periodUnitVariant.getValue().name().startsWith(constant.name()))
				.findFirst().get();
		prepare(task, startDateTimeVariant.getValue().toJavaUtilDate(), repeatingVariant.getValue(),
				repetitionSettings.periodLengthVariant.getValue(), chronUnit,
				(repetitionSettings.getEndDateTimeVariant().getValue() == null) ? null
						: repetitionSettings.getEndDateTimeVariant().getValue().toJavaUtilDate());
	}

	@Override
	public void finalizeAutomaticTrigger() throws Exception {
		MiscUtils.willRethrowCommonly(compositeException -> {
			compositeException.tryCactch(() -> {
				scheduler.shutdownNow();
			});
			compositeException.tryCactch(() -> {
				scheduler.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
			});
			scheduler = null;
			this.activationHandler = null;
		});
	}

	@Override
	public boolean isAutomaticTriggerReady() {
		return activationHandler != null;
	}

	public static void customizeUI(InfoCustomizations infoCustomizations) {
		// com.otk.jesb.activation.builtin.Schedule form customization
		{
			// field control positions
			InfoCustomizations
					.getTypeCustomization(infoCustomizations, com.otk.jesb.activation.builtin.Schedule.class.getName())
					.setCustomFieldsOrder(java.util.Arrays.asList("startDateTimeVariant", "repeatingVariant",
							"repetitionSettings", "timeZoneIdentifierVariant"));
			// startDateTimeVariant control customization
			{
				InfoCustomizations
						.getFieldCustomization(infoCustomizations,
								com.otk.jesb.activation.builtin.Schedule.class.getName(), "startDateTimeVariant")
						.setCustomFieldCaption("Start At");
				InfoCustomizations.getTypeCustomization(InfoCustomizations.getFieldCustomization(infoCustomizations,
						com.otk.jesb.ui.GUI.VariantCustomizations.getAdapterTypeName(
								com.otk.jesb.activation.builtin.Schedule.class.getName(), "startDateTimeVariant"),
						com.otk.jesb.ui.GUI.VariantCustomizations.getConstantValueFieldName("startDateTimeVariant"))
						.getSpecificTypeCustomizations(), com.otk.jesb.meta.DateTime.class.getName())
						.setSpecificProperties(new java.util.HashMap<String, Object>() {
							private static final long serialVersionUID = 1L;
							{
								ReflectionUIUtils.setFieldControlPluginIdentifier(this,
										"com.otk.jesb.ui.GUI$JESDDateTimePickerPlugin");
								ReflectionUIUtils.setFieldControlPluginConfiguration(this,
										"com.otk.jesb.ui.GUI$JESDDateTimePickerPlugin",
										(java.io.Serializable) com.otk.jesb.PluginBuilder
												.readControlPluginConfiguration(
														"<xy.reflect.ui.control.swing.plugin.DateTimePickerPlugin_-DateTimePickerConfiguration><dateFormat>yyyy-MM-dd</dateFormat><timeFormat>HH:mm:ss</timeFormat></xy.reflect.ui.control.swing.plugin.DateTimePickerPlugin_-DateTimePickerConfiguration>"));
							}
						});
			}
			// repeatingVariant control customization
			{
				InfoCustomizations
						.getFieldCustomization(infoCustomizations,
								com.otk.jesb.activation.builtin.Schedule.class.getName(), "repeatingVariant")
						.setCustomFieldCaption("Repeat");
			}
			// repetitionSettings control customization
			{
				InfoCustomizations
						.getFieldCustomization(infoCustomizations,
								com.otk.jesb.activation.builtin.Schedule.class.getName(), "repetitionSettings")
						.setCustomFieldCaption("Repetition Settings");
				InfoCustomizations
						.getFieldCustomization(infoCustomizations,
								com.otk.jesb.activation.builtin.Schedule.class.getName(), "repetitionSettings")
						.setValueValidityDetectionForced(true);
				InfoCustomizations
						.getFieldCustomization(infoCustomizations,
								com.otk.jesb.activation.builtin.Schedule.class.getName(), "repetitionSettings")
						.setFormControlEmbeddingForced(true);
				RepetitionSettingsStructure.customizeUI(infoCustomizations);
			}
			// timeZoneIdentifierVariant control customization
			{
				InfoCustomizations
						.getFieldCustomization(infoCustomizations,
								com.otk.jesb.activation.builtin.Schedule.class.getName(), "timeZoneIdentifierVariant")
						.setCustomFieldCaption("Time Zone");
			}
			// hide UI customization method
			InfoCustomizations.getMethodCustomization(infoCustomizations,
					com.otk.jesb.activation.builtin.Schedule.class.getName(), ReflectionUIUtils.buildMethodSignature(
							"void", "customizeUI", java.util.Arrays.asList(InfoCustomizations.class.getName())))
					.setHidden(true);
		}
	}

	@Override
	public void validate(boolean recursively, com.otk.jesb.solution.Plan plan) throws ValidationError {
		super.validate(recursively, plan);
		if (recursively) {
			try {
				startDateTimeVariant.validate();
			} catch (ValidationError e) {
				throw new ValidationError("Failed to validate 'Start At'", e);
			}
		}
		if (recursively) {
			try {
				repeatingVariant.validate();
			} catch (ValidationError e) {
				throw new ValidationError("Failed to validate 'Repeat'", e);
			}
		}
		if (recursively) {
			try {
				repetitionSettings.validate(recursively, plan);
			} catch (ValidationError e) {
				throw new ValidationError("Failed to validate 'Repetition Settings'", e);
			}
		}
		if (recursively) {
			try {
				timeZoneIdentifierVariant.validate();
			} catch (ValidationError e) {
				throw new ValidationError("Failed to validate 'Time Zone'", e);
			}
		}
		if (repeatingVariant.getValue()) {
			if (repetitionSettings.periodLengthVariant.getValue() <= 0) {
				throw new ValidationError("Invalid repetition period length: "
						+ repetitionSettings.periodLengthVariant.getValue() + " (positive number expected)");
			}
			if (repetitionSettings.endDateTimeVariant.getValue() != null) {
				if (!repetitionSettings.endDateTimeVariant.getValue().toJavaUtilDate()
						.after(startDateTimeVariant.getValue().toJavaUtilDate())) {
					throw new ValidationError(
							"Invalid schedule end date/time: It is not after the schedule start date/time");
				}
				if (!repetitionSettings.endDateTimeVariant.getValue().toJavaUtilDate().after(new java.util.Date())) {
					throw new ValidationError("Invalid schedule end date/time: It has passed");
				}
			}
		} else {
			if (!startDateTimeVariant.getValue().toJavaUtilDate().after(new java.util.Date())) {
				throw new ValidationError("Invalid schedule start date/time: It has passed");
			}
		}
		if (timeZoneIdentifierVariant.getValue() != null) {
			if (!ZoneId.getAvailableZoneIds().contains(timeZoneIdentifierVariant.getValue())) {
				throw new ValidationError("Invalid time zone: '" + timeZoneIdentifierVariant.getValue() + "': Expected "
						+ ZoneId.getAvailableZoneIds());
			}
		}
	}

	private ScheduledFuture<?> prepare(Runnable task, java.util.Date start, boolean repeating, long periodLength,
			ChronoUnit periodUnit, java.util.Date end) {
		java.util.Date now = new java.util.Date();
		java.util.Date next;
		if (repeating) {
			next = computeNextOccurrence(start, periodLength, periodUnit, now);
			if (end != null) {
				if (end.before(next)) {
					return null;
				}
			}
		} else {
			next = start;
		}
		long nowMilliseconds = now.getTime();
		long nextMilliseconds = next.getTime();
		long delayMilliseconds = nextMilliseconds - nowMilliseconds;
		return scheduler.schedule(() -> {
			if (repeating) {
				prepare(task, start, repeating, periodLength, periodUnit, end);
			}
			task.run();
		}, delayMilliseconds, TimeUnit.MILLISECONDS);
	}

	private java.util.Date computeNextOccurrence(java.util.Date start, long periodLength, ChronoUnit periodUnit,
			java.util.Date now) {
		if (start.after(now)) {
			return start;
		}
		ZoneId timeZone = (timeZoneIdentifierVariant.getValue() != null)
				? ZoneId.of(timeZoneIdentifierVariant.getValue())
				: ZoneId.systemDefault();
		ZonedDateTime startZonedDateTime = start.toInstant().atZone(timeZone);
		ZonedDateTime nowZonedDateTime = now.toInstant().atZone(timeZone);
		long elapsed = periodUnit.between(startZonedDateTime, nowZonedDateTime);
		long missed = (elapsed / periodLength) + 1;
		return java.util.Date.from(startZonedDateTime.plus(missed * periodLength, periodUnit).toInstant());
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

	public static class Metadata implements ActivatorMetadata {

		@Override
		public String getActivatorName() {
			return "Schedule";
		}

		@Override
		public Class<? extends Activator> getActivatorClass() {
			return Schedule.class;
		}

		@Override
		public ResourcePath getActivatorIconImagePath() {
			return new ResourcePath(
					ResourcePath.specifyClassPathResourceLocation(Schedule.class.getName().replace(".", "/") + ".png"));
		}

	}

	static public class RepetitionSettingsStructure implements ActivatorStructure {

		private Variant<RepetitionSettingsStructurePeriodUnitStructure> periodUnitVariant = new Variant<RepetitionSettingsStructurePeriodUnitStructure>(
				RepetitionSettingsStructurePeriodUnitStructure.class,
				RepetitionSettingsStructurePeriodUnitStructure.DAYS);
		private Variant<Long> periodLengthVariant = new Variant<Long>(Long.class, 1l);
		private Variant<com.otk.jesb.meta.DateTime> endDateTimeVariant = new Variant<com.otk.jesb.meta.DateTime>(
				com.otk.jesb.meta.DateTime.class, null);

		public RepetitionSettingsStructure() {
		}

		public Variant<RepetitionSettingsStructurePeriodUnitStructure> getPeriodUnitVariant() {
			return periodUnitVariant;
		}

		public void setPeriodUnitVariant(Variant<RepetitionSettingsStructurePeriodUnitStructure> periodUnitVariant) {
			this.periodUnitVariant = periodUnitVariant;
		}

		public Variant<Long> getPeriodLengthVariant() {
			return periodLengthVariant;
		}

		public void setPeriodLengthVariant(Variant<Long> periodLengthVariant) {
			this.periodLengthVariant = periodLengthVariant;
		}

		public Variant<com.otk.jesb.meta.DateTime> getEndDateTimeVariant() {
			return endDateTimeVariant;
		}

		public void setEndDateTimeVariant(Variant<com.otk.jesb.meta.DateTime> endDateTimeVariant) {
			this.endDateTimeVariant = endDateTimeVariant;
		}

		@Override
		public String toString() {
			return "RepetitionSettingsStructure [periodUnitVariant=" + periodUnitVariant + ", periodLengthVariant="
					+ periodLengthVariant + ", endDateTimeVariant=" + endDateTimeVariant + "]";
		}

		public static void customizeUI(InfoCustomizations infoCustomizations) {
			// RepetitionSettingsStructure form customization
			{
				// field control positions
				InfoCustomizations.getTypeCustomization(infoCustomizations, RepetitionSettingsStructure.class.getName())
						.setCustomFieldsOrder(java.util.Arrays.asList("periodUnitVariant", "periodLengthVariant",
								"endDateTimeVariant"));
				// periodUnitVariant control customization
				{
					InfoCustomizations.getFieldCustomization(infoCustomizations,
							RepetitionSettingsStructure.class.getName(), "periodUnitVariant")
							.setCustomFieldCaption("Period Unit");
				}
				// periodLengthVariant control customization
				{
					InfoCustomizations.getFieldCustomization(infoCustomizations,
							RepetitionSettingsStructure.class.getName(), "periodLengthVariant")
							.setCustomFieldCaption("Period Length");
				}
				// endDateTimeVariant control customization
				{
					InfoCustomizations.getFieldCustomization(infoCustomizations,
							RepetitionSettingsStructure.class.getName(), "endDateTimeVariant")
							.setCustomFieldCaption("End At");
					InfoCustomizations.getFieldCustomization(infoCustomizations,
							com.otk.jesb.ui.GUI.VariantCustomizations.getAdapterTypeName(
									RepetitionSettingsStructure.class.getName(), "endDateTimeVariant"),
							com.otk.jesb.ui.GUI.VariantCustomizations.getConstantValueFieldName("endDateTimeVariant"))
							.setNullValueDistinctForced(true);
					InfoCustomizations
							.getTypeCustomization(
									InfoCustomizations
											.getFieldCustomization(infoCustomizations,
													com.otk.jesb.ui.GUI.VariantCustomizations.getAdapterTypeName(
															RepetitionSettingsStructure.class.getName(),
															"endDateTimeVariant"),
													com.otk.jesb.ui.GUI.VariantCustomizations
															.getConstantValueFieldName("endDateTimeVariant"))
											.getSpecificTypeCustomizations(),
									com.otk.jesb.meta.DateTime.class.getName())
							.setSpecificProperties(new java.util.HashMap<String, Object>() {
								private static final long serialVersionUID = 1L;
								{
									ReflectionUIUtils.setFieldControlPluginIdentifier(this,
											"com.otk.jesb.ui.GUI$JESDDateTimePickerPlugin");
									ReflectionUIUtils.setFieldControlPluginConfiguration(this,
											"com.otk.jesb.ui.GUI$JESDDateTimePickerPlugin",
											(java.io.Serializable) com.otk.jesb.PluginBuilder
													.readControlPluginConfiguration(
															"<xy.reflect.ui.control.swing.plugin.DateTimePickerPlugin_-DateTimePickerConfiguration><dateFormat>yyyy-MM-dd</dateFormat><timeFormat>HH:mm:ss</timeFormat></xy.reflect.ui.control.swing.plugin.DateTimePickerPlugin_-DateTimePickerConfiguration>"));
								}
							});
				}
				// hide UI customization method
				InfoCustomizations.getMethodCustomization(infoCustomizations,
						RepetitionSettingsStructure.class.getName(), ReflectionUIUtils.buildMethodSignature("void",
								"customizeUI", java.util.Arrays.asList(InfoCustomizations.class.getName())))
						.setHidden(true);
			}
		}

		@Override
		public void validate(boolean recursively, com.otk.jesb.solution.Plan plan) throws ValidationError {
			if (recursively) {
				try {
					periodLengthVariant.validate();
				} catch (ValidationError e) {
					throw new ValidationError("Failed to validate 'Period Length'", e);
				}
			}
			if (recursively) {
				try {
					endDateTimeVariant.validate();
				} catch (ValidationError e) {
					throw new ValidationError("Failed to validate 'End At'", e);
				}
			}
		}

		static public enum RepetitionSettingsStructurePeriodUnitStructure {
			MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS, MONTHS, YEARS;
		}

	}

}
