package com.otk.jesb.operation.builtin;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ParameterMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.otk.jesb.PotentialError;
import com.otk.jesb.Reference;
import com.otk.jesb.Session;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.resource.builtin.JDBCConnection;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;

import com.otk.jesb.solution.Step;

import xy.reflect.ui.info.ResourcePath;

public class JDBCStoredProcedureCall extends JDBCQuery {

	private ProcedureDescriptor procedure;

	public JDBCStoredProcedureCall(Session session, JDBCConnection connection, ProcedureDescriptor procedure,
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
		return connection.during(connectionInstance -> {
			try (CallableStatement preparedStatement = connectionInstance
					.prepareCall(procedure.getCallQueryString(connectionInstance))) {
				ParameterValues parametervalues = getParameterValues();
				int parameterIndex = 1;
				if (procedure.getReturnParameter() != null) {
					preparedStatement.registerOutParameter(parameterIndex++,
							procedure.getReturnParameter().getSqlType());
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
						preparedStatement.setObject(parameterIndex,
								parametervalues.getParameterValueByIndex(valueIndex++));
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
				return customResultClass.getConstructors()[0].newInstance(resultArguments.toArray());
			} catch (Exception e) {
				throw new PotentialError(e);
			}
		});
	}

	public static class Builder implements OperationBuilder<JDBCStoredProcedureCall> {

		private String catalogName;
		private String schemaName;
		private ProcedureDescriptor procedure;

		private JDBCQuery.Builder util = new JDBCQuery.Builder() {

			@Override
			protected Class<?> createCustomResultClass() {
				Class<?> superResult = super.createCustomResultClass();
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
				return result;
			}

			@Override
			public List<ParameterDefinition> getParameterDefinitions() {
				throw new UnsupportedOperationException();
			}

		};

		public Reference<JDBCConnection> getConnectionReference() {
			return util.getConnectionReference();
		}

		public void setConnectionReference(Reference<JDBCConnection> connectionReference) {
			util.setConnectionReference(connectionReference);
		}

		public RootInstanceBuilder getParameterValuesBuilder() {
			return util.getParameterValuesBuilder();
		}

		public void setParameterValuesBuilder(RootInstanceBuilder parameterValuesBuilder) {
			util.setParameterValuesBuilder(parameterValuesBuilder);
		}

		public String getCatalogName() {
			return catalogName;
		}

		public void setCatalogName(String catalogName) {
			this.catalogName = catalogName;
		}

		public String getSchemaName() {
			return schemaName;
		}

		public void setSchemaName(String schemaName) {
			this.schemaName = schemaName;
		}

		public ProcedureDescriptor getProcedure() {
			return procedure;
		}

		public void setProcedure(ProcedureDescriptor procedure) {
			this.procedure = procedure;
			updateStatement();
		}

		private void updateStatement() {
			try {
				util.getStatementVariant()
						.setConstantValue(
								((util.getConnection() != null) && (procedure != null))
										? util.getConnection().during(
												connectionInstance -> procedure.getCallQueryString(connectionInstance))
										: null);
			} catch (Exception e) {
				throw new UnexpectedError(e);
			}
		}

		public List<ProcedureDescriptor> getProcedureOptions() {
			JDBCConnection connection = util.getConnection();
			if (connection == null) {
				return Collections.emptyList();
			}
			try {
				return connection.during(connectionInstance -> {
					try {
						return ProcedureDescriptor.list(connectionInstance, catalogName, schemaName, null);
					} catch (Exception e) {
						throw new PotentialError(e);
					}
				});
			} catch (Exception e) {
				return Collections.emptyList();
			}
		}

		@Override
		public JDBCStoredProcedureCall build(ExecutionContext context, ExecutionInspector executionInspector)
				throws Exception {
			JDBCStoredProcedureCall result = new JDBCStoredProcedureCall(context.getSession(), util.getConnection(),
					procedure, util.upToDateCustomResultClass.get());
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
			try {
				util.upToDateParameterValuesClass.get();
			} catch (Throwable t) {
				throw new ValidationError("Failed to get compiled parameter definitions", t);
			}

			try {
				util.upToDateCustomResultClass.get();
			} catch (Throwable t) {
				throw new ValidationError("Failed to get compiled result column definitions", t);
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

	}

	public static class ProcedureDescriptor {
		private static final String RETURN_VALUE_NAME = "<RETURN_VALUE>";

		private String catalogName;
		private String schemaName;
		private String procedureName;
		private String remarks;
		private ProcedureParameterDescriptor returnParameter;
		private List<ProcedureParameterDescriptor> parameters = new ArrayList<>();

		public String getCatalogName() {
			return catalogName;
		}

		public void setCatalogName(String catalogName) {
			this.catalogName = catalogName;
		}

		public String getSchemaName() {
			return schemaName;
		}

		public void setSchemaName(String schemaName) {
			this.schemaName = schemaName;
		}

		public String getProcedureName() {
			return procedureName;
		}

		public void setProcedureName(String procedureName) {
			this.procedureName = procedureName;
		}

		public String getRemarks() {
			return remarks;
		}

		public void setRemarks(String remarks) {
			this.remarks = remarks;
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

		public String getCallQueryString(Connection connectionInstance) {
			String placeHolders = String.join(",", Collections.nCopies(getParameters().size(), "?"));
			String procedureQualifiedName;
			try {
				procedureQualifiedName = getProcedureQualifiedName(
						connectionInstance.getMetaData().getIdentifierQuoteString());
			} catch (SQLException e) {
				throw new UnexpectedError(e);
			}
			if (isOfFunctionKind()) {
				return "{?= call " + procedureQualifiedName + "(" + placeHolders + ")}";
			} else {
				return "{call " + procedureQualifiedName + "(" + placeHolders + ")}";
			}
		}

		public static List<ProcedureDescriptor> list(Connection connectionInstance, String catalogName,
				String schemaPattern, String procedureNamePattern) throws Exception {
			List<ProcedureDescriptor> result = new ArrayList<>();
			DatabaseMetaData meta = connectionInstance.getMetaData();
			try (ResultSet procedureResultSet = meta.getProcedures(catalogName, schemaPattern, procedureNamePattern)) {
				while (procedureResultSet.next()) {
					ProcedureDescriptor procedure = new ProcedureDescriptor();
					procedure.setCatalogName(procedureResultSet.getString("PROCEDURE_CAT"));
					procedure.setSchemaName(procedureResultSet.getString("PROCEDURE_SCHEM"));
					procedure.setProcedureName(procedureResultSet.getString("PROCEDURE_NAME"));
					procedure.setRemarks(procedureResultSet.getString("REMARKS"));
					List<ProcedureParameterDescriptor> parameters = new ArrayList<ProcedureParameterDescriptor>();
					try (ResultSet columnResultSet = meta.getProcedureColumns(procedure.getCatalogName(),
							procedure.getSchemaName(), procedure.getProcedureName(), "%")) {
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
					procedure.setParameters(parameters);
					ParameterMetaData parameterMetaData;
					try {
						CallableStatement preparedStatement = connectionInstance
								.prepareCall(procedure.getCallQueryString(connectionInstance));
						parameterMetaData = preparedStatement.getParameterMetaData();
					} catch (SQLException e) {
						continue;
					}
					if (procedure.isOfFunctionKind()) {
						procedure.getReturnParameter()
								.setTypeName((parameterMetaData != null) ? parameterMetaData.getParameterClassName(1)
										: Object.class.getName());
					}
					for (int i = 0; i < procedure.parameters.size(); i++) {
						procedure.parameters.get(i)
								.setTypeName((parameterMetaData != null)
										? parameterMetaData
												.getParameterClassName((procedure.isOfFunctionKind() ? +1 : 0) + i + 1)
										: Object.class.getName());
					}
					result.add(procedure);
				}
			}
			return result;
		}

		private String getProcedureQualifiedName(String quote) {
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

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((catalogName == null) ? 0 : catalogName.hashCode());
			result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
			result = prime * result + ((procedureName == null) ? 0 : procedureName.hashCode());
			result = prime * result + ((remarks == null) ? 0 : remarks.hashCode());
			result = prime * result + ((returnParameter == null) ? 0 : returnParameter.hashCode());
			result = prime * result + ((schemaName == null) ? 0 : schemaName.hashCode());
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
			if (catalogName == null) {
				if (other.catalogName != null)
					return false;
			} else if (!catalogName.equals(other.catalogName))
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
			if (remarks == null) {
				if (other.remarks != null)
					return false;
			} else if (!remarks.equals(other.remarks))
				return false;
			if (returnParameter == null) {
				if (other.returnParameter != null)
					return false;
			} else if (!returnParameter.equals(other.returnParameter))
				return false;
			if (schemaName == null) {
				if (other.schemaName != null)
					return false;
			} else if (!schemaName.equals(other.schemaName))
				return false;
			return true;
		}

		@Override
		public String toString() {
			String result = isOfFunctionKind() ? "Function" : "Procedure";
			result += " " + getProcedureQualifiedName(null);
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

	public static class Metadata implements OperationMetadata<JDBCStoredProcedureCall> {

		@Override
		public String getOperationTypeName() {
			return "JDBC Stored Procedure Call";
		}

		@Override
		public String getCategoryName() {
			return "JDBC";
		}

		@Override
		public Class<? extends OperationBuilder<JDBCStoredProcedureCall>> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(ResourcePath.specifyClassPathResourceLocation(
					JDBCStoredProcedureCall.class.getName().replace(".", "/") + ".png"));
		}

	}

}
