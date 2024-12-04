package com.otk.jesb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.otk.jesb.Plan.ExecutionContext;

import xy.reflect.ui.info.field.FieldInfoProxy;
import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.method.InvocationData;
import xy.reflect.ui.info.method.MethodInfoProxy;
import xy.reflect.ui.info.type.BasicTypeInfoProxy;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.util.ReflectionUIUtils;

public class JDBCQueryActivity implements Activity {

	private JDBCConnectionResource connection;
	private String statement;
	private List<Object> parameterValues;

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

	public List<Object> getParameterValues() {
		return parameterValues;
	}

	public void setParameterValues(List<Object> parameterValues) {
		this.parameterValues = parameterValues;
	}

	@Override
	public Result execute() throws Exception {
		Connection conn = DriverManager.getConnection(connection.getUrl(), connection.getUserName(),
				connection.getPassword());
		PreparedStatement preparedStatement = conn.prepareStatement(statement);
		for (int i = 0; i < parameterValues.size(); i++) {
			preparedStatement.setObject(i + 1, parameterValues.get(i));
		}
		ResultSet resultSet = preparedStatement.executeQuery();
		ResultSetMetaData metaData = resultSet.getMetaData();
		Result result = new Result();
		while (resultSet.next()) {
			ResultRow row = new ResultRow();
			for (int iColumn = 1; iColumn < metaData.getColumnCount(); iColumn++) {
				row.cellValues.put(metaData.getColumnName(iColumn), resultSet.getObject(iColumn));
			}
			result.rows.add(row);
		}
		return result;
	}

	public static class Builder implements ActivityBuilder {

		private String uniqueIdentifier = Utils.getUniqueIdentifier();
		private String connectionPath;
		private String statement;
		private List<ParameterDefinition> parameterDefinitions = new ArrayList<ParameterDefinition>();
		private ObjectSpecification parameterValuesSpecification = new ObjectSpecification();
		private ITypeInfo parameterValuesTypeInfo;

		public Builder() {
			parameterValuesTypeInfo = createParameterValuesTypeInfo();
			parameterValuesSpecification.setTypeName(parameterValuesTypeInfo.getName());
			ObjectSpecification.TypeInfoProvider.register(parameterValuesTypeInfo, this);
		}

		private ITypeInfo createParameterValuesTypeInfo() {
			return new BasicTypeInfoProxy(ITypeInfo.NULL_BASIC_TYPE_INFO) {

				@Override
				public String getName() {
					return JDBCQueryActivity.class.getName() + "ParameterValues" + uniqueIdentifier;
				}

				@Override
				public List<IMethodInfo> getConstructors() {
					return Collections.singletonList(new MethodInfoProxy(IMethodInfo.NULL_METHOD_INFO) {

						@Override
						public String getName() {
							return "";
						}

						@Override
						public String getSignature() {
							return ReflectionUIUtils.buildMethodSignature(this);
						}

						@Override
						public Object invoke(Object object, InvocationData invocationData) {
							return new ArrayList<Object>();
						}

					});
				}

				@Override
				public List<IFieldInfo> getFields() {
					List<IFieldInfo> result = new ArrayList<IFieldInfo>();
					for (int i = 0; i < parameterDefinitions.size(); i++) {
						final int finalI = i;
						ParameterDefinition parameterDefinition = parameterDefinitions.get(i);
						result.add(new FieldInfoProxy(IFieldInfo.NULL_FIELD_INFO) {

							@SuppressWarnings("unchecked")
							@Override
							public Object getValue(Object object) {
								return ((List<Object>) object).get(finalI);
							}

							@SuppressWarnings("unchecked")
							@Override
							public void setValue(Object object, Object value) {
								((List<Object>) object).set(finalI, value);
							}

							@Override
							public boolean isGetOnly() {
								return false;
							}

							@Override
							public ITypeInfo getType() {
								return ReflectionUIUtils
										.getStandardTypeInfo(parameterDefinition.getParameterTypeName());
							}

							@Override
							public String getName() {
								return parameterDefinition.getParameterName();
							}

							@Override
							public String getCaption() {
								return parameterDefinition.getParameterName();
							}

						});
					}
					return result;
				}

			};
		}

		public String getUniqueIdentifier() {
			return uniqueIdentifier;
		}

		public void setUniqueIdentifier(String uniqueIdentifier) {
			this.uniqueIdentifier = uniqueIdentifier;
			parameterValuesSpecification.setTypeName(parameterValuesTypeInfo.getName());			
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
			if(parameterValuesSpecification == null) {
				throw new AssertionError();
			}
			this.parameterValuesSpecification = parameterValuesSpecification;
		}

		@Override
		public Activity build(ExecutionContext context) throws Exception {
			JDBCQueryActivity result = new JDBCQueryActivity();
			result.setConnection(Workspace.JDBC_CONNECTIONS.get(Integer.valueOf(connectionPath)));
			result.setStatement(statement);
			@SuppressWarnings("unchecked")
			List<Object> parameterValues = (List<Object>) parameterValuesSpecification.build(context);
			result.setParameterValues(parameterValues);
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

	public static class Result implements ActivityResult {
		private List<ResultRow> rows = new ArrayList<JDBCQueryActivity.ResultRow>();

		public List<ResultRow> getRows() {
			return rows;
		}

	}

	public static class ResultRow {

		private Map<String, Object> cellValues = new HashMap<String, Object>();

		public List<String> getColumnNames() {
			return new ArrayList<String>(cellValues.keySet());
		}

		public Object getCellValue(String columnName) {
			return cellValues.get(columnName);
		}

	}

}
