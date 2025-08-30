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

		public RepetitionSettingsStructure() {

		}

		@Override
		public String toString() {
			return "RepetitionSettingsStructure []";
		}

		static public class GroupBuilder {

			public GroupBuilder() {

			}

			@Override
			public String toString() {
				return "GroupBuilder []";
			}

			public RepetitionSettingsStructure build(com.otk.jesb.solution.Plan.ExecutionContext context,
					com.otk.jesb.solution.Plan.ExecutionInspector executionInspector) throws Exception {
				return new RepetitionSettingsStructure();
			}

			public void validate(boolean recursively, com.otk.jesb.solution.Plan currentPlan,
					com.otk.jesb.solution.Step currentStep) {

			}

		}

	}
}