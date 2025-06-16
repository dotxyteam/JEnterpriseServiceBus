package com.otk.jesb.resource.builtin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.otk.jesb.Template;
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

	private Template<String> driverClassNameTemplate = new Template<String>(String.class);
	private Template<String> urlTemplate = new Template<String>(String.class);
	private Template<String> userNameTemplate = new Template<String>(String.class);
	private Template<String> passwordTemplate = new Template<String>(String.class);

	public JDBCConnection() {
		this(JDBCConnection.class.getSimpleName() + MiscUtils.getDigitalUniqueIdentifier());
	}

	public JDBCConnection(String name) {
		super(name);
	}

	public Template<String> getDriverClassNameTemplate() {
		return driverClassNameTemplate;
	}

	public void setDriverClassNameTemplate(Template<String> driverClassNameTemplate) {
		this.driverClassNameTemplate = driverClassNameTemplate;
	}

	public Template<String> getUrlTemplate() {
		return urlTemplate;
	}

	public void setUrlTemplate(Template<String> urlTemplate) {
		this.urlTemplate = urlTemplate;
	}

	public Template<String> getUserNameTemplate() {
		return userNameTemplate;
	}

	public void setUserNameTemplate(Template<String> userNameTemplate) {
		this.userNameTemplate = userNameTemplate;
	}

	public Template<String> getPasswordTemplate() {
		return passwordTemplate;
	}

	public void setPasswordTemplate(Template<String> passwordTemplate) {
		this.passwordTemplate = passwordTemplate;
	}

	public String test() throws ClassNotFoundException, SQLException {
		ClassUtils.getCachedClassForName(driverClassNameTemplate.getValue());
		DriverManager.getConnection(urlTemplate.getValue(), userNameTemplate.getValue(), passwordTemplate.getValue());
		return "Connection successful !";
	}

	public Connection build() throws ClassNotFoundException, SQLException {
		ClassUtils.getCachedClassForName(driverClassNameTemplate.getValue());
		return DriverManager.getConnection(urlTemplate.getValue(), userNameTemplate.getValue(),
				passwordTemplate.getValue());
	}

	@Override
	public void validate(boolean recursively) throws ValidationError {
		super.validate(recursively);
		try {
			ClassUtils.getCachedClassForName(driverClassNameTemplate.getValue());
		} catch (ClassNotFoundException t) {
			throw new ValidationError("Failed to load the driver class '" + driverClassNameTemplate.getValue() + "'",
					t);
		}
		try {
			DriverManager.getConnection(urlTemplate.getValue(), userNameTemplate.getValue(),
					passwordTemplate.getValue());
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
