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

	public boolean isDriverClassNameVariable() {
		return driverClassNameVariant.isVariable();
	}

	public void setDriverClassNameVariable(boolean b) {
		driverClassNameVariant.setVariable(b);
	}

	public boolean isUrlVariable() {
		return urlVariant.isVariable();
	}

	public void setUrlVariable(boolean b) {
		urlVariant.setVariable(b);
	}

	public boolean isUserNameVariable() {
		return userNameVariant.isVariable();
	}

	public void setUserNameVariable(boolean b) {
		userNameVariant.setVariable(b);
	}

	public boolean isPasswordVariable() {
		return passwordVariant.isVariable();
	}

	public void setPasswordVariable(boolean b) {
		passwordVariant.setVariable(b);
	}

	public String getDriverClassName() {
		return driverClassNameVariant.getValue();
	}

	public void setDriverClassName(String driverClassName) {
		driverClassNameVariant.setValue(driverClassName);
	}

	public String getUrl() {
		return urlVariant.getValue();
	}

	public void setUrl(String url) {
		urlVariant.setValue(url);
	}

	public String getUserName() {
		return userNameVariant.getValue();
	}

	public void setUserName(String userName) {
		userNameVariant.setValue(userName);
	}

	public String getPassword() {
		return passwordVariant.getValue();
	}

	public void setPassword(String password) {
		passwordVariant.setValue(password);
	}

	public String test() throws ClassNotFoundException, SQLException {
		ClassUtils.getCachedClassForName(getDriverClassName());
		DriverManager.getConnection(getUrl(), getUserName(), getPassword());
		return "Connection successful!";
	}

	public Connection build() throws ClassNotFoundException, SQLException {
		ClassUtils.getCachedClassForName(getDriverClassName());
		return DriverManager.getConnection(getUrl(), getUserName(), getPassword());
	}

	@Override
	public void validate(boolean recursively) throws ValidationError {
		super.validate(recursively);
		String driverClassName = getDriverClassName();
		String url = getUrl();
		String userName = getUserName();
		String password = getPassword();
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
