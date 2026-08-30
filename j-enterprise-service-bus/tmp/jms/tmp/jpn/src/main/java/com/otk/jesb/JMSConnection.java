package com.otk.jesb;

import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.ResourceMetadata;
import xy.reflect.ui.info.custom.InfoCustomizations;
import xy.reflect.ui.util.ReflectionUIUtils;
import com.otk.jesb.ValidationError;
import xy.reflect.ui.info.ResourcePath;
import com.otk.jesb.solution.Solution;

public class JMSConnection extends Resource{

	
	public JMSConnection(String name) {
		super(name);
	}
	public JMSConnection(){
	}
	
	@Override
	public String toString() {
		return "JMSConnection []";
	}
	
	public static xy.reflect.ui.info.type.factory.IInfoProxyFactory getUICustomizationsFactory(com.otk.jesb.ui.GUI.JESBSubCustomizedUI customizedUI) {
		return new xy.reflect.ui.info.type.factory.InfoCustomizationsFactory(customizedUI) {
			InfoCustomizations infoCustomizations = new InfoCustomizations();
			{
				customizeUI(infoCustomizations);
			}
			@Override
			public String getIdentifier() {
				return "MethodBasedSubInfoCustomizationsFactory [of=" + JMSConnection.class.getName() + "]";
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
	public static void customizeUI(InfoCustomizations infoCustomizations) {
		// JMSConnection form customization
		{
			// field control positions
			InfoCustomizations.getTypeCustomization(infoCustomizations, JMSConnection.class.getName())
			.setCustomFieldsOrder(java.util.Arrays.asList());
			// hide UI customization method
			InfoCustomizations.getMethodCustomization(infoCustomizations, JMSConnection.class.getName(), ReflectionUIUtils.buildMethodSignature("void", "customizeUI", java.util.Arrays.asList(InfoCustomizations.class.getName())))
			.setHidden(true);
			InfoCustomizations.getMethodCustomization(infoCustomizations, JMSConnection.class.getName(), ReflectionUIUtils.buildMethodSignature(xy.reflect.ui.info.type.factory.IInfoProxyFactory.class.getName(), "getUICustomizationsFactory", java.util.Arrays.asList(com.otk.jesb.ui.GUI.JESBSubCustomizedUI.class.getName())))
			.setHidden(true);
		}
	}
	
	@Override
	public void validate(boolean recursively, Solution solutionInstance) throws ValidationError {
	}
	
	public static class Metadata implements ResourceMetadata{
		
		@Override
		public String getResourceTypeName() {
			return "JMS Connection";
		}
		
		@Override
		public Class<? extends Resource> getResourceClass() {
			return JMSConnection.class;
		}
		
		@Override
		public ResourcePath getResourceIconImagePath() {
			return new ResourcePath(ResourcePath.specifyClassPathResourceLocation(JMSConnection.class.getName().replace(".", "/") + ".png"));
		}
		
	}
	

}
