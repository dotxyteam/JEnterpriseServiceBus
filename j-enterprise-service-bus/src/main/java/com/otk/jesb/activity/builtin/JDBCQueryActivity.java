package com.otk.jesb.activity.builtin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.otk.jesb.InstanceSpecification;
import com.otk.jesb.Solution;
import com.otk.jesb.InstanceSpecification.DynamicValue;
import com.otk.jesb.Plan.ExecutionContext;
import com.otk.jesb.Plan.ValidationContext;
import com.otk.jesb.activity.Activity;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;
import com.otk.jesb.activity.ActivityResult;
import com.otk.jesb.meta.ClassProvider;
import com.otk.jesb.resource.builtin.JDBCConnection;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.info.ResourcePath;

public class JDBCQueryActivity implements Activity {

	private JDBCConnection connection;
	private String statement;
	private ParameterValues parameterValues;
	private String builderUniqueIdentifier;
	private Class<? extends ActivityResult> customResultClass;

	public JDBCConnection getConnection() {
		return connection;
	}

	public void setConnection(JDBCConnection connection) {
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

	public Class<? extends ActivityResult> getCustomResultClass() {
		return customResultClass;
	}

	public void setCustomResultClass(Class<? extends ActivityResult> customResultClass) {
		this.customResultClass = customResultClass;
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
		if (customResultClass != null) {
			return (ActivityResult) customResultClass.getConstructor(ResultSet.class).newInstance(resultSet);
		} else {
			return new GenericResult(resultSet);
		}
	}

	public static class Metadata implements ActivityMetadata {

		@Override
		public String getActivityTypeName() {
			return "JDBC Query";
		}

		@Override
		public Class<? extends ActivityBuilder> getActivityBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getActivityIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(JDBCQueryActivity.class.getName().replace(".", "/") + ".png"));
		}

	}

	public static class Builder implements ActivityBuilder {

		private String uniqueIdentifier = MiscUtils.getDigitalUniqueIdentifier();
		private JDBCConnection connection;
		private String statement;
		private List<ParameterDefinition> parameterDefinitions = new ArrayList<ParameterDefinition>();
		private InstanceSpecification parameterValuesSpecification = new InstanceSpecification();
		private List<ColumnDefinition> resultColumnDefinitions;

		private Class<? extends ActivityResult> customResultClass;
		private Class<? extends ParameterValues> parameterValuesClass;

		public Builder() {
			updateDynamicClasses();
			parameterValuesSpecification.setTypeName(parameterValuesClass.getName());
		}

		private void updateDynamicClasses() {
			{
				if (parameterValuesClass != null) {
					ClassProvider.unregister(parameterValuesClass.getClassLoader());
				}
				parameterValuesClass = createParameterValuesClass();
				ClassProvider.register(parameterValuesClass.getClassLoader());
			}
			{
				if (customResultClass != null) {
					ClassProvider.unregister(customResultClass.getClassLoader());
				}
				customResultClass = createCustomResultClass();
				if (customResultClass != null) {
					ClassProvider.register(customResultClass.getClassLoader());
				}
			}
		}

		@SuppressWarnings("unchecked")
		private Class<? extends ActivityResult> createCustomResultClass() {
			if (resultColumnDefinitions == null) {
				return null;
			}
			String resultClassName = JDBCQueryActivity.class.getSimpleName() + "Result" + uniqueIdentifier;
			String resultRowClassName = "ResultRow";
			StringBuilder javaSource = new StringBuilder();
			javaSource.append(
					"public class " + resultClassName + " implements " + ActivityResult.class.getName() + "{" + "\n");
			javaSource.append("  private " + List.class.getName() + "<" + resultRowClassName + "> rows = new "
					+ ArrayList.class.getName() + "<" + resultRowClassName + ">();\n");
			javaSource.append("  public " + resultClassName + "(" + ResultSet.class.getName() + " resultSet) throws "
					+ SQLException.class.getName() + "{\n");
			javaSource.append("    while (resultSet.next()) {\n");
			javaSource.append("      " + resultRowClassName + " row = new " + resultRowClassName + "();\n");
			for (int i = 0; i < resultColumnDefinitions.size(); i++) {
				ColumnDefinition columnDefinition = resultColumnDefinitions.get(i);
				javaSource.append(
						"      row." + columnDefinition.getColumnName() + " = (" + columnDefinition.getColumnTypeName()
								+ ")resultSet.getObject(\"" + columnDefinition.getColumnName() + "\");\n");
			}
			javaSource.append("      rows.add(row);\n");
			javaSource.append("    }\n");
			javaSource.append("  }\n");
			javaSource.append("  public " + List.class.getName() + "<" + resultRowClassName + "> getRows(){\n");
			javaSource.append("    return rows;\n");
			javaSource.append("  }\n");
			{
				javaSource.append("public static class " + resultRowClassName + "{" + "\n");
				for (int i = 0; i < resultColumnDefinitions.size(); i++) {
					ColumnDefinition columnDefinition = resultColumnDefinitions.get(i);
					javaSource.append("  private " + columnDefinition.getColumnTypeName() + " "
							+ columnDefinition.getColumnName() + ";\n");
				}
				for (int i = 0; i < resultColumnDefinitions.size(); i++) {
					ColumnDefinition columnDefinition = resultColumnDefinitions.get(i);
					String getterMethoName = "get" + columnDefinition.getColumnName().substring(0, 1).toUpperCase()
							+ columnDefinition.getColumnName().substring(1);
					javaSource.append(
							"  public " + columnDefinition.getColumnTypeName() + " " + getterMethoName + "() {" + "\n");
					javaSource.append("    return " + columnDefinition.getColumnName() + ";" + "\n");
					javaSource.append("  }" + "\n");
				}
				javaSource.append("}" + "\n");
			}
			javaSource.append("}" + "\n");
			return (Class<? extends ActivityResult>) MiscUtils.createClass(resultClassName, javaSource.toString(),
					JDBCQueryActivity.class.getClassLoader());
		}

		@SuppressWarnings("unchecked")
		private Class<? extends ParameterValues> createParameterValuesClass() {
			String className = JDBCQueryActivity.class.getSimpleName() + "ParameterValues" + uniqueIdentifier;
			StringBuilder javaSource = new StringBuilder();
			javaSource.append("public class " + className + " implements "
					+ ParameterValues.class.getName().replace("$", ".") + "{" + "\n");
			for (int i = 0; i < parameterDefinitions.size(); i++) {
				ParameterDefinition parameterDefinition = parameterDefinitions.get(i);
				javaSource.append("  private " + parameterDefinition.getParameterTypeName() + " "
						+ parameterDefinition.getParameterName() + ";\n");
			}
			List<String> constructorParameterDeclarations = new ArrayList<String>();
			for (int i = 0; i < parameterDefinitions.size(); i++) {
				ParameterDefinition parameterDefinition = parameterDefinitions.get(i);
				constructorParameterDeclarations
						.add(parameterDefinition.getParameterTypeName() + " " + parameterDefinition.getParameterName());
			}
			javaSource.append("  public " + className + "("
					+ xy.reflect.ui.util.MiscUtils.stringJoin(constructorParameterDeclarations, ", ") + "){" + "\n");
			for (int i = 0; i < parameterDefinitions.size(); i++) {
				ParameterDefinition parameterDefinition = parameterDefinitions.get(i);
				javaSource.append("    this." + parameterDefinition.getParameterName() + " = "
						+ parameterDefinition.getParameterName() + ";\n");
			}
			javaSource.append("  }" + "\n");
			javaSource.append("  public Object getParameterValueByIndex(int i) {" + "\n");
			for (int i = 0; i < parameterDefinitions.size(); i++) {
				ParameterDefinition parameterDefinition = parameterDefinitions.get(i);
				javaSource
						.append("    if(i == " + i + ") return " + parameterDefinition.getParameterName() + ";" + "\n");
			}
			javaSource.append("    throw new " + AssertionError.class.getName() + "();" + "\n");
			javaSource.append("  }" + "\n");

			javaSource.append("}" + "\n");
			return (Class<? extends ParameterValues>) MiscUtils.createClass(className, javaSource.toString(),
					JDBCQueryActivity.class.getClassLoader());
		}

		public String getUniqueIdentifier() {
			return uniqueIdentifier;
		}

		public void setUniqueIdentifier(String uniqueIdentifier) {
			this.uniqueIdentifier = uniqueIdentifier;
			updateDynamicClasses();
			parameterValuesSpecification.setTypeName(parameterValuesClass.getName());
		}

		public JDBCConnection getConnection() {
			return connection;
		}

		public void setConnection(JDBCConnection connection) {
			this.connection = connection;
		}

		public static List<JDBCConnection> getConnectionChoices() {
			return MiscUtils.findResources(Solution.INSTANCE, JDBCConnection.class);
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
			updateDynamicClasses();
		}

		public InstanceSpecification getParameterValuesSpecification() {
			return parameterValuesSpecification;
		}

		public void setParameterValuesSpecification(InstanceSpecification parameterValuesSpecification) {
			if (parameterValuesSpecification == null) {
				throw new AssertionError();
			}
			this.parameterValuesSpecification = parameterValuesSpecification;
		}

		public List<ColumnDefinition> getResultColumnDefinitions() {
			return resultColumnDefinitions;
		}

		public void setResultColumnDefinitions(List<ColumnDefinition> resultColumnDefinitions) {
			this.resultColumnDefinitions = resultColumnDefinitions;
			updateDynamicClasses();
		}

		public void retrieveResultColumnDefinitions() throws SQLException {
			Connection conn = DriverManager.getConnection(connection.getUrl(), connection.getUserName(),
					connection.getPassword());
			PreparedStatement preparedStatement = conn.prepareStatement(statement);
			ResultSetMetaData metaData = preparedStatement.getMetaData();
			this.resultColumnDefinitions = new ArrayList<ColumnDefinition>();
			for (int i = 0; i < metaData.getColumnCount(); i++) {
				this.resultColumnDefinitions
						.add(new ColumnDefinition(metaData.getColumnLabel(i + 1), metaData.getColumnClassName(i + 1)));
			}
			updateDynamicClasses();
		}

		public void clearResultColumnDefinitions() throws SQLException {
			this.resultColumnDefinitions = null;
			updateDynamicClasses();
		}

		@Override
		public Activity build(ExecutionContext context) throws Exception {
			JDBCQueryActivity result = new JDBCQueryActivity();
			result.setConnection(connection);
			result.setStatement(statement);
			ParameterValues parameterValues = (ParameterValues) parameterValuesSpecification.build(context);
			result.setParameterValues(parameterValues);
			result.setBuilderUniqueIdentifier(uniqueIdentifier);
			result.setCustomResultClass(customResultClass);
			return result;
		}

		@Override
		public Class<? extends ActivityResult> getActivityResultClass() {
			if (customResultClass != null) {
				return customResultClass;
			} else {
				return GenericResult.class;
			}
		}

		@Override
		public boolean completeValidationContext(ValidationContext validationContext,
				DynamicValue currentDynamicValue) {
			return parameterValuesSpecification.completeValidationContext(validationContext, currentDynamicValue);
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

	public static class GenericResult implements ActivityResult {
		private List<GenericResultRow> rows = new ArrayList<GenericResultRow>();

		public GenericResult(ResultSet resultSet) throws SQLException {
			ResultSetMetaData metaData = resultSet.getMetaData();
			while (resultSet.next()) {
				GenericResultRow row = new GenericResultRow();
				for (int iColumn = 1; iColumn < metaData.getColumnCount(); iColumn++) {
					row.getCellValues().put(metaData.getColumnName(iColumn), resultSet.getObject(iColumn));
				}
				rows.add(row);
			}
		}

		public List<GenericResultRow> getRows() {
			return rows;
		}

	}

	public static class GenericResultRow {

		private Map<String, Object> cellValues = new HashMap<String, Object>();

		public List<String> getColumnNames() {
			return new ArrayList<String>(cellValues.keySet());
		}

		public Map<String, Object> getCellValues() {
			return cellValues;
		}

	}

}