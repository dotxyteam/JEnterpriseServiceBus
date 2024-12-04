package com.otk.jesb;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JDBCQueryActivity implements Activity {

	private JDBCConnectionResource connection;
	private String Statement;

	public JDBCConnectionResource getConnection() {
		return connection;
	}

	public void setConnection(JDBCConnectionResource connection) {
		this.connection = connection;
	}

	public String getStatement() {
		return Statement;
	}

	public void setStatement(String statement) {
		Statement = statement;
	}

	List<JDBCConnectionResource> getconnectionChoices() {
		return Workspace.JDBC_CONNECTIONS;
	}

	@Override
	public JDBCQueryActivityResult execute() throws Exception {
		Connection conn = DriverManager.getConnection(connection.getUrl(), connection.getUserName(),
				connection.getPassword());
		Statement stat = conn.createStatement();
		ResultSet resultSet = stat.executeQuery(Statement);
		ResultSetMetaData metaData = resultSet.getMetaData();
		JDBCQueryActivityResult result = new JDBCQueryActivityResult();
		while (resultSet.next()) {
			JDBCQueryActivityResultRow row = new JDBCQueryActivityResultRow();
			for (int iColumn = 1; iColumn < metaData.getColumnCount(); iColumn++) {
				row.cellValues.put(metaData.getColumnName(iColumn), resultSet.getObject(iColumn));
			}
			result.rows.add(row);
		}
		return result;
	}

	public static class JDBCQueryActivityResult implements Result {
		private List<JDBCQueryActivityResultRow> rows = new ArrayList<JDBCQueryActivity.JDBCQueryActivityResultRow>();

		public List<JDBCQueryActivityResultRow> getRows() {
			return rows;
		}

	}

	public static class JDBCQueryActivityResultRow {

		private Map<String, Object> cellValues = new HashMap<String, Object>();

		public List<String> getColumnNames() {
			return new ArrayList<String>(cellValues.keySet());
		}

		public Object getCellValue(String columnName) {
			return cellValues.get(columnName);
		}

	}

}
