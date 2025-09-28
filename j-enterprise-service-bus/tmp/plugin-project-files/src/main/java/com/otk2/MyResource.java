package com.otk2;

public class MyResource extends com.otk.jesb.resource.Resource{
	private java.lang.String prop1;
	private com.otk.jesb.Variant<java.lang.Integer> prop2Variant=new com.otk.jesb.Variant<java.lang.Integer>(java.lang.Integer.class, -1);
	private Prop3Structure prop3=new Prop3Structure();
	
	public MyResource(){
		
	}
	public java.lang.String getProp1() {
		return prop1;
	}
	public void setProp1(java.lang.String prop1) {
		this.prop1 = prop1;
	}
	
	public com.otk.jesb.Variant<java.lang.Integer> getProp2Variant() {
		return prop2Variant;
	}
	public void setProp2Variant(com.otk.jesb.Variant<java.lang.Integer> prop2Variant) {
		this.prop2Variant = prop2Variant;
	}
	
	public Prop3Structure getProp3() {
		return prop3;
	}
	public void setProp3(Prop3Structure prop3) {
		this.prop3 = prop3;
	}
	
	@Override
	public String toString() {
		return "MyResource [prop1=" + prop1 + ", prop2Variant=" + prop2Variant + ", prop3=" + prop3 + "]";
	}
	public static void customizeUI(xy.reflect.ui.info.custom.InfoCustomizations infoCustomizations) {
		/* com.otk2.MyResource form customization */
		{
			/* field control positions */
			xy.reflect.ui.info.custom.InfoCustomizations.getTypeCustomization(infoCustomizations, com.otk2.MyResource.class.getName()).setCustomFieldsOrder(java.util.Arrays.asList("prop1", "prop2Variant", "prop3"));
			/* prop1 control customization */
			{
				xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations, com.otk2.MyResource.class.getName(), "prop1").setCustomFieldCaption("1st Property");
			}
			/* prop2Variant control customization */
			{
				xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations, com.otk2.MyResource.class.getName(), "prop2Variant").setCustomFieldCaption("2nd Property");
				xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations, com.otk.jesb.ui.GUI.VariantCustomizations.getAdapterTypeName(com.otk2.MyResource.class.getName(),"prop2Variant"), com.otk.jesb.ui.GUI.VariantCustomizations.getConstantValueFieldName("prop2Variant")).setNullValueDistinctForced(true);
				xy.reflect.ui.info.custom.InfoCustomizations.getTypeCustomization(xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations, com.otk.jesb.ui.GUI.VariantCustomizations.getAdapterTypeName(com.otk2.MyResource.class.getName(),"prop2Variant"), com.otk.jesb.ui.GUI.VariantCustomizations.getConstantValueFieldName("prop2Variant")).getSpecificTypeCustomizations(), java.lang.Integer.class.getName()).setSpecificProperties(new java.util.HashMap<String, Object>() {
					private static final long serialVersionUID = 1L;
					{
						xy.reflect.ui.util.ReflectionUIUtils.setFieldControlPluginIdentifier(this, "xy.reflect.ui.control.swing.plugin.SpinnerPlugin");
						xy.reflect.ui.util.ReflectionUIUtils.setFieldControlPluginConfiguration(this, "xy.reflect.ui.control.swing.plugin.SpinnerPlugin", (java.io.Serializable) com.otk.jesb.PluginBuilder.readControlPluginConfiguration("<xy.reflect.ui.control.swing.plugin.SpinnerPlugin_-SpinnerConfiguration><minimum>-100</minimum><maximum>100</maximum><stepSize>1</stepSize></xy.reflect.ui.control.swing.plugin.SpinnerPlugin_-SpinnerConfiguration>"));
					}
				});
			}
			/* prop3 control customization */
			{
				xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations, com.otk2.MyResource.class.getName(), "prop3").setCustomFieldCaption("3rd Property");
				xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations, com.otk2.MyResource.class.getName(), "prop3").setValueValidityDetectionForced(true);
				xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations, com.otk2.MyResource.class.getName(), "prop3").setNullValueDistinctForced(true);
				xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations, com.otk2.MyResource.class.getName(), "prop3").setFormControlEmbeddingForced(true);
				Prop3Structure.customizeUI(infoCustomizations);
			}
			/* hide UI customization method */
			xy.reflect.ui.info.custom.InfoCustomizations.getMethodCustomization(infoCustomizations, com.otk2.MyResource.class.getName(), xy.reflect.ui.util.ReflectionUIUtils.buildMethodSignature("void", "customizeUI", java.util.Arrays.asList(xy.reflect.ui.info.custom.InfoCustomizations.class.getName()))).setHidden(true);
		}
	}
	@Override
	public void validate(boolean recursively) throws com.otk.jesb.ValidationError {
	if (recursively) {
		try {
			prop2Variant.validate();
		} catch (com.otk.jesb.ValidationError e) {
			throw new com.otk.jesb.ValidationError("Failed to validate '2nd Property'", e);
		}
	}
	if (recursively) {
		try {
			prop3.validate(recursively);
		} catch (com.otk.jesb.ValidationError e) {
			throw new com.otk.jesb.ValidationError("Failed to validate '3rd Property'", e);
		}
	}
	}
	public static class Metadata implements com.otk.jesb.resource.ResourceMetadata{
		@Override
		public String getResourceTypeName() {
			return "My Resource";
		}
		@Override
		public Class<? extends com.otk.jesb.resource.Resource> getResourceClass() {
			return MyResource.class;
		}
		@Override
		public xy.reflect.ui.info.ResourcePath getResourceIconImagePath() {
			return new xy.reflect.ui.info.ResourcePath(xy.reflect.ui.info.ResourcePath.specifyClassPathResourceLocation(MyResource.class.getName().replace(".", "/") + ".png"));
		}
	}
	
	static 
	public class Prop3Structure implements com.otk.jesb.resource.ResourceStructure{
		private java.lang.String subProp1;
		private java.lang.Integer subProp2=10;
		
		public Prop3Structure(){
			
		}
		public java.lang.String getSubProp1() {
			return subProp1;
		}
		public void setSubProp1(java.lang.String subProp1) {
			this.subProp1 = subProp1;
		}
		
		public java.lang.Integer getSubProp2() {
			return subProp2;
		}
		public void setSubProp2(java.lang.Integer subProp2) {
			this.subProp2 = subProp2;
		}
		
		@Override
		public String toString() {
			return "Prop3Structure [subProp1=" + subProp1 + ", subProp2=" + subProp2 + "]";
		}
		public static void customizeUI(xy.reflect.ui.info.custom.InfoCustomizations infoCustomizations) {
			/* Prop3Structure form customization */
			{
				/* field control positions */
				xy.reflect.ui.info.custom.InfoCustomizations.getTypeCustomization(infoCustomizations, Prop3Structure.class.getName()).setCustomFieldsOrder(java.util.Arrays.asList("subProp1", "subProp2"));
				/* subProp1 control customization */
				{
					xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations, Prop3Structure.class.getName(), "subProp1").setCustomFieldCaption("Sub-Property 1");
				}
				/* subProp2 control customization */
				{
				}
				/* hide UI customization method */
				xy.reflect.ui.info.custom.InfoCustomizations.getMethodCustomization(infoCustomizations, Prop3Structure.class.getName(), xy.reflect.ui.util.ReflectionUIUtils.buildMethodSignature("void", "customizeUI", java.util.Arrays.asList(xy.reflect.ui.info.custom.InfoCustomizations.class.getName()))).setHidden(true);
			}
		}
		@Override
		public void validate(boolean recursively) throws com.otk.jesb.ValidationError {
		}
		
		
		
	}
}