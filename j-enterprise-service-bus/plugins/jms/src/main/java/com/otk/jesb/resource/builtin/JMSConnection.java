package com.otk.jesb.resource.builtin;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import com.otk.jesb.resource.Resource;
import com.otk.jesb.solution.Solution;

public abstract class JMSConnection extends Resource {

	public abstract ConnectionFactory getConnectionFactory(Solution solutionInstance);

	public JMSConnection() {
		super();
	}

	public JMSConnection(String name) {
		super(name);
	}

	public String test(Solution solutionInstance) throws JMSException {
		ConnectionFactory factory = getConnectionFactory(solutionInstance);
		try (Connection connection = factory.createConnection()) {
			connection.start();
			connection.stop();
		}
		return "Connection successful!";
	}
}
