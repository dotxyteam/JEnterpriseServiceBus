package com.otk.jesb.operation.builtin;

import java.sql.PreparedStatement;

import com.otk.jesb.Session;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.resource.builtin.JDBCConnection;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import xy.reflect.ui.info.ResourcePath;

public class JDBCUpdate extends JDBCOperation {

	public JDBCUpdate(Session session, JDBCConnection connection) {
		super(session, connection);
	}

	@Override
	public Object execute() throws Exception {
		PreparedStatement preparedStatement = prepare();
		int affectedRows = preparedStatement.executeUpdate();
		return new Result(affectedRows);
	}

	public static class Metadata implements OperationMetadata<JDBCUpdate> {

		@Override
		public String getOperationTypeName() {
			return "JDBC Update";
		}

		@Override
		public String getCategoryName() {
			return "JDBC";
		}

		@Override
		public Class<? extends OperationBuilder<JDBCUpdate>> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(JDBCUpdate.class.getName().replace(".", "/") + ".png"));
		}

	}

	public static class Builder extends JDBCOperation.Builder<JDBCUpdate> {

		@Override
		public JDBCUpdate build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			JDBCUpdate result = new JDBCUpdate(context.getSession(), getConnection());
			result.setStatement(getStatementVariant().getValue());
			result.setParameterValues(buildParameterValues(context));
			return result;
		}

		@Override
		public Class<?> getOperationResultClass(Plan currentPlan, Step currentStep) {
			return Result.class;
		}

	}

	public static class Result {

		public final int affectedRowCount;

		public Result(int affectedRowCount) {
			this.affectedRowCount = affectedRowCount;
		}

	}
}
