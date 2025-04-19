package com.otk.jesb.activity.builtin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.otk.jesb.Plan.ExecutionContext;
import com.otk.jesb.Plan.ExecutionInspector;
import com.otk.jesb.Function;
import com.otk.jesb.Solution;
import com.otk.jesb.ValidationContext;
import com.otk.jesb.activity.Activity;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.instantiation.CompilationContext;
import com.otk.jesb.instantiation.EvaluationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.resource.builtin.JDBCConnection;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.info.ResourcePath;
import com.otk.jesb.util.Accessor;

public class JDBCUpdateActivity implements Activity {

	private JDBCConnection connection;
	private String statement;
	private ParameterValues parameterValues;

	public JDBCConnection getConnection() {
		return connection;
	}

	public void setConnection(JDBCConnection connection) {
		this.connection = connection;
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

	@Override
	public Object execute() throws Exception {
		Class.forName(connection.getDriverClassName());
		Connection conn = DriverManager.getConnection(connection.getUrl(), connection.getUserName(),
				connection.getPassword());
		PreparedStatement preparedStatement = conn.prepareStatement(statement);
		int expectedParameterCount = preparedStatement.getParameterMetaData().getParameterCount();
		if (expectedParameterCount != parameterValues.countParameters()) {
			throw new Exception("Unexpected defined parameter count: " + parameterValues.countParameters()
					+ ". Expected " + expectedParameterCount + " parameter(s).");
		}
		for (int i = 0; i < expectedParameterCount; i++) {
			preparedStatement.setObject(i + 1, parameterValues.getParameterValueByIndex(i));
		}
		int affectedRows = preparedStatement.executeUpdate();
		return new Result(affectedRows);
	}

	public static class Metadata implements ActivityMetadata {

		@Override
		public String getActivityTypeName() {
			return "JDBC Update";
		}

		@Override
		public String getCategoryName() {
			return "JDBC";
		}

		@Override
		public Class<? extends ActivityBuilder> getActivityBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getActivityIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(JDBCUpdateActivity.class.getName().replace(".", "/") + ".png"));
		}

	}

	public static class Builder implements ActivityBuilder {

		private JDBCConnection connection;
		private String statement;
		private List<ParameterDefinition> parameterDefinitions = new ArrayList<ParameterDefinition>();
		private RootInstanceBuilder parameterValuesBuilder = new RootInstanceBuilder("Parameters",
				new Accessor<String>() {
					@Override
					public String get() {
						return parameterValuesClass.getName();
					}
				});

		private Class<? extends ParameterValues> parameterValuesClass;

		public Builder() {
			updateDynamicClasses();
		}

		private void updateDynamicClasses() {
			parameterValuesClass = createParameterValuesClass();
		}

		@SuppressWarnings("unchecked")
		private Class<? extends ParameterValues> createParameterValuesClass() {
			String className = JDBCUpdateActivity.class.getName() + "ParameterValues"
					+ MiscUtils.getDigitalUniqueIdentifier();
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
				constructorParameterDeclarations
						.add(parameterDefinition.getParameterTypeName() + " " + parameterDefinition.getParameterName());
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
				javaSource
						.append("    if(i == " + i + ") return " + parameterDefinition.getParameterName() + ";" + "\n");
			}
			javaSource.append("    throw new " + AssertionError.class.getName() + "();" + "\n");
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
				return (Class<? extends ParameterValues>) MiscUtils.IN_MEMORY_JAVA_COMPILER.compile(className,
						javaSource.toString());
			} catch (CompilationError e) {
				throw new AssertionError(e);
			}
		}

		public JDBCConnection getConnection() {
			return connection;
		}

		public void setConnection(JDBCConnection connection) {
			this.connection = connection;
		}

		public static List<JDBCConnection> getConnectionOptions() {
			return MiscUtils.findResources(Solution.INSTANCE, JDBCConnection.class);
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
			updateDynamicClasses();
		}

		public RootInstanceBuilder getParameterValuesBuilder() {
			return parameterValuesBuilder;
		}

		public void setParameterValuesBuilder(RootInstanceBuilder parameterValuesBuilder) {
			if (parameterValuesBuilder == null) {
				throw new AssertionError();
			}
			this.parameterValuesBuilder = parameterValuesBuilder;
		}

		@Override
		public Activity build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			JDBCUpdateActivity result = new JDBCUpdateActivity();
			result.setConnection(connection);
			result.setStatement(statement);
			ParameterValues parameterValues = (ParameterValues) parameterValuesBuilder
					.build(new EvaluationContext(context, null));
			result.setParameterValues(parameterValues);
			return result;
		}

		@Override
		public CompilationContext findFunctionCompilationContext(Function currentFunction,
				ValidationContext validationContext) {
			return parameterValuesBuilder.getFacade().findFunctionCompilationContext(currentFunction,
					validationContext);
		}

		@Override
		public Class<?> getActivityResultClass() {
			return Result.class;
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

		public static List<String> getParameterTypeNameOptions() {
			List<String> result = new ArrayList<String>();
			for (Class<?> clazz : Arrays.asList(String.class, Boolean.class, Integer.class, Long.class, Float.class,
					Double.class, byte[].class, java.sql.Date.class, java.sql.Time.class, java.sql.Timestamp.class)) {
				result.add(clazz.getName());
			}
			return result;
		}

	}

	public static interface ParameterValues {
		public Object getParameterValueByIndex(int i);

		public int countParameters();
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

	public static class Result {

		private int affectedRowCount;

		public Result(int affectedRowCount) {
			this.affectedRowCount = affectedRowCount;
		}

		public int getAffectedRowCount() {
			return affectedRowCount;
		}

	}
}
