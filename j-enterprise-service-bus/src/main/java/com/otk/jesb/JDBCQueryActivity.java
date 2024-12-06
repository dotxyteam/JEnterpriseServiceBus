package com.otk.jesb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.otk.jesb.Plan.ExecutionContext;

import xy.reflect.ui.ReflectionUI;
import xy.reflect.ui.info.field.FieldInfoProxy;
import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.method.InvocationData;
import xy.reflect.ui.info.method.MethodInfoProxy;
import xy.reflect.ui.info.parameter.IParameterInfo;
import xy.reflect.ui.info.parameter.ParameterInfoProxy;
import xy.reflect.ui.info.type.BasicTypeInfoProxy;
import xy.reflect.ui.info.type.DefaultTypeInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.iterable.StandardCollectionTypeInfo;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;
import xy.reflect.ui.util.ReflectionUIUtils;

public class JDBCQueryActivity implements Activity {

	private JDBCConnectionResource connection;
	private String statement;
	private List<Object> parameterValues;
	private String builderUniqueIdentifier;
	private List<ColumnDefinition> resultColumnDefinitions;

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

	public String getBuilderUniqueIdentifier() {
		return builderUniqueIdentifier;
	}

	public void setBuilderUniqueIdentifier(String builderUniqueIdentifier) {
		this.builderUniqueIdentifier = builderUniqueIdentifier;
	}

	public List<ColumnDefinition> getResultColumnDefinitions() {
		return resultColumnDefinitions;
	}

	public void setResultColumnDefinitions(List<ColumnDefinition> resultColumnDefinitions) {
		this.resultColumnDefinitions = resultColumnDefinitions;
	}

	@Override
	public Result execute() throws Exception {
		Connection conn = DriverManager.getConnection(connection.getUrl(), connection.getUserName(),
				connection.getPassword());
		PreparedStatement preparedStatement = conn.prepareStatement(statement);
		if(preparedStatement.getParameterMetaData().getParameterCount() != parameterValues.size()) {
			throw new AssertionError();
		}
		for (int i = 0; i < parameterValues.size(); i++) {
			preparedStatement.setObject(i + 1, parameterValues.get(i));
		}
		ResultSet resultSet = preparedStatement.executeQuery();
		Result result = new Result(resultSet, resultColumnDefinitions, builderUniqueIdentifier);
		return result;
	}

	public static class Builder implements ActivityBuilder {

		private String uniqueIdentifier = Utils.getDigitalUniqueIdentifier();
		private String connectionPath;
		private String statement;
		private List<ParameterDefinition> parameterDefinitions = new ArrayList<ParameterDefinition>();
		private ObjectSpecification parameterValuesSpecification = new ObjectSpecification();
		private ITypeInfo parameterValuesTypeInfo;
		private List<ColumnDefinition> resultColumnDefinitions;

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
						public List<IParameterInfo> getParameters() {
							List<IParameterInfo> result = new ArrayList<IParameterInfo>();
							for (int i = 0; i < parameterDefinitions.size(); i++) {
								ParameterDefinition parameterDefinition = parameterDefinitions.get(i);
								result.add(new ParameterInfoProxy(IParameterInfo.NULL_PARAMETER_INFO) {

									@Override
									public String getName() {
										return parameterDefinition.getParameterName();
									}

									@Override
									public String getCaption() {
										return parameterDefinition.getParameterName();
									}

									@Override
									public ITypeInfo getType() {
										return ReflectionUIUtils
												.buildTypeInfo(parameterDefinition.getParameterTypeName());
									}

									@Override
									public Object getDefaultValue(Object object) {
										ITypeInfo type = getType();
										if (ReflectionUIUtils.canCreateDefaultInstance(type, true)) {
											return ReflectionUIUtils.createDefaultInstance(type, true);
										} else {
											return null;
										}
									}

								});
							}
							return result;
						}

						@Override
						public Object invoke(Object object, InvocationData invocationData) {
							List<Object> result = new ArrayList<Object>();
							for (int i = 0; i < getParameters().size(); i++) {
								result.add(invocationData.getParameterValue(i));
							}
							return result;
						}

					});
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
						.add(new ColumnDefinition(metaData.getColumnName(i + 1), metaData.getColumnClassName(i + 1)));
			}
		}

		@Override
		public Activity build(ExecutionContext context) throws Exception {
			JDBCQueryActivity result = new JDBCQueryActivity();
			result.setConnection(Workspace.JDBC_CONNECTIONS.get(Integer.valueOf(connectionPath)));
			result.setStatement(statement);
			@SuppressWarnings("unchecked")
			List<Object> parameterValues = (List<Object>) parameterValuesSpecification.build(context);
			result.setParameterValues(parameterValues);
			result.setBuilderUniqueIdentifier(uniqueIdentifier);
			result.setResultColumnDefinitions(resultColumnDefinitions);
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

	public static class Result extends VirtualClassInstance implements ActivityResult {
		private List<ResultRow> rows = new ArrayList<ResultRow>();
		private String builderUniqueIdentifier;
		private List<ColumnDefinition> resultColumnDefinitions;

		public Result(ResultSet resultSet, List<ColumnDefinition> resultColumnDefinitions,
				String builderUniqueIdentifier) throws SQLException {
			this.resultColumnDefinitions = resultColumnDefinitions;
			this.builderUniqueIdentifier = builderUniqueIdentifier;
			ResultSetMetaData metaData = resultSet.getMetaData();
			while (resultSet.next()) {
				ResultRow row = new ResultRow();
				for (int iColumn = 1; iColumn < metaData.getColumnCount(); iColumn++) {
					row.cellValues.put(metaData.getColumnName(iColumn), resultSet.getObject(iColumn));
				}
				rows.add(row);
			}
		}

		@Override
		protected ITypeInfo getVirtualClassDescription() {
			return new BasicTypeInfoProxy(ITypeInfo.NULL_BASIC_TYPE_INFO) {

				@Override
				public String getName() {
					return JDBCQueryActivity.class.getName() + "Output" + builderUniqueIdentifier;
				}

				@Override
				public List<IFieldInfo> getFields() {
					return Collections.singletonList(new FieldInfoProxy(IFieldInfo.NULL_FIELD_INFO) {

						@Override
						public String getName() {
							return "rows";
						}

						@Override
						public String getCaption() {
							return "Rows";
						}

						@Override
						public Object getValue(Object object) {
							return rows;
						}

						@Override
						public ITypeInfo getType() {
							ITypeInfo itemType = new DefaultTypeInfo(
									new JavaTypeInfoSource(ReflectionUI.getDefault(), ResultRow.class, null));
							return new StandardCollectionTypeInfo(
									new JavaTypeInfoSource(ReflectionUI.getDefault(), List.class, null), itemType);
						}

					});
				}

			};
		}

	}

	public static class ResultRow {

		private Map<String, Object> cellValues = new HashMap<String, Object>();

		public List<String> getColumnNames() {
			return new ArrayList<String>(cellValues.keySet());
		}

		public Map<String, Object> getCellValues() {
			return cellValues;
		}

	}

}
