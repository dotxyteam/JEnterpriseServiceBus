package com.otk.jesb.activity.builtin;

import java.sql.PreparedStatement;
import com.otk.jesb.activity.Activity;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;
import com.otk.jesb.resource.builtin.JDBCConnection;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import xy.reflect.ui.info.ResourcePath;

public class JDBCUpdateActivity extends JDBCActivity {

	public JDBCUpdateActivity(JDBCConnection connection) {
		super(connection);
	}

	@Override
	public Object execute() throws Exception {
		PreparedStatement preparedStatement = prepare();
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

	public static class Builder extends JDBCActivity.Builder {

		@Override
		public Activity build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			JDBCUpdateActivity result = new JDBCUpdateActivity(getConnection());
			result.setStatement(getStatement());
			result.setParameterValues(buildParameterValues(context));
			return result;
		}

		@Override
		public Class<?> getActivityResultClass(Plan currentPlan, Step currentStep) {
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

		private int affectedRowCount;

		public Result(int affectedRowCount) {
			this.affectedRowCount = affectedRowCount;
		}

		public int getAffectedRowCount() {
			return affectedRowCount;
		}

	}
}
