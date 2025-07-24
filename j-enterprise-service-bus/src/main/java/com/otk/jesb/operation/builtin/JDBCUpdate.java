package com.otk.jesb.operation.builtin;

import java.sql.PreparedStatement;

import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.resource.builtin.JDBCConnection;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import xy.reflect.ui.info.ResourcePath;

public class JDBCUpdate extends JDBCOperation {

	public JDBCUpdate(JDBCConnection connection) {
		super(connection);
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
			JDBCUpdate result = new JDBCUpdate(getConnection());
			result.setStatement(getStatementVariant().getValue());
			result.setParameterValues(buildParameterValues(context));
			return result;
		}

		@Override
		public Class<?> getOperationResultClass(Plan currentPlan, Step currentStep) {
			return Result.class;
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

	public static class Result {

		public final int affectedRowCount;

		public Result(int affectedRowCount) {
			this.affectedRowCount = affectedRowCount;
		}

	}
}
