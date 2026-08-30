package com.otk.jesb;

import com.otk.jesb.activation.Activator;
import com.otk.jesb.activation.ActivatorMetadata;
import com.otk.jesb.activation.ActivationHandler;
import com.otk.jesb.Reference;
import com.otk.jesb.Variant;
import xy.reflect.ui.info.custom.InfoCustomizations;
import xy.reflect.ui.util.ReflectionUIUtils;
import com.otk.jesb.ValidationError;
import xy.reflect.ui.info.ResourcePath;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Solution;

public class ReceiveMessage extends Activator{

	private MessageKindStructure MessageKind=MessageKindStructure.Queue;
	private Reference<com.otk.jesb.JMSConnection> ConnectionReference=new Reference<com.otk.jesb.JMSConnection>(com.otk.jesb.JMSConnection.class);
	private Variant<String> DestinationVariant=new Variant<String>(String.class);
	
	private ActivationHandler activationHandler;
	
	public ReceiveMessage(){
	}
	
	public MessageKindStructure getMessageKind() {
		return MessageKind;
	}
	
	public void setMessageKind(MessageKindStructure MessageKind) {
		this.MessageKind = MessageKind;
	}
	
	public Reference<com.otk.jesb.JMSConnection> getConnectionReference() {
		return ConnectionReference;
	}
	
	public void setConnectionReference(Reference<com.otk.jesb.JMSConnection> ConnectionReference) {
		this.ConnectionReference = ConnectionReference;
	}
	
	public Variant<String> getDestinationVariant() {
		return DestinationVariant;
	}
	
	public void setDestinationVariant(Variant<String> DestinationVariant) {
		this.DestinationVariant = DestinationVariant;
	}
	
	@Override
	public String toString() {
		return "ReceiveMessage [MessageKind=" + MessageKind + ", ConnectionReference=" + ConnectionReference + ", DestinationVariant=" + DestinationVariant + "]";
	}
	
	@Override
	public Class<?> getInputClass(Solution solutionInstance) {
		return null;
	}
	
	@Override
	public Class<?> getOutputClass(Solution solutionInstance) {
		return null;
	}
	
	@Override
	public boolean isAutomaticallyTriggerable() {
		return true;
	}
	@Override
	public void initializeAutomaticTrigger(ActivationHandler activationHandler, Solution solutionInstance) throws Exception {
		this.activationHandler = activationHandler;
	}
	
	@Override
	public void finalizeAutomaticTrigger(Solution solutionInstance) throws Exception {
		this.activationHandler = null;
	}
	
	@Override
	public boolean isAutomaticTriggerReady() {
		return activationHandler != null;
	}
	
	public static xy.reflect.ui.info.type.factory.IInfoProxyFactory getUICustomizationsFactory(com.otk.jesb.ui.GUI.JESBSubCustomizedUI customizedUI) {
		return new xy.reflect.ui.info.type.factory.InfoCustomizationsFactory(customizedUI) {
			InfoCustomizations infoCustomizations = new InfoCustomizations();
			{
				customizeUI(infoCustomizations);
			}
			@Override
			public String getIdentifier() {
				return "MethodBasedSubInfoCustomizationsFactory [of=" + ReceiveMessage.class.getName() + "]";
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
		// ReceiveMessage form customization
		{
			// field control positions
			InfoCustomizations.getTypeCustomization(infoCustomizations, ReceiveMessage.class.getName())
			.setCustomFieldsOrder(java.util.Arrays.asList("MessageKind", "ConnectionReference", "DestinationVariant"));
			// MessageKind control customization
			{
				InfoCustomizations.getFieldCustomization(infoCustomizations, ReceiveMessage.class.getName(), "MessageKind")
				.setCustomFieldCaption("Message Kind");
			}
			// ConnectionReference control customization
			{
				InfoCustomizations.getFieldCustomization(infoCustomizations, ReceiveMessage.class.getName(), "ConnectionReference")
				.setCustomFieldCaption("Connection");
				InfoCustomizations.getFieldCustomization(infoCustomizations, ReceiveMessage.class.getName(), "ConnectionReference")
				.setFormControlEmbeddingForced(true);
			}
			// DestinationVariant control customization
			{
				InfoCustomizations.getFieldCustomization(infoCustomizations, com.otk.jesb.ui.GUI.VariantCustomizations.getAdapterTypeName(ReceiveMessage.class.getName(),"DestinationVariant"), com.otk.jesb.ui.GUI.VariantCustomizations.getConstantValueFieldName("DestinationVariant"))
				.setCustomFieldCaption("Destination");
			}
			// hide UI customization method
			InfoCustomizations.getMethodCustomization(infoCustomizations, ReceiveMessage.class.getName(), ReflectionUIUtils.buildMethodSignature("void", "customizeUI", java.util.Arrays.asList(InfoCustomizations.class.getName())))
			.setHidden(true);
			InfoCustomizations.getMethodCustomization(infoCustomizations, ReceiveMessage.class.getName(), ReflectionUIUtils.buildMethodSignature(xy.reflect.ui.info.type.factory.IInfoProxyFactory.class.getName(), "getUICustomizationsFactory", java.util.Arrays.asList(com.otk.jesb.ui.GUI.JESBSubCustomizedUI.class.getName())))
			.setHidden(true);
		}
	}
	
	@Override
	public void validate(boolean recursively, Solution solutionInstance, Plan plan) throws ValidationError {
		super.validate(recursively, solutionInstance, plan);
		if (ConnectionReference.resolve(solutionInstance) == null) {
			throw new ValidationError("Failed to resolve the 'Connection' reference");
		}
		if (recursively) {
			try {
				DestinationVariant.validate(solutionInstance);
			} catch (ValidationError e) {
				throw new ValidationError("Failed to validate 'Destination'", e);
			}
		}
	}
	
	public static class Metadata implements ActivatorMetadata{
		
		@Override
		public String getActivatorName() {
			return "Receive  Message";
		}
		
		@Override
		public Class<? extends Activator> getActivatorClass() {
			return ReceiveMessage.class;
		}
		
		@Override
		public ResourcePath getActivatorIconImagePath() {
			return new ResourcePath(ResourcePath.specifyClassPathResourceLocation(ReceiveMessage.class.getName().replace(".", "/") + ".png"));
		}
		
	}
	
	static public enum MessageKindStructure{
		Queue, Topic;
	}

}
