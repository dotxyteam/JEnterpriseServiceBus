package com.otk.jesb.operation.builtin;

import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.resource.builtin.JDBCConnection;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.Variant;
import com.otk.jesb.PotentialError;
import com.otk.jesb.Reference;
import com.otk.jesb.Session;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.CodeBuilder;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.Pair;
import com.otk.jesb.util.UpToDate;
import com.otk.jesb.util.UpToDate.VersionAccessException;

public abstract class JDBCOperation implements Operation {

	protected Session session;
	protected JDBCConnection connection;
	protected String statement;
	protected ParameterValues parameterValues;

	public JDBCOperation(Session session, JDBCConnection connection) {
		this.session = session;
		this.connection = connection;
	}

	protected JDBCConnection getConnection() {
		return connection;
	}

	protected Session getSession() {
		return session;
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

	protected PreparedStatement prepare() throws Exception {
		Connection connectionInstance = connection.during(session);
		PreparedStatement preparedStatement = connectionInstance.prepareStatement(statement);
		ParameterMetaData parameterMetaData = preparedStatement.getParameterMetaData();
		if (parameterMetaData != null) {
			int expectedParameterCount = preparedStatement.getParameterMetaData().getParameterCount();
			if (expectedParameterCount != parameterValues.countParameters()) {
				throw new IllegalStateException(
						"Unexpected defined parameter count: " + parameterValues.countParameters() + ". Expected "
								+ expectedParameterCount + " parameter(s).");
			}
		}
		for (int i = 0; i < parameterValues.countParameters(); i++) {
			preparedStatement.setObject(i + 1, parameterValues.getParameterValueByIndex(i));
		}
		return preparedStatement;
	}

	public static abstract class Builder<T extends JDBCOperation> implements OperationBuilder<T> {

		protected Reference<JDBCConnection> connectionReference = new Reference<JDBCConnection>(JDBCConnection.class);
		protected Variant<String> statementVariant = new Variant<String>(String.class);
		protected List<ParameterDefinition> parameterDefinitions = new ArrayList<ParameterDefinition>();
		protected boolean parameterDefinitionAutomatic = false;

		protected UpToDate<Solution, Class<? extends ParameterValues>> upToDateParameterValuesClass = new UpToDateParameterValuesClass();
		protected RootInstanceBuilder parameterValuesBuilder = new RootInstanceBuilder("Parameters",
				new ParameterValuesClassNameAccessor());
		protected UpToDateMetaParameterDefinitions upToDateMetaParameterDefinitions = new UpToDateMetaParameterDefinitions();

		public boolean isParameterDefinitionAutomatic() {
			return parameterDefinitionAutomatic;
		}

		public void setParameterDefinitionAutomatic(boolean parameterDefinitionAutomatic) {
			this.parameterDefinitionAutomatic = parameterDefinitionAutomatic;
		}

		public Reference<JDBCConnection> getConnectionReference() {
			return connectionReference;
		}

		public void setConnectionReference(Reference<JDBCConnection> connectionReference) {
			this.connectionReference = connectionReference;
		}

		public Variant<String> getStatementVariant() {
			return statementVariant;
		}

		public void setStatementVariant(Variant<String> statementVariant) {
			this.statementVariant = statementVariant;
		}

		protected List<ParameterDefinition> computeParameterDefinitions(Solution solutionInstance) {
			if (parameterDefinitionAutomatic) {
				try {
					return upToDateMetaParameterDefinitions.get(solutionInstance);
				} catch (VersionAccessException e) {
					throw new PotentialError(e);
				}
			} else {
				return parameterDefinitions;
			}
		}

		public List<ParameterDefinition> getParameterDefinitions() {
			return parameterDefinitions;
		}

		public void setParameterDefinitions(List<ParameterDefinition> parameterDefinitions) {
			this.parameterDefinitions = parameterDefinitions;
		}

		public RootInstanceBuilder getParameterValuesBuilder() {
			return parameterValuesBuilder;
		}

		public void setParameterValuesBuilder(RootInstanceBuilder parameterValuesBuilder) {
			if (parameterValuesBuilder == null) {
				throw new UnexpectedError();
			}
			this.parameterValuesBuilder = parameterValuesBuilder;
		}

		protected JDBCConnection getConnection(Solution solutionInstance) {
			return connectionReference.resolve(solutionInstance);
		}

		public void retrieveParameterDefinitions(Solution solutionInstance) {
			try {
				this.parameterDefinitions = upToDateMetaParameterDefinitions.get(solutionInstance);
			} catch (VersionAccessException e) {
				throw new PotentialError(e);
			}
		}

		protected List<ParameterDefinition> obtainMetaParameterDefinitions(Solution solutionInstance) throws Exception {
			String statement = getStatementVariant().getValue(solutionInstance);
			if ((statement == null) || statement.trim().isEmpty()) {
				return null;
			}
			JDBCConnection connection = getConnection(solutionInstance);
			if (connection == null) {
				return null;
			}
			return connection.during(connectionInstance -> {
				try {
					PreparedStatement preparedStatement = connectionInstance.prepareStatement(statement);
					ParameterMetaData parameterMetaData = preparedStatement.getParameterMetaData();
					if (parameterMetaData == null) {
						throw new SQLException("No SQL statement parameter meta data found");
					}
					List<ParameterDefinition> result = new ArrayList<JDBCOperation.ParameterDefinition>();
					for (int i = 0; i < parameterMetaData.getParameterCount(); i++) {
						ParameterDefinition parameterDefinition = new ParameterDefinition();
						String parameterTypeName = parameterMetaData.getParameterClassName(i + 1);
						String parameterName = MiscUtils.extractSimpleNameFromClassName(parameterTypeName);
						parameterName = parameterName.substring(0, 1).toLowerCase() + parameterName.substring(1);
						parameterDefinition.setParameterName(parameterName);
						parameterDefinition.setParameterTypeName(parameterTypeName);
						result.add(parameterDefinition);
					}
					MiscUtils.makeNumberedNamesUnique(result, ParameterDefinition::getParameterName,
							ParameterDefinition::setParameterName);
					return result;
				} catch (SQLException e) {
					throw new PotentialError(e);
				}
			}, solutionInstance);
		}

		protected ParameterValues buildParameterValues(ExecutionContext context) throws Exception {
			Solution solutionInstance = context.getSession().getSolutionInstance();
			return (ParameterValues) getParameterValuesBuilder().build(new InstantiationContext(
					context.getVariables(), context.getPlan()
							.getValidationContext(context.getCurrentStep(), solutionInstance).getVariableDeclarations(),
					solutionInstance));
		}

		@Override
		public void validate(boolean recursively, Solution solutionInstance, Plan plan, Step step)
				throws ValidationError {
			JDBCConnection connection = getConnection(solutionInstance);
			if (connection == null) {
				throw new ValidationError("Failed to resolve the connection reference");
			}
			String statement = getStatementVariant().getValue(solutionInstance);
			if ((statement == null) || statement.trim().isEmpty()) {
				throw new ValidationError("Statement not provided");
			}
			try {
				connection.during(connectionInstance -> {
					try {
						return connectionInstance.prepareStatement(statement);
					} catch (SQLException e) {
						throw new PotentialError(e);
					}
				}, solutionInstance);
			} catch (Exception e) {
				throw new ValidationError(e.getMessage(), e);
			}
			if (recursively) {
				validateParameterDefinitions(solutionInstance);
				if (parameterValuesBuilder != null) {
					try {
						parameterValuesBuilder.getFacade(solutionInstance).validate(recursively,
								plan.getValidationContext(step, solutionInstance).getVariableDeclarations());
					} catch (ValidationError e) {
						throw new ValidationError("Failed to validate the parameter values builder", e);
					}
				}
			}
		}

		public void validateParameterDefinitions(Solution solutionInstance) throws ValidationError {
			List<ParameterDefinition> parameterDefinitions;
			try {
				parameterDefinitions = computeParameterDefinitions(solutionInstance);
			} catch (Throwable t) {
				throw new ValidationError("Failed to get parameter definitions", t);
			}
			if (parameterDefinitions != null) {
				if (!parameterDefinitionAutomatic) {
					List<ParameterDefinition> metaParameterDefinitions;
					try {
						metaParameterDefinitions = upToDateMetaParameterDefinitions.get(solutionInstance);
					} catch (VersionAccessException e) {
						metaParameterDefinitions = null;
					}
					if (metaParameterDefinitions != null) {
						List<String> metaParameterTypeNames = metaParameterDefinitions.stream()
								.map(ParameterDefinition::getParameterTypeName).collect(Collectors.toList());
						List<String> parameterTypeNames = parameterDefinitions.stream()
								.map(ParameterDefinition::getParameterTypeName).collect(Collectors.toList());
						if (!metaParameterTypeNames.equals(parameterTypeNames)) {
							throw new ValidationError("Parameter types are not valid. Unexpected: " + parameterTypeNames
									+ ". Expected: " + metaParameterTypeNames);
						}
					}
				}
			}
			List<String> parameterNames = new ArrayList<String>();
			for (ParameterDefinition parameterDefinition : parameterDefinitions) {
				if (parameterNames.contains(parameterDefinition.getParameterName())) {
					throw new ValidationError(
							"Duplicate parameter name detected: '" + parameterDefinition.getParameterName() + "'");
				} else {
					parameterNames.add(parameterDefinition.getParameterName());
				}
				try {
					parameterDefinition.validate();
				} catch (ValidationError e) {
					throw new ValidationError(
							"Failed to validate the parameter '" + parameterDefinition.getParameterName() + "'", e);
				}
			}

		}

		private class ParameterValuesClassNameAccessor extends Accessor<Solution, String> {
			@Override
			public String get(Solution solutionInstance) {
				try {
					Class<? extends ParameterValues> parameterValuesClass = upToDateParameterValuesClass
							.get(solutionInstance);
					if (parameterValuesClass == null) {
						return null;
					}
					return parameterValuesClass.getName();
				} catch (VersionAccessException e) {
					throw new PotentialError(e);
				}
			}
		}

		private class UpToDateMetaParameterDefinitions extends UpToDate<Solution, List<ParameterDefinition>> {

			@Override
			protected Object retrieveLastVersionIdentifier(Solution solutionInstance) {
				JDBCConnection connection = getConnection(solutionInstance);
				return new Pair<String, String>(
						(connection != null) ? solutionInstance.getSerializer().write(connection) : null,
						getStatementVariant().getValue(solutionInstance));
			}

			@Override
			protected List<ParameterDefinition> obtainLatest(Solution solutionInstance, Object versionIdentifier)
					throws VersionAccessException {
				try {
					return obtainMetaParameterDefinitions(solutionInstance);
				} catch (Exception e) {
					throw new VersionAccessException(e);
				}
			}

		}

		private class UpToDateParameterValuesClass extends UpToDate<Solution, Class<? extends ParameterValues>> {
			@Override
			protected Object retrieveLastVersionIdentifier(Solution solutionInstance) {
				return solutionInstance.getSerializer().write(computeParameterDefinitions(solutionInstance));
			}

			@SuppressWarnings("unchecked")
			@Override
			protected Class<? extends ParameterValues> obtainLatest(Solution solutionInstance,
					Object versionIdentifier) {
				List<ParameterDefinition> parameterDefinitions = computeParameterDefinitions(solutionInstance);
				if (parameterDefinitions == null) {
					return null;
				}
				String className = JDBCQuery.class.getName() + "ParameterValues"
						+ MiscUtils.toDigitalUniqueIdentifier(this);
				CodeBuilder javaSource = new CodeBuilder();
				javaSource.append("package " + MiscUtils.extractPackageNameFromClassName(className) + ";" + "\n");
				javaSource.append("\n");
				javaSource.append("public class " + MiscUtils.extractSimpleNameFromClassName(className) + " implements "
						+ MiscUtils.adaptClassNameToSourceCode(ParameterValues.class.getName()) + "{" + "\n");
				javaSource.indenting(() -> {
					javaSource.append("\n");
					for (int i = 0; i < parameterDefinitions.size(); i++) {
						ParameterDefinition parameterDefinition = parameterDefinitions.get(i);
						javaSource.append("private " + parameterDefinition.getParameterTypeName() + " "
								+ parameterDefinition.getParameterName() + ";\n");
					}
					List<String> constructorParameterDeclarations = new ArrayList<String>();
					for (int i = 0; i < parameterDefinitions.size(); i++) {
						ParameterDefinition parameterDefinition = parameterDefinitions.get(i);
						constructorParameterDeclarations.add(parameterDefinition.getParameterTypeName() + " "
								+ parameterDefinition.getParameterName());
					}
					javaSource.append("\n");
					javaSource.append("public " + MiscUtils.extractSimpleNameFromClassName(className) + "("
							+ MiscUtils.stringJoin(constructorParameterDeclarations, ", ") + "){" + "\n");
					javaSource.indenting(() -> {
						for (int i = 0; i < parameterDefinitions.size(); i++) {
							ParameterDefinition parameterDefinition = parameterDefinitions.get(i);
							javaSource.append("this." + parameterDefinition.getParameterName() + " = "
									+ parameterDefinition.getParameterName() + ";\n");
						}
					});
					javaSource.append("}" + "\n");
					javaSource.append("\n");
					{
						javaSource.append("@Override" + "\n");
						javaSource.append("public Object getParameterValueByIndex(int i) {" + "\n");
						javaSource.indenting(() -> {
							for (int i = 0; i < parameterDefinitions.size(); i++) {
								ParameterDefinition parameterDefinition = parameterDefinitions.get(i);
								javaSource.append("if(i == " + i + ") return " + parameterDefinition.getParameterName()
										+ ";" + "\n");
							}
							javaSource.append("throw new " + UnexpectedError.class.getName() + "();" + "\n");
						});
						javaSource.append("}" + "\n");
					}
					javaSource.append("\n");
					{
						javaSource.append("@Override" + "\n");
						javaSource.append("public int countParameters() {" + "\n");
						javaSource.appendIndented("return " + parameterDefinitions.size() + ";" + "\n");
						javaSource.append("}" + "\n");
					}
					for (int i = 0; i < parameterDefinitions.size(); i++) {
						ParameterDefinition parameterDefinition = parameterDefinitions.get(i);
						javaSource.append("\n");
						javaSource.append("public " + parameterDefinition.getParameterTypeName() + " get"
								+ parameterDefinition.getParameterName().substring(0, 1).toUpperCase()
								+ parameterDefinition.getParameterName().substring(1) + "() {" + "\n");
						javaSource.appendIndented("return " + parameterDefinition.getParameterName() + ";" + "\n");
						javaSource.append("}" + "\n");
					}
				});
				javaSource.append("\n");
				javaSource.append("}" + "\n");
				try {
					return (Class<? extends ParameterValues>) solutionInstance.getRuntime().getInMemoryCompiler()
							.compile(className, javaSource.toString());
				} catch (CompilationError e) {
					throw new UnexpectedError(e);
				}
			}
		}
	}

	public static class ParameterDefinition {

		private String parameterName;
		private String parameterTypeName = getParameterTypeNameOptions().get(0);

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

		public static List<String> getParameterTypeNameOptions() {
			List<String> result = new ArrayList<String>();
			for (Class<?> clazz : Arrays.asList(String.class, Boolean.class, Integer.class, Long.class, Float.class,
					Double.class, byte[].class, java.sql.Date.class, java.sql.Time.class, java.sql.Timestamp.class)) {
				result.add(clazz.getName());
			}
			return result;
		}

		public void validate() throws ValidationError {
			if (parameterName == null) {
				throw new ValidationError("The parameter name is not provided");
			}
			if (!MiscUtils.VARIABLE_NAME_PATTERN.matcher(parameterName).matches()) {
				throw new ValidationError("Invalid parameter name: '" + parameterName
						+ "' (should match the following regular expression: "
						+ MiscUtils.VARIABLE_NAME_PATTERN.pattern() + ")");
			}
			if (!getParameterTypeNameOptions().contains(parameterTypeName)) {
				throw new ValidationError("Illegal parameter type name: '" + parameterTypeName + "': Expected one of "
						+ getParameterTypeNameOptions());
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((parameterName == null) ? 0 : parameterName.hashCode());
			result = prime * result + ((parameterTypeName == null) ? 0 : parameterTypeName.hashCode());
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
			ParameterDefinition other = (ParameterDefinition) obj;
			if (parameterName == null) {
				if (other.parameterName != null)
					return false;
			} else if (!parameterName.equals(other.parameterName))
				return false;
			if (parameterTypeName == null) {
				if (other.parameterTypeName != null)
					return false;
			} else if (!parameterTypeName.equals(other.parameterTypeName))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "ParameterDefinition [parameterName=" + parameterName + ", parameterTypeName=" + parameterTypeName
					+ "]";
		}

	}

	public static interface ParameterValues {
		public Object getParameterValueByIndex(int i);

		public int countParameters();
	}

}
