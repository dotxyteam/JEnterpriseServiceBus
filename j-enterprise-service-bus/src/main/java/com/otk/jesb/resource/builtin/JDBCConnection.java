package com.otk.jesb.resource.builtin;

import java.sql.Connection;
import java.sql.Driver;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import javax.swing.SwingUtilities;

import com.otk.jesb.Variant;
import com.otk.jesb.Session;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.ResourceMetadata;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.ui.GUI;
import xy.reflect.ui.info.ResourcePath;

public class JDBCConnection extends Resource {

	private static final int VALIDITY_CHECK_TIMEOUT_SECONDS = Integer
			.valueOf(System.getProperty(JDBCConnection.class.getName() + ".validityCheckTimeoutSeconds", "10"));

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				GUI.INSTANCE.openObjectFrame(new JDBCConnection("test"));
			}
		});
	}

	private Variant<String> driverClassNameVariant = new Variant<String>(String.class);
	private Variant<String> urlVariant = new Variant<String>(String.class);
	private Variant<String> userNameVariant = new Variant<String>(String.class);
	private Variant<String> passwordVariant = new Variant<String>(String.class);

	private Semaphore instanceMutex = new Semaphore(1);
	private Connection currentSessionInstance;
	private Session currentSession;
	private AutoCloseable currentSessionInstanceClosableWrapper;

	public JDBCConnection() {
		super();
	}

	public JDBCConnection(String name) {
		super(name);
	}

	public Variant<String> getDriverClassNameVariant() {
		return driverClassNameVariant;
	}

	public void setDriverClassNameVariant(Variant<String> driverClassNameVariant) {
		this.driverClassNameVariant = driverClassNameVariant;
	}

	public Variant<String> getUrlVariant() {
		return urlVariant;
	}

	public void setUrlVariant(Variant<String> urlVariant) {
		this.urlVariant = urlVariant;
	}

	public Variant<String> getUserNameVariant() {
		return userNameVariant;
	}

	public void setUserNameVariant(Variant<String> userNameVariant) {
		this.userNameVariant = userNameVariant;
	}

	public Variant<String> getPasswordVariant() {
		return passwordVariant;
	}

	public void setPasswordVariant(Variant<String> passwordVariant) {
		this.passwordVariant = passwordVariant;
	}

	private Connection open(Solution solutionInstance) throws Exception {
		String driverClassName = getDriverClassNameVariant().getValue(solutionInstance);
		String url = getUrlVariant().getValue(solutionInstance);
		String userName = getUserNameVariant().getValue(solutionInstance);
		String password = getPasswordVariant().getValue(solutionInstance);
		Class<?> driverClass = solutionInstance.getRuntime().getJESBClass(driverClassName);
		Driver driverInstance = (Driver) driverClass.getDeclaredConstructor().newInstance();
		Properties properties = new Properties();
		if (userName != null) {
			properties.setProperty("user", userName);
		}
		if (password != null) {
			properties.setProperty("password", password);
		}
		return driverInstance.connect(url, properties);
	}

	public <T> T during(Function<Connection, T> callable, Solution solutionInstance) throws Exception {
		synchronized (this) {
			if (currentSessionInstance != null) {
				return callable.apply(currentSessionInstance);
			}
			instanceMutex.acquire();
			try {
				Connection instance = open(solutionInstance);
				try {
					return callable.apply(instance);
				} finally {
					instance.close();
				}
			} finally {
				instanceMutex.release();
			}
		}
	}

	public Connection during(Session session) throws Exception {
		Solution solutionInstance = session.getSolutionInstance();
		synchronized (this) {
			if (currentSessionInstance == null) {
				instanceMutex.acquire();
				currentSession = session;
				currentSessionInstance = open(solutionInstance);
				session.getClosables().add(currentSessionInstanceClosableWrapper = new AutoCloseable() {
					@Override
					public void close() throws Exception {
						synchronized (JDBCConnection.this) {
							currentSessionInstanceClosableWrapper = null;
							currentSessionInstance.close();
							currentSessionInstance = null;
							currentSession = null;
							instanceMutex.release();
						}
					}
				});
			} else {
				if (currentSession != session) {
					throw new IllegalArgumentException(
							"Unable to share " + this + " instance between " + currentSession + " and " + session);
				}
				if (!currentSessionInstance.isValid(VALIDITY_CHECK_TIMEOUT_SECONDS)) {
					if (!session.getClosables().remove(currentSessionInstanceClosableWrapper)) {
						throw new UnexpectedError();
					}
					currentSessionInstanceClosableWrapper.close();
					return during(session);
				}
			}
			return currentSessionInstance;
		}
	}

	@Override
	public void validate(boolean recursively, Solution solutionInstance) throws ValidationError {
		super.validate(recursively, solutionInstance);
		String driverClassName = getDriverClassNameVariant().getValue(solutionInstance);
		String url = getUrlVariant().getValue(solutionInstance);
		if ((driverClassName == null) || (driverClassName.trim().length() == 0)) {
			throw new ValidationError("Driver class name not provided");
		}
		if ((url == null) || (url.trim().length() == 0)) {
			throw new ValidationError("Connection URL not provided");
		}
	}

	public String test(Solution solutionInstance) throws Exception {
		instanceMutex.acquire();
		try {
			open(solutionInstance).close();
		} finally {
			instanceMutex.release();
		}
		return "Connection successful!";
	}

	public static class Metadata implements ResourceMetadata {

		@Override
		public ResourcePath getResourceIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(JDBCConnection.class.getName().replace(".", "/") + ".png"));
		}

		@Override
		public Class<? extends Resource> getResourceClass() {
			return JDBCConnection.class;
		}

		@Override
		public String getResourceTypeName() {
			return "JDBC Connection";
		}

	}

}
