package com.otk;

public class Schedule extends com.otk.jesb.resource.Resource {
	private com.otk.jesb.Variant<com.otk.jesb.meta.DateTime> momentVariant = new com.otk.jesb.Variant<com.otk.jesb.meta.DateTime>(
			com.otk.jesb.meta.DateTime.class, com.otk.jesb.meta.DateTime.NOW);
	private com.otk.jesb.Variant<java.lang.Boolean> repeatingVariant = new com.otk.jesb.Variant<java.lang.Boolean>(
			java.lang.Boolean.class, false);
	private RepetitionSettingsStructure.GroupBuilder repetitionSettingsGroupBuilder = new RepetitionSettingsStructure.GroupBuilder();

	public Schedule() {

	}

	public com.otk.jesb.Variant<com.otk.jesb.meta.DateTime> getMomentVariant() {
		return momentVariant;
	}

	public void setMomentVariant(com.otk.jesb.Variant<com.otk.jesb.meta.DateTime> momentVariant) {
		this.momentVariant = momentVariant;
	}

	public com.otk.jesb.Variant<java.lang.Boolean> getRepeatingVariant() {
		return repeatingVariant;
	}

	public void setRepeatingVariant(com.otk.jesb.Variant<java.lang.Boolean> repeatingVariant) {
		this.repeatingVariant = repeatingVariant;
	}

	public RepetitionSettingsStructure.GroupBuilder getRepetitionSettingsGroupBuilder() {
		return repetitionSettingsGroupBuilder;
	}

	public void setRepetitionSettingsGroupBuilder(
			RepetitionSettingsStructure.GroupBuilder repetitionSettingsGroupBuilder) {
		this.repetitionSettingsGroupBuilder = repetitionSettingsGroupBuilder;
	}

	@Override
	public String toString() {
		return "Schedule [momentVariant=" + momentVariant + ", repeatingVariant=" + repeatingVariant
				+ ", repetitionSettingsGroupBuilder=" + repetitionSettingsGroupBuilder + "]";
	}

	@Override
	public void validate(boolean recursively) {
	}

	public class Metadata implements com.otk.jesb.resource.ResourceMetadata {
		@Override
		public String getResourceTypeName() {
			return "Schedule";
		}

		@Override
		public Class<? extends com.otk.jesb.resource.Resource> getResourceClass() {
			return Schedule.class;
		}

		@Override
		public xy.reflect.ui.info.ResourcePath getResourceIconImagePath() {
			return new xy.reflect.ui.info.ResourcePath(xy.reflect.ui.info.ResourcePath
					.specifyClassPathResourceLocation(Schedule.class.getName().replace(".", "/") + ".png"));
		}
	}

	static public class RepetitionSettingsStructure {
		private final RepetitionSettingsStructurePeriodUnitStructure periodUnit;
		private final long period;

		public RepetitionSettingsStructure(RepetitionSettingsStructurePeriodUnitStructure periodUnit, long period) {
			this.periodUnit = periodUnit;
			this.period = period;
		}

		public RepetitionSettingsStructurePeriodUnitStructure getPeriodUnit() {
			return periodUnit;
		}

		public long getPeriod() {
			return period;
		}

		@Override
		public String toString() {
			return "RepetitionSettingsStructure [periodUnit=" + periodUnit + ", period=" + period + "]";
		}

		static public class GroupBuilder {
			private com.otk.jesb.Variant<RepetitionSettingsStructurePeriodUnitStructure> periodUnitVariant = new com.otk.jesb.Variant<RepetitionSettingsStructurePeriodUnitStructure>(
					RepetitionSettingsStructurePeriodUnitStructure.class,
					RepetitionSettingsStructurePeriodUnitStructure.MINUTES);
			private com.otk.jesb.Variant<java.lang.Long> periodVariant = new com.otk.jesb.Variant<java.lang.Long>(
					java.lang.Long.class, 1l);

			public GroupBuilder() {

			}

			public com.otk.jesb.Variant<RepetitionSettingsStructurePeriodUnitStructure> getPeriodUnitVariant() {
				return periodUnitVariant;
			}

			public void setPeriodUnitVariant(
					com.otk.jesb.Variant<RepetitionSettingsStructurePeriodUnitStructure> periodUnitVariant) {
				this.periodUnitVariant = periodUnitVariant;
			}

			public com.otk.jesb.Variant<java.lang.Long> getPeriodVariant() {
				return periodVariant;
			}

			public void setPeriodVariant(com.otk.jesb.Variant<java.lang.Long> periodVariant) {
				this.periodVariant = periodVariant;
			}

			@Override
			public String toString() {
				return "GroupBuilder [periodUnitVariant=" + periodUnitVariant + ", periodVariant=" + periodVariant
						+ "]";
			}

			public RepetitionSettingsStructure build(com.otk.jesb.solution.Plan.ExecutionContext context,
					com.otk.jesb.solution.Plan.ExecutionInspector executionInspector) throws Exception {
				RepetitionSettingsStructurePeriodUnitStructure periodUnit = this.periodUnitVariant.getValue();
				long period = this.periodVariant.getValue();
				return new RepetitionSettingsStructure(periodUnit, period);
			}

			public void validate(boolean recursively, com.otk.jesb.solution.Plan currentPlan,
					com.otk.jesb.solution.Step currentStep) {

			}

		}

		static public enum RepetitionSettingsStructurePeriodUnitStructure {
			MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS, MONTHS, YEARS;
		}
	}
}