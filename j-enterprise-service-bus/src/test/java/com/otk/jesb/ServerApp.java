package com.otk.jesb;

import java.net.InetSocketAddress;
import java.util.Arrays;

import javax.jws.WebService;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class ServerApp {
	public static void main(String[] args) throws Exception {
		// Configure Jetty
		Server server = new Server(new InetSocketAddress("localhost", 8080));
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        //ServletContextHandler context = new ServletContextHandler();
		context.setContextPath("/");
		server.setHandler(context);

		// Register CXF servlet
		CXFNonSpringServlet cxfServlet = new CXFNonSpringServlet();
		ServletHolder servletHolder = new ServletHolder(cxfServlet);
		context.addServlet(servletHolder, "/services/*");

		// Start Jetty
		server.start();

		// Publish service after Jetty has started
		@SuppressWarnings("resource")
		EndpointImpl endpoint = new EndpointImpl(new HelloServiceImpl());
		endpoint.publish("/HelloService");
		System.out.println("SOAP service started at http://localhost:8080/services/HelloService?wsdl");

		JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        factory.setAddress("/rest"); // relatif au CXFServlet
        factory.setServiceBeans(Arrays.asList(new HelloRestService()));
        factory.create();
        System.out.println("REST service available at http://localhost:8080/services/rest/hello?name=Test");

		server.join();
	}

	@WebService
	public interface HelloService {
		String sayHello(String name);
	}

	@WebService(endpointInterface = "com.otk.jesb.ServerApp$HelloService")
	public static class HelloServiceImpl implements HelloService {
		@Override
		public String sayHello(String name) {
			return "Hello, " + name + "!";
		}
	}
	
	@Path("/hello")
	public static class HelloRestService {

	    @GET
	    @Produces(MediaType.TEXT_PLAIN)
	    public String sayHello(@QueryParam("name") String name) {
	        return "Hello, " + (name != null ? name : "world") + "!";
	    }
	}
}
