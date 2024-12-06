package com.otk.jesb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.otk.jesb.Plan.ExecutionContext;

public class JDBCQueryActivity implements Activity {

	private JDBCConnectionResource connection;
	private String statement;
	private ParameterValues parameterValues;
	private String builderUniqueIdentifier;
	private Class<? extends ActivityResult> resultClass;

	public JDBCConnectionResource getConnection() {
		return connection;
	}

	public void setConnection(JDBCConnectionResource connection) {
		this.connection = connection;
	}

	public String getStatement() {
		return statement;
	}

	public void setStatement(String statement) {
		this.statement = statement;
	}

	public ParameterValues getParameterValues() {
		return parameterValues;
	}

	public void setParameterValues(ParameterValues parameterValues) {
		this.parameterValues = parameterValues;
	}

	public String getBuilderUniqueIdentifier() {
		return builderUniqueIdentifier;
	}

	public void setBuilderUniqueIdentifier(String builderUniqueIdentifier) {
		this.builderUniqueIdentifier = builderUniqueIdentifier;
	}

	public Class<? extends ActivityResult> getResultClass() {
		return resultClass;
	}

	public void setResultClass(Class<? extends ActivityResult> resultClass) {
		this.resultClass = resultClass;
	}

	@Override
	public ActivityResult execute() throws Exception {
		Connection conn = DriverManager.getConnection(connection.getUrl(), connection.getUserName(),
				connection.getPassword());
		PreparedStatement preparedStatement = conn.prepareStatement(statement);
		for (int i = 0; i < preparedStatement.getParameterMetaData().getParameterCount(); i++) {
			preparedStatement.setObject(i + 1, parameterValues.getParameterValueByIndex(i));
		}
		ResultSet resultSet = preparedStatement.executeQuery();
		ActivityResult result = (ActivityResult) resultClass.getConstructor().newInstance(resultSet);
		return result;
	}

	public static class Builder implements ActivityBuilder {

		private String uniqueIdentifier = Utils.getDigitalUniqueIdentifier();
		private String connectionPath;
		private String statement;
		private List<ParameterDefinition> parameterDefinitions = new ArrayList<ParameterDefinition>();
		private ObjectSpecification parameterValuesSpecification = new ObjectSpecification();
		private List<ColumnDefinition> resultColumnDefinitions;

		private Class<? extends ActivityResult> resultClass;
		private Class<?> resultRowClass;
		private Class<? extends ParameterValues> parameterValuesClass;

		public Builder() {
			createDynamicClasses();
			parameterValuesSpecification.setTypeName(parameterValuesClass.getName());
		}

		private void createDynamicClasses() {
			parameterValuesClass = createParameterValuesClass();
			resultRowClass = createResultRowClass();
			resultClass = createResultClass();
		}

		private Class<?> createResultRowClass() {
			StringBuilder javaSource = new StringBuilder();
			String resultRowClassName = JDBCQueryActivity.class.getSimpleName() + "ResultRow" + uniqueIdentifier;
			javaSource.append("public class " + resultRowClassName + "{" + "\n");
			for (int i = 0; i < resultColumnDefinitions.size(); i++) {
				ColumnDefinition columnDefinition = resultColumnDefinitions.get(i);
				javaSource.append("  private " + columnDefinition.getColumnTypeName() + " "
						+ columnDefinition.getColumnName() + ";\n");
			}
			for (int i = 0; i < resultColumnDefinitions.size(); i++) {
				ColumnDefinition columnDefinition = resultColumnDefinitions.get(i);
				String getterMethoName = "get" + columnDefinition.getColumnName().substring(0, 1).toUpperCase()
						+ columnDefinition.getColumnName().substring(1);
				String setterMethoName = "set" + columnDefinition.getColumnName().substring(0, 1).toUpperCase()
						+ columnDefinition.getColumnName().substring(1);
				javaSource.append("  public " + columnDefinition.getColumnTypeName() + getterMethoName + "() {" + "\n");
				javaSource.append("    return " + columnDefinition.getColumnName() + ";" + "\n");
				javaSource.append("  }" + "\n");
				javaSource.append("  public void " + setterMethoName + "(" + columnDefinition.getColumnTypeName()
						+ " value) {" + "\n");
				javaSource.append("    this. " + columnDefinition.getColumnName() + " = value;" + "\n");
				javaSource.append("  }" + "\n");
			}
			javaSource.append("}" + "\n");
			return Utils.createClass(resultRowClassName, javaSource.toString(),
					JDBCQueryActivity.class.getClassLoader());
		}

		@SuppressWarnings("unchecked")
		private Class<? extends ActivityResult> createResultClass() {
			if (resultColumnDefinitions == null) {
				return null;
			}
			String resultClassName = JDBCQueryActivity.class.getSimpleName() + "Result" + uniqueIdentifier;
			String resultRowClassName = resultRowClass.getName();
			StringBuilder javaSource = new StringBuilder();
			javaSource.append("public class " + resultClassName + "{" + "\n");
			javaSource.append("  private " + List.class.getName() + "<" + resultRowClassName + "> rows = new "
					+ ArrayList.class.getName() + "<" + resultRowClassName + ">();\n");
			javaSource.append("  public " + resultClassName + "(" + ResultSet.class.getName() + " resultSet){\n");
			javaSource.append("    while (resultSet.next()) {\n");
			javaSource.append("      " + resultRowClassName + " row = new " + resultRowClassName + "();;\n");
			for (int i = 0; i < resultColumnDefinitions.size(); i++) {
				ColumnDefinition columnDefinition = resultColumnDefinitions.get(i);
				String setterMethodName = "set" + columnDefinition.getColumnName().substring(0, 1).toUpperCase()
						+ columnDefinition.getColumnName().substring(1);
				javaSource.append("      row." + setterMethodName + "((" + columnDefinition.getColumnTypeName()
						+ ")resultSet.getObject(\"" + columnDefinition.getColumnName() + "\"));\n");
			}
			javaSource.append("      rows.add(row);\n");
			javaSource.append("    };\n");
			javaSource.append("  };\n");
			javaSource.append("  public " + resultRowClassName + " getRows(){\n");
			javaSource.append("    return rows;\n");
			javaSource.append("  };\n");
			javaSource.append("}" + "\n");
			return (Class<? extends ActivityResult>) Utils.createClass(resultClassName, javaSource.toString(),
					resultRowClass.getClassLoader());
		}

		@SuppressWarnings("unchecked")
		private Class<? extends ParameterValues> createParameterValuesClass() {
			String className = JDBCQueryActivity.class.getSimpleName() + "ParameterValues" + uniqueIdentifier;
			StringBuilder javaSource = new StringBuilder();
			javaSource.append(
					"public class " + className + " implements " + ParameterValues.class.getName() + "{" + "\n");
			for (int i = 0; i < parameterDefinitions.size(); i++) {
				ParameterDefinition parameterDefinition = parameterDefinitions.get(i);
				javaSource.append("  private " + parameterDefinition.getParameterTypeName() + " "
						+ parameterDefinition.getParameterName() + ";\n");
			}
			for (int i = 0; i < parameterDefinitions.size(); i++) {
				ParameterDefinition parameterDefinition = parameterDefinitions.get(i);
				String getterMethoName = "get" + parameterDefinition.getParameterName().substring(0, 1).toUpperCase()
						+ parameterDefinition.getParameterName().substring(1);
				String setterMethoName = "set" + parameterDefinition.getParameterName().substring(0, 1).toUpperCase()
						+ parameterDefinition.getParameterName().substring(1);
				javaSource.append(
						"  public " + parameterDefinition.getParameterTypeName() + getterMethoName + "() {" + "\n");
				javaSource.append("    return " + parameterDefinition.getParameterName() + ";" + "\n");
				javaSource.append("  }" + "\n");
				javaSource.append("  public void " + setterMethoName + "(" + parameterDefinition.getParameterTypeName()
						+ " value) {" + "\n");
				javaSource.append("    this. " + parameterDefinition.getParameterName() + " = value;" + "\n");
				javaSource.append("  }" + "\n");
			}
			javaSource.append("  public Object getParameterValueByIndex(int i) {" + "\n");
			for (int i = 0; i < parameterDefinitions.size(); i++) {
				ParameterDefinition parameterDefinition = parameterDefinitions.get(i);
				javaSource
						.append("    if(i == " + i + ") return " + parameterDefinition.getParameterName() + ";" + "\n");
			}
			javaSource.append("    throw new " + AssertionError.class.getName() + "();" + "\n");
			javaSource.append("  }" + "\n");

			javaSource.append("}" + "\n");
			return (Class<? extends ParameterValues>) Utils.createClass(className, javaSource.toString(),
					JDBCQueryActivity.class.getClassLoader());
		}

		public String getUniqueIdentifier() {
			return uniqueIdentifier;
		}

		public void setUniqueIdentifier(String uniqueIdentifier) {
			this.uniqueIdentifier = uniqueIdentifier;
			createDynamicClasses();
			parameterValuesSpecification.setTypeName(parameterValuesClass.getName());
		}

		public String getConnectionPath() {
			return connectionPath;
		}

		public void setConnectionPath(String connectionPath) {
			this.connectionPath = connectionPath;
		}

		public static List<String> getConnectionPathChoices() {
			List<String> result = new ArrayList<String>();
			for (int i = 0; i < Workspace.JDBC_CONNECTIONS.size(); i++) {
				result.add(String.valueOf(i));
			}
			return result;
		}

		public String getStatement() {
			return statement;
		}

		public void setStatement(String statement) {
			this.statement = statement;
		}

		public List<ParameterDefinition> getParameterDefinitions() {
			return parameterDefinitions;
		}

		public void setParameterDefinitions(List<ParameterDefinition> parameterDefinitions) {
			this.parameterDefinitions = parameterDefinitions;
		}

		public ObjectSpecification getParameterValuesSpecification() {
			return parameterValuesSpecification;
		}

		public void setParameterValuesSpecification(ObjectSpecification parameterValuesSpecification) {
			if (parameterValuesSpecification == null) {
				throw new AssertionError();
			}
			this.parameterValuesSpecification = parameterValuesSpecification;
		}

		public List<ColumnDefinition> getResultColumnDefinitions() {
			return resultColumnDefinitions;
		}

		public void retrieveResultColumnDefinitions() throws SQLException {
			JDBCConnectionResource connection = Workspace.JDBC_CONNECTIONS.get(Integer.valueOf(connectionPath));
			Connection conn = DriverManager.getConnection(connection.getUrl(), connection.getUserName(),
					connection.getPassword());
			PreparedStatement preparedStatement = conn.prepareStatement(statement);
			ResultSetMetaData metaData = preparedStatement.getMetaData();
			this.resultColumnDefinitions = new ArrayList<ColumnDefinition>();
			for (int i = 0; i < metaData.getColumnCount(); i++) {
				this.resultColumnDefinitions
						.add(new ColumnDefinition(metaData.getColumnLabel(i + 1), metaData.getColumnClassName(i + 1)));
			}
		}

		@Override
		public Activity build(ExecutionContext context) throws Exception {
			JDBCQueryActivity result = new JDBCQueryActivity();
			result.setConnection(Workspace.JDBC_CONNECTIONS.get(Integer.valueOf(connectionPath)));
			result.setStatement(statement);
			ParameterValues parameterValues = (ParameterValues) parameterValuesSpecification.build(context);
			result.setParameterValues(parameterValues);
			result.setBuilderUniqueIdentifier(uniqueIdentifier);
			result.setResultClass(resultClass);
			return result;
		}

	}

	public static class ParameterDefinition {

		private String parameterName;
		private String parameterTypeName;

		public String getParameterName() {
			return parameterName;
		}

		public void setParameterName(String parameterName) {
			this.parameterName = parameterName;
		}

		public String getParameterTypeName() {
			return parameterTypeName;
		}

		public void setParameterTypeName(String parameterTypeName) {
			this.parameterTypeName = parameterTypeName;
		}

		public static List<String> getParameterTypeNameChoices() {
			List<String> result = new ArrayList<String>();
			for (Class<?> clazz : Arrays.asList(String.class, Boolean.class, Integer.class, Long.class, Float.class,
					Double.class, byte[].class, java.sql.Date.class, java.sql.Time.class, java.sql.Timestamp.class)) {
				result.add(clazz.getName());
			}
			return result;
		}

	}

	public static interface ParameterValues {
		public Object getParameterValueByIndex(int i);
	}

	public static class ColumnDefinition {

		private String columnName;
		private String columnTypeName;

		public ColumnDefinition(String columnName, String columnTypeName) {
			this.columnName = columnName;
			this.columnTypeName = columnTypeName;
		}

		public String getColumnName() {
			return columnName;
		}

		public void setColumnName(String columnName) {
			this.columnName = columnName;
		}

		public String getColumnTypeName() {
			return columnTypeName;
		}

		public void setColumnTypeName(String columnTypeName) {
			this.columnTypeName = columnTypeName;
		}

	}

}
