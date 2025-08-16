package com.otk.jesb.operation.builtin;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import com.otk.jesb.PotentialError;
import com.otk.jesb.Reference;
import com.otk.jesb.Session;
import com.otk.jesb.Structure;
import com.otk.jesb.Structure.ClassicStructure;
import com.otk.jesb.Structure.SimpleElement;
import com.otk.jesb.ValidationError;
import com.otk.jesb.Variant;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.resource.builtin.JDBCConnection;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.solution.Step;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.Pair;
import com.otk.jesb.util.UpToDate;
import com.otk.jesb.util.UpToDate.VersionAccessException;

import xy.reflect.ui.info.ResourcePath;

public class JDBCQuery extends JDBCOperation {

	private Class<?> customResultClass;

	public JDBCQuery(Session session, JDBCConnection connection, Class<?> customResultClass) {
		super(session, connection);
		this.customResultClass = customResultClass;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object execute() throws Exception {
		PreparedStatement preparedStatement = prepare();
		ResultSet resultSet = preparedStatement.executeQuery();
		List<ColumnDefinition> resultColumnDefinitions = retrieveResultColumnDefinitions(preparedStatement);
		if (customResultClass != null) {
			Class<?> customResultRowClass = customResultClass.getComponentType();
			Constructor<?> customResultRowConstructor = customResultRowClass.getConstructors()[0];
			Parameter[] customResultRowConstructorParameters = customResultRowConstructor.getParameters();
			if (resultColumnDefinitions.size() != customResultRowConstructorParameters.length) {
				throw new ValidationError("Unexpected result row column count: " + resultColumnDefinitions.size()
						+ ". Expected " + customResultRowConstructorParameters.length + " column(s).");
			}
			List<Object> customResultRowStandardList = new ArrayList<Object>();
			while (resultSet.next()) {
				Object[] parameterValues = new Object[resultColumnDefinitions.size()];
				for (int iColumn = 1; iColumn <= resultColumnDefinitions.size(); iColumn++) {
					Parameter parameter = customResultRowConstructorParameters[iColumn - 1];
					ColumnDefinition resultColumnDefinition = resultColumnDefinitions.get(iColumn - 1);
					if (!parameter.getName().matches(resultColumnDefinition.getColumnName())) {
						throw new ValidationError("Unexpected result row column name: '"
								+ resultColumnDefinition.getColumnName() + "' at the " + iColumn
								+ "th position. Expected '" + parameter.getName() + "'");
					}
					parameterValues[iColumn - 1] = resultSet.getObject(iColumn);
				}
				Object row = customResultRowConstructor.newInstance(parameterValues);
				customResultRowStandardList.add(row);
			}
			Object customResult = Array.newInstance(customResultRowClass, customResultRowStandardList.size());
			for (int i = 0; i < customResultRowStandardList.size(); i++) {
				Array.set(customResult, i, customResultRowStandardList.get(i));
			}
			return customResult;
		} else {
			List<ColumnDefinition> columns = retrieveResultColumnDefinitions(preparedStatement);
			List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
			while (resultSet.next()) {
				Map<String, Object> row = new HashMap<String, Object>();
				for (int iColumn = 1; iColumn <= resultColumnDefinitions.size(); iColumn++) {
					row.put(resultColumnDefinitions.get(iColumn - 1).getColumnName(), resultSet.getObject(iColumn));
				}
				rows.add(row);
			}
			return new GenericResult(columns.toArray(new String[columns.size()]), rows.toArray(new Map[rows.size()]));
		}
	}

	private static List<ColumnDefinition> retrieveResultColumnDefinitions(PreparedStatement preparedStatement)
			throws SQLException {
		ResultSetMetaData metaData = preparedStatement.getMetaData();
		if (metaData == null) {
			throw new SQLException("No SQL statement meta data found");
		}
		List<ColumnDefinition> result = new ArrayList<ColumnDefinition>();
		for (int i = 0; i < metaData.getColumnCount(); i++) {
			String columnClassName = metaData.getColumnClassName(i + 1);
			String columnName = metaData.getColumnLabel(i + 1);
			while (result.stream().map(ColumnDefinition::getColumnName).anyMatch(Predicate.isEqual(columnName))) {
				columnName = MiscUtils.nextNumbreredName(columnName);
			}
			result.add(new ColumnDefinition(columnName, columnClassName));
		}
		return result;
	}

	public static class Metadata implements OperationMetadata<JDBCQuery> {

		@Override
		public String getOperationTypeName() {
			return "JDBC Query";
		}

		@Override
		public String getCategoryName() {
			return "JDBC";
		}

		@Override
		public Class<? extends OperationBuilder<JDBCQuery>> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(JDBCQuery.class.getName().replace(".", "/") + ".png"));
		}

	}

	public static class Builder extends JDBCOperation.Builder<JDBCQuery> {

		private List<ColumnDefinition> resultColumnDefinitions;
		private boolean resultColumnDefinitionAutomatic = true;

		private UpToDate<Class<?>> upToDateCustomResultClass = new UpToDateCustomResultClass();
		private UpToDateResultColumnDefinitions upToDateResultColumnDefinitions = new UpToDateResultColumnDefinitions();

		public boolean isResultColumnDefinitionAutomatic() {
			return resultColumnDefinitionAutomatic;
		}

		public void setResultColumnDefinitionAutomatic(boolean resultColumnDefinitionAutomatic) {
			this.resultColumnDefinitionAutomatic = resultColumnDefinitionAutomatic;
		}

		private List<ColumnDefinition> computeResultColumnDefinitions() {
			if (resultColumnDefinitionAutomatic) {
				try {
					return upToDateResultColumnDefinitions.get();
				} catch (VersionAccessException e) {
					throw new PotentialError(e);
				}
			} else {
				return resultColumnDefinitions;
			}
		}

		public List<ColumnDefinition> getResultColumnDefinitions() {
			return resultColumnDefinitions;
		}

		public void setResultColumnDefinitions(List<ColumnDefinition> resultColumnDefinitions) {
			this.resultColumnDefinitions = resultColumnDefinitions;
		}

		@Override
		public void setStatementVariant(Variant<String> statementVariant) {
			super.setStatementVariant(statementVariant);
		}

		@Override
		public void setConnectionReference(Reference<JDBCConnection> connectionReference) {
			super.setConnectionReference(connectionReference);
		}

		private Class<?> createCustomResultClass() {
			Structure resultRowStructure = createResultRowStructure();
			if (resultRowStructure == null) {
				return null;
			}
			String resultRowClassName = JDBCQuery.class.getName() + "ResultRow"
					+ MiscUtils.toDigitalUniqueIdentifier(this);
			Class<?> resultRowClass;
			try {
				resultRowClass = MiscUtils.IN_MEMORY_COMPILER.compile(resultRowClassName,
						resultRowStructure.generateJavaTypeSourceCode(resultRowClassName));
			} catch (CompilationError e) {
				throw new PotentialError(e);
			}
			return MiscUtils.getArrayType(resultRowClass);
		}

		private Structure createResultRowStructure() {
			List<ColumnDefinition> resultColumnDefinitions = computeResultColumnDefinitions();
			if (resultColumnDefinitions == null) {
				return null;
			}
			ClassicStructure rowStructure = new ClassicStructure();
			{
				for (int i = 0; i < resultColumnDefinitions.size(); i++) {
					ColumnDefinition columnDefinition = resultColumnDefinitions.get(i);
					SimpleElement columnElement = new SimpleElement();
					rowStructure.getElements().add(columnElement);
					columnElement.setName(columnDefinition.getColumnName());
					columnElement.setTypeNameOrAlias(columnDefinition.getColumnTypeName());
				}
			}
			return rowStructure;
		}

		public void retrieveResultColumnDefinitions() throws SQLException, ClassNotFoundException {
			try {
				this.resultColumnDefinitions = upToDateResultColumnDefinitions.get();
			} catch (VersionAccessException e) {
				throw new PotentialError(e);
			}
		}

		public void clearResultColumnDefinitions() throws SQLException {
			this.resultColumnDefinitions = null;
		}

		@Override
		public JDBCQuery build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			JDBCQuery result = new JDBCQuery(context.getSession(), getConnection(), upToDateCustomResultClass.get());
			result.setStatement(getStatementVariant().getValue());
			result.setParameterValues(buildParameterValues(context));
			return result;
		}

		@Override
		public Class<?> getOperationResultClass(Plan currentPlan, Step currentStep) {
			Class<?> customResultClass;
			try {
				customResultClass = upToDateCustomResultClass.get();
			} catch (VersionAccessException e) {
				throw new PotentialError(e);
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
				List<ColumnDefinition> resultColumnDefinitions;
				try {
					resultColumnDefinitions = computeResultColumnDefinitions();
				} catch (Throwable t) {
					throw new PotentialError("Failed to get result column definitions", t);
				}
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
				return computeResultColumnDefinitions();
			}

			@Override
			protected Class<?> obtainLatest(Object versionIdentifier) {
				return createCustomResultClass();
			}
		}

		private class UpToDateResultColumnDefinitions extends UpToDate<List<ColumnDefinition>> {

			@Override
			protected Object retrieveLastVersionIdentifier() {
				JDBCConnection connection = getConnection();
				return new Pair<String, String>((connection != null) ? MiscUtils.serialize(connection) : null,
						getStatementVariant().getValue());
			}

			@Override
			protected List<ColumnDefinition> obtainLatest(Object versionIdentifier) throws VersionAccessException {
				try {
					JDBCConnection connection = getConnection();
					if (connection == null) {
						return null;
					}
					return connection.during(new Function<Connection, List<ColumnDefinition>>() {
						@Override
						public List<ColumnDefinition> apply(Connection connectionInstance) {
							String statement = getStatementVariant().getValue();
							if ((statement == null) || statement.trim().isEmpty()) {
								return null;
							}
							try {
								PreparedStatement preparedStatement = connectionInstance.prepareStatement(statement);
								return JDBCQuery.retrieveResultColumnDefinitions(preparedStatement);
							} catch (SQLException e) {
								throw new PotentialError(e);
							}
						}
					});
				} catch (Exception e) {
					throw new VersionAccessException(e);
				}
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
				MiscUtils.getJESBClass(columnTypeName);
			} catch (Throwable t) {
				throw new ValidationError("Invalid column type name: '" + columnTypeName + "'");
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((columnName == null) ? 0 : columnName.hashCode());
			result = prime * result + ((columnTypeName == null) ? 0 : columnTypeName.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ColumnDefinition other = (ColumnDefinition) obj;
			if (columnName == null) {
				if (other.columnName != null)
					return false;
			} else if (!columnName.equals(other.columnName))
				return false;
			if (columnTypeName == null) {
				if (other.columnTypeName != null)
					return false;
			} else if (!columnTypeName.equals(other.columnTypeName))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "ColumnDefinition [columnName=" + columnName + ", columnTypeName=" + columnTypeName + "]";
		}

	}

	public static class GenericResult {
		public final Map<String, Object>[] rows;
		public final String[] columnNames;

		public GenericResult(String[] columnNames, Map<String, Object>[] rows) {
			this.columnNames = columnNames;
			this.rows = rows;
		}

	}

}
