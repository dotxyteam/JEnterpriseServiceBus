package com.otk3;
public class Schedule extends com.otk.jesb.activation.Activator{
private com.otk.jesb.Variant<com.otk.jesb.meta.DateTime> momentVariant=new com.otk.jesb.Variant<com.otk.jesb.meta.DateTime>(com.otk.jesb.meta.DateTime.class, com.otk.jesb.meta.DateTime.NOW);
private com.otk.jesb.Variant<java.lang.Boolean> repeatingVariant=new com.otk.jesb.Variant<java.lang.Boolean>(java.lang.Boolean.class, false);
private RepetitionSettingsStructure.GroupBuilder repetitionSettingsGroupBuilder=new RepetitionSettingsStructure.GroupBuilder();
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

public RepetitionSettingsStructure.GroupBuilder getRepetitionSettingsGroupBuilder() {
return repetitionSettingsGroupBuilder;
}
public void setRepetitionSettingsGroupBuilder(RepetitionSettingsStructure.GroupBuilder repetitionSettingsGroupBuilder) {
this.repetitionSettingsGroupBuilder = repetitionSettingsGroupBuilder;
}

public com.otk.jesb.Reference<com.otk.jesb.resource.builtin.SharedStructureModel> getReferenceReference() {
return referenceReference;
}
public void setReferenceReference(com.otk.jesb.Reference<com.otk.jesb.resource.builtin.SharedStructureModel> referenceReference) {
this.referenceReference = referenceReference;
}

@Override
public String toString() {
return "Schedule [momentVariant=" + momentVariant + ", repeatingVariant=" + repeatingVariant + ", repetitionSettingsGroupBuilder=" + repetitionSettingsGroupBuilder + ", referenceReference=" + referenceReference + "]";
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

static public class RepetitionSettingsStructure{
private final RepetitionSettingsStructurePeriodUnitStructure periodUnit;
private final long period;
public RepetitionSettingsStructure(RepetitionSettingsStructurePeriodUnitStructure periodUnit, long period){
this.periodUnit=periodUnit;
this.period=period;
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
static public class GroupBuilder implements com.otk.jesb.operation.ParameterBuilder<RepetitionSettingsStructure>{
private com.otk.jesb.Variant<RepetitionSettingsStructurePeriodUnitStructure> periodUnitVariant=new com.otk.jesb.Variant<RepetitionSettingsStructurePeriodUnitStructure>(RepetitionSettingsStructurePeriodUnitStructure.class, RepetitionSettingsStructurePeriodUnitStructure.MINUTES);
private com.otk.jesb.Variant<java.lang.Long> periodVariant=new com.otk.jesb.Variant<java.lang.Long>(java.lang.Long.class, 1l);
public GroupBuilder(){

}
public com.otk.jesb.Variant<RepetitionSettingsStructurePeriodUnitStructure> getPeriodUnitVariant() {
return periodUnitVariant;
}
public void setPeriodUnitVariant(com.otk.jesb.Variant<RepetitionSettingsStructurePeriodUnitStructure> periodUnitVariant) {
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
return "GroupBuilder [periodUnitVariant=" + periodUnitVariant + ", periodVariant=" + periodVariant + "]";
}
@Override
public RepetitionSettingsStructure build(com.otk.jesb.solution.Plan.ExecutionContext context, com.otk.jesb.solution.Plan.ExecutionInspector executionInspector) throws Exception {
RepetitionSettingsStructurePeriodUnitStructure periodUnit = this.periodUnitVariant.getValue();
long period = this.periodVariant.getValue();
return new RepetitionSettingsStructure(periodUnit, period);
}
@Override
public void validate(boolean recursively, com.otk.jesb.solution.Plan currentPlan, com.otk.jesb.solution.Step currentStep) {

}
public static void customizeUI(xy.reflect.ui.info.custom.InfoCustomizations infoCustomizations) {
/* RepetitionSettingsStructure.GroupBuilder form customization */
{
/* field control positions */
xy.reflect.ui.info.custom.InfoCustomizations.getTypeCustomization(infoCustomizations, RepetitionSettingsStructure.GroupBuilder.class.getName()).setCustomFieldsOrder(java.util.Arrays.asList("periodUnitVariant", "periodVariant"));
/* periodUnitVariant control customization */
{
xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations, RepetitionSettingsStructure.GroupBuilder.class.getName(), "periodUnitVariant").setCustomFieldCaption("Period Unit");
}
/* periodVariant control customization */
{
xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations, RepetitionSettingsStructure.GroupBuilder.class.getName(), "periodVariant").setCustomFieldCaption("Period");
}
/* hide UI customization method */
xy.reflect.ui.info.custom.InfoCustomizations.getMethodCustomization(infoCustomizations, RepetitionSettingsStructure.GroupBuilder.class.getName(), xy.reflect.ui.util.ReflectionUIUtils.buildMethodSignature("void", "customizeUI", java.util.Arrays.asList(xy.reflect.ui.info.custom.InfoCustomizations.class.getName()))).setHidden(true);
/* hide 'build(...)' method */
xy.reflect.ui.info.custom.InfoCustomizations.getMethodCustomization(infoCustomizations, RepetitionSettingsStructure.GroupBuilder.class.getName(), xy.reflect.ui.util.ReflectionUIUtils.buildMethodSignature(RepetitionSettingsStructure.class.getName(), "build", java.util.Arrays.asList(com.otk.jesb.solution.Plan.ExecutionContext.class.getName(), com.otk.jesb.solution.Plan.ExecutionInspector.class.getName()))).setHidden(true);
}
}


}
static public enum RepetitionSettingsStructurePeriodUnitStructure{
MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS, MONTHS, YEARS;
}
}
}