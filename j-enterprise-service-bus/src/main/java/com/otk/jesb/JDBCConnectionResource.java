package com.otk.jesb;

import java.sql.DriverManager;
import java.sql.SQLException;

import xy.reflect.ui.control.swing.customizer.SwingCustomizer;

public class JDBCConnectionResource extends Resource {

    public static void main(String[] args) {
        SwingCustomizer.getDefault().openObjectFrame(new JDBCConnectionResource("test"));
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

    public JDBCConnectionResource(String name) {
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
}
