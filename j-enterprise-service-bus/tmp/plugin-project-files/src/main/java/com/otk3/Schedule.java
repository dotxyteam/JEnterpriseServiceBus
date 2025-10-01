package com.otk3;

import com.otk.jesb.activation.Activator;
import com.otk.jesb.activation.ActivatorStructure;
import com.otk.jesb.activation.ActivatorMetadata;
import com.otk.jesb.activation.ActivationHandler;
import com.otk.jesb.Reference;
import com.otk.jesb.Variant;
import xy.reflect.ui.info.custom.InfoCustomizations;
import xy.reflect.ui.util.ReflectionUIUtils;
import com.otk.jesb.ValidationError;
import xy.reflect.ui.info.ResourcePath;

public class Schedule extends Activator{

	private Variant<com.otk.jesb.meta.DateTime> startVariant=new Variant<com.otk.jesb.meta.DateTime>(com.otk.jesb.meta.DateTime.class, com.otk.jesb.meta.DateTime.now());
	private Variant<Boolean> repeatingVariant=new Variant<Boolean>(Boolean.class, false);
	private RepetitionSettingsStructure repetitionSettings=new RepetitionSettingsStructure();
	private Reference<com.otk.jesb.resource.builtin.SharedStructureModel> referenceReference=new Reference<com.otk.jesb.resource.builtin.SharedStructureModel>(com.otk.jesb.resource.builtin.SharedStructureModel.class);
	
	private ActivationHandler activationHandler;
	
	public Schedule(){
	}
	
	public Variant<com.otk.jesb.meta.DateTime> getStartVariant() {
		return startVariant;
	}
	
	public void setStartVariant(Variant<com.otk.jesb.meta.DateTime> startVariant) {
		this.startVariant = startVariant;
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
	
	public Reference<com.otk.jesb.resource.builtin.SharedStructureModel> getReferenceReference() {
		return referenceReference;
	}
	
	public void setReferenceReference(Reference<com.otk.jesb.resource.builtin.SharedStructureModel> referenceReference) {
		this.referenceReference = referenceReference;
	}
	
	@Override
	public String toString() {
		return "Schedule [startVariant=" + startVariant + ", repeatingVariant=" + repeatingVariant + ", repetitionSettings=" + repetitionSettings + ", referenceReference=" + referenceReference + "]";
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
		new java.util.Timer().schedule(new java.util.TimerTask(){
		        @Override
		        public void run(){
		               
		        }
		    }, 
		    startVariant.getValue().toJavaUtilDate()
		);
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
	
	public static void customizeUI(InfoCustomizations infoCustomizations) {
		// com.otk3.Schedule form customization
		{
			// field control positions
			InfoCustomizations.getTypeCustomization(infoCustomizations, com.otk3.Schedule.class.getName())
			.setCustomFieldsOrder(java.util.Arrays.asList("startVariant", "repeatingVariant", "repetitionSettings", "referenceReference"));
			// startVariant control customization
			{
				InfoCustomizations.getFieldCustomization(infoCustomizations, com.otk3.Schedule.class.getName(), "startVariant")
				.setCustomFieldCaption("Start At");
				InfoCustomizations.getTypeCustomization(InfoCustomizations.getFieldCustomization(infoCustomizations, com.otk.jesb.ui.GUI.VariantCustomizations.getAdapterTypeName(com.otk3.Schedule.class.getName(),"startVariant"), com.otk.jesb.ui.GUI.VariantCustomizations.getConstantValueFieldName("startVariant"))
				.getSpecificTypeCustomizations(), com.otk.jesb.meta.DateTime.class.getName()).setSpecificProperties(new java.util.HashMap<String, Object>() {
					private static final long serialVersionUID = 1L;
					{
						ReflectionUIUtils.setFieldControlPluginIdentifier(this, "com.otk.jesb.ui.GUI$JESDDateTimePickerPlugin");
						ReflectionUIUtils.setFieldControlPluginConfiguration(this, "com.otk.jesb.ui.GUI$JESDDateTimePickerPlugin", (java.io.Serializable) com.otk.jesb.PluginBuilder.readControlPluginConfiguration("<xy.reflect.ui.control.swing.plugin.DateTimePickerPlugin_-DateTimePickerConfiguration><dateFormat>yyyy-MM-dd</dateFormat><timeFormat>HH:mm:ss</timeFormat></xy.reflect.ui.control.swing.plugin.DateTimePickerPlugin_-DateTimePickerConfiguration>"));
					}
				});
			}
			// repeatingVariant control customization
			{
				InfoCustomizations.getFieldCustomization(infoCustomizations, com.otk3.Schedule.class.getName(), "repeatingVariant")
				.setCustomFieldCaption("Repeat");
			}
			// repetitionSettings control customization
			{
				InfoCustomizations.getFieldCustomization(infoCustomizations, com.otk3.Schedule.class.getName(), "repetitionSettings")
				.setCustomFieldCaption("Repetition Settings");
				InfoCustomizations.getFieldCustomization(infoCustomizations, com.otk3.Schedule.class.getName(), "repetitionSettings")
				.setValueValidityDetectionForced(true);
				InfoCustomizations.getFieldCustomization(infoCustomizations, com.otk3.Schedule.class.getName(), "repetitionSettings").setFormControlEmbeddingForced(true);
				RepetitionSettingsStructure.customizeUI(infoCustomizations);
			}
			// referenceReference control customization
			{
				InfoCustomizations.getFieldCustomization(infoCustomizations, com.otk3.Schedule.class.getName(), "referenceReference")
				.setCustomFieldCaption("Referennce");
				InfoCustomizations.getFieldCustomization(infoCustomizations, com.otk3.Schedule.class.getName(), "referenceReference")
				.setFormControlEmbeddingForced(true);
			}
			// hide UI customization method
			InfoCustomizations.getMethodCustomization(infoCustomizations, com.otk3.Schedule.class.getName(), ReflectionUIUtils.buildMethodSignature("void", "customizeUI", java.util.Arrays.asList(InfoCustomizations.class.getName())))
			.setHidden(true);
		}
	}
	
	@Override
	public void validate(boolean recursively, com.otk.jesb.solution.Plan plan) throws ValidationError {
		super.validate(recursively, plan);
		if (recursively) {
			try {
				startVariant.validate();
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
		if (referenceReference.resolve() == null) {
			throw new ValidationError("Failed to resolve the 'Referennce' reference");
		}
	}
	
	static public class InputClassStructure{
	
		public final com.otk.jesb.meta.DateTime activationMoment;
		
		public InputClassStructure(com.otk.jesb.meta.DateTime activationMoment){
			this.activationMoment=activationMoment;
		}
		
		@Override
		public String toString() {
			return "InputClassStructure [activationMoment=" + activationMoment + "]";
		}
		
	
	}
	
	public static class Metadata implements ActivatorMetadata{
		
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
			return new ResourcePath(ResourcePath.specifyClassPathResourceLocation(Schedule.class.getName().replace(".", "/") + ".png"));
		}
		
	}
	
	static public class RepetitionSettingsStructure implements ActivatorStructure{
	
		private Variant<java.util.concurrent.TimeUnit> periodUnitVariant=new Variant<java.util.concurrent.TimeUnit>(java.util.concurrent.TimeUnit.class, java.util.concurrent.TimeUnit.DAYS);
		private Variant<Long> periodLengthVariant=new Variant<Long>(Long.class, 1l);
		private Variant<com.otk.jesb.meta.DateTime> endVariant=new Variant<com.otk.jesb.meta.DateTime>(com.otk.jesb.meta.DateTime.class, null);
		
		
		
		public RepetitionSettingsStructure(){
		}
		
		public Variant<java.util.concurrent.TimeUnit> getPeriodUnitVariant() {
			return periodUnitVariant;
		}
		
		public void setPeriodUnitVariant(Variant<java.util.concurrent.TimeUnit> periodUnitVariant) {
			this.periodUnitVariant = periodUnitVariant;
		}
		
		public Variant<Long> getPeriodLengthVariant() {
			return periodLengthVariant;
		}
		
		public void setPeriodLengthVariant(Variant<Long> periodLengthVariant) {
			this.periodLengthVariant = periodLengthVariant;
		}
		
		public Variant<com.otk.jesb.meta.DateTime> getEndVariant() {
			return endVariant;
		}
		
		public void setEndVariant(Variant<com.otk.jesb.meta.DateTime> endVariant) {
			this.endVariant = endVariant;
		}
		
		@Override
		public String toString() {
			return "RepetitionSettingsStructure [periodUnitVariant=" + periodUnitVariant + ", periodLengthVariant=" + periodLengthVariant + ", endVariant=" + endVariant + "]";
		}
		
		
		
		
		public static void customizeUI(InfoCustomizations infoCustomizations) {
			// RepetitionSettingsStructure form customization
			{
				// field control positions
				InfoCustomizations.getTypeCustomization(infoCustomizations, RepetitionSettingsStructure.class.getName())
				.setCustomFieldsOrder(java.util.Arrays.asList("periodUnitVariant", "periodLengthVariant", "endVariant"));
				// periodUnitVariant control customization
				{
					InfoCustomizations.getFieldCustomization(infoCustomizations, RepetitionSettingsStructure.class.getName(), "periodUnitVariant")
					.setCustomFieldCaption("Period Unit");
				}
				// periodLengthVariant control customization
				{
					InfoCustomizations.getFieldCustomization(infoCustomizations, RepetitionSettingsStructure.class.getName(), "periodLengthVariant")
					.setCustomFieldCaption("Period Length");
				}
				// endVariant control customization
				{
					InfoCustomizations.getFieldCustomization(infoCustomizations, RepetitionSettingsStructure.class.getName(), "endVariant")
					.setCustomFieldCaption("End At");
					InfoCustomizations.getFieldCustomization(infoCustomizations, com.otk.jesb.ui.GUI.VariantCustomizations.getAdapterTypeName(RepetitionSettingsStructure.class.getName(),"endVariant"), com.otk.jesb.ui.GUI.VariantCustomizations.getConstantValueFieldName("endVariant"))
					.setNullValueDistinctForced(true);
					InfoCustomizations.getTypeCustomization(InfoCustomizations.getFieldCustomization(infoCustomizations, com.otk.jesb.ui.GUI.VariantCustomizations.getAdapterTypeName(RepetitionSettingsStructure.class.getName(),"endVariant"), com.otk.jesb.ui.GUI.VariantCustomizations.getConstantValueFieldName("endVariant"))
					.getSpecificTypeCustomizations(), com.otk.jesb.meta.DateTime.class.getName()).setSpecificProperties(new java.util.HashMap<String, Object>() {
						private static final long serialVersionUID = 1L;
						{
							ReflectionUIUtils.setFieldControlPluginIdentifier(this, "com.otk.jesb.ui.GUI$JESDDateTimePickerPlugin");
							ReflectionUIUtils.setFieldControlPluginConfiguration(this, "com.otk.jesb.ui.GUI$JESDDateTimePickerPlugin", (java.io.Serializable) com.otk.jesb.PluginBuilder.readControlPluginConfiguration("<xy.reflect.ui.control.swing.plugin.DateTimePickerPlugin_-DateTimePickerConfiguration><dateFormat>yyyy-MM-dd</dateFormat><timeFormat>HH:mm:ss</timeFormat></xy.reflect.ui.control.swing.plugin.DateTimePickerPlugin_-DateTimePickerConfiguration>"));
						}
					});
				}
				// hide UI customization method
				InfoCustomizations.getMethodCustomization(infoCustomizations, RepetitionSettingsStructure.class.getName(), ReflectionUIUtils.buildMethodSignature("void", "customizeUI", java.util.Arrays.asList(InfoCustomizations.class.getName())))
				.setHidden(true);
			}
		}
		
		@Override
		public void validate(boolean recursively, com.otk.jesb.solution.Plan plan) throws ValidationError {
				if (recursively) {
				try {
					periodUnitVariant.validate();
				} catch (ValidationError e) {
					throw new ValidationError("Failed to validate 'Period Unit'", e);
				}
			}
			if (recursively) {
				try {
					periodLengthVariant.validate();
				} catch (ValidationError e) {
					throw new ValidationError("Failed to validate 'Period Length'", e);
				}
			}
			if (recursively) {
				try {
					endVariant.validate();
				} catch (ValidationError e) {
					throw new ValidationError("Failed to validate 'End At'", e);
				}
			}
		}
		
		
		
	
	}
	

}
