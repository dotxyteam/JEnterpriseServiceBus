package com.otk.jesb.operation.builtin;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.otk.jesb.Structure;
import com.otk.jesb.Structure.ClassicStructure;
import com.otk.jesb.Structure.SimpleElement;
import com.otk.jesb.Structure.StructuredElement;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.meta.TypeInfoProvider;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.resource.builtin.JDBCConnection;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.solution.Step;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.UpToDate;
import com.otk.jesb.util.UpToDate.VersionAccessException;

import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.method.InvocationData;
import xy.reflect.ui.info.parameter.IParameterInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.iterable.IListTypeInfo;

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
		ResultSetMetaData metaData = resultSet.getMetaData();
		if (customResultClass != null) {
			Constructor<?> customResultConstructor = customResultClass.getConstructors()[0];
			Class<?> customResultRowListClass = customResultConstructor.getParameterTypes()[0];
			IListTypeInfo customResultRowListTypeInfo = (IListTypeInfo) TypeInfoProvider
					.getTypeInfo(customResultRowListClass);
			ITypeInfo customResultRowTypeInfo = customResultRowListTypeInfo.getItemType();
			IMethodInfo customResultRowConstructorInfo = customResultRowTypeInfo.getConstructors().get(0);
			List<IParameterInfo> customResultRowConstructorParameterInfos = customResultRowConstructorInfo
					.getParameters();
			if (metaData.getColumnCount() != customResultRowConstructorParameterInfos.size()) {
				throw new ValidationError("Unexpected result row column count: " + metaData.getColumnCount()
						+ ". Expected " + customResultRowConstructorParameterInfos.size() + " column(s).");
			}
			List<Object> customResultRowStandardList = new ArrayList<Object>();
			List<ColumnDefinition> resultColumnDefinitions = retrieveResultColumnDefinitions(metaData);
			while (resultSet.next()) {
				Object[] parameterValues = new Object[metaData.getColumnCount()];
				for (int iColumn = 1; iColumn < metaData.getColumnCount(); iColumn++) {
					IParameterInfo parameterInfo = customResultRowConstructorParameterInfos.get(iColumn - 1);
					ColumnDefinition resultColumnDefinition = resultColumnDefinitions.get(iColumn - 1);
					if (!parameterInfo.getName().matches(resultColumnDefinition.getColumnName())) {
						throw new ValidationError(
								"Unexpected result row column name: '" + metaData.getColumnName(iColumn) + "' at the "
										+ iColumn + "th position. Expected '" + parameterInfo.getName() + "'");
					}
					parameterValues[iColumn - 1] = resultSet.getObject(iColumn);
				}
				InvocationData invocationData = new InvocationData(null, customResultRowConstructorInfo,
						parameterValues);
				Object row = customResultRowConstructorInfo.invoke(null, invocationData);
				customResultRowStandardList.add(row);
			}
			Object customResultRowList = customResultRowListTypeInfo.fromArray(customResultRowStandardList.toArray());
			return customResultClass.getConstructors()[0].newInstance(customResultRowList);
		} else {
			List<GenericResultRow> rows = new ArrayList<JDBCQuery.GenericResultRow>();
			while (resultSet.next()) {
				GenericResultRow row = new GenericResultRow();
				for (int iColumn = 1; iColumn < metaData.getColumnCount(); iColumn++) {
					row.cellValues.put(metaData.getColumnName(iColumn), resultSet.getObject(iColumn));
				}
				rows.add(row);
			}
			return new GenericResult(rows.toArray(new GenericResultRow[rows.size()]));
		}
	}

	private static List<ColumnDefinition> retrieveResultColumnDefinitions(ResultSetMetaData metaData)
			throws SQLException {
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

		private UpToDate<Class<?>> upToDateCustomResultClass = new UpToDateCustomResultClass();

		private Class<?> createCustomResultClass() {
			if (resultColumnDefinitions == null) {
				return null;
			}
			String resultClassName = JDBCQuery.class.getName() + "Result" + MiscUtils.toDigitalUniqueIdentifier(this);
			try {
				return MiscUtils.IN_MEMORY_COMPILER.compile(resultClassName,
						createCustomResultStructure().generateJavaTypeSourceCode(resultClassName));
			} catch (CompilationError e) {
				throw new UnexpectedError(e);
			}
		}

		private Structure createCustomResultStructure() {
			ClassicStructure resultStructure = new ClassicStructure();
			{
				StructuredElement rowsElement = new StructuredElement();
				resultStructure.getElements().add(rowsElement);
				rowsElement.setName("rows");
				rowsElement.setMultiple(true);
				rowsElement.setStructure(createResultRowStructure());
			}
			return resultStructure;
		}

		private Structure createResultRowStructure() {
			ClassicStructure rowStructure = new ClassicStructure();
			{
				for (int i = 0; i < resultColumnDefinitions.size(); i++) {
					ColumnDefinition columnDefinition = resultColumnDefinitions.get(i);
					SimpleElement columnElement = new SimpleElement();
					rowStructure.getElements().add(columnElement);
					columnElement.setName(columnDefinition.getColumnName());
					columnElement.setTypeName(columnDefinition.getColumnTypeName());
				}
			}
			return rowStructure;
		}

		public List<ColumnDefinition> getResultColumnDefinitions() {
			return resultColumnDefinitions;
		}

		public void setResultColumnDefinitions(List<ColumnDefinition> resultColumnDefinitions) {
			this.resultColumnDefinitions = resultColumnDefinitions;
		}

		public void retrieveResultColumnDefinitions() throws SQLException, ClassNotFoundException {
			JDBCConnection connection = getConnection();
			Connection conn = connection.build();
			PreparedStatement preparedStatement = conn.prepareStatement(getStatementVariant().getValue());
			ResultSetMetaData metaData = preparedStatement.getMetaData();
			this.resultColumnDefinitions = JDBCQuery.retrieveResultColumnDefinitions(metaData);
		}

		public void clearResultColumnDefinitions() throws SQLException {
			this.resultColumnDefinitions = null;
		}

		@Override
		public JDBCQuery build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			JDBCQuery result = new JDBCQuery(getConnection(), upToDateCustomResultClass.get());
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
		public final GenericResultRow[] rows;

		public GenericResult(GenericResultRow[] rows) {
			this.rows = rows;
		}

	}

	public static class GenericResultRow {

		public final Map<String, Object> cellValues = new HashMap<String, Object>();

		public List<String> getColumnNames() {
			return new ArrayList<String>(cellValues.keySet());
		}

		@Override
		public String toString() {
			return cellValues.toString();
		}

	}

}
