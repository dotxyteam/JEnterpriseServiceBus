package com.otk5;
public class Schedule extends com.otk.jesb.activation.Activator{
private com.otk.jesb.Variant<com.otk.jesb.meta.DateTime> momentVariant=new com.otk.jesb.Variant<com.otk.jesb.meta.DateTime>(com.otk.jesb.meta.DateTime.class, com.otk.jesb.meta.DateTime.NOW);
private com.otk.jesb.Variant<java.lang.Boolean> repeatingVariant=new com.otk.jesb.Variant<java.lang.Boolean>(java.lang.Boolean.class, false);
private RepetitionSettingsStructure repetitionSettings=new RepetitionSettingsStructure();
private com.otk.jesb.Reference<com.otk.jesb.resource.builtin.SharedStructureModel> referenceReference=new com.otk.jesb.Reference<com.otk.jesb.resource.builtin.SharedStructureModel>(com.otk.jesb.resource.builtin.SharedStructureModel.class);
public Schedule(){

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

public RepetitionSettingsStructure getRepetitionSettings() {
return repetitionSettings;
}
public void setRepetitionSettings(RepetitionSettingsStructure repetitionSettings) {
this.repetitionSettings = repetitionSettings;
}

public com.otk.jesb.Reference<com.otk.jesb.resource.builtin.SharedStructureModel> getReferenceReference() {
return referenceReference;
}
public void setReferenceReference(com.otk.jesb.Reference<com.otk.jesb.resource.builtin.SharedStructureModel> referenceReference) {
this.referenceReference = referenceReference;
}

@Override
public String toString() {
return "Schedule [momentVariant=" + momentVariant + ", repeatingVariant=" + repeatingVariant + ", repetitionSettings=" + repetitionSettings + ", referenceReference=" + referenceReference + "]";
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
}@Override
public void initializeAutomaticTrigger(com.otk.jesb.activation.ActivationHandler activationHandler) throws Exception {
}
@Override
public void finalizeAutomaticTrigger() throws Exception {
}
@Override
public boolean isAutomaticTriggerReady() {
return false;
}
public static void customizeUI(xy.reflect.ui.info.custom.InfoCustomizations infoCustomizations) {
/* com.otk5.Schedule form customization */
{
/* field control positions */
xy.reflect.ui.info.custom.InfoCustomizations.getTypeCustomization(infoCustomizations, com.otk5.Schedule.class.getName()).setCustomFieldsOrder(java.util.Arrays.asList("moment", "repeating", "repetitionSettings", "reference"));
/* moment control customization */
{
xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations, com.otk5.Schedule.class.getName(), "moment").setCustomFieldCaption("moment");
}
/* repeating control customization */
{
xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations, com.otk5.Schedule.class.getName(), "repeating").setCustomFieldCaption("Repeat");
}
/* repetitionSettings control customization */
{
xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations, com.otk5.Schedule.class.getName(), "repetitionSettings").setCustomFieldCaption("Repetition Settings");
xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations, com.otk5.Schedule.class.getName(), "repetitionSettings").setFormControlEmbeddingForced(true);
RepetitionSettingsStructure.customizeUI(infoCustomizations);
}
/* reference control customization */
{
xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations, com.otk5.Schedule.class.getName(), "reference").setCustomFieldCaption("Referennce");
xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations, com.otk5.Schedule.class.getName(), "referenceReference").setFormControlEmbeddingForced(true);
}
/* hide UI customization method */
xy.reflect.ui.info.custom.InfoCustomizations.getMethodCustomization(infoCustomizations, com.otk5.Schedule.class.getName(), xy.reflect.ui.util.ReflectionUIUtils.buildMethodSignature("void", "customizeUI", java.util.Arrays.asList(xy.reflect.ui.info.custom.InfoCustomizations.class.getName()))).setHidden(true);
}
}
@Override
public void validate(boolean recursively, com.otk.jesb.solution.Plan plan) {
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
public static class Metadata implements com.otk.jesb.activation.ActivatorMetadata{
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
return new xy.reflect.ui.info.ResourcePath(xy.reflect.ui.info.ResourcePath.specifyClassPathResourceLocation(Schedule.class.getName().replace(".", "/") + ".png"));
}
}

static public class RepetitionSettingsStructure implements com.otk.jesb.activation.ActivatorStructure{

public RepetitionSettingsStructure(){

}

@Override
public String toString() {
return "RepetitionSettingsStructure []";
}
public static void customizeUI(xy.reflect.ui.info.custom.InfoCustomizations infoCustomizations) {
/* RepetitionSettingsStructure form customization */
{
/* field control positions */
xy.reflect.ui.info.custom.InfoCustomizations.getTypeCustomization(infoCustomizations, RepetitionSettingsStructure.class.getName()).setCustomFieldsOrder(java.util.Arrays.asList());
/* hide UI customization method */
xy.reflect.ui.info.custom.InfoCustomizations.getMethodCustomization(infoCustomizations, RepetitionSettingsStructure.class.getName(), xy.reflect.ui.util.ReflectionUIUtils.buildMethodSignature("void", "customizeUI", java.util.Arrays.asList(xy.reflect.ui.info.custom.InfoCustomizations.class.getName()))).setHidden(true);
}
}
@Override
public void validate(boolean recursively, com.otk.jesb.solution.Plan plan) {
}




}
}