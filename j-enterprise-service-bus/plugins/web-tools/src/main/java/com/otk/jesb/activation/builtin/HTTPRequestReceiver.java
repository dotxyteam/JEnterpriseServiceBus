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
import com.otk.jesb.solution.Solution;

public abstract class HTTPRequestReceiver extends Activator {

	protected abstract RequestHandler createRequestHandler(String servicePath, Solution solutionInstance);

	protected abstract boolean isCompatibleWith(RequestHandler requestHandler, Solution solutionInstance);

	private Reference<HTTPServer> serverReference = new Reference<HTTPServer>(HTTPServer.class);
	private int servicePathOptionIndex = -1;

	public Reference<HTTPServer> getServerReference() {
		return serverReference;
	}

	public void setServerReference(Reference<HTTPServer> serverReference) {
		this.serverReference = serverReference;
	}

	protected HTTPServer expectServer(Solution solutionInstance) {
		HTTPServer result = serverReference.resolve(solutionInstance);
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
	public String getServicePath(Solution solutionInstance) {
		if (servicePathOptionIndex == -1) {
			return null;
		}
		List<String> options = getServicePathOptions(solutionInstance);
		if (servicePathOptionIndex >= options.size()) {
			return null;
		}
		return options.get(servicePathOptionIndex);
	}

	public void setServicePath(String servicePath, Solution solutionInstance) {
		if (servicePath == null) {
			this.servicePathOptionIndex = -1;
			return;
		}
		List<String> options = getServicePathOptions(solutionInstance);
		if (!options.contains(servicePath)) {
			this.servicePathOptionIndex = -1;
			return;
		}
		this.servicePathOptionIndex = options.indexOf(servicePath);
	}

	public void addServicePathOption(String servicePath, Solution solutionInstance) throws Exception {
		HTTPServer server = expectServer(solutionInstance);
		RequestHandler newRequestHandler = createRequestHandler(servicePath, solutionInstance);
		newRequestHandler.validate(server, solutionInstance);
		server.getRequestHandlers().add(newRequestHandler);
		setServicePath(servicePath, solutionInstance);
	}

	public List<String> getServicePathOptions(Solution solutionInstance) {
		try {
			HTTPServer server = expectServer(solutionInstance);
			List<String> result = new ArrayList<String>();
			for (RequestHandler requestHandler : server.getRequestHandlers()) {
				if (!isCompatibleWith(requestHandler, solutionInstance)) {
					continue;
				}
				result.add(requestHandler.getServicePathVariant().getValue(solutionInstance));
			}
			return result;
		} catch (IllegalStateException e) {
			return Collections.emptyList();
		}
	}

	public RequestHandler getRequestHandler(Solution solutionInstance) {
		try {
			String servicePath = getServicePath(solutionInstance);
			if (servicePath == null) {
				return null;
			}
			HTTPServer server = expectServer(solutionInstance);
			RequestHandler result = server.expectRequestHandler(getServicePath(solutionInstance), solutionInstance);
			if (!isCompatibleWith(result, solutionInstance)) {
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
	public void validate(boolean recursively, Solution solutionInstance, Plan plan) throws ValidationError {
		super.validate(recursively, solutionInstance, plan);
		try {
			expectServer(solutionInstance);
		} catch (IllegalStateException e) {
			throw new ValidationError(e.getMessage(), e);
		}
		if (getRequestHandler(solutionInstance) == null) {
			throw new ValidationError("Failed to resolve the request handler reference");
		}
	}

}
