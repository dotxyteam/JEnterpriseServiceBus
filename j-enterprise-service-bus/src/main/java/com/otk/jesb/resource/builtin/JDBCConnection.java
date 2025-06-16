package com.otk.jesb.resource.builtin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.otk.jesb.Expression;
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

	private Expression<String> driverClassNameExpression = new Expression<String>(String.class);
	private Expression<String> urlExpression = new Expression<String>(String.class);
	private Expression<String> userNameExpression = new Expression<String>(String.class);
	private Expression<String> passwordExpression = new Expression<String>(String.class);

	public JDBCConnection() {
		this(JDBCConnection.class.getSimpleName() + MiscUtils.getDigitalUniqueIdentifier());
	}

	public JDBCConnection(String name) {
		super(name);
	}

	public Expression<String> getDriverClassNameExpression() {
		return driverClassNameExpression;
	}

	public void setDriverClassNameExpression(Expression<String> driverClassNameExpression) {
		this.driverClassNameExpression = driverClassNameExpression;
	}

	public Expression<String> getUrlExpression() {
		return urlExpression;
	}

	public void setUrlExpression(Expression<String> urlExpression) {
		this.urlExpression = urlExpression;
	}

	public Expression<String> getUserNameExpression() {
		return userNameExpression;
	}

	public void setUserNameExpression(Expression<String> userNameExpression) {
		this.userNameExpression = userNameExpression;
	}

	public Expression<String> getPasswordExpression() {
		return passwordExpression;
	}

	public void setPasswordExpression(Expression<String> passwordExpression) {
		this.passwordExpression = passwordExpression;
	}

	public boolean isDriverClassNameDynamic() {
		return driverClassNameExpression.isDynamic();
	}

	public void setDriverClassNameDynamic(boolean b) {
		driverClassNameExpression.setDynamic(b);
	}

	public boolean isUrlExpression() {
		return urlExpression.isDynamic();
	}

	public void setUrlExpression(boolean b) {
		urlExpression.setDynamic(b);
	}

	public boolean isUserNameExpression() {
		return userNameExpression.isDynamic();
	}

	public void setUserNameExpression(boolean b) {
		userNameExpression.setDynamic(b);
	}

	public boolean isPasswordExpression() {
		return passwordExpression.isDynamic();
	}

	public void setPasswordExpression(boolean b) {
		passwordExpression.setDynamic(b);
	}

	public String getDriverClassName() {
		return driverClassNameExpression.evaluate();
	}

	public void setDriverClassName(String driverClassName) {
		driverClassNameExpression.represent(driverClassName);
	}

	public String getUrl() {
		return urlExpression.evaluate();
	}

	public void setUrl(String url) {
		urlExpression.represent(url);
	}

	public String getUserName() {
		return userNameExpression.evaluate();
	}

	public void setUserName(String userName) {
		userNameExpression.represent(userName);
	}

	public String getPassword() {
		return passwordExpression.evaluate();
	}

	public void setPassword(String password) {
		passwordExpression.represent(password);
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
		try {
			ClassUtils.getCachedClassForName(getDriverClassName());
		} catch (ClassNotFoundException t) {
			throw new ValidationError("Failed to load the driver class '" + getDriverClassName() + "'", t);
		}
		try {
			DriverManager.getConnection(getUrl(), getUserName(), getPassword());
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
