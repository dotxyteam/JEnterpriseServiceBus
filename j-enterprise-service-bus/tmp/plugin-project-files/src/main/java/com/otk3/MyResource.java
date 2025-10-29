package com.otk3;

import java.io.Serializable;
import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.ResourceStructure;
import com.otk.jesb.resource.ResourceMetadata;
import com.otk.jesb.Variant;
import xy.reflect.ui.info.custom.InfoCustomizations;
import xy.reflect.ui.util.ReflectionUIUtils;
import com.otk.jesb.ValidationError;
import xy.reflect.ui.info.ResourcePath;

public class MyResource extends Resource{

	private String prop1;
	private Variant<Integer> prop2Variant=new Variant<Integer>(Integer.class, -1);
	private Prop3Structure prop3=new Prop3Structure();
	
	public MyResource(){
	}
	
	public String getProp1() {
		return prop1;
	}
	
	public void setProp1(String prop1) {
		this.prop1 = prop1;
	}
	
	public Variant<Integer> getProp2Variant() {
		return prop2Variant;
	}
	
	public void setProp2Variant(Variant<Integer> prop2Variant) {
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
	
	public static void customizeUI(InfoCustomizations infoCustomizations) {
		// MyResource form customization
		{
			// field control positions
			InfoCustomizations.getTypeCustomization(infoCustomizations, MyResource.class.getName())
			.setCustomFieldsOrder(java.util.Arrays.asList("prop1", "prop2Variant", "prop3"));
			// prop1 control customization
			{
				InfoCustomizations.getFieldCustomization(infoCustomizations, MyResource.class.getName(), "prop1")
				.setCustomFieldCaption("1st Property");
			}
			// prop2Variant control customization
			{
				InfoCustomizations.getFieldCustomization(infoCustomizations, MyResource.class.getName(), "prop2Variant")
				.setCustomFieldCaption("2nd Property");
				InfoCustomizations.getFieldCustomization(infoCustomizations, com.otk.jesb.ui.GUI.VariantCustomizations.getAdapterTypeName(MyResource.class.getName(),"prop2Variant"), com.otk.jesb.ui.GUI.VariantCustomizations.getConstantValueFieldName("prop2Variant"))
				.setNullValueDistinctForced(true);
				InfoCustomizations.getTypeCustomization(InfoCustomizations.getFieldCustomization(infoCustomizations, com.otk.jesb.ui.GUI.VariantCustomizations.getAdapterTypeName(MyResource.class.getName(),"prop2Variant"), com.otk.jesb.ui.GUI.VariantCustomizations.getConstantValueFieldName("prop2Variant"))
				.getSpecificTypeCustomizations(), Integer.class.getName()).setSpecificProperties(new java.util.HashMap<String, Object>() {
					private static final long serialVersionUID = 1L;
					{
						ReflectionUIUtils.setFieldControlPluginIdentifier(this, "xy.reflect.ui.control.swing.plugin.SpinnerPlugin");
						ReflectionUIUtils.setFieldControlPluginConfiguration(this, "xy.reflect.ui.control.swing.plugin.SpinnerPlugin", (Serializable) com.otk.jesb.PluginBuilder.readControlPluginConfiguration("<xy.reflect.ui.control.swing.plugin.SpinnerPlugin_-SpinnerConfiguration><minimum>-100</minimum><maximum>100</maximum><stepSize>1</stepSize></xy.reflect.ui.control.swing.plugin.SpinnerPlugin_-SpinnerConfiguration>"));
					}
				});
			}
			// prop3 control customization
			{
				InfoCustomizations.getFieldCustomization(infoCustomizations, MyResource.class.getName(), "prop3")
				.setCustomFieldCaption("3rd Property");
				InfoCustomizations.getFieldCustomization(infoCustomizations, MyResource.class.getName(), "prop3")
				.setValueValidityDetectionForced(true);
				InfoCustomizations.getFieldCustomization(infoCustomizations, MyResource.class.getName(), "prop3")
				.setNullValueDistinctForced(true);
				InfoCustomizations.getFieldCustomization(infoCustomizations, MyResource.class.getName(), "prop3").
				setFormControlEmbeddingForced(true);
				Prop3Structure.customizeUI(infoCustomizations);
			}
			// hide UI customization method
			InfoCustomizations.getMethodCustomization(infoCustomizations, MyResource.class.getName(), ReflectionUIUtils.buildMethodSignature("void", "customizeUI", java.util.Arrays.asList(InfoCustomizations.class.getName())))
			.setHidden(true);
		}
	}
	
	@Override
	public void validate(boolean recursively) throws ValidationError {
		if (recursively) {
			try {
				prop2Variant.validate();
			} catch (ValidationError e) {
				throw new ValidationError("Failed to validate '2nd Property'", e);
			}
		}
		if (recursively) {
			try {
				prop3.validate(recursively);
			} catch (ValidationError e) {
				throw new ValidationError("Failed to validate '3rd Property'", e);
			}
		}
	}
	
	public static class Metadata implements ResourceMetadata{
		
		@Override
		public String getResourceTypeName() {
			return "My Resource";
		}
		
		@Override
		public Class<? extends Resource> getResourceClass() {
			return MyResource.class;
		}
		
		@Override
		public ResourcePath getResourceIconImagePath() {
			return new ResourcePath(ResourcePath.specifyClassPathResourceLocation(MyResource.class.getName().replace(".", "/") + ".png"));
		}
		
	}
	
	static public class Prop3Structure implements ResourceStructure{
	
		private String subProp1;
		private Integer subProp2=10;
		
		public Prop3Structure(){
		}
		
		public String getSubProp1() {
			return subProp1;
		}
		
		public void setSubProp1(String subProp1) {
			this.subProp1 = subProp1;
		}
		
		public Integer getSubProp2() {
			return subProp2;
		}
		
		public void setSubProp2(Integer subProp2) {
			this.subProp2 = subProp2;
		}
		
		@Override
		public String toString() {
			return "Prop3Structure [subProp1=" + subProp1 + ", subProp2=" + subProp2 + "]";
		}
		
		public static void customizeUI(InfoCustomizations infoCustomizations) {
			// Prop3Structure form customization
			{
				// field control positions
				InfoCustomizations.getTypeCustomization(infoCustomizations, Prop3Structure.class.getName())
				.setCustomFieldsOrder(java.util.Arrays.asList("subProp1", "subProp2"));
				// subProp1 control customization
				{
					InfoCustomizations.getFieldCustomization(infoCustomizations, Prop3Structure.class.getName(), "subProp1")
					.setCustomFieldCaption("Sub-Property 1");
				}
				// subProp2 control customization
				{
				}
				// hide UI customization method
				InfoCustomizations.getMethodCustomization(infoCustomizations, Prop3Structure.class.getName(), ReflectionUIUtils.buildMethodSignature("void", "customizeUI", java.util.Arrays.asList(InfoCustomizations.class.getName())))
				.setHidden(true);
			}
		}
		
		@Override
		public void validate(boolean recursively) throws ValidationError {
		}
		
		
		
	
	}
	

}
