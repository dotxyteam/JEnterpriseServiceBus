package com.otk.jesb.operation.builtin;

import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;

import java.io.Serializable;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Destination;
import javax.jms.Message;

import org.apache.activemq.ActiveMQConnectionFactory;

import com.otk.jesb.Reference;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.activation.builtin.ReceiveJMSMessage.MessageTypeStructure;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;

import xy.reflect.ui.info.ResourcePath;
import com.otk.jesb.solution.Step;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Solution;

public class SendJMSMessage implements Operation {

	public static void main(String[] args) throws Exception {
		ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
		Connection jmsAPIConnection = connectionFactory.createConnection();
		jmsAPIConnection.start();
		Session session = jmsAPIConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Queue queue = session.createQueue("aze");
		MessageProducer producer = session.createProducer(queue);
		Message message = session.createMessage();
		producer.send(queue, message);
		jmsAPIConnection.close();
	}

	private final MessageKindStructure messageKind;
	private final com.otk.jesb.resource.builtin.JMSConnection connection;
	private final DestinationStructure destination;
	private final MessageBodyStructure messageBody;
	private MessageTypeStructure messageType;

	public SendJMSMessage(MessageKindStructure messageKind, MessageTypeStructure messageType,
			com.otk.jesb.resource.builtin.JMSConnection connection, DestinationStructure destination,
			MessageBodyStructure messageBody) {
		this.messageKind = messageKind;
		this.messageType = messageType;
		this.connection = connection;
		this.destination = destination;
		this.messageBody = messageBody;
	}

	public MessageKindStructure getMessageKind() {
		return messageKind;
	}

	public MessageTypeStructure getMessageType() {
		return messageType;
	}

	public com.otk.jesb.resource.builtin.JMSConnection getConnection() {
		return connection;
	}

	public DestinationStructure getDestination() {
		return destination;
	}

	public MessageBodyStructure getMessageBody() {
		return messageBody;
	}

	@Override
	public String toString() {
		return "SendJMSMessage [messageKind=" + messageKind + ", connection=" + connection + ", destination="
				+ destination + "]";
	}

	@Override
	public Object execute(Solution solutionInstance) throws Throwable {
		ConnectionFactory connectionFactory = connection.getConnectionFactory(solutionInstance);
		Connection jmsAPIConnection = connectionFactory.createConnection();
		jmsAPIConnection.start();
		Session session = jmsAPIConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Destination jmsAPIDestination;
		if (messageKind == MessageKindStructure.Queue) {
			jmsAPIDestination = session.createQueue(destination.value);
		} else if (messageKind == MessageKindStructure.Topic) {
			jmsAPIDestination = session.createTopic(destination.value);
		} else {
			throw new UnexpectedError();
		}
		MessageProducer producer = session.createProducer(jmsAPIDestination);
		Message message;
		if (messageType == MessageTypeStructure.Text) {
			message = session.createTextMessage();
			((TextMessage) message).setText(((TextMessageBodyStructure) messageBody).text);
		} else if (messageType == MessageTypeStructure.Bytes) {
			message = session.createBytesMessage();
			((BytesMessage) message).writeBytes(((BytesMessageBodyStructure) messageBody).bytes);
		} else if (messageType == MessageTypeStructure.Object) {
			message = session.createObjectMessage();
			((ObjectMessage) message).setObject(((ObjectMessageBodyStructure) messageBody).object);
		} else if (messageType == MessageTypeStructure.Simple) {
			message = session.createMessage();
		} else {
			throw new UnexpectedError();
		}
		producer.send(jmsAPIDestination, message);
		jmsAPIConnection.close();
		return null;
	}

	static public class Builder implements OperationBuilder<SendJMSMessage> {

		private MessageKindStructure messageKind = MessageKindStructure.Queue;
		private MessageTypeStructure messageType = MessageTypeStructure.Text;
		private Reference<com.otk.jesb.resource.builtin.JMSConnection> connectionReference = new Reference<com.otk.jesb.resource.builtin.JMSConnection>(
				com.otk.jesb.resource.builtin.JMSConnection.class);
		private RootInstanceBuilder destinationDynamicBuilder = new RootInstanceBuilder("destination",
				new DestinationStructureClassNameAccessor());
		private RootInstanceBuilder messageBodyDynamicBuilder = new RootInstanceBuilder("messageBody",
				new MessageBodyStructureClassNameAccessor());

		public Builder() {
		}

		public RootInstanceBuilder getDestinationDynamicBuilder() {
			return destinationDynamicBuilder;
		}

		public void setDestinationDynamicBuilder(RootInstanceBuilder destinationDynamicBuilder) {
			this.destinationDynamicBuilder = destinationDynamicBuilder;
		}

		public RootInstanceBuilder getMessageBodyDynamicBuilder() {
			return messageBodyDynamicBuilder;
		}

		public void setMessageBodyDynamicBuilder(RootInstanceBuilder messageBodyDynamicBuilder) {
			this.messageBodyDynamicBuilder = messageBodyDynamicBuilder;
		}

		public MessageKindStructure getMessageKind() {
			return messageKind;
		}

		public void setMessageKind(MessageKindStructure messageKind) {
			this.messageKind = messageKind;
		}

		public MessageTypeStructure getMessageType() {
			return messageType;
		}

		public void setMessageType(MessageTypeStructure messageType) {
			this.messageType = messageType;
		}

		public Reference<com.otk.jesb.resource.builtin.JMSConnection> getConnectionReference() {
			return connectionReference;
		}

		public void setConnectionReference(Reference<com.otk.jesb.resource.builtin.JMSConnection> connectionReference) {
			this.connectionReference = connectionReference;
		}

		@Override
		public String toString() {
			return "Builder [messageKind=" + messageKind + ", messageType=" + messageType + ", connectionReference="
					+ connectionReference + ", destinationDynamicBuilder=" + destinationDynamicBuilder
					+ ", messageBodyDynamicBuilder=" + messageBodyDynamicBuilder + "]";
		}

		@Override
		public SendJMSMessage build(Plan.ExecutionContext context, Plan.ExecutionInspector executionInspector)
				throws Exception {
			Solution solutionInstance = context.getSession().getSolutionInstance();
			MessageKindStructure messageKind = this.messageKind;
			com.otk.jesb.resource.builtin.JMSConnection connection = this.connectionReference.resolve(solutionInstance);
			DestinationStructure destination = (DestinationStructure) this.destinationDynamicBuilder
					.build(new InstantiationContext(context.getVariables(), context.getPlan()
							.getValidationContext(context.getCurrentStep(), solutionInstance).getVariableDeclarations(),
							solutionInstance));
			MessageBodyStructure messageBody = (MessageBodyStructure) this.messageBodyDynamicBuilder
					.build(new InstantiationContext(context.getVariables(), context.getPlan()
							.getValidationContext(context.getCurrentStep(), solutionInstance).getVariableDeclarations(),
							solutionInstance));
			return new SendJMSMessage(messageKind, messageType, connection, destination, messageBody);
		}

		@Override
		public Class<?> getOperationResultClass(Solution solutionInstance, Plan currentPlan, Step currentStep) {
			return null;
		}

		@Override
		public void validate(boolean recursively, Solution solutionInstance, Plan currentPlan, Step currentStep)
				throws ValidationError {
			if (connectionReference.resolve(solutionInstance) == null) {
				throw new ValidationError("Failed to resolve the 'Connection' reference");
			}
		}

		public class MessageBodyStructureClassNameAccessor extends Accessor<Solution, String> {
			@Override
			public String get(Solution solutionInstance) {
				if (messageType == MessageTypeStructure.Text) {
					return TextMessageBodyStructure.class.getName();
				} else if (messageType == MessageTypeStructure.Bytes) {
					return BytesMessageBodyStructure.class.getName();
				} else if (messageType == MessageTypeStructure.Object) {
					return ObjectMessageBodyStructure.class.getName();
				} else if (messageType == MessageTypeStructure.Simple) {
					return SimpleMessageBodyStructure.class.getName();
				} else {
					throw new UnexpectedError();
				}
			}
		}

		public class DestinationStructureClassNameAccessor extends Accessor<Solution, String> {
			@Override
			public String get(Solution solutionInstance) {
				return DestinationStructure.class.getName();
			}
		}
	}

	public static class Metadata implements OperationMetadata<SendJMSMessage> {

		@Override
		public String getOperationTypeName() {
			return "Send JMS Message";
		}

		@Override
		public String getCategoryName() {
			return "JMS";
		}

		@Override
		public Class<? extends OperationBuilder<SendJMSMessage>> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(SendJMSMessage.class.getName().replace(".", "/") + ".png"));
		}

	}

	static public enum MessageKindStructure {
		Queue, Topic;
	}

	static public abstract class MessageBodyStructure {
		@Override
		public String toString() {
			return "MessageBodyStructure []";
		}

	}

	static public class TextMessageBodyStructure extends MessageBodyStructure {

		public String text;
	}

	static public class BytesMessageBodyStructure extends MessageBodyStructure {

		public byte[] bytes;

	}

	static public class ObjectMessageBodyStructure extends MessageBodyStructure {

		public Serializable object;

	}

	static public class SimpleMessageBodyStructure extends MessageBodyStructure {

	}

	static public class DestinationStructure {

		public String value;

	}
}
