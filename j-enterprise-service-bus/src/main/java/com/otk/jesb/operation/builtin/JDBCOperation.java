package com.otk.jesb.operation.builtin;

import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

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
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.Pair;
import com.otk.jesb.util.UpToDate;
import com.otk.jesb.util.UpToDate.VersionAccessException;

public abstract class JDBCOperation implements Operation {

	private Session session;
	private JDBCConnection connection;
	private String statement;
	private ParameterValues parameterValues;

	public JDBCOperation(Session session, JDBCConnection connection) {
		this.session = session;
		this.connection = connection;
	}

	public JDBCConnection getConnection() {
		return connection;
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

	private static List<ParameterDefinition> retrieveParameterDefinitions(PreparedStatement preparedStatement)
			throws SQLException {
		ParameterMetaData parameterMetaData = preparedStatement.getParameterMetaData();
		if (parameterMetaData == null) {
			throw new SQLException("No SQL statement paramater meta data found");
		}
		List<ParameterDefinition> result = new ArrayList<JDBCOperation.ParameterDefinition>();
		for (int i = 0; i < parameterMetaData.getParameterCount(); i++) {
			ParameterDefinition parameterDefinition = new ParameterDefinition();
			String parameterTypeName = parameterMetaData.getParameterClassName(i + 1);
			String parameterName = MiscUtils.extractSimpleNameFromClassName(parameterTypeName);
			parameterName = parameterName.substring(0, 1).toLowerCase() + parameterName.substring(1);
			while (result.stream().map(ParameterDefinition::getParameterName)
					.anyMatch(Predicate.isEqual(parameterName))) {
				parameterName = MiscUtils.nextNumbreredName(parameterName);
			}
			parameterDefinition.setParameterName(parameterName);
			parameterDefinition.setParameterTypeName(parameterTypeName);
			result.add(parameterDefinition);
		}
		return result;
	}

	public static abstract class Builder<T extends JDBCOperation> implements OperationBuilder<T> {

		private Reference<JDBCConnection> connectionReference = new Reference<JDBCConnection>(JDBCConnection.class);
		private Variant<String> statementVariant = new Variant<String>(String.class);
		private List<ParameterDefinition> parameterDefinitions = new ArrayList<ParameterDefinition>();
		private boolean parameterDefinitionAutomatic = false;

		private UpToDate<Class<? extends ParameterValues>> upToDateParameterValuesClass = new UpToDateParameterValuesClass();
		private RootInstanceBuilder parameterValuesBuilder = new RootInstanceBuilder("Parameters",
				new ParameterValuesClassNameAccessor());
		private UpToDateParameterDefinitions upToDateParameterDefinitions = new UpToDateParameterDefinitions();

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

		public List<ParameterDefinition> getParameterDefinitions() {
			if (parameterDefinitionAutomatic) {
				try {
					return upToDateParameterDefinitions.get();
				} catch (VersionAccessException e) {
					throw new PotentialError(e);
				}
			} else {
				return parameterDefinitions;
			}
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

		protected JDBCConnection getConnection() {
			return connectionReference.resolve();
		}

		public void retrieveParameterDefinitions() {
			try {
				this.parameterDefinitions = upToDateParameterDefinitions.get();
			} catch (VersionAccessException e) {
				throw new PotentialError(e);
			}
		}

		protected ParameterValues buildParameterValues(ExecutionContext context) throws Exception {
			return (ParameterValues) getParameterValuesBuilder().build(new InstantiationContext(context.getVariables(),
					context.getPlan().getValidationContext(context.getCurrentStep()).getVariableDeclarations()));
		}

		@Override
		public void validate(boolean recursively, Plan plan, Step step) throws ValidationError {
			if (getConnection() == null) {
				throw new ValidationError("Failed to resolve the connection reference");
			}
			List<ParameterDefinition> parameterDefinitions;
			try {
				parameterDefinitions = getParameterDefinitions();
			} catch (Throwable t) {
				throw new ValidationError("Failed to get parameter definitions", t);
			}
			try {
				int expectedParameterCount = getConnection().during(connectionInstance -> {
					try {
						PreparedStatement preparedStatement = connectionInstance
								.prepareStatement(getStatementVariant().getValue());
						return preparedStatement.getParameterMetaData().getParameterCount();
					} catch (SQLException e) {
						throw new PotentialError(e);
					}
				});
				if (expectedParameterCount != parameterDefinitions.size()) {
					throw new ValidationError("Unexpected defined parameter count: " + parameterDefinitions.size()
							+ ". Expected " + expectedParameterCount + " parameter(s).");
				}
			} catch (Exception e) {
				throw new ValidationError("Failed to validate the JDBC opertation", e);
			}
			if (recursively) {
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

		private class ParameterValuesClassNameAccessor extends Accessor<String> {
			@Override
			public String get() {
				try {
					return upToDateParameterValuesClass.get().getName();
				} catch (VersionAccessException e) {
					throw new PotentialError(e);
				}
			}
		}

		private class UpToDateParameterDefinitions extends UpToDate<List<ParameterDefinition>> {

			@Override
			protected Object retrieveLastVersionIdentifier() {
				JDBCConnection connection = getConnection();
				return new Pair<String, String>((connection != null) ? MiscUtils.serialize(connection) : null,
						getStatementVariant().getValue());
			}

			@Override
			protected List<ParameterDefinition> obtainLatest(Object versionIdentifier) throws VersionAccessException {
				try {
					JDBCConnection connection = getConnection();
					if (connection == null) {
						return null;
					}
					String statement = getStatementVariant().getValue();
					if ((statement == null) || statement.trim().isEmpty()) {
						return null;
					}
					return connection.during(connectionInstance -> {
						try {
							PreparedStatement preparedStatement = connectionInstance.prepareStatement(statement);
							return JDBCOperation.retrieveParameterDefinitions(preparedStatement);
						} catch (SQLException e) {
							throw new PotentialError(e);
						}
					});
				} catch (Exception e) {
					throw new VersionAccessException(e);
				}
			}

		}

		private class UpToDateParameterValuesClass extends UpToDate<Class<? extends ParameterValues>> {
			@Override
			protected Object retrieveLastVersionIdentifier() {
				return MiscUtils.serialize(getParameterDefinitions());
			}

			@SuppressWarnings("unchecked")
			@Override
			protected Class<? extends ParameterValues> obtainLatest(Object versionIdentifier) {
				List<ParameterDefinition> parameterDefinitions = getParameterDefinitions();
				String className = JDBCQuery.class.getName() + "ParameterValues"
						+ MiscUtils.toDigitalUniqueIdentifier(this);
				StringBuilder javaSource = new StringBuilder();
				javaSource.append("package " + MiscUtils.extractPackageNameFromClassName(className) + ";" + "\n");
				javaSource.append("public class " + MiscUtils.extractSimpleNameFromClassName(className) + " implements "
						+ MiscUtils.adaptClassNameToSourceCode(ParameterValues.class.getName()) + "{" + "\n");
				for (int i = 0; i < parameterDefinitions.size(); i++) {
					ParameterDefinition parameterDefinition = parameterDefinitions.get(i);
					javaSource.append("  private " + parameterDefinition.getParameterTypeName() + " "
							+ parameterDefinition.getParameterName() + ";\n");
				}
				List<String> constructorParameterDeclarations = new ArrayList<String>();
				for (int i = 0; i < parameterDefinitions.size(); i++) {
					ParameterDefinition parameterDefinition = parameterDefinitions.get(i);
					constructorParameterDeclarations.add(
							parameterDefinition.getParameterTypeName() + " " + parameterDefinition.getParameterName());
				}
				javaSource.append("  public " + MiscUtils.extractSimpleNameFromClassName(className) + "("
						+ MiscUtils.stringJoin(constructorParameterDeclarations, ", ") + "){" + "\n");
				for (int i = 0; i < parameterDefinitions.size(); i++) {
					ParameterDefinition parameterDefinition = parameterDefinitions.get(i);
					javaSource.append("    this." + parameterDefinition.getParameterName() + " = "
							+ parameterDefinition.getParameterName() + ";\n");
				}
				javaSource.append("  }" + "\n");
				javaSource.append("  @Override" + "\n");
				javaSource.append("  public Object getParameterValueByIndex(int i) {" + "\n");
				for (int i = 0; i < parameterDefinitions.size(); i++) {
					ParameterDefinition parameterDefinition = parameterDefinitions.get(i);
					javaSource.append(
							"    if(i == " + i + ") return " + parameterDefinition.getParameterName() + ";" + "\n");
				}
				javaSource.append("    throw new " + UnexpectedError.class.getName() + "();" + "\n");
				javaSource.append("  }" + "\n");
				javaSource.append("  @Override" + "\n");
				javaSource.append("  public int countParameters() {" + "\n");
				javaSource.append("    return " + parameterDefinitions.size() + ";" + "\n");
				javaSource.append("  }" + "\n");
				for (int i = 0; i < parameterDefinitions.size(); i++) {
					ParameterDefinition parameterDefinition = parameterDefinitions.get(i);
					javaSource.append("  public " + parameterDefinition.getParameterTypeName() + " get"
							+ parameterDefinition.getParameterName().substring(0, 1).toUpperCase()
							+ parameterDefinition.getParameterName().substring(1) + "() {" + "\n");
					javaSource.append("    return " + parameterDefinition.getParameterName() + ";" + "\n");
					javaSource.append("  }" + "\n");
				}
				javaSource.append("}" + "\n");
				try {
					return (Class<? extends ParameterValues>) MiscUtils.IN_MEMORY_COMPILER.compile(className,
							javaSource.toString());
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

	}

	public static interface ParameterValues {
		public Object getParameterValueByIndex(int i);

		public int countParameters();
	}

}
