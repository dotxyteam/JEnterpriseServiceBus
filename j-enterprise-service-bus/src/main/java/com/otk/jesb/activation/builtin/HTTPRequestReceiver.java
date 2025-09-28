package com.otk.jesb.activation.builtin;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.otk.jesb.Reference;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.activation.Activator;
import com.otk.jesb.resource.builtin.HTTPServer;
import com.otk.jesb.resource.builtin.HTTPServer.RequestHandler;
import com.otk.jesb.solution.Plan;

public abstract class HTTPRequestReceiver extends Activator {

	protected abstract RequestHandler createRequestHandler(String servicePath);

	protected abstract boolean isCompatibleWith(RequestHandler requestHandler);

	private Reference<HTTPServer> serverReference = new Reference<HTTPServer>(HTTPServer.class);
	private int servicePathOptionIndex = -1;

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

	public int getServicePathOptionIndex() {
		return servicePathOptionIndex;
	}

	public void setServicePathOptionIndex(int servicePathOptionIndex) {
		this.servicePathOptionIndex = servicePathOptionIndex;
	}

	@Transient
	public String getServicePath() {
		if (servicePathOptionIndex == -1) {
			return null;
		}
		List<String> options = getServicePathOptions();
		if (servicePathOptionIndex >= options.size()) {
			return null;
		}
		return options.get(servicePathOptionIndex);
	}

	public void setServicePath(String servicePath) {
		if (servicePath == null) {
			this.servicePathOptionIndex = -1;
			return;
		}
		List<String> options = getServicePathOptions();
		if (!options.contains(servicePath)) {
			this.servicePathOptionIndex = -1;
			return;
		}
		this.servicePathOptionIndex = options.indexOf(servicePath);
	}

	public void addServicePathOption(String servicePath) throws Exception {
		HTTPServer server = expectServer();
		RequestHandler newRequestHandler = createRequestHandler(servicePath);
		newRequestHandler.validate(server);
		server.getRequestHandlers().add(newRequestHandler);
		setServicePath(servicePath);
	}

	public List<String> getServicePathOptions() {
		try {
			HTTPServer server = expectServer();
			List<String> result = new ArrayList<String>();
			for (RequestHandler requestHandler : server.getRequestHandlers()) {
				if (!isCompatibleWith(requestHandler)) {
					continue;
				}
				result.add(requestHandler.getServicePathVariant().getValue());
			}
			return result;
		} catch (IllegalStateException e) {
			return Collections.emptyList();
		}
	}

	public RequestHandler getRequestHandler() {
		try {
			String servicePath = getServicePath();
			if(servicePath == null) {
				return null;
			}
			HTTPServer server = expectServer();
			RequestHandler result = server.expectRequestHandler(getServicePath());
			if (!isCompatibleWith(result)) {
				throw new UnexpectedError();
			}
			return result;
		} catch (IllegalStateException | IllegalArgumentException e) {
			return null;
		}
	}

	@Override
	public boolean isAutomaticallyTriggerable() {
		return true;
	}

	@Override
	public void validate(boolean recursively, Plan plan) throws ValidationError {
		super.validate(recursively, plan);
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
