package com.otk.jesb.operation.builtin;

import java.beans.Transient;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ParameterMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Objects;
import com.otk.jesb.PotentialError;
import com.otk.jesb.Reference;
import com.otk.jesb.Session;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.Variant;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.resource.builtin.JDBCConnection;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.solution.Step;

import xy.reflect.ui.info.ResourcePath;

public class JDBCProcedureCall extends JDBCQuery {

	private ProcedureDescriptor procedure;

	public JDBCProcedureCall(Session session, JDBCConnection connection, ProcedureDescriptor procedure,
			Class<?> customResultClass) {
		super(session, connection, customResultClass);
		this.procedure = procedure;
	}

	public ProcedureDescriptor getProcedure() {
		return procedure;
	}

	@Override
	public Object execute() throws Exception {
		JDBCConnection connection = getConnection();
		Connection connectionInstance = connection.during(getSession());
		try (CallableStatement preparedStatement = connectionInstance
				.prepareCall(procedure.getCallQueryStringVariant().getValue())) {
			ParameterValues parametervalues = getParameterValues();
			int parameterIndex = 1;
			if (procedure.getReturnParameter() != null) {
				preparedStatement.registerOutParameter(parameterIndex++, procedure.getReturnParameter().getSqlType());
			}
			int valueIndex = 0;
			for (ProcedureParameterDescriptor parameter : procedure.getParameters()) {
				switch (parameter.getColumnKind()) {
				case DatabaseMetaData.procedureColumnIn: {
					preparedStatement.setObject(parameterIndex++,
							parametervalues.getParameterValueByIndex(valueIndex++));
					break;
				}
				case DatabaseMetaData.procedureColumnOut: {
					preparedStatement.registerOutParameter(parameterIndex++, parameter.getSqlType());
					break;
				}
				case DatabaseMetaData.procedureColumnInOut: {
					preparedStatement.setObject(parameterIndex, parametervalues.getParameterValueByIndex(valueIndex++));
					preparedStatement.registerOutParameter(parameterIndex++, parameter.getSqlType());
					break;
				}
				default: {
					preparedStatement.setObject(parameterIndex++, null);
					break;
				}
				}
			}
			preparedStatement.execute();
			List<Object> resultArguments = new ArrayList<Object>();
			parameterIndex = 1;
			if (procedure.getReturnParameter() != null) {
				resultArguments.add(preparedStatement.getObject(parameterIndex++));
			}
			for (ProcedureParameterDescriptor parameter : procedure.getParameters()) {
				switch (parameter.getColumnKind()) {
				case DatabaseMetaData.procedureColumnOut:
				case DatabaseMetaData.procedureColumnInOut: {
					resultArguments.add(preparedStatement.getObject(parameterIndex));
					break;
				}
				}
				parameterIndex++;
			}
			return resultClass.getConstructors()[0].newInstance(resultArguments.toArray());
		} catch (Exception e) {
			throw new PotentialError(e);
		}
	}

	public static class Builder implements OperationBuilder<JDBCProcedureCall> {

		private String searchCatalogName;
		private String searchSchemaName;
		private ProcedureDescriptor procedure;

		private JDBCQuery.Builder util = new Util();

		public Reference<JDBCConnection> getConnectionReference() {
			return util.getConnectionReference();
		}

		public void setConnectionReference(Reference<JDBCConnection> connectionReference) {
			util.setConnectionReference(connectionReference);
			JDBCConnection connection = util.getConnection();
			if (connection != null) {
				try {
					connection.during(connectionInstance -> {
						try {
							searchCatalogName = connectionInstance.getCatalog();
							searchSchemaName = connectionInstance.getSchema();
						} catch (Exception ignore) {
						}
						return null;
					});
				} catch (Exception ignore) {
				}
			}
		}

		public RootInstanceBuilder getParameterValuesBuilder() {
			return util.getParameterValuesBuilder();
		}

		public void setParameterValuesBuilder(RootInstanceBuilder parameterValuesBuilder) {
			util.setParameterValuesBuilder(parameterValuesBuilder);
		}

		@Transient
		public String getSearchCatalogName() {
			return searchCatalogName;
		}

		public void setSearchCatalogName(String searchCatalogName) {
			this.searchCatalogName = searchCatalogName;
		}

		@Transient
		public String getSearchSchemaName() {
			return searchSchemaName;
		}

		public void setSearchSchemaName(String searchSchemaName) {
			this.searchSchemaName = searchSchemaName;
		}

		public ProcedureDescriptor getProcedure() {
			return procedure;
		}

		public void setProcedure(ProcedureDescriptor procedure) {
			this.procedure = procedure;
		}

		public List<ProcedureDescriptor> getProcedureOptions() {
			List<ProcedureDescriptor> result = new ArrayList<JDBCProcedureCall.ProcedureDescriptor>();
			JDBCConnection connection = util.getConnection();
			if (connection != null) {
				try {
					connection.during(connectionInstance -> {
						try {
							result.addAll(ProcedureDescriptor
									.list(connectionInstance, searchCatalogName, searchSchemaName, null).stream()
									.filter(procedure -> {
										procedure.fix(connection);
										return true;
									}).collect(Collectors.toList()));
							return null;
						} catch (Exception e) {
							throw new PotentialError(e);
						}
					});
				} catch (Exception ignore) {
				}
			}
			return result;
		}

		@Override
		public JDBCProcedureCall build(ExecutionContext context, ExecutionInspector executionInspector)
				throws Exception {
			JDBCProcedureCall result = new JDBCProcedureCall(context.getSession(), util.getConnection(), procedure,
					util.upToDateResultClass.get());
			result.setParameterValues(util.buildParameterValues(context));
			return result;
		}

		@Override
		public Class<?> getOperationResultClass(Plan currentPlan, Step currentStep) {
			return util.getOperationResultClass(currentPlan, currentStep);
		}

		@Override
		public void validate(boolean recursively, Plan plan, Step step) throws ValidationError {
			if (util.getConnection() == null) {
				throw new ValidationError("Failed to resolve the connection reference");
			}
			if (procedure == null) {
				throw new ValidationError("Procedure not selected");
			}
			try {
				util.upToDateParameterValuesClass.get();
			} catch (Throwable t) {
				throw new ValidationError("Failed to get compiled parameter definitions", t);
			}

			try {
				util.upToDateResultClass.get();
			} catch (Throwable t) {
				throw new ValidationError("Failed to get compiled result column definitions", t);
			}
			try {
				util.getConnection().during(connectionInstance -> {
					try {
						procedure.validate(connectionInstance);
					} catch (ValidationError e) {
						throw new PotentialError(e);
					}
					return null;
				});
			} catch (Exception e) {
				throw new ValidationError("Failed to validate the procedure descriptor", e);
			}
			if (recursively) {
				RootInstanceBuilder parameterValuesBuilder = util.getParameterValuesBuilder();
				if (parameterValuesBuilder != null) {
					try {
						parameterValuesBuilder.getFacade().validate(recursively,
								plan.getValidationContext(step).getVariableDeclarations());
					} catch (ValidationError e) {
						throw new ValidationError("Failed to validate the parameter values builder", e);
					}
				}
			}
		}

		private class Util extends JDBCQuery.Builder {

			@Override
			protected Class<?> createResultClass() {
				Class<?> superResult = super.createResultClass();
				if (superResult == null) {
					return null;
				}
				return superResult.getComponentType();
			}

			@Override
			protected List<ColumnDefinition> computeResultColumnDefinitions() {
				if (procedure == null) {
					return null;
				}
				List<ColumnDefinition> result = new ArrayList<ColumnDefinition>();
				for (ProcedureParameterDescriptor parameter : procedure.getParameters()) {
					if ((parameter.getColumnKind() != DatabaseMetaData.procedureColumnOut)
							&& (parameter.getColumnKind() != DatabaseMetaData.procedureColumnInOut)) {
						continue;
					}
					ColumnDefinition columnDefinition = new ColumnDefinition(parameter.getName(),
							parameter.getTypeName());
					result.add(columnDefinition);
				}
				ProcedureParameterDescriptor returnParameter = procedure.getReturnParameter();
				if (returnParameter != null) {
					ColumnDefinition resultColumnDefinition = new ColumnDefinition(returnParameter.getName(),
							returnParameter.getTypeName());
					result.add(resultColumnDefinition);
				}
				MiscUtils.makeNumberedNamesUnique(result, ColumnDefinition::getColumnName,
						ColumnDefinition::setColumnName);
				return result;
			}

			@Override
			public List<ColumnDefinition> getResultColumnDefinitions() {
				throw new UnsupportedOperationException();
			}

			@Override
			protected List<ParameterDefinition> computeParameterDefinitions() {
				if (procedure == null) {
					return null;
				}
				List<ParameterDefinition> result = new ArrayList<ParameterDefinition>();
				for (ProcedureParameterDescriptor parameter : procedure.getParameters()) {
					if ((parameter.getColumnKind() != DatabaseMetaData.procedureColumnIn)
							&& (parameter.getColumnKind() != DatabaseMetaData.procedureColumnInOut)) {
						continue;
					}
					ParameterDefinition parameterDefinition = new ParameterDefinition();
					parameterDefinition.setParameterName(parameter.getName());
					parameterDefinition.setParameterTypeName(parameter.getTypeName());
					result.add(parameterDefinition);
				}
				MiscUtils.makeNumberedNamesUnique(result, ParameterDefinition::getParameterName,
						ParameterDefinition::setParameterName);
				return result;
			}

			@Override
			public List<ParameterDefinition> getParameterDefinitions() {
				throw new UnsupportedOperationException();
			}

		};
	}

	public static class ProcedureDescriptor {
		private static final String RETURN_VALUE_NAME = "<RETURN_VALUE>";

		private String procedureName;
		private ProcedureParameterDescriptor returnParameter;
		private List<ProcedureParameterDescriptor> parameters = new ArrayList<>();
		private Variant<String> callQueryStringVariant = new Variant<String>(String.class);

		public static List<ProcedureDescriptor> list(Connection connectionInstance, String catalogName,
				String schemaPattern, String procedureNamePattern) throws Exception {
			List<ProcedureDescriptor> result = new ArrayList<>();
			DatabaseMetaData meta = connectionInstance.getMetaData();
			try (ResultSet procedureResultSet = meta.getProcedures(catalogName, schemaPattern, procedureNamePattern)) {
				while (procedureResultSet.next()) {
					ProcedureDescriptor procedure = new ProcedureDescriptor();
					String foundCatalogName = procedureResultSet.getString("PROCEDURE_CAT");
					String foundSchemaName = procedureResultSet.getString("PROCEDURE_SCHEM");
					String foundProcedureName = procedureResultSet.getString("PROCEDURE_NAME");
					List<ProcedureParameterDescriptor> parameters = new ArrayList<ProcedureParameterDescriptor>();
					try (ResultSet columnResultSet = meta.getProcedureColumns(foundCatalogName, foundSchemaName,
							foundProcedureName, "%")) {
						while (columnResultSet.next()) {
							ProcedureParameterDescriptor parameter = new ProcedureParameterDescriptor();
							parameter.setColumnKind(columnResultSet.getInt("COLUMN_TYPE"));
							parameter.setSqlType(columnResultSet.getInt("DATA_TYPE"));
							if (parameter.getColumnKind() == DatabaseMetaData.procedureColumnReturn) {
								parameter.setName(RETURN_VALUE_NAME);
								procedure.setReturnParameter(parameter);
							} else {
								parameter.setName(columnResultSet.getString("COLUMN_NAME"));
								parameters.add(parameter);
							}
						}
					}
					procedure.setProcedureName(foundProcedureName);
					procedure.setParameters(parameters);
					procedure.getCallQueryStringVariant().setConstantValue(buildCallQueryString(
							Objects.equal(foundCatalogName, connectionInstance.getCatalog()) ? null : foundCatalogName,
							Objects.equal(foundSchemaName, connectionInstance.getSchema()) ? null : foundSchemaName,
							foundProcedureName, parameters, procedure.isOfFunctionKind(), connectionInstance));
					try {
						procedure.updateParameterTypeNames(connectionInstance);
					} catch (SQLException ignore) {
					}
					result.add(procedure);
				}
			}
			return result;
		}

		public void fix(JDBCConnection connection) {
			if (connection != null) {
				String driverClassName = connection.getDriverClassNameVariant().getValue();
				if (driverClassName != null) {
					if (driverClassName.contains("microsoft.sqlserver")) {
						if (!callQueryStringVariant.isVariable()) {
							if (callQueryStringVariant.getValue() != null) {
								String newProcedureName = procedureName.replaceAll(";\\d+$", "");
								if (!newProcedureName.equals(procedureName)) {
									callQueryStringVariant.setConstantValue(
											callQueryStringVariant.getValue().replace(procedureName, newProcedureName));
								}
							}
						}
					}
				}
			}
		}

		public String getProcedureName() {
			return procedureName;
		}

		public void setProcedureName(String procedureName) {
			this.procedureName = procedureName;
		}

		public ProcedureParameterDescriptor getReturnParameter() {
			return returnParameter;
		}

		public void setReturnParameter(ProcedureParameterDescriptor returnParameter) {
			this.returnParameter = returnParameter;
		}

		public boolean isOfFunctionKind() {
			return returnParameter != null;
		}

		public List<ProcedureParameterDescriptor> getParameters() {
			return parameters;
		}

		public void setParameters(List<ProcedureParameterDescriptor> parameters) {
			this.parameters = parameters;
		}

		public Variant<String> getCallQueryStringVariant() {
			return callQueryStringVariant;
		}

		public void setCallQueryStringVariant(Variant<String> callQueryStringVariant) {
			this.callQueryStringVariant = callQueryStringVariant;
		}

		private static String buildCallQueryString(String catalogName, String schemaName, String procedureName,
				List<ProcedureParameterDescriptor> parameters, boolean ofFunctionKind, Connection connectionInstance) {
			String placeHolders = String.join(",", Collections.nCopies(parameters.size(), "?"));
			String procedureQualifiedName;
			try {
				procedureQualifiedName = getProcedureQualifiedName(catalogName, schemaName, procedureName,
						connectionInstance.getMetaData().getIdentifierQuoteString());
			} catch (SQLException e) {
				throw new UnexpectedError(e);
			}
			if (ofFunctionKind) {
				return "{?= call " + procedureQualifiedName + "(" + placeHolders + ")}";
			} else {
				return "{call " + procedureQualifiedName + "(" + placeHolders + ")}";
			}
		}

		private static String getProcedureQualifiedName(String catalogName, String schemaName, String procedureName,
				String quote) {
			if (quote == null) {
				quote = "";
			}
			quote = quote.trim();
			String result = quote + procedureName + quote;
			if (catalogName != null) {
				result = quote + catalogName + quote + "." + result;
			}
			if (schemaName != null) {
				result = quote + schemaName + quote + "." + result;
			}
			return result;
		}

		public void updateParameterTypeNames(Connection connectionInstance) throws SQLException {
			ParameterMetaData parameterMetaData = null;
			try {
				CallableStatement preparedStatement = connectionInstance
						.prepareCall(getCallQueryStringVariant().getValue());
				parameterMetaData = preparedStatement.getParameterMetaData();
			} finally {
				if (isOfFunctionKind()) {
					getReturnParameter()
							.setTypeName((parameterMetaData != null) ? parameterMetaData.getParameterClassName(1)
									: Object.class.getName());
				}
				for (int i = 0; i < parameters.size(); i++) {
					parameters.get(i)
							.setTypeName((parameterMetaData != null)
									? parameterMetaData.getParameterClassName((isOfFunctionKind() ? +1 : 0) + i + 1)
									: Object.class.getName());
				}
			}
		}

		public void validate(Connection connectionInstance) throws ValidationError {
			try {
				connectionInstance.prepareCall(getCallQueryStringVariant().getValue());
			} catch (SQLException e) {
				throw new ValidationError("Failed to validate the procedure call query string", e);
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((callQueryStringVariant == null) ? 0 : callQueryStringVariant.hashCode());
			result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
			result = prime * result + ((procedureName == null) ? 0 : procedureName.hashCode());
			result = prime * result + ((returnParameter == null) ? 0 : returnParameter.hashCode());
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
			ProcedureDescriptor other = (ProcedureDescriptor) obj;
			if (callQueryStringVariant == null) {
				if (other.callQueryStringVariant != null)
					return false;
			} else if (!callQueryStringVariant.equals(other.callQueryStringVariant))
				return false;
			if (parameters == null) {
				if (other.parameters != null)
					return false;
			} else if (!parameters.equals(other.parameters))
				return false;
			if (procedureName == null) {
				if (other.procedureName != null)
					return false;
			} else if (!procedureName.equals(other.procedureName))
				return false;
			if (returnParameter == null) {
				if (other.returnParameter != null)
					return false;
			} else if (!returnParameter.equals(other.returnParameter))
				return false;
			return true;
		}

		@Override
		public String toString() {
			String result = isOfFunctionKind() ? "Function" : "Procedure";
			result += " " + procedureName;
			result += "("
					+ parameters.stream().map(ProcedureParameterDescriptor::toString).collect(Collectors.joining(", "))
					+ ")";
			if (returnParameter != null) {
				result += ": " + returnParameter.getTypeName();
			}
			return result;
		}

	}

	public static class ProcedureParameterDescriptor {
		private String name;
		private int columnKind;
		private int sqlType;
		private String typeName;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getColumnKind() {
			return columnKind;
		}

		public void setColumnKind(int columnKind) {
			this.columnKind = columnKind;
		}

		public int getSqlType() {
			return sqlType;
		}

		public void setSqlType(int sqlType) {
			this.sqlType = sqlType;
		}

		public String getTypeName() {
			return typeName;
		}

		public void setTypeName(String typeName) {
			this.typeName = typeName;
		}

		public String getMode() {
			switch (columnKind) {
			case DatabaseMetaData.procedureColumnIn:
				return "IN";
			case DatabaseMetaData.procedureColumnOut:
				return "OUT";
			case DatabaseMetaData.procedureColumnInOut:
				return "INOUT";
			case DatabaseMetaData.procedureColumnReturn:
				return "RETURN";
			default:
				return "OTHER";
			}
		}

		public void setMode(String mode) {
			if ("IN".equals(mode)) {
				columnKind = DatabaseMetaData.procedureColumnIn;
			} else if ("OUT".equals(mode)) {
				columnKind = DatabaseMetaData.procedureColumnOut;
			} else if ("INOUT".equals(mode)) {
				columnKind = DatabaseMetaData.procedureColumnInOut;
			} else if ("RETURN".equals(mode)) {
				columnKind = DatabaseMetaData.procedureColumnReturn;
			} else {
				columnKind = DatabaseMetaData.procedureColumnUnknown;
			}
		}

		public List<String> getModeOptions() {
			return Arrays.asList("IN", "OUT", "INOUT", "RETURN", "OTHER");
		}

		public void validate() throws ValidationError {
			try {
				MiscUtils.getJESBClass(typeName);
			} catch (PotentialError e) {
				throw new ValidationError("Failed to validate the parameter type name", e);
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + columnKind;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + sqlType;
			result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
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
			ProcedureParameterDescriptor other = (ProcedureParameterDescriptor) obj;
			if (columnKind != other.columnKind)
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (sqlType != other.sqlType)
				return false;
			if (typeName == null) {
				if (other.typeName != null)
					return false;
			} else if (!typeName.equals(other.typeName))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return String.format("%s %s %s", getMode(), typeName, name);
		}
	}

	public static class Metadata implements OperationMetadata<JDBCProcedureCall> {

		@Override
		public String getOperationTypeName() {
			return "JDBC Procedure Call";
		}

		@Override
		public String getCategoryName() {
			return "JDBC";
		}

		@Override
		public Class<? extends OperationBuilder<JDBCProcedureCall>> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(JDBCProcedureCall.class.getName().replace(".", "/") + ".png"));
		}

	}

}
