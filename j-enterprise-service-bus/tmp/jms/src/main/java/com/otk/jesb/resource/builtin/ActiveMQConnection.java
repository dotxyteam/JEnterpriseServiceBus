package com.otk.jesb.resource.builtin;

import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.ResourceMetadata;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;

import com.otk.jesb.ValidationError;
import com.otk.jesb.Variant;

import xy.reflect.ui.info.ResourcePath;
import com.otk.jesb.solution.Solution;

public class ActiveMQConnection extends JMSConnection {

	public static void main(String[] args) throws JMSException {
		new ActiveMQConnection().test(new Solution());
	}

	private Variant<String> brokerURLVariant = new Variant<String>(String.class, "tcp://localhost:61616");

	public ActiveMQConnection(String name) {
		super(name);
	}

	public ActiveMQConnection() {
	}

	public Variant<String> getBrokerURLVariant() {
		return brokerURLVariant;
	}

	public void setBrokerURLVariant(Variant<String> brokerURLVariant) {
		this.brokerURLVariant = brokerURLVariant;
	}

	@Override
	public ConnectionFactory getConnectionFactory(Solution solutionInstance) {
		return new ActiveMQConnectionFactory(getBrokerURLVariant().getValue(solutionInstance));
	}

	@Override
	public String toString() {
		return "ActiveMQConnection []";
	}

	@Override
	public void validate(boolean recursively, Solution solutionInstance) throws ValidationError {
	}

	public static class Metadata implements ResourceMetadata {

		@Override
		public String getResourceTypeName() {
			return "ActiveMQ Connection";
		}

		@Override
		public Class<? extends Resource> getResourceClass() {
			return ActiveMQConnection.class;
		}

		@Override
		public ResourcePath getResourceIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(ActiveMQConnection.class.getName().replace(".", "/") + ".png"));
		}

	}

}
