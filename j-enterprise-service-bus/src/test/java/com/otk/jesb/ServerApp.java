package com.otk.jesb;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.annotation.Priority;
import javax.jws.WebService;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.Priorities;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;
import org.apache.cxf.jaxrs.swagger.ui.SwaggerUiService;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import my.service.PetsApi;
import my.service.SwaggerInitializerResource;
import my.service.impl.PetsApiServiceImpl;

public class ServerApp {
	public static void main(String[] args) throws Exception {
		// Configure Jetty
		Server server = new Server(new InetSocketAddress("localhost", 8081));
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		// ServletContextHandler context = new ServletContextHandler();
		context.setContextPath("/");
		server.setHandler(context);

		// Register CXF servlet
		CXFNonSpringServlet cxfServlet = new CXFNonSpringServlet();
		ServletHolder servletHolder = new ServletHolder(cxfServlet);
		context.addServlet(servletHolder, "/services/*");
		// context.addServlet(DefaultServlet.class, "/");

		// Start Jetty
		server.start();

		// Publish service after Jetty has started
		@SuppressWarnings("resource")
		EndpointImpl endpoint = new EndpointImpl(new HelloServiceImpl());
		endpoint.publish("/HelloService");
		System.out.println("SOAP service started at http://localhost:8080/services/HelloService?wsdl");

		JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
		factory.setAddress("/rest"); // relatif au CXFServlet
		factory.setServiceBeans(Arrays.asList(new PetsApiServiceImpl(), new SwaggerInitializerResource()));
		factory.setProvider(new JacksonJsonProvider());

		OpenApiFeature openApiFeature = new CustomOpenApiFeature();
		openApiFeature.setSupportSwaggerUi(true);
		openApiFeature.setUseContextBasedConfig(true);
		openApiFeature.setPrettyPrint(true);
		openApiFeature.setResourcePackages(new HashSet<String>(
				Arrays.asList(PetsApi.class.getPackage().getName())));
		factory.setFeatures(Arrays.asList(openApiFeature));

		factory.create();
		System.out.println("REST service available at http://localhost:8080/services/rest/swagger-ui");

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

	protected static class CustomOpenApiFeature extends OpenApiFeature{

		public CustomOpenApiFeature() {
			setDelegate(new Portable() {

				@Override
				public Registration getSwaggerUi(Bus bus, Properties swaggerProps, boolean runAsFilter) {
					final Registration registration = new Registration();

					if (checkSupportSwaggerUiProp(swaggerProps)) {
						String swaggerUiRoot = findSwaggerUiRoot();

						if (swaggerUiRoot != null) {
							final SwaggerUiResourceLocator locator = new SwaggerUiResourceLocator(swaggerUiRoot);
							SwaggerUiService swaggerUiService = new SwaggerUiService(locator, getSwaggerUiMediaTypes());
							swaggerUiService.setConfig(getSwaggerUiConfig());

							if (!runAsFilter) {
								registration.getResources().add(swaggerUiService);
							} else {
								registration.getProviders().add(new SwaggerUiServiceFilter(swaggerUiService));
							}

							registration.getProviders().add(new SwaggerUiResourceFilter(locator));
							bus.setProperty("swagger.service.ui.available", "true");
						}
					}

					return registration;
				}

			});
		}

		@PreMatching
		@Priority(Priorities.USER + 1)
		class SwaggerUiServiceFilter implements ContainerRequestFilter {
			private final SwaggerUiService uiService;

			SwaggerUiServiceFilter(SwaggerUiService uiService) {
				this.uiService = uiService;
			}

			@Override
			public void filter(ContainerRequestContext rc) throws IOException {
				if (HttpMethod.GET.equals(rc.getRequest().getMethod())) {
					UriInfo ui = rc.getUriInfo();
					String path = ui.getPath();
					int uiPathIndex = path.lastIndexOf("api-docs");
					if (uiPathIndex >= 0) {
						String resourcePath = uiPathIndex + 8 < path.length() ? path.substring(uiPathIndex + 8) : "";
						rc.abortWith(uiService.getResource(ui, resourcePath));
					}
				}
			}
		}

		@PreMatching
		@Priority(Priorities.USER)
		class SwaggerUiResourceFilter implements ContainerRequestFilter {
			private final Pattern PATTERN = Pattern
					.compile(".*[.]js|.*[.]gz|.*[.]map|oauth2*[.]html|.*[.]png|.*[.]css|.*[.]ico|"
							+ "/css/.*|/images/.*|/lib/.*|/fonts/.*");

			private final SwaggerUiResourceLocator locator;

			SwaggerUiResourceFilter(SwaggerUiResourceLocator locator) {
				this.locator = locator;
			}

			@Override
			public void filter(ContainerRequestContext rc) throws IOException {
				if (HttpMethod.GET.equals(rc.getRequest().getMethod())) {
					UriInfo ui = rc.getUriInfo();
					String path = "/" + ui.getPath();
					if (PATTERN.matcher(path).matches() && locator.exists(path)) {
						rc.setRequestUri(URI.create("api-docs" + path));
					}
				}
			}
		}

		class SwaggerUiResourceLocator extends org.apache.cxf.jaxrs.swagger.ui.SwaggerUiResourceLocator {
			private String swaggerUiRoot;

			public SwaggerUiResourceLocator(String swaggerUiRoot) {
				super(swaggerUiRoot);
				this.swaggerUiRoot = swaggerUiRoot;
			}

			/**
			 * Locate Swagger UI resource corresponding to resource path
			 * 
			 * @param resourcePath resource path
			 * @return Swagger UI resource URL
			 * @throws MalformedURLException
			 */
			public URL locate(String resourcePath) throws MalformedURLException {
				if ("/swagger-initializer.js".equals(resourcePath)) {
					throw new MalformedURLException();
				}
				if (StringUtils.isEmpty(resourcePath) || "/".equals(resourcePath)) {
					resourcePath = "index.html";
				}

				if (resourcePath.startsWith("/")) {
					resourcePath = resourcePath.substring(1);
				}
				URL ret;

				try {
					ret = URI.create(swaggerUiRoot + resourcePath).toURL();
				} catch (IllegalArgumentException ex) {
					throw new MalformedURLException(ex.getMessage());
				}
				return ret;
			}

			/**
			 * Checks the existence of the Swagger UI resource corresponding to resource
			 * path
			 * 
			 * @param resourcePath resource path
			 * @return "true" if Swagger UI resource exists, "false" otherwise
			 */
			public boolean exists(String resourcePath) {
				try {
					// The connect() will try to locate the entry (jar file, classpath resource)
					// and fail with FileNotFoundException /IOException if there is none.
					locate(resourcePath).openConnection().connect();
					return true;
				} catch (IOException ex) {
					return false;
				}
			}
		}
	}
}
