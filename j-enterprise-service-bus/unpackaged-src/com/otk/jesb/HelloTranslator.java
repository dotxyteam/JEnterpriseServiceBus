package com.otk.jesb;

import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.ResourceMetadata;
import com.otk.jesb.Variant;
import xy.reflect.ui.info.custom.InfoCustomizations;
import xy.reflect.ui.util.ReflectionUIUtils;
import com.otk.jesb.ValidationError;
import xy.reflect.ui.info.ResourcePath;
import com.otk.jesb.solution.Solution;

public class HelloTranslator extends Resource{

	private Variant<LanguageStructure> languageVariant=new Variant<LanguageStructure>(LanguageStructure.class, LanguageStructure.English);
	
	
	public HelloTranslator(String name) {
		super(name);
	}
	public HelloTranslator(){
	}
	
	public Variant<LanguageStructure> getLanguageVariant() {
		return languageVariant;
	}
	
	public void setLanguageVariant(Variant<LanguageStructure> languageVariant) {
		this.languageVariant = languageVariant;
	}
	
	@Override
	public String toString() {
		return "HelloTranslator [languageVariant=" + languageVariant + "]";
	}
	
	public static void customizeUI(InfoCustomizations infoCustomizations) {
		// HelloTranslator form customization
		{
			// field control positions
			InfoCustomizations.getTypeCustomization(infoCustomizations, HelloTranslator.class.getName())
			.setCustomFieldsOrder(java.util.Arrays.asList("languageVariant"));
			// languageVariant control customization
			{
				InfoCustomizations.getFieldCustomization(infoCustomizations, com.otk.jesb.ui.GUI.VariantCustomizations.getAdapterTypeName(HelloTranslator.class.getName(),"languageVariant"), com.otk.jesb.ui.GUI.VariantCustomizations.getConstantValueFieldName("languageVariant"))
				.setCustomFieldCaption("Language");
			}
			// hide UI customization method
			InfoCustomizations.getMethodCustomization(infoCustomizations, HelloTranslator.class.getName(), ReflectionUIUtils.buildMethodSignature("void", "customizeUI", java.util.Arrays.asList(InfoCustomizations.class.getName())))
			.setHidden(true);
		}
	}public static xy.reflect.ui.info.type.factory.IInfoProxyFactory getUICustomizationsFactory(com.otk.jesb.ui.GUI.JESBSubCustomizedUI customizedUI) {
		return new xy.reflect.ui.info.type.factory.InfoCustomizationsFactory(customizedUI) {
			InfoCustomizations infoCustomizations = new InfoCustomizations();
			{
				customizeUI(infoCustomizations);
			}
			@Override
			public String getIdentifier() {
				return "MethodBasedSubInfoCustomizationsFactory [of=" + HelloTranslator.class.getName() + "]";
			}
			@Override
			protected xy.reflect.ui.info.type.factory.IInfoProxyFactory getInfoCustomizationsSetupFactory() {
				return xy.reflect.ui.info.type.factory.IInfoProxyFactory.NULL_INFO_PROXY_FACTORY;
			}
			@Override
			public InfoCustomizations accessInfoCustomizations() {
				return infoCustomizations;
			}
		};
	}
	
	@Override
	public void validate(boolean recursively, Solution solutionInstance) throws ValidationError {
	}
	
	public static class Metadata implements ResourceMetadata{
		
		@Override
		public String getResourceTypeName() {
			return "Hello Translator";
		}
		
		@Override
		public Class<? extends Resource> getResourceClass() {
			return HelloTranslator.class;
		}
		
		@Override
		public ResourcePath getResourceIconImagePath() {
			return new ResourcePath(ResourcePath.specifyClassPathResourceLocation(HelloTranslator.class.getName().replace(".", "/") + ".png"));
		}
		
	}
	
	public String translateHello(String string, Solution solutionInstance) {
	    LanguageStructure language = languageVariant.getValue(solutionInstance);
	    if (language == LanguageStructure.English) {
	        return string;
	    } else if (language == LanguageStructure.French) {
	        return string.replace("Hello", "Bonjour");
	    } else if (language == LanguageStructure.Spanish) {
	        return string.replace("Hello", "Hola");
	    } else {
	        throw new IllegalStateException();
	    }
	}
	
	static public enum LanguageStructure{
		English, French, Spanish;
	}

}
