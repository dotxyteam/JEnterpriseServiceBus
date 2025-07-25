package com.otk.jesb.operation.builtin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.resource.builtin.JDBCConnection;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.Reference;
import com.otk.jesb.Template;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.UpToDate;
import com.otk.jesb.util.UpToDate.VersionAccessException;

import xy.reflect.ui.util.ClassUtils;

public abstract class JDBCOperation implements Operation {

	private JDBCConnection connection;
	private String statement;
	private ParameterValues parameterValues;

	public JDBCOperation(JDBCConnection connection) {
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
		ClassUtils.getCachedClassForName(connection.getDriverClassNameTemplate().getValue());
		Connection conn = DriverManager.getConnection(connection.getUrlTemplate().getValue(),
				connection.getUserNameTemplate().getValue(), connection.getPasswordTemplate().getValue());
		PreparedStatement preparedStatement = conn.prepareStatement(statement);
		int expectedParameterCount = preparedStatement.getParameterMetaData().getParameterCount();
		if (expectedParameterCount != parameterValues.countParameters()) {
			throw new IllegalStateException("Unexpected defined parameter count: " + parameterValues.countParameters()
					+ ". Expected " + expectedParameterCount + " parameter(s).");
		}
		for (int i = 0; i < expectedParameterCount; i++) {
			preparedStatement.setObject(i + 1, parameterValues.getParameterValueByIndex(i));
		}
		return preparedStatement;
	}

	public static abstract class Builder implements OperationBuilder {

		private Reference<JDBCConnection> connectionReference = new Reference<JDBCConnection>(JDBCConnection.class);
		private Template<String> statementTemplate = new Template<String>(String.class);
		private List<ParameterDefinition> parameterDefinitions = new ArrayList<ParameterDefinition>();

		private UpToDate<Class<? extends ParameterValues>> upToDateParameterValuesClass = new UpToDateParameterValuesClass();
		private RootInstanceBuilder parameterValuesBuilder = new RootInstanceBuilder("Parameters",
				new ParameterValuesClassNameAccessor());

		protected JDBCConnection getConnection() {
			return connectionReference.resolve();
		}

		public Reference<JDBCConnection> getConnectionReference() {
			return connectionReference;
		}

		public void setConnectionReference(Reference<JDBCConnection> connectionReference) {
			this.connectionReference = connectionReference;
		}

		public static List<JDBCConnection> getConnectionOptions() {
			return MiscUtils.findResources(Solution.INSTANCE, JDBCConnection.class);
		}

		public Template<String> getStatementTemplate() {
			return statementTemplate;
		}

		public void setStatementTemplate(Template<String> statementTemplate) {
			this.statementTemplate = statementTemplate;
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

		protected ParameterValues buildParameterValues(ExecutionContext context) throws Exception {
			return (ParameterValues) getParameterValuesBuilder().build(new InstantiationContext(context.getVariables(),
					context.getPlan().getValidationContext(context.getCurrentStep()).getVariableDeclarations()));
		}

		@Override
		public void validate(boolean recursively, Plan plan, Step step) throws ValidationError {
			if (getConnection() == null) {
				throw new ValidationError("Failed to resolve the connection reference");
			}
			try {
				Connection sqlConnection = getConnection().build();
				PreparedStatement preparedStatement = sqlConnection.prepareStatement(statementTemplate.getValue());
				int expectedParameterCount = preparedStatement.getParameterMetaData().getParameterCount();
				if (expectedParameterCount != parameterDefinitions.size()) {
					throw new ValidationError("Unexpected defined parameter count: " + parameterDefinitions.size()
							+ ". Expected " + expectedParameterCount + " parameter(s).");
				}
			} catch (ClassNotFoundException | SQLException e) {
				throw new ValidationError("Failed to validate the JDBC opertation settings", e);
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
					throw new UnexpectedError(e);
				}
			}
		}

		private class UpToDateParameterValuesClass extends UpToDate<Class<? extends ParameterValues>> {
			@Override
			protected Object retrieveLastVersionIdentifier() {
				return MiscUtils.serialize(parameterDefinitions);
			}

			@SuppressWarnings("unchecked")
			@Override
			protected Class<? extends ParameterValues> obtainLatest(Object versionIdentifier) {
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
