package com.otk.jesb.operation.builtin;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.operation.builtin.JDBCQuery.ColumnDefinition;
import com.otk.jesb.resource.builtin.JDBCConnection;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.Reference;
import com.otk.jesb.Session;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.solution.Solution;

import xy.reflect.ui.info.ResourcePath;

public class JDBCGeneric implements Operation {

	protected Session session;
	protected JDBCConnection connection;
	protected String statement;

	public JDBCGeneric(Session session, JDBCConnection connection) {
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

	protected Statement prepare() throws Exception {
		Connection connectionInstance = connection.during(session);
		Statement statement = connectionInstance.createStatement();
		return statement;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object execute(Solution solutionInstance) throws Throwable {
		Statement statementExecutor = prepare();
		if (statementExecutor.execute(statement)) {
			ResultSet resultSet = statementExecutor.getResultSet();
			List<ColumnDefinition> columns = JDBCQuery.retrieveResultColumnDefinitions(resultSet.getMetaData());
			List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
			while (resultSet.next()) {
				Map<String, Object> row = new HashMap<String, Object>();
				for (int iColumn = 1; iColumn < columns.size(); iColumn++) {
					row.put(columns.get(iColumn - 1).getColumnName(), resultSet.getObject(iColumn));
				}
				rows.add(row);
			}
			return new GenericResult(columns.toArray(new String[columns.size()]), rows.toArray(new Map[rows.size()]));
		} else {
			int affectedRows = statementExecutor.getUpdateCount();
			String columnName = "<affectedRows>";
			return new GenericResult(new String[] { columnName },
					new Map[] { Collections.singletonMap(columnName, affectedRows) });
		}
	}

	public static class Metadata implements OperationMetadata<JDBCGeneric> {

		@Override
		public String getOperationTypeName() {
			return "JDBC Generic";
		}

		@Override
		public String getCategoryName() {
			return "JDBC";
		}

		@Override
		public Class<? extends OperationBuilder<JDBCGeneric>> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(JDBCGeneric.class.getName().replace(".", "/") + ".png"));
		}

	}

	public static class Builder implements OperationBuilder<JDBCGeneric> {

		protected Reference<JDBCConnection> connectionReference = new Reference<JDBCConnection>(JDBCConnection.class);
		protected RootInstanceBuilder statementBuilder = new RootInstanceBuilder("Statement", String.class.getName());

		public Reference<JDBCConnection> getConnectionReference() {
			return connectionReference;
		}

		public void setConnectionReference(Reference<JDBCConnection> connectionReference) {
			this.connectionReference = connectionReference;
		}

		public RootInstanceBuilder getStatementBuilder() {
			return statementBuilder;
		}

		public void setStatementBuilder(RootInstanceBuilder statementBuilder) {
			if (statementBuilder == null) {
				throw new UnexpectedError();
			}
			this.statementBuilder = statementBuilder;
		}

		protected JDBCConnection getConnection(Solution solutionInstance) {
			return connectionReference.resolve(solutionInstance);
		}

		@Override
		public Class<?> getOperationResultClass(Solution solutionInstance, Plan currentPlan, Step currentStep) {
			return GenericResult.class;
		}

		@Override
		public JDBCGeneric build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			Solution solutionInstance = context.getSession().getSolutionInstance();
			JDBCGeneric result = new JDBCGeneric(context.getSession(), getConnection(solutionInstance));
			result.setStatement((String) getStatementBuilder().build(new InstantiationContext(
					context.getVariables(), context.getPlan()
							.getValidationContext(context.getCurrentStep(), solutionInstance).getVariableDeclarations(),
					solutionInstance)));
			return result;
		}

		@Override
		public void validate(boolean recursively, Solution solutionInstance, Plan plan, Step step)
				throws ValidationError {
			JDBCConnection connection = getConnection(solutionInstance);
			if (connection == null) {
				throw new ValidationError("Failed to resolve the connection reference");
			}
			if (recursively) {
				if (statementBuilder != null) {
					try {
						statementBuilder.getFacade(solutionInstance).validate(recursively,
								plan.getValidationContext(step, solutionInstance).getVariableDeclarations());
					} catch (ValidationError e) {
						throw new ValidationError("Failed to validate the statement builder", e);
					}
				}
			}
		}

	}

	public static class GenericResult {
		public final Map<String, Object>[] rows;
		public final String[] columnNames;

		public GenericResult(String[] columnNames, Map<String, Object>[] rows) {
			this.columnNames = columnNames;
			this.rows = rows;
		}

	}
}
