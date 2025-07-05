package com.otk.jesb.resource.builtin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.otk.jesb.EnvironmentVariant;
import com.otk.jesb.ValidationError;
import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.ResourceMetadata;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.control.swing.customizer.SwingCustomizer;
import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.util.ClassUtils;

public class JDBCConnection extends Resource {

	public static void main(String[] args) {
		SwingCustomizer.getDefault().openObjectFrame(new JDBCConnection("test"));
	}

	private EnvironmentVariant<String> driverClassNameVariant = new EnvironmentVariant<String>(String.class);
	private EnvironmentVariant<String> urlVariant = new EnvironmentVariant<String>(String.class);
	private EnvironmentVariant<String> userNameVariant = new EnvironmentVariant<String>(String.class);
	private EnvironmentVariant<String> passwordVariant = new EnvironmentVariant<String>(String.class);

	public JDBCConnection() {
		this(JDBCConnection.class.getSimpleName() + MiscUtils.getDigitalUniqueIdentifier());
	}

	public JDBCConnection(String name) {
		super(name);
	}

	public EnvironmentVariant<String> getDriverClassNameVariant() {
		return driverClassNameVariant;
	}

	public void setDriverClassNameVariant(EnvironmentVariant<String> driverClassNameVariant) {
		this.driverClassNameVariant = driverClassNameVariant;
	}

	public EnvironmentVariant<String> getUrlVariant() {
		return urlVariant;
	}

	public void setUrlVariant(EnvironmentVariant<String> urlVariant) {
		this.urlVariant = urlVariant;
	}

	public EnvironmentVariant<String> getUserNameVariant() {
		return userNameVariant;
	}

	public void setUserNameVariant(EnvironmentVariant<String> userNameVariant) {
		this.userNameVariant = userNameVariant;
	}

	public EnvironmentVariant<String> getPasswordVariant() {
		return passwordVariant;
	}

	public void setPasswordVariant(EnvironmentVariant<String> passwordVariant) {
		this.passwordVariant = passwordVariant;
	}

	public String test() throws ClassNotFoundException, SQLException {
		String driverClassName = getDriverClassNameVariant().getValue();
		String url = getUrlVariant().getValue();
		String userName = getUserNameVariant().getValue();
		String password = getPasswordVariant().getValue();
		ClassUtils.getCachedClassForName(driverClassName);
		DriverManager.getConnection(url, userName, password);
		return "Connection successful!";
	}

	public Connection build() throws ClassNotFoundException, SQLException {
		String driverClassName = getDriverClassNameVariant().getValue();
		String url = getUrlVariant().getValue();
		String userName = getUserNameVariant().getValue();
		String password = getPasswordVariant().getValue();
		ClassUtils.getCachedClassForName(driverClassName);
		return DriverManager.getConnection(url, userName, password);
	}

	@Override
	public void validate(boolean recursively) throws ValidationError {
		super.validate(recursively);
		String driverClassName = getDriverClassNameVariant().getValue();
		String url = getUrlVariant().getValue();
		String userName = getUserNameVariant().getValue();
		String password = getPasswordVariant().getValue();
		if ((driverClassName == null) || (driverClassName.trim().length() == 0)) {
			throw new ValidationError("Driver class name not provided");
		}
		if ((url == null) || (url.trim().length() == 0)) {
			throw new ValidationError("Connection URL not provided");
		}
		try {
			ClassUtils.getCachedClassForName(driverClassName);
		} catch (ClassNotFoundException t) {
			throw new ValidationError("Failed to load the driver class '" + driverClassName + "'", t);
		}
		try {
			DriverManager.getConnection(url, userName, password);
		} catch (SQLException t) {
			throw new ValidationError("Failed to create the connection", t);
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
