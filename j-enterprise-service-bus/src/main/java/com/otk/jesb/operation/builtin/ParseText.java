package com.otk.jesb.operation.builtin;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.Reference;
import com.otk.jesb.ValidationError;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.resource.builtin.TextFormat;
import com.otk.jesb.resource.builtin.TextFormat.Record;
import com.otk.jesb.resource.builtin.TextFormat.Table;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.util.UpToDate.VersionAccessException;
import com.otk.jesb.solution.Solution;

import xy.reflect.ui.info.ResourcePath;

public class ParseText implements Operation {

	private Class<?> resultRowClass;
	private TextFormat textFormat;
	private String text;
	private int skippedHeaderRecordCount;
	private boolean blankRecordSkipped;

	public boolean isBlankRecordSkipped() {
		return blankRecordSkipped;
	}

	public void setBlankRecordSkipped(boolean blankRecordSkipped) {
		this.blankRecordSkipped = blankRecordSkipped;
	}

	public int getSkippedHeaderRecordCount() {
		return skippedHeaderRecordCount;
	}

	public void setSkippedHeaderRecordCount(int skippedHeaderRecordCount) {
		this.skippedHeaderRecordCount = skippedHeaderRecordCount;
	}

	public Class<?> getResultRowClass() {
		return resultRowClass;
	}

	public void setResultRowClass(Class<?> resultRowClass) {
		this.resultRowClass = resultRowClass;
	}

	public TextFormat getTextFormat() {
		return textFormat;
	}

	public void setTextFormat(TextFormat textFormat) {
		this.textFormat = textFormat;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@Override
	public Object execute(Solution solutionInstance) throws Exception {
		List<Object> resultRecordObjects = new ArrayList<Object>();
		Table table = textFormat.parse(text, skippedHeaderRecordCount, blankRecordSkipped);
		for (Record record : table.getRecords()) {
			Object resultRecordObject = resultRowClass.getConstructors()[0]
					.newInstance(record.getCells().stream().map(cell -> cell.getValue()).toArray());
			resultRecordObjects.add(resultRecordObject);
		}
		Object result = Array.newInstance(resultRowClass, resultRecordObjects.size());
		int iRecord = 0;
		for (Object resultRecordObject : resultRecordObjects) {
			Array.set(result, iRecord, resultRecordObject);
			iRecord++;
		}
		return result;
	}

	public static class Builder implements OperationBuilder<ParseText> {

		private RootInstanceBuilder inputBuilder = new RootInstanceBuilder("Input", Input.class.getName());
		private Reference<TextFormat> textFormatReference = new Reference<TextFormat>(TextFormat.class);

		public RootInstanceBuilder getInputBuilder() {
			return inputBuilder;
		}

		public void setInputBuilder(RootInstanceBuilder inputBuilder) {
			this.inputBuilder = inputBuilder;
		}

		public Reference<TextFormat> getTextFormatReference() {
			return textFormatReference;
		}

		public void setTextFormatReference(Reference<TextFormat> textFormatReference) {
			this.textFormatReference = textFormatReference;
		}

		@Override
		public ParseText build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			Solution solutionInstance = context.getSession().getSolutionInstance();
			ParseText result = new ParseText();
			TextFormat textFormat = textFormatReference.resolve(solutionInstance);
			result.setResultRowClass(
					textFormat.getUpToDateRecordSchemaClass().get(solutionInstance).getComponentType());
			result.setTextFormat(textFormatReference.resolve(solutionInstance));
			Input input = (Input) inputBuilder.build(new InstantiationContext(
					context.getVariables(), context.getPlan()
							.getValidationContext(context.getCurrentStep(), solutionInstance).getVariableDeclarations(),
					solutionInstance));
			result.setText(input.getText());
			result.setSkippedHeaderRecordCount(input.getSkippedHeaderRecordCount());
			result.setBlankRecordSkipped(input.isSkipBlankRecords());
			return result;
		}

		@Override
		public Class<?> getOperationResultClass(Solution solutionInstance, Plan currentPlan, Step currentStep) {
			TextFormat textFormat = textFormatReference.resolve(solutionInstance);
			if (textFormat == null) {
				return null;
			}
			try {
				return textFormat.getUpToDateRecordSchemaClass().get(solutionInstance);
			} catch (VersionAccessException e) {
				return null;
			}
		}

		@Override
		public void validate(boolean recursively, Solution solutionInstance, Plan plan, Step step)
				throws ValidationError {
			if (recursively) {
				inputBuilder.getFacade(solutionInstance).validate(recursively,
						plan.getValidationContext(step, solutionInstance).getVariableDeclarations());
			}
		}

		public static class Input {
			private String text;
			private int skippedHeaderRecordCount = 0;
			private boolean skipBlankRecords = false;

			public Input(String text) {
				this.text = text;
			}

			public boolean isSkipBlankRecords() {
				return skipBlankRecords;
			}

			public void setSkipBlankRecords(boolean skipBlankRecords) {
				this.skipBlankRecords = skipBlankRecords;
			}

			public int getSkippedHeaderRecordCount() {
				return skippedHeaderRecordCount;
			}

			public void setSkippedHeaderRecordCount(int skippedHeaderRecordCount) {
				this.skippedHeaderRecordCount = skippedHeaderRecordCount;
			}

			public String getText() {
				return text;
			}

		}

	}

	public static class Metadata implements OperationMetadata<ParseText> {

		@Override
		public String getOperationTypeName() {
			return "Parse Text";
		}

		@Override
		public String getCategoryName() {
			return "Text Format";
		}

		@Override
		public Class<? extends OperationBuilder<ParseText>> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(ParseText.class.getName().replace(".", "/") + ".png"));
		}
	}

}
