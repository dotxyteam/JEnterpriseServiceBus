package com.otk.jesb;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;

public class ServerApp0 {
	public static void main(String[] args) throws Exception {
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");

		Server jettyServer = new Server(8989);
		jettyServer.setHandler(context);

		ServletHandler handler = new ServletHandler();
		handler.addServletWithMapping(MyServletHandler.class, "/myServlet");

		jettyServer.setHandler(handler);

		try {
			jettyServer.start();
			jettyServer.join();
		} finally {
			jettyServer.destroy();
		}
	}

	public static class MyServletHandler extends HttpServlet {

		private static final long serialVersionUID = 1L;

		protected void doGet(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {
			System.out.println("Control is in servlet");
			//RequestDispatcher requestDispatcher = request.getRequestDispatcher("index.html"); // this returns null.
																								// hence i am unable to
																								// request dispatch to
																								// view the html
																								// webpage.

		}

	}
}
