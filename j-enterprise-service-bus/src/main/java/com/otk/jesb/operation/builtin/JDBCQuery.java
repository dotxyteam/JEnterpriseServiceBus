package com.otk.jesb.operation.builtin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.meta.TypeInfoProvider;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.resource.builtin.JDBCConnection;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.UpToDate;
import com.otk.jesb.util.UpToDate.VersionAccessException;

import xy.reflect.ui.info.ResourcePath;

public class JDBCQuery extends JDBCOperation {

	private Class<?> customResultClass;

	public JDBCQuery(JDBCConnection connection, Class<?> customResultClass) {
		super(connection);
		this.customResultClass = customResultClass;
	}

	@Override
	public Object execute() throws Exception {
		PreparedStatement preparedStatement = prepare();
		ResultSet resultSet = preparedStatement.executeQuery();
		if (customResultClass != null) {
			return customResultClass.getConstructor(ResultSet.class).newInstance(resultSet);
		} else {
			return new GenericResult(resultSet);
		}
	}

	public static class Metadata implements OperationMetadata {

		@Override
		public String getOperationTypeName() {
			return "JDBC Query";
		}

		@Override
		public String getCategoryName() {
			return "JDBC";
		}

		@Override
		public Class<? extends OperationBuilder> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(JDBCQuery.class.getName().replace(".", "/") + ".png"));
		}

	}

	public static class Builder extends JDBCOperation.Builder {

		private List<ColumnDefinition> resultColumnDefinitions;

		private UpToDate<Class<?>> upToDateCustomResultClass = new UpToDateCustomResultClass();

		private Class<?> createCustomResultClass() {
			if (resultColumnDefinitions == null) {
				return null;
			}
			String resultClassName = JDBCQuery.class.getName() + "Result" + MiscUtils.toDigitalUniqueIdentifier(this);
			String resultRowClassName = "ResultRow";
			StringBuilder javaSource = new StringBuilder();
			javaSource.append("package " + MiscUtils.extractPackageNameFromClassName(resultClassName) + ";" + "\n");
			javaSource.append("public class " + MiscUtils.extractSimpleNameFromClassName(resultClassName) + "{" + "\n");
			javaSource.append("  private " + List.class.getName() + "<" + resultRowClassName + "> rows = new "
					+ ArrayList.class.getName() + "<" + resultRowClassName + ">();\n");
			javaSource.append("  public " + MiscUtils.extractSimpleNameFromClassName(resultClassName) + "("
					+ ResultSet.class.getName() + " resultSet) throws " + SQLException.class.getName() + "{\n");
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
			try {
				return MiscUtils.IN_MEMORY_COMPILER.compile(resultClassName, javaSource.toString());
			} catch (CompilationError e) {
				throw new UnexpectedError(e);
			}
		}

		public List<ColumnDefinition> getResultColumnDefinitions() {
			return resultColumnDefinitions;
		}

		public void setResultColumnDefinitions(List<ColumnDefinition> resultColumnDefinitions) {
			this.resultColumnDefinitions = resultColumnDefinitions;
		}

		public void retrieveResultColumnDefinitions() throws SQLException {
			JDBCConnection connection = getConnection();
			Connection conn = DriverManager.getConnection(connection.getUrl(), connection.getUserName(),
					connection.getPassword());
			PreparedStatement preparedStatement = conn.prepareStatement(getStatement());
			ResultSetMetaData metaData = preparedStatement.getMetaData();
			this.resultColumnDefinitions = new ArrayList<ColumnDefinition>();
			for (int i = 0; i < metaData.getColumnCount(); i++) {
				this.resultColumnDefinitions
						.add(new ColumnDefinition(metaData.getColumnLabel(i + 1), metaData.getColumnClassName(i + 1)));
			}
		}

		public void clearResultColumnDefinitions() throws SQLException {
			this.resultColumnDefinitions = null;
		}

		@Override
		public Operation build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			JDBCQuery result = new JDBCQuery(getConnection(), upToDateCustomResultClass.get());
			result.setStatement(getStatement());
			result.setParameterValues(buildParameterValues(context));
			return result;
		}

		@Override
		public Class<?> getOperationResultClass(Plan currentPlan, Step currentStep) {
			Class<?> customResultClass;
			try {
				customResultClass = upToDateCustomResultClass.get();
			} catch (VersionAccessException e) {
				throw new UnexpectedError(e);
			}
			if (customResultClass != null) {
				return customResultClass;
			} else {
				return GenericResult.class;
			}
		}

		@Override
		public void validate(boolean recursively, Plan plan, Step step) throws ValidationError {
			super.validate(recursively, plan, step);
			if (recursively) {
				if (resultColumnDefinitions != null) {
					List<String> columnNames = new ArrayList<String>();
					for (ColumnDefinition columnDefinition : resultColumnDefinitions) {
						if (columnNames.contains(columnDefinition.getColumnName())) {
							throw new ValidationError(
									"Duplicate column name detected: '" + columnDefinition.getColumnName() + "'");
						} else {
							columnNames.add(columnDefinition.getColumnName());
						}
						try {
							columnDefinition.validate();
						} catch (ValidationError e) {
							throw new ValidationError("Failed to validate the column '"
									+ columnDefinition.getColumnName() + "' definition", e);
						}
					}
				}
			}
		}

		private class UpToDateCustomResultClass extends UpToDate<Class<?>> {
			@Override
			protected Object retrieveLastVersionIdentifier() {
				return (resultColumnDefinitions == null) ? null : MiscUtils.serialize(resultColumnDefinitions);
			}

			@Override
			protected Class<?> obtainLatest(Object versionIdentifier) {
				return createCustomResultClass();
			}
		}

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

		public void validate() throws ValidationError {
			if (!MiscUtils.VARIABLE_NAME_PATTERN.matcher(columnName).matches()) {
				throw new ValidationError(
						"Invalid column name: '" + columnTypeName + "' (should match the following regular expression: "
								+ MiscUtils.VARIABLE_NAME_PATTERN.pattern() + ")");
			}
			try {
				TypeInfoProvider.getClass(columnTypeName);
			} catch (Throwable t) {
				throw new ValidationError("Invalid column type name: '" + columnTypeName + "'");
			}
		}

	}

	public static class GenericResult {
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
