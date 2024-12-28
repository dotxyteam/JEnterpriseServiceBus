package com.otk.jesb.resource.builtin;

import java.sql.DriverManager;
import java.sql.SQLException;

import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.ResourceMetadata;

import xy.reflect.ui.control.swing.customizer.SwingCustomizer;
import xy.reflect.ui.info.ResourcePath;

public class JDBCConnection extends Resource {

	public static void main(String[] args) {
		SwingCustomizer.getDefault().openObjectFrame(new JDBCConnection("test"));
	}

	static {
		try {
			Class.forName("org.hsqldb.jdbcDriver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	private String url;
	private String userName;
	private String password;

	public JDBCConnection(String name) {
		super(name);
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

	public String test() throws SQLException {
		DriverManager.getConnection(url, userName, password);
		return "Connection successful";
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
