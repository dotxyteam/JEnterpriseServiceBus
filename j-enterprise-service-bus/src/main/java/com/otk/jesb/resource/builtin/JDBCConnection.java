package com.otk.jesb.resource.builtin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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

	private String driverClassName;
	private String url;
	private String userName;
	private String password;

	public JDBCConnection() {
		this(JDBCConnection.class.getSimpleName() + MiscUtils.getDigitalUniqueIdentifier());
	}

	public JDBCConnection(String name) {
		super(name);
	}

	public String getDriverClassName() {
		return driverClassName;
	}

	public void setDriverClassName(String driverClassName) {
		this.driverClassName = driverClassName;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String test() throws ClassNotFoundException, SQLException {
		ClassUtils.getCachedClassForName(driverClassName);
		DriverManager.getConnection(url, userName, password);
		return "Connection successful !";
	}
	
	public Connection build() throws ClassNotFoundException, SQLException {
		ClassUtils.getCachedClassForName(driverClassName);
		return DriverManager.getConnection(url, userName, password);
	}

	@Override
	public void validate(boolean recursively) throws ValidationError {
		super.validate(recursively);
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
