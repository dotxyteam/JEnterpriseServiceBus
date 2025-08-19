package com.otk.jesb.operation.builtin;

import com.otk.jesb.Session;
import com.otk.jesb.resource.builtin.JDBCConnection;

public class JDBCStoredProcedureCall extends JDBCQuery {

	public JDBCStoredProcedureCall(Session session, JDBCConnection connection, Class<?> customResultClass) {
		super(session, connection, customResultClass);
	}

}
