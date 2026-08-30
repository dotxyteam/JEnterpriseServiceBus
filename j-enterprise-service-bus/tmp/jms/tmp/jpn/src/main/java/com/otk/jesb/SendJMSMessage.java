package com.otk.jesb;

import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.Reference;
import xy.reflect.ui.info.custom.InfoCustomizations;
import xy.reflect.ui.util.ReflectionUIUtils;
import com.otk.jesb.ValidationError;
import xy.reflect.ui.info.ResourcePath;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Solution;

public class SendJMSMessage implements Operation{

	private final MessageKindStructure MessageKind;
	private final com.otk.jesb.JMSConnection Connection;
	private final String Destination;
	private final MessageBodyStructure messageBody;
	
	public SendJMSMessage(MessageKindStructure MessageKind, com.otk.jesb.JMSConnection Connection, String Destination, MessageBodyStructure messageBody){
		this.MessageKind=MessageKind;
		this.Connection=Connection;
		this.Destination=Destination;
		this.messageBody=messageBody;
	}
	
	public MessageKindStructure getMessageKind() {
		return MessageKind;
	}
	
	public com.otk.jesb.JMSConnection getConnection() {
		return Connection;
	}
	
	public String getDestination() {
		return Destination;
	}
	
	public MessageBodyStructure getMessageBody() {
		return messageBody;
	}
	
	@Override
	public String toString() {
		return "SendJMSMessage [MessageKind=" + MessageKind + ", Connection=" + Connection + ", Destination=" + Destination + ", messageBody=" + messageBody + "]";
	}
	
	@Override
	public Object execute(Solution solutionInstance) throws Throwable {
		return null;
	}
	
	static public class Builder implements OperationBuilder<SendJMSMessage>{
	
		private MessageKindStructure MessageKind;
		private Reference<com.otk.jesb.JMSConnection> ConnectionReference=new Reference<com.otk.jesb.JMSConnection>(com.otk.jesb.JMSConnection.class);
		private String Destination;
		private RootInstanceBuilder messageBodyDynamicBuilder=new RootInstanceBuilder("messageBody", MessageBodyStructure.class.getName());
		
		public Builder(){
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
		
		public String getDestination() {
			return Destination;
		}
		
		public void setDestination(String Destination) {
			this.Destination = Destination;
		}
		
		public RootInstanceBuilder getMessageBodyDynamicBuilder() {
			return messageBodyDynamicBuilder;
		}
		
		public void setMessageBodyDynamicBuilder(RootInstanceBuilder messageBodyDynamicBuilder) {
			this.messageBodyDynamicBuilder = messageBodyDynamicBuilder;
		}
		
		@Override
		public String toString() {
			return "Builder [MessageKind=" + MessageKind + ", ConnectionReference=" + ConnectionReference + ", Destination=" + Destination + ", messageBodyDynamicBuilder=" + messageBodyDynamicBuilder + "]";
		}
		
		@Override
		public SendJMSMessage build(Plan.ExecutionContext context, Plan.ExecutionInspector executionInspector) throws Exception {
			Solution solutionInstance = context.getSession().getSolutionInstance();
			MessageKindStructure MessageKind = this.MessageKind;
			com.otk.jesb.JMSConnection Connection = this.ConnectionReference.resolve(solutionInstance);
			String Destination = this.Destination;
			MessageBodyStructure messageBody = (MessageBodyStructure) this.messageBodyDynamicBuilder.build(new InstantiationContext(context.getVariables(), context.getPlan().getValidationContext(context.getCurrentStep(), solutionInstance).getVariableDeclarations(), solutionInstance));
			return new SendJMSMessage(MessageKind, Connection, Destination, messageBody);
		}
		@Override
		public Class<?> getOperationResultClass(Solution solutionInstance, Plan currentPlan, Step currentStep) {
			return null;
		}
		@Override
		public void validate(boolean recursively, Solution solutionInstance, Plan currentPlan, Step currentStep) throws ValidationError{
			if (ConnectionReference.resolve(solutionInstance) == null) {
				throw new ValidationError("Failed to resolve the 'Connection' reference");
			}
			if (recursively) {
				try {
					messageBodyDynamicBuilder.getFacade(solutionInstance).validate(recursively, currentPlan.getValidationContext(currentStep, solutionInstance).getVariableDeclarations());
				} catch (ValidationError e) {
					throw new ValidationError("Failed to validate 'messageBody'", e);
				}
			}
		}
		public static xy.reflect.ui.info.type.factory.IInfoProxyFactory getUICustomizationsFactory(com.otk.jesb.ui.GUI.JESBSubCustomizedUI customizedUI) {
			return new xy.reflect.ui.info.type.factory.InfoCustomizationsFactory(customizedUI) {
				InfoCustomizations infoCustomizations = new InfoCustomizations();
				{
					customizeUI(infoCustomizations);
				}
				@Override
				public String getIdentifier() {
					return "MethodBasedSubInfoCustomizationsFactory [of=" + SendJMSMessage.Builder.class.getName() + "]";
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
			// SendJMSMessage.Builder form customization
			{
				// field control positions
				InfoCustomizations.getTypeCustomization(infoCustomizations, SendJMSMessage.Builder.class.getName())
				.setCustomFieldsOrder(java.util.Arrays.asList("MessageKind", "ConnectionReference", "Destination", "messageBodyDynamicBuilder"));
				// MessageKind control customization
				{
					InfoCustomizations.getFieldCustomization(infoCustomizations, SendJMSMessage.Builder.class.getName(), "MessageKind")
					.setCustomFieldCaption("Message Kind");
				}
				// ConnectionReference control customization
				{
					InfoCustomizations.getFieldCustomization(infoCustomizations, SendJMSMessage.Builder.class.getName(), "ConnectionReference")
					.setCustomFieldCaption("Connection");
					InfoCustomizations.getFieldCustomization(infoCustomizations, SendJMSMessage.Builder.class.getName(), "ConnectionReference")
					.setFormControlEmbeddingForced(true);
				}
				// Destination control customization
				{
					InfoCustomizations.getFieldCustomization(infoCustomizations, SendJMSMessage.Builder.class.getName(), "Destination")
					.setCustomFieldCaption("Destination");
				}
				// messageBodyDynamicBuilder control customization
				{
					InfoCustomizations.getFieldCustomization(infoCustomizations, SendJMSMessage.Builder.class.getName(), "messageBodyDynamicBuilder")
					.setFormControlEmbeddingForced(true);
					InfoCustomizations.getFieldCustomization(infoCustomizations, SendJMSMessage.Builder.class.getName(), "messageBodyDynamicBuilder")
					.setValueValidityDetectionForced(true);
				}
				// hide UI customization method
				InfoCustomizations.getMethodCustomization(infoCustomizations, SendJMSMessage.Builder.class.getName(), ReflectionUIUtils.buildMethodSignature("void", "customizeUI", java.util.Arrays.asList(InfoCustomizations.class.getName())))
				.setHidden(true);
				InfoCustomizations.getMethodCustomization(infoCustomizations, SendJMSMessage.Builder.class.getName(), ReflectionUIUtils.buildMethodSignature(xy.reflect.ui.info.type.factory.IInfoProxyFactory.class.getName(), "getUICustomizationsFactory", java.util.Arrays.asList(com.otk.jesb.ui.GUI.JESBSubCustomizedUI.class.getName())))
				.setHidden(true);
			}
		}
		
	
	}
	
	
	public static class Metadata implements OperationMetadata<SendJMSMessage>{
		
		@Override
		public String getOperationTypeName() {
			return "Send JMS Message";
		}
		
		@Override
		public String getCategoryName() {
			return "null";
		}
		
		@Override
		public Class<? extends OperationBuilder<SendJMSMessage>> getOperationBuilderClass() {
			return Builder.class;
		}
		
		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(ResourcePath.specifyClassPathResourceLocation(SendJMSMessage.class.getName().replace(".", "/") + ".png"));
		}
		
	}
	
	static public enum MessageKindStructure{
		Queue, Topic;
	}
	static public class MessageBodyStructure{
	
		public final String text;
		
		public MessageBodyStructure(String text){
			this.text=text;
		}
		
		@Override
		public String toString() {
			return "MessageBodyStructure [text=" + text + "]";
		}
		
	
	}
	

}
