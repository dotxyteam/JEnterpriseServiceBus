import java.sql.*;
import java.util.*;

public class ProcedureInvoker {

	private static final String RETURN_VALUE_NAME = "<RETURN_VALUE>";

	private final Connection connectionInstance;

	public ProcedureInvoker(Connection connectionInstance) {
		this.connectionInstance = connectionInstance;
	}

	public static class ProcedureDescriptor {
		private String catalogName;
		private String schemaName;
		private String procedureName;
		private String remarks;
		private Integer returnSqlType;
		private List<ParameterDescriptor> parameters = new ArrayList<>();

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

		public Integer getReturnSqlType() {
			return returnSqlType;
		}

		public void setReturnSqlType(Integer returnSqlType) {
			this.returnSqlType = returnSqlType;
		}

		public boolean isOfFunctionKind() {
			return returnSqlType != null;
		}

		public List<ParameterDescriptor> getParameters() {
			return parameters;
		}

		public void setParameters(List<ParameterDescriptor> parameters) {
			this.parameters = parameters;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((catalogName == null) ? 0 : catalogName.hashCode());
			result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
			result = prime * result + ((procedureName == null) ? 0 : procedureName.hashCode());
			result = prime * result + ((remarks == null) ? 0 : remarks.hashCode());
			result = prime * result + ((returnSqlType == null) ? 0 : returnSqlType.hashCode());
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
			if (returnSqlType == null) {
				if (other.returnSqlType != null)
					return false;
			} else if (!returnSqlType.equals(other.returnSqlType))
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
			return String.format("%s %s.%s.%s (%s)\n  Params: %s", isOfFunctionKind() ? "Fonction" : "Procédure",
					catalogName, schemaName, procedureName, remarks, parameters);
		}

	}

	public static class ParameterDescriptor {
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
			ParameterDescriptor other = (ParameterDescriptor) obj;
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
			return String.format("%s %s(%s)", getMode(), name, typeName);
		}
	}

	public List<ProcedureDescriptor> listProcedures(String catalogName, String schemaPattern,
			String procedureNamePattern) throws SQLException {
		List<ProcedureDescriptor> result = new ArrayList<>();
		DatabaseMetaData meta = connectionInstance.getMetaData();
		try (ResultSet procedureResultSet = meta.getProcedures(catalogName, schemaPattern, procedureNamePattern)) {
			while (procedureResultSet.next()) {
				ProcedureDescriptor procedure = new ProcedureDescriptor();
				procedure.setCatalogName(procedureResultSet.getString("PROCEDURE_CAT"));
				procedure.setSchemaName(procedureResultSet.getString("PROCEDURE_SCHEM"));
				procedure.setProcedureName(procedureResultSet.getString("PROCEDURE_NAME"));
				procedure.setRemarks(procedureResultSet.getString("REMARKS"));
				List<ParameterDescriptor> parameters = new ArrayList<>();
				try (ResultSet columnResultSet = meta.getProcedureColumns(procedure.getCatalogName(),
						procedure.getSchemaName(), procedure.getProcedureName(), "%")) {
					while (columnResultSet.next()) {
						int columnType = columnResultSet.getInt("COLUMN_TYPE");
						if (columnType == DatabaseMetaData.procedureColumnReturn) {
							procedure.setReturnSqlType(columnResultSet.getInt("DATA_TYPE"));
						} else {
							ParameterDescriptor parameter = new ParameterDescriptor();
							parameter.setColumnKind(columnType);
							parameter.setName(columnResultSet.getString("COLUMN_NAME"));
							parameter.setSqlType(columnResultSet.getInt("DATA_TYPE"));
							parameters.add(parameter);
						}

					}
				}
				procedure.setParameters(parameters);
				CallableStatement preparedStatement = prepare(procedure);
				ParameterMetaData parameterMetaData = preparedStatement.getParameterMetaData();
				for (int i = 0; i < procedure.parameters.size(); i++) {
					procedure.parameters.get(i).setTypeName(
							parameterMetaData.getParameterClassName((procedure.isOfFunctionKind() ? +1 : 0) + i + 1));
				}
				result.add(procedure);
			}
		}
		return result;
	}

	private CallableStatement prepare(ProcedureDescriptor procedure) throws SQLException {
		String placeHolders = String.join(",", Collections.nCopies(procedure.getParameters().size(), "?"));
		String sql;
		if (procedure.isOfFunctionKind()) {
			sql = "{?= call " + procedure.procedureName + "(" + placeHolders + ")}";
		} else {
			sql = "{call " + procedure.procedureName + "(" + placeHolders + ")}";
		}
		return connectionInstance.prepareCall(sql);
	}

	public Map<String, Object> invoke(ProcedureDescriptor procedure, Object... inputParameterValues)
			throws SQLException {
		try (CallableStatement preparedStatement = prepare(procedure)) {
			int parameterIndex = 1;
			if (procedure.isOfFunctionKind()) {
				preparedStatement.registerOutParameter(parameterIndex++, procedure.getReturnSqlType());
			}
			int valueIndex = 0;
			for (ParameterDescriptor p : procedure.getParameters()) {
				switch (p.getColumnKind()) {
				case DatabaseMetaData.procedureColumnIn: {
					preparedStatement.setObject(parameterIndex++, inputParameterValues[valueIndex++]);
					break;
				}
				case DatabaseMetaData.procedureColumnOut: {
					preparedStatement.registerOutParameter(parameterIndex++, p.getSqlType());
					break;
				}
				case DatabaseMetaData.procedureColumnInOut: {
					preparedStatement.setObject(parameterIndex, inputParameterValues[valueIndex++]);
					preparedStatement.registerOutParameter(parameterIndex++, p.getSqlType());
					break;
				}
				default: {
					preparedStatement.setObject(parameterIndex++, null);
					break;
				}
				}
			}
			preparedStatement.execute();
			Map<String, Object> resultMap = new LinkedHashMap<>();
			parameterIndex = 1;
			if (procedure.isOfFunctionKind()) {
				resultMap.put(RETURN_VALUE_NAME, preparedStatement.getObject(parameterIndex++));
			}
			for (ParameterDescriptor parameter : procedure.getParameters()) {
				switch (parameter.getColumnKind()) {
				case DatabaseMetaData.procedureColumnOut:
				case DatabaseMetaData.procedureColumnInOut: {
					resultMap.put(parameter.getName(), preparedStatement.getObject(parameterIndex));
					break;
				}
				}
				parameterIndex++;
			}
			return resultMap;
		}
	}

}
