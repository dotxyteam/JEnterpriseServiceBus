package com.otk.jesb.resource.builtin;

import java.sql.Connection;
import java.sql.Driver;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.function.Function;
import javax.swing.SwingUtilities;

import com.otk.jesb.Variant;
import com.otk.jesb.Session;
import com.otk.jesb.ValidationError;
import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.ResourceMetadata;
import com.otk.jesb.ui.GUI;
import com.otk.jesb.util.MiscUtils;

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
	private WeakHashMap<Session, Connection> instanceBySession = new WeakHashMap<Session, Connection>();

	public JDBCConnection() {
		this(JDBCConnection.class.getSimpleName() + MiscUtils.getDigitalUniqueIdentifier());
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

	private Connection open() throws Exception {
		String driverClassName = getDriverClassNameVariant().getValue();
		String url = getUrlVariant().getValue();
		String userName = getUserNameVariant().getValue();
		String password = getPasswordVariant().getValue();
		Class<?> driverClass = MiscUtils.getJESBClass(driverClassName);
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

	public <T> T during(Function<Connection, T> callable) throws Exception {
		synchronized (this) {
			Connection instance = open();
			try {
				return callable.apply(instance);
			} finally {
				instance.close();
			}
		}
	}

	public Connection during(Session session) throws Exception {
		synchronized (session) {
			synchronized (instanceBySession) {
				Connection instance = instanceBySession.get(session);
				if ((instance != null) && !instance.isValid(VALIDITY_CHECK_TIMEOUT_SECONDS)) {
					session.getClosables().remove(instance);
					try {
						instance.close();
					} catch (Throwable ignore) {
					}
					session = null;
				}
				if (instance == null) {
					instance = open();
					session.getClosables().add(instance);
					instanceBySession.put(session, instance);
				}
				return instance;
			}
		}
	}

	@Override
	public void validate(boolean recursively) throws ValidationError {
		super.validate(recursively);
		String driverClassName = getDriverClassNameVariant().getValue();
		String url = getUrlVariant().getValue();
		if ((driverClassName == null) || (driverClassName.trim().length() == 0)) {
			throw new ValidationError("Driver class name not provided");
		}
		if ((url == null) || (url.trim().length() == 0)) {
			throw new ValidationError("Connection URL not provided");
		}
		try {
			synchronized (this) {
				open().close();
			}
		} catch (Exception e) {
			throw new ValidationError("Failed to create the connection", e);
		}
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
