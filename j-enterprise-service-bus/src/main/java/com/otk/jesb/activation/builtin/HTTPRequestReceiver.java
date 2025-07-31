package com.otk.jesb.activation.builtin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.otk.jesb.Reference;
import com.otk.jesb.ValidationError;
import com.otk.jesb.activation.Activator;
import com.otk.jesb.resource.builtin.HTTPServer;
import com.otk.jesb.resource.builtin.HTTPServer.RequestHandler;
import com.otk.jesb.solution.Plan;

public abstract class HTTPRequestReceiver extends Activator {

	protected abstract RequestHandler createRequestHandler(String servicePath);

	protected abstract boolean isCompatibleWith(RequestHandler requestHandler);

	private Reference<HTTPServer> serverReference = new Reference<HTTPServer>(HTTPServer.class);
	private String servicePath = "/";

	public Reference<HTTPServer> getServerReference() {
		return serverReference;
	}

	public void setServerReference(Reference<HTTPServer> serverReference) {
		this.serverReference = serverReference;
	}

	protected HTTPServer expectServer() {
		HTTPServer result = serverReference.resolve();
		if (result == null) {
			throw new IllegalStateException("Failed to resolve the server reference");
		}
		return result;
	}

	public String getServicePath() {
		return servicePath;
	}

	public void setServicePath(String servicePath) {
		this.servicePath = servicePath;
	}

	public void addServicePathOption(String servicePath) throws Exception {
		HTTPServer server = expectServer();
		RequestHandler newRequestHandler = createRequestHandler(servicePath);
		newRequestHandler.validate(server);
		server.getRequestHandlers().add(newRequestHandler);
		this.servicePath = servicePath;
	}

	public List<String> getServicePathOptions() {
		try {
			HTTPServer server = expectServer();
			List<String> result = new ArrayList<String>();
			for (RequestHandler requestHandler : server.getRequestHandlers()) {
				if (!isCompatibleWith(requestHandler)) {
					continue;
				}
				result.add(requestHandler.getServicePath());
			}
			return result;
		} catch (IllegalStateException e) {
			return Collections.emptyList();
		}
	}

	public RequestHandler getRequestHandler() {
		try {
			HTTPServer server = expectServer();
			RequestHandler result = server.expectRequestHandler(servicePath);
			if (!isCompatibleWith(result)) {
				return null;
			}
			return result;
		} catch (IllegalStateException|IllegalArgumentException e) {
			return null;
		}
	}

	@Override
	public boolean isAutomaticallyTriggerable() {
		return true;
	}

	@Override
	public void validate(boolean recursively, Plan plan) throws ValidationError {
		try {
			expectServer();
		} catch (IllegalStateException e) {
			throw new ValidationError(e.getMessage(), e);
		}
		if (getRequestHandler() == null) {
			throw new ValidationError("Failed to resolve the request handler reference");
		}
	}

}
