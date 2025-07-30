package com.otk.jesb.resource.builtin;

import java.beans.Transient;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import javax.swing.SwingUtilities;

import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.otk.jesb.Variant;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.ResourceMetadata;
import com.otk.jesb.ui.GUI;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.info.ResourcePath;

public class HTTPServer extends Resource {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				GUI.INSTANCE.openObjectFrame(new HTTPServer("test"));
			}
		});
	}

	private Variant<String> hostNameVariant = new Variant<String>(String.class, "localhost");
	private Variant<Integer> portVariant = new Variant<Integer>(Integer.class, 8080);
	private List<RequestHandler> requestHandlers = new ArrayList<HTTPServer.RequestHandler>();

	private Server jettyServer;

	public HTTPServer() {
		this(HTTPServer.class.getSimpleName() + MiscUtils.getDigitalUniqueIdentifier());
	}

	public HTTPServer(String name) {
		super(name);
	}

	public Variant<String> getHostNameVariant() {
		return hostNameVariant;
	}

	public void setHostNameVariant(Variant<String> hostNameVariant) {
		this.hostNameVariant = hostNameVariant;
	}

	public Variant<Integer> getPortVariant() {
		return portVariant;
	}

	public void setPortVariant(Variant<Integer> portVariant) {
		this.portVariant = portVariant;
	}

	public List<RequestHandler> getRequestHandlers() {
		return requestHandlers;
	}

	public void setRequestHandlers(List<RequestHandler> requestHandlers) {
		this.requestHandlers = requestHandlers;
	}

	public RequestHandler expectRequestHandler(String servicePath) {
		RequestHandler result = requestHandlers.stream()
				.filter(requestHandler -> servicePath.equals(requestHandler.getServicePath())).findFirst().orElse(null);
		if (result == null) {
			throw new IllegalArgumentException("No request handler found for service path '" + servicePath + "'");
		}
		return result;
	}

	public String getLocaBaseURL() {
		String hostName = getHostNameVariant().getValue();
		Integer port = getPortVariant().getValue();
		return "http://" + hostName + ":" + port;
	}

	public String test() throws Exception {
		String hostName = getHostNameVariant().getValue();
		Integer port = getPortVariant().getValue();
		Server server = new Server(new InetSocketAddress(hostName, port));
		server.start();
		server.stop();
		return "Connection successful!";
	}

	private synchronized void start() throws Exception {
		if (isActive()) {
			throw new UnexpectedError();
		}
		String hostName = getHostNameVariant().getValue();
		Integer port = getPortVariant().getValue();
		jettyServer = new Server(new InetSocketAddress(hostName, port));
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		context.setContextPath("/");
		jettyServer.setHandler(context);
		CXFNonSpringServlet cxfServlet = new CXFNonSpringServlet();
		ServletHolder servletHolder = new ServletHolder(cxfServlet);
		context.addServlet(servletHolder, "/*");
		jettyServer.start();
	}

	private synchronized void stop() throws Exception {
		if (!isActive()) {
			throw new UnexpectedError();
		}
		jettyServer.stop();
		jettyServer = null;
	}

	private boolean isActive() {
		return jettyServer != null;
	}

	@Override
	public void validate(boolean recursively) throws ValidationError {
		super.validate(recursively);
		String hostName = getHostNameVariant().getValue();
		Integer port = getPortVariant().getValue();
		if ((hostName == null) || (hostName.trim().length() == 0)) {
			throw new ValidationError("Host name not provided");
		}
		if (port == null) {
			throw new ValidationError("Port not provided");
		}
		if ((port < 0) || (port > 65535)) {
			throw new ValidationError("Invalid port (must be between 0 and 65535)");
		}
	}

	public static abstract class RequestHandler {

		protected abstract void install(HTTPServer server) throws Exception;

		protected abstract void uninstall(HTTPServer server) throws Exception;

		protected String servicePath;
		protected boolean active = false;

		public RequestHandler(String servicePath) {
			this.servicePath = servicePath;
		}

		public String getServicePath() {
			return servicePath;
		}

		public void setServicePath(String servicePath) {
			this.servicePath = servicePath;
		}

		@Transient
		public boolean isActive() {
			return active;
		}

		public void setActive(boolean active) {
			this.active = active;
		}

		public void activate(HTTPServer server) throws Exception {
			synchronized (server) {
				if (!server.requestHandlers.contains(this)) {
					throw new UnexpectedError();
				}
				if (server.requestHandlers.stream().noneMatch(RequestHandler::isActive)) {
					server.start();
				}
				install(server);
			}
		}

		public void deactivate(HTTPServer server) throws Exception {
			synchronized (server) {
				if (!server.requestHandlers.contains(this)) {
					throw new UnexpectedError();
				}
				uninstall(server);
				if (server.requestHandlers.stream().noneMatch(RequestHandler::isActive)) {
					server.stop();
				}
			}
		}

		public void validate(HTTPServer server) throws ValidationError {
			if ((servicePath == null) || servicePath.isEmpty()) {
				throw new ValidationError("Service path not provided");
			}
			if (server.requestHandlers.stream().filter(Predicate.isEqual(this).negate())
					.map(RequestHandler::getServicePath).filter(Objects::nonNull)
					.anyMatch(Predicate.isEqual(servicePath))) {
				throw new ValidationError("Duplicate service path: '" + servicePath + "'");
			}
		}
	}

	public static class Metadata implements ResourceMetadata {

		@Override
		public ResourcePath getResourceIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(HTTPServer.class.getName().replace(".", "/") + ".png"));
		}

		@Override
		public Class<? extends Resource> getResourceClass() {
			return HTTPServer.class;
		}

		@Override
		public String getResourceTypeName() {
			return "HTTP Server";
		}

	}

}
