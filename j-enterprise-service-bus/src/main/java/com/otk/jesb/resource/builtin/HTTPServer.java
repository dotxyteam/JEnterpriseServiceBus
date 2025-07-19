package com.otk.jesb.resource.builtin;

import java.net.InetSocketAddress;

import javax.swing.SwingUtilities;

import org.eclipse.jetty.server.Server;

import com.otk.jesb.Variant;
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

	public String test() throws Exception {
		String hostName = getHostNameVariant().getValue();
		Integer port = getPortVariant().getValue();
		Server server = new Server(new InetSocketAddress(hostName, port));
		server.start();
		server.stop();
		return "Connection successful!";
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
