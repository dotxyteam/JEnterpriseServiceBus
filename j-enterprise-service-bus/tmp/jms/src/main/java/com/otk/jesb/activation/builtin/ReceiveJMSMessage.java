package com.otk.jesb.activation.builtin;

import com.otk.jesb.activation.Activator;
import com.otk.jesb.activation.ActivatorMetadata;
import com.otk.jesb.resource.builtin.ActiveMQConnection;
import com.otk.jesb.resource.builtin.JMSConnection;
import com.otk.jesb.activation.ActivationHandler;

import java.io.Serializable;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import com.otk.jesb.Log;
import com.otk.jesb.Reference;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.Variant;
import com.otk.jesb.ValidationError;
import xy.reflect.ui.info.ResourcePath;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.util.MiscUtils;

public class ReceiveJMSMessage extends Activator {

	public static void main(String[] args) throws Exception {
		Solution solutionInstance = new Solution();
		ActiveMQConnection connection = new ActiveMQConnection();
		ConnectionFactory connectionFactory = connection.getConnectionFactory(solutionInstance);
		Connection jmsAPIConnection = connectionFactory.createConnection();
		Session session = jmsAPIConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Queue queue = session.createQueue("aze");
		MessageProducer producer = session.createProducer(queue);
		TextMessage message = session.createTextMessage();
		message.setText("Hello world");
		producer.send(queue, message);
		MessageConsumer consumer = session.createConsumer(queue);
		jmsAPIConnection.start();
		message = (TextMessage) consumer.receive();
		System.out.println(message);
		jmsAPIConnection.close();
	}

	private MessageKindStructure messageKind = MessageKindStructure.Queue;
	private MessageTypeStructure messageType = MessageTypeStructure.Text;
	private Reference<com.otk.jesb.resource.builtin.JMSConnection> connectionReference = new Reference<com.otk.jesb.resource.builtin.JMSConnection>(
			com.otk.jesb.resource.builtin.JMSConnection.class);
	private Variant<String> destinationVariant = new Variant<String>(String.class);

	private ActivationHandler activationHandler;
	private Thread thread;
	private Connection jmsAPIConnection;

	public ReceiveJMSMessage() {
	}

	public MessageKindStructure getMessageKind() {
		return messageKind;
	}

	public MessageTypeStructure getMessageType() {
		return messageType;
	}

	public void setMessageType(MessageTypeStructure messageType) {
		this.messageType = messageType;
	}

	public void setMessageKind(MessageKindStructure messageKind) {
		this.messageKind = messageKind;
	}

	public Reference<com.otk.jesb.resource.builtin.JMSConnection> getConnectionReference() {
		return connectionReference;
	}

	public void setConnectionReference(Reference<com.otk.jesb.resource.builtin.JMSConnection> connectionReference) {
		this.connectionReference = connectionReference;
	}

	public Variant<String> getDestinationVariant() {
		return destinationVariant;
	}

	public void setDestinationVariant(Variant<String> destinationVariant) {
		this.destinationVariant = destinationVariant;
	}

	@Override
	public String toString() {
		return "ReceiveJMSMessage [messageKind=" + messageKind + ", connectionReference=" + connectionReference
				+ ", destinationVariant=" + destinationVariant + "]";
	}

	@Override
	public Class<?> getInputClass(Solution solutionInstance) {
		if (messageType == MessageTypeStructure.Text) {
			return TextInput.class;
		} else if (messageType == MessageTypeStructure.Bytes) {
			return BytesInput.class;
		} else if (messageType == MessageTypeStructure.Object) {
			return ObjectInput.class;
		} else if (messageType == MessageTypeStructure.Simple) {
			return SimpleInput.class;
		} else {
			throw new UnexpectedError();
		}
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
	public void initializeAutomaticTrigger(ActivationHandler activationHandler, Solution solutionInstance)
			throws Exception {
		this.activationHandler = activationHandler;
		JMSConnection connection = connectionReference.resolve(solutionInstance);
		ConnectionFactory connectionFactory = connection.getConnectionFactory(solutionInstance);
		jmsAPIConnection = connectionFactory.createConnection();
		jmsAPIConnection.start();
		Session session = jmsAPIConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Destination jmsAPIDestination;
		if (messageKind == MessageKindStructure.Queue) {
			jmsAPIDestination = session.createQueue(destinationVariant.getValue(solutionInstance));
		} else if (messageKind == MessageKindStructure.Topic) {
			jmsAPIDestination = session.createTopic(destinationVariant.getValue(solutionInstance));
		} else {
			throw new UnexpectedError();
		}
		MessageConsumer consumer = session.createConsumer(jmsAPIDestination);
		thread = new Thread(ReceiveJMSMessage.class.getSimpleName() + "Worker-" + hashCode()) {

			@Override
			public void run() {
				while (true) {
					try {
						if (isInterrupted()) {
							break;
						}
						Input input;
						Message message;
						if (messageType == MessageTypeStructure.Text) {
							message = (TextMessage) consumer.receive();
							input = new TextInput();
							((TextInput) input).setMessageBody(((TextMessage) message).getText());
						} else if (messageType == MessageTypeStructure.Bytes) {
							message = (BytesMessage) consumer.receive();
							input = new BytesInput();
							((BytesMessage) message).reset();// reset message read mode
							long length = ((BytesMessage) message).getBodyLength();
							if (length > Integer.MAX_VALUE) {
								throw new IllegalStateException("Message too big!");
							}
							byte[] data = new byte[(int) length];
							((BytesMessage) message).readBytes(data);
							((BytesInput) input).setMessageBody(data);
						} else if (messageType == MessageTypeStructure.Object) {
							message = (ObjectMessage) consumer.receive();
							input = new ObjectInput();
							((ObjectInput) input).setMessageBody(((ObjectMessage) message).getObject());
						} else if (messageType == MessageTypeStructure.Simple) {
							message = consumer.receive();
							input = new SimpleInput();
						} else {
							throw new UnexpectedError();
						}
						try {
							input.setCorrelationId(message.getJMSCorrelationID());
						} catch (AbstractMethodError e) {
						}
						try {
							input.setMessageId(message.getJMSMessageID());
						} catch (AbstractMethodError e) {
						}
						try {
							input.setType(message.getJMSType());
						} catch (AbstractMethodError e) {
						}
						try {
							input.setDeliveryMode(message.getJMSDeliveryMode());
						} catch (AbstractMethodError e) {
						}
						try {
							input.setDeliveryTime(message.getJMSDeliveryTime());
						} catch (AbstractMethodError e) {
						}
						try {
							input.setDestination(
									(message.getJMSDestination() != null) ? message.getJMSDestination().toString()
											: null);
						} catch (AbstractMethodError e) {
						}
						try {
							input.setExpiration(message.getJMSExpiration());
						} catch (AbstractMethodError e) {
						}
						try {
							input.setPriority(message.getJMSPriority());
						} catch (AbstractMethodError e) {
						}
						try {
							input.setRedelivered(message.getJMSRedelivered());
						} catch (AbstractMethodError e) {
						}
						try {
							input.setReplyTo(
									(message.getJMSReplyTo() != null) ? message.getJMSReplyTo().toString() : null);
						} catch (AbstractMethodError e) {
						}
						try {
							input.setTimestamp(message.getJMSTimestamp());
						} catch (AbstractMethodError e) {
						}
						activationHandler.trigger(input);
					} catch (Throwable t) {
						if (MiscUtils.isInterruptionException(t)) {
							break;
						} else {
							Log.get().error(t);
							throw new UnexpectedError(t);
						}
					}
				}
			}

		};
		thread.start();
	}

	@Override
	public void finalizeAutomaticTrigger(Solution solutionInstance) throws Exception {
		MiscUtils.willRethrowCommonly((compositeException) -> {
			compositeException.tryCactch(() -> {
				while (thread.isAlive()) {
					thread.interrupt();
					MiscUtils.relieveCPU();
				}
			});
			compositeException.tryCactch(() -> {
				jmsAPIConnection.close();
			});
			jmsAPIConnection = null;
			thread = null;
			this.activationHandler = null;
		});
	}

	@Override
	public boolean isAutomaticTriggerReady() {
		return activationHandler != null;
	}

	@Override
	public void validate(boolean recursively, Solution solutionInstance, Plan plan) throws ValidationError {
		super.validate(recursively, solutionInstance, plan);
		if (connectionReference.resolve(solutionInstance) == null) {
			throw new ValidationError("Failed to resolve the 'connection' reference");
		}
		if (recursively) {
			try {
				destinationVariant.validate(solutionInstance);
			} catch (ValidationError e) {
				throw new ValidationError("Failed to validate 'destination'", e);
			}
		}
	}

	public static abstract class Input {

		private long timestamp;
		private String replyTo;
		private boolean redelivered;
		private int priority;
		private long expiration;
		private String destination;
		private long deliveryTime;
		private int deliveryMode;
		private String type;
		private String correlationId;
		private String messageId;

		public long getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}

		public String getReplyTo() {
			return replyTo;
		}

		public void setReplyTo(String replyTo) {
			this.replyTo = replyTo;
		}

		public boolean isRedelivered() {
			return redelivered;
		}

		public void setRedelivered(boolean redelivered) {
			this.redelivered = redelivered;
		}

		public int getPriority() {
			return priority;
		}

		public void setPriority(int priority) {
			this.priority = priority;
		}

		public long getExpiration() {
			return expiration;
		}

		public void setExpiration(long expiration) {
			this.expiration = expiration;
		}

		public String getDestination() {
			return destination;
		}

		public void setDestination(String destination) {
			this.destination = destination;
		}

		public long getDeliveryTime() {
			return deliveryTime;
		}

		public void setDeliveryTime(long deliveryTime) {
			this.deliveryTime = deliveryTime;
		}

		public int getDeliveryMode() {
			return deliveryMode;
		}

		public void setDeliveryMode(int deliveryMode) {
			this.deliveryMode = deliveryMode;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getCorrelationId() {
			return correlationId;
		}

		public void setCorrelationId(String correlationId) {
			this.correlationId = correlationId;
		}

		public String getMessageId() {
			return messageId;
		}

		public void setMessageId(String messageId) {
			this.messageId = messageId;
		}

		@Override
		public String toString() {
			return "Input [timestamp=" + timestamp + ", replyTo=" + replyTo + ", redelivered=" + redelivered
					+ ", priority=" + priority + ", expiration=" + expiration + ", destination=" + destination
					+ ", deliveryTime=" + deliveryTime + ", deliveryMode=" + deliveryMode + ", type=" + type
					+ ", correlationId=" + correlationId + ", messageId=" + messageId + "]";
		}

	}

	public static class TextInput extends Input {

		private String messageBody;

		public String getMessageBody() {
			return messageBody;
		}

		public void setMessageBody(String messageBody) {
			this.messageBody = messageBody;
		}
	}

	public static class BytesInput extends Input {

		private byte[] messageBody;

		public byte[] getMessageBody() {
			return messageBody;
		}

		public void setMessageBody(byte[] messageBody) {
			this.messageBody = messageBody;
		}
	}

	public static class ObjectInput extends Input {

		private Serializable messageBody;

		public Serializable getMessageBody() {
			return messageBody;
		}

		public void setMessageBody(Serializable messageBody) {
			this.messageBody = messageBody;
		}

	}

	public static class SimpleInput extends Input {

	}

	public static class Metadata implements ActivatorMetadata {

		@Override
		public String getActivatorName() {
			return "Receive JMS Message";
		}

		@Override
		public Class<? extends Activator> getActivatorClass() {
			return ReceiveJMSMessage.class;
		}

		@Override
		public ResourcePath getActivatorIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(ReceiveJMSMessage.class.getName().replace(".", "/") + ".png"));
		}

	}

	static public enum MessageKindStructure {
		Queue, Topic;
	}

	static public enum MessageTypeStructure {
		Text, Bytes, Object, Simple;
	}

}
