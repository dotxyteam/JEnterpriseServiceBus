package com.otk.jesb.activation.builtin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.otk.jesb.Preferences;
import com.otk.jesb.Reference;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.activation.ActivationHandler;
import com.otk.jesb.activation.ActivationStrategy;
import com.otk.jesb.activation.ActivationStrategyMetadata;
import com.otk.jesb.resource.builtin.HTTPServer;
import com.otk.jesb.resource.builtin.WSDL;
import com.otk.jesb.resource.builtin.WSDL.OperationDescriptor.OperationInput;
import com.otk.jesb.resource.builtin.WSDL.ServiceSpecificationDescriptor;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.util.UpToDate;
import com.otk.jesb.util.UpToDate.VersionAccessException;

import xy.reflect.ui.info.ResourcePath;

public class ReceiveSOAPRequest extends ActivationStrategy {

	private Reference<HTTPServer> serverReference = new Reference<HTTPServer>(HTTPServer.class);
	private Reference<WSDL> wsdlReference = new Reference<WSDL>(WSDL.class);
	private String serviceName;
	private String operationSignature;
	private String servicePath;

	private ActivationHandler activationHandler;
	private UpToDateOperationOutputClass upToDateOperationOutputClass = new UpToDateOperationOutputClass();
	private Server jettyServer;

	private HTTPServer getServer() {
		return serverReference.resolve();
	}

	private WSDL getWSDL() {
		return wsdlReference.resolve();
	}

	public Reference<HTTPServer> getServerReference() {
		return serverReference;
	}

	public void setServerReference(Reference<HTTPServer> serverReference) {
		this.serverReference = serverReference;
	}

	public Reference<WSDL> getWsdlReference() {
		return wsdlReference;
	}

	public void setWsdlReference(Reference<WSDL> wsdlReference) {
		this.wsdlReference = wsdlReference;
		tryToSelectValuesAutomatically();
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
		tryToSelectValuesAutomatically();
	}

	public String getOperationSignature() {
		return operationSignature;
	}

	public void setOperationSignature(String operationSignature) {
		this.operationSignature = operationSignature;
		tryToSelectValuesAutomatically();
	}

	public String getServicePath() {
		return servicePath;
	}

	public void setServicePath(String servicePath) {
		this.servicePath = servicePath;
	}

	private void tryToSelectValuesAutomatically() {
		try {
			if (serviceName == null) {
				List<String> options = getServiceNameOptions();
				if (options.size() > 0) {
					serviceName = options.get(0);
				}
			}
			if (operationSignature == null) {
				List<String> options = getOperationSignatureOptions();
				if (options.size() > 0) {
					operationSignature = options.get(0);
				}
			}
		} catch (Throwable t) {
		}
	}

	public List<String> getServiceNameOptions() {
		WSDL wsdl = getWSDL();
		if (wsdl == null) {
			return Collections.emptyList();
		}
		return wsdl.getServiceSpecificationDescriptors().stream().map(s -> s.getServiceName())
				.collect(Collectors.toList());
	}

	public List<String> getOperationSignatureOptions() {
		WSDL.ServiceSpecificationDescriptor service = retrieveServiceSpecificationDescriptor();
		if (service == null) {
			return Collections.emptyList();
		}
		return service.getOperationDescriptors().stream().map(o -> o.getOperationSignature())
				.collect(Collectors.toList());
	}

	private WSDL.ServiceSpecificationDescriptor retrieveServiceSpecificationDescriptor() {
		WSDL wsdl = getWSDL();
		if (wsdl == null) {
			return null;
		}
		return wsdl.getServiceSpecificationDescriptor(serviceName);
	}

	private WSDL.OperationDescriptor retrieveOperationDescriptor() {
		WSDL.ServiceSpecificationDescriptor service = retrieveServiceSpecificationDescriptor();
		if (service == null) {
			return null;
		}
		return service.getOperationDescriptor(operationSignature);
	}

	@Override
	public Class<?> getInputClass() {
		WSDL.OperationDescriptor operation = retrieveOperationDescriptor();
		if (operation == null) {
			return null;
		}
		return operation.getOperationInputClass();
	}

	@Override
	public Class<?> getOutputClass() {
		try {
			return upToDateOperationOutputClass.get();
		} catch (VersionAccessException e) {
			throw new UnexpectedError(e);
		}
	}

	@Override
	public void initializeAutomaticTrigger(ActivationHandler activationHandler) throws Exception {
		this.activationHandler = activationHandler;
		HTTPServer server = getServer();
		if (server == null) {
			throw new UnexpectedError("Failed to resolve the server reference");
		}

		String hostName = server.getHostNameVariant().getValue();
		Integer port = server.getPortVariant().getValue();

		// Configure Jetty
		jettyServer = new Server(new InetSocketAddress(hostName, port));

		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		// ServletContextHandler context = new ServletContextHandler();
		context.setContextPath("/");
		jettyServer.setHandler(context);

		// Register CXF servlet
		CXFServlet cxfServlet = new CXFServlet();
		ServletHolder servletHolder = new ServletHolder(cxfServlet);
		context.addServlet(servletHolder, "/*");

		// Start Jetty
		jettyServer.start();

		// Publish service after Jetty has started
		InvocationHandler invocationHandler = new InvocationHandler() {

			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				System.out
						.println("proxy=" + proxy + ", " + "method=" + method + ", " + "args=" + Arrays.toString(args));
				WSDL.OperationDescriptor operation = retrieveOperationDescriptor();
				if (operation == null) {
					throw new UnexpectedError("Failed to get the operation descriptor");
				}
				if (!method.equals(operation.retrieveMethod())) {
					return null;
				}
				OperationInput operationInput = (OperationInput) operation.getOperationInputClass()
						.getConstructor(method.getParameterTypes()).newInstance(args);
				return activationHandler.trigger(operationInput);
			}
		};
		ServiceSpecificationDescriptor service = retrieveServiceSpecificationDescriptor();
		if (service == null) {
			throw new UnexpectedError("Failed to get the service descriptor");
		}
		EndpointImpl endpoint = new EndpointImpl(service.getImplementationClass()
				.getConstructor(InvocationHandler.class).newInstance(invocationHandler));
		endpoint.publish("/" + ((servicePath != null) ? servicePath : ""));
		if (Preferences.INSTANCE.isLogVerbose()) {
			System.out.println("Published SOAP service: " + "http://" + hostName + ":" + port + "/"
					+ ((servicePath != null) ? servicePath : "") + "?WSDL");
		}
	}

	@Override
	public void finalizeAutomaticTrigger() throws Exception {
		jettyServer.stop();
		this.activationHandler = null;
	}

	@Override
	public boolean isAutomaticTriggerReady() {
		return activationHandler != null;
	}

	@Override
	public boolean isAutomaticallyTriggerable() {
		return true;
	}

	@Override
	public void validate(boolean recursively, Plan plan) throws ValidationError {
		if (getServer() == null) {
			throw new ValidationError("Failed to resolve the HTTP server reference");
		}
		if (getWSDL() == null) {
			throw new ValidationError("Failed to resolve the WSDL reference");
		}
		if (retrieveServiceSpecificationDescriptor() == null) {
			throw new ValidationError("Invalid service name '" + serviceName + "'");
		}
		if (retrieveOperationDescriptor() == null) {
			throw new ValidationError("Invalid operation signature '" + operationSignature + "'");
		}
	}

	private class UpToDateOperationOutputClass extends UpToDate<Class<?>> {
		@Override
		protected Object retrieveLastVersionIdentifier() {
			WSDL.OperationDescriptor operation = retrieveOperationDescriptor();
			if (operation == null) {
				return null;
			}
			return operation.retrieveMethod();
		}

		@Override
		protected Class<?> obtainLatest(Object versionIdentifier) {
			WSDL.OperationDescriptor operation = retrieveOperationDescriptor();
			if (operation == null) {
				return null;
			}
			return operation.retrieveMethod().getReturnType();
		}
	}

	public static class Metadata implements ActivationStrategyMetadata {

		@Override
		public ResourcePath getActivationStrategyIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(ReceiveSOAPRequest.class.getName().replace(".", "/") + ".png"));
		}

		@Override
		public Class<? extends ActivationStrategy> getActivationStrategyClass() {
			return ReceiveSOAPRequest.class;
		}

		@Override
		public String getActivationStrategyName() {
			return "Receive SOAP Request";
		}

	}
}
