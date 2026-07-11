package com.otk.jesb.operation.builtin;

import java.lang.reflect.Array;
import com.otk.jesb.PotentialError;
import com.otk.jesb.Reference;
import com.otk.jesb.ValidationError;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.resource.builtin.TextFormat;
import com.otk.jesb.resource.builtin.TextFormat.Cell;
import com.otk.jesb.resource.builtin.TextFormat.DelimitedColumn;
import com.otk.jesb.resource.builtin.TextFormat.DelimitedKind;
import com.otk.jesb.resource.builtin.TextFormat.FixedColumn;
import com.otk.jesb.resource.builtin.TextFormat.FixedKind;
import com.otk.jesb.resource.builtin.TextFormat.Record;
import com.otk.jesb.resource.builtin.TextFormat.Table;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.UpToDate.VersionAccessException;
import com.otk.jesb.solution.Solution;

import xy.reflect.ui.info.ResourcePath;

public class RenderText implements Operation {

	private Object inputObjectArray;
	private TextFormat textFormat;

	public Object getInputObjectArray() {
		return inputObjectArray;
	}

	public void setInputObjectArray(Object inputObjectArray) {
		this.inputObjectArray = inputObjectArray;
	}

	public TextFormat getTextFormat() {
		return textFormat;
	}

	public void setTextFormat(TextFormat textFormat) {
		this.textFormat = textFormat;
	}

	@Override
	public Object execute(Solution solutionInstance) throws Exception {
		Table table = new Table();
		if (textFormat.getKind() instanceof DelimitedKind) {
			DelimitedKind delimitedKind = (DelimitedKind) textFormat.getKind();
			for (int iRecord = 0; iRecord < Array.getLength(inputObjectArray); iRecord++) {
				Object inputRecordObject = Array.get(inputObjectArray, iRecord);
				Record record = new Record();
				table.getRecords().add(record);
				for (DelimitedColumn column : delimitedKind.getColumns()) {
					Object cellValue = inputRecordObject.getClass().getField(column.getName()).get(inputRecordObject);
					Cell cell = new Cell();
					record.getCells().add(cell);
					cell.setValue(cellValue);
				}
			}
		} else if (textFormat.getKind() instanceof FixedKind) {
			FixedKind fixedKind = (FixedKind) textFormat.getKind();
			for (int iRecord = 0; iRecord < Array.getLength(inputObjectArray); iRecord++) {
				Object inputRecordObject = Array.get(inputObjectArray, iRecord);
				Record record = new Record();
				table.getRecords().add(record);
				for (FixedColumn column : fixedKind.getColumns()) {
					Object cellValue = inputRecordObject.getClass().getField(column.getName()).get(inputRecordObject);
					Cell cell = new Cell();
					record.getCells().add(cell);
					cell.setValue(cellValue);
				}
			}
		} else {
			throw new UnexpectedError();
		}
		return textFormat.render(table);
	}

	public static class Builder implements OperationBuilder<RenderText> {

		private RootInstanceBuilder textFormatSchemaObjectBuilder = new RootInstanceBuilder("Input",
				new TextFormatSchemaObjectClassNameAccessor());
		private Reference<TextFormat> textFormatReference = new Reference<TextFormat>(TextFormat.class);

		public RootInstanceBuilder getTextFormatSchemaObjectBuilder() {
			return textFormatSchemaObjectBuilder;
		}

		public void setTextFormatSchemaObjectBuilder(RootInstanceBuilder textFormatSchemaObjectBuilder) {
			this.textFormatSchemaObjectBuilder = textFormatSchemaObjectBuilder;
		}

		public Reference<TextFormat> getTextFormatReference() {
			return textFormatReference;
		}

		public void setTextFormatReference(Reference<TextFormat> textFormatReference) {
			this.textFormatReference = textFormatReference;
		}

		@Override
		public RenderText build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			Solution solutionInstance = context.getSession().getSolutionInstance();
			RenderText result = new RenderText();
			result.setTextFormat(textFormatReference.resolve(solutionInstance));
			result.setInputObjectArray(textFormatSchemaObjectBuilder.build(new InstantiationContext(
					context.getVariables(), context.getPlan()
							.getValidationContext(context.getCurrentStep(), solutionInstance).getVariableDeclarations(),
					solutionInstance)));
			return result;
		}

		@Override
		public Class<?> getOperationResultClass(Solution solutionInstance, Plan currentPlan, Step currentStep) {
			return String.class;
		}

		@Override
		public void validate(boolean recursively, Solution solutionInstance, Plan plan, Step step)
				throws ValidationError {
			if (recursively) {
				TextFormat textFormat = textFormatReference.resolve(solutionInstance);
				if (textFormat != null) {
					textFormatSchemaObjectBuilder.getFacade(solutionInstance).validate(recursively,
							plan.getValidationContext(step, solutionInstance).getVariableDeclarations());
				}
			}
		}

		private class TextFormatSchemaObjectClassNameAccessor extends Accessor<Solution, String> {

			@Override
			public String get(Solution solutionInstance) {
				TextFormat textFormat = textFormatReference.resolve(solutionInstance);
				if (textFormat == null) {
					return null;
				}
				try {
					return textFormat.getUpToDateRecordSchemaClass().get(solutionInstance).getName();
				} catch (VersionAccessException e) {
					throw new PotentialError(e);
				}
			}

		}

	}

	public static class Metadata implements OperationMetadata<RenderText> {

		@Override
		public String getOperationTypeName() {
			return "Render Text";
		}

		@Override
		public String getCategoryName() {
			return "Format";
		}

		@Override
		public Class<? extends OperationBuilder<RenderText>> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(RenderText.class.getName().replace(".", "/") + ".png"));
		}
	}

}
