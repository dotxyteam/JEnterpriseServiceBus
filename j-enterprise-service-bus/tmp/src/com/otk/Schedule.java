package com.otk;

public class Schedule extends com.otk.jesb.resource.Resource {
	private com.otk.jesb.Variant<com.otk.jesb.meta.DateTime> momentVariant = new com.otk.jesb.Variant<com.otk.jesb.meta.DateTime>(
			com.otk.jesb.meta.DateTime.class, com.otk.jesb.meta.DateTime.NOW);
	private com.otk.jesb.Variant<java.lang.Boolean> repeatingVariant = new com.otk.jesb.Variant<java.lang.Boolean>(
			java.lang.Boolean.class, false);
	private com.otk.jesb.Variant<java.lang.Long> periodVariant = new com.otk.jesb.Variant<java.lang.Long>(
			java.lang.Long.class, 1l);

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

	public com.otk.jesb.Variant<java.lang.Long> getPeriodVariant() {
		return periodVariant;
	}

	public void setPeriodVariant(com.otk.jesb.Variant<java.lang.Long> periodVariant) {
		this.periodVariant = periodVariant;
	}

	@Override
	public String toString() {
		return "Schedule [momentVariant=" + momentVariant + ", repeatingVariant=" + repeatingVariant
				+ ", periodVariant=" + periodVariant + "]";
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

}