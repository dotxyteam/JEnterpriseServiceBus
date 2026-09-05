package com.otk.jesb.resource.builtin;

import com.otk.jesb.ValidationError;
import com.otk.jesb.Structure.ClassicStructure;
import com.otk.jesb.Structure.SimpleElement;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.ResourceMetadata;
import com.otk.jesb.solution.Solution;

import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.PotentialError;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.util.InstantiationUtils;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.UpToDate;

import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.util.ClassUtils;

public class TextFormat extends Resource {

	public static void main(String[] args) {
		Table table = new Table();
		{
			Record record1 = new Record();
			{
				table.records.add(record1);

				Cell cell1 = new Cell();
				record1.cells.add(cell1);
				cell1.value = 123;

				Cell cell2 = new Cell();
				record1.cells.add(cell2);
				cell2.value = "azerty";
			}
			Record record2 = new Record();
			{
				table.records.add(record2);

				Cell cell3 = new Cell();
				record2.cells.add(cell3);
				cell3.value = 456;

				Cell cell4 = new Cell();
				record2.cells.add(cell4);
				cell4.value = "uiop";
			}
		}
		TextFormat textFormat = new TextFormat();
		DelimitedKind dKind = new DelimitedKind();
		{
			textFormat.kind = dKind;

			DelimitedColumn column1 = new DelimitedColumn();
			dKind.columns.add(column1);
			column1.name = "c1";
			column1.type = Integer.class;

			DelimitedColumn column2 = new DelimitedColumn();
			dKind.columns.add(column2);
			column2.name = "c2";
			column2.type = String.class;
			String renderedTable = dKind.render(table);
			System.out.println(renderedTable);

			Table table2 = dKind.parse(renderedTable);
			String renderedTable2 = dKind.render(table2);
			System.out.println(renderedTable2);
		}

		FixedKind fKind = new FixedKind();
		{
			textFormat.kind = fKind;

			FixedColumn column1 = new FixedColumn();
			fKind.columns.add(column1);
			column1.name = "c1";
			column1.type = Integer.class;
			column1.size = 10;

			FixedColumn column2 = new FixedColumn();
			fKind.columns.add(column2);
			column2.name = "c2";
			column2.type = String.class;
			column2.size = 20;

			String renderedTable = fKind.render(table);
			System.out.println(renderedTable);

			Table table2 = fKind.parse(renderedTable);
			String renderedTable2 = fKind.render(table2);
			System.out.println(renderedTable2);
		}

	}

	private Kind kind = new DelimitedKind();

	private UpToDateRecordSchemaClass upToDateRecordSchemaClass = new UpToDateRecordSchemaClass();

	public TextFormat(String name) {
		super(name);
	}

	public TextFormat() {
		super();
	}

	public Kind getKind() {
		return kind;
	}

	public void setKind(Kind kind) {
		this.kind = kind;
	}

	public String render(Table table) {
		return kind.render(table);
	}

	public Table parse(String s) {
		return kind.parse(s);
	}

	public UpToDateRecordSchemaClass getUpToDateRecordSchemaClass() {
		return upToDateRecordSchemaClass;
	}

	@Override
	public void validate(boolean recursively, Solution solutionInstance) throws ValidationError {
		super.validate(recursively, solutionInstance);
		if (recursively) {
			kind.validate(recursively, solutionInstance);
		}
	}

	@Override
	public String toString() {
		return "TextFormat [kind=" + kind + "]";
	}

	public class UpToDateRecordSchemaClass extends UpToDate<Solution, Class<?>> {
		@Override
		protected Object retrieveLastVersionIdentifier(Solution solutionInstance) {
			return TextFormat.this.toString();
		}

		@Override
		protected Class<?> obtainLatest(Solution solutionInstance, Object versionIdentifier) {
			TextFormat textFormat = TextFormat.this;
			ClassicStructure resultRowStructure = new ClassicStructure();
			if (textFormat.getKind() instanceof DelimitedKind) {
				DelimitedKind kind = (DelimitedKind) textFormat.getKind();
				for (DelimitedColumn column : kind.getColumns()) {
					SimpleElement columnElement = new SimpleElement();
					resultRowStructure.getElements().add(columnElement);
					columnElement.setName(column.getName());
					columnElement.setTypeNameOrAlias(column.getType().getName());
				}
			} else if (textFormat.getKind() instanceof FixedKind) {
				FixedKind kind = (FixedKind) textFormat.getKind();
				for (FixedColumn column : kind.getColumns()) {
					SimpleElement columnElement = new SimpleElement();
					resultRowStructure.getElements().add(columnElement);
					columnElement.setName(column.getName());
					columnElement.setTypeNameOrAlias(column.getType().getName());
				}
			} else {
				throw new UnexpectedError();
			}
			String resultRowClassName = TextFormat.class.getName() + "Record"
					+ InstantiationUtils.toRelativeTypeNameVariablePart(MiscUtils.toDigitalUniqueIdentifier(this));
			Class<?> resultRowClass;
			try {
				resultRowClass = solutionInstance.getRuntime().getInMemoryCompiler().compile(resultRowClassName,
						resultRowStructure.generateJavaTypeSourceCode(resultRowClassName, solutionInstance));
			} catch (CompilationError e) {
				throw new PotentialError(e);
			}
			return MiscUtils.getArrayType(resultRowClass);
		}
	}

	public static class Table {
		private List<Record> records = new ArrayList<TextFormat.Record>();

		public List<Record> getRecords() {
			return records;
		}

		public void setRecords(List<Record> records) {
			this.records = records;
		}
	}

	public static class Record {
		private List<Cell> cells = new ArrayList<TextFormat.Cell>();

		public List<Cell> getCells() {
			return cells;
		}

		public void setCells(List<Cell> cells) {
			this.cells = cells;
		}
	}

	public static class Cell {
		private Object value;

		public Object getValue() {
			return value;
		}

		public void setValue(Object value) {
			this.value = value;
		}
	}

	public static abstract class Kind {
		protected abstract String render(Table table);

		public abstract void validate(boolean recursively, Solution solutionInstance) throws ValidationError;

		protected abstract Table parse(String s);
	}

	public static class DelimitedKind extends Kind {

		public enum RecordDelimiter {
			CR, LF, CRLF;

			public String getStringValue() {
				if (this == CR) {
					return "\n";
				} else if (this == LF) {
					return "\r";
				} else if (this == CRLF) {
					return "\n\r";
				} else {
					throw new UnexpectedError();
				}
			}

		}

		private String columnSeparator = ",";
		private RecordDelimiter recordDelimiter = RecordDelimiter.CR;
		private List<DelimitedColumn> columns = new ArrayList<DelimitedColumn>();

		public String getColumnSeparator() {
			return columnSeparator;
		}

		public void setColumnSeparator(String columnSeparator) {
			this.columnSeparator = columnSeparator;
		}

		public RecordDelimiter getRecordDelimiter() {
			return recordDelimiter;
		}

		public void setRecordDelimiter(RecordDelimiter recordDelimiter) {
			this.recordDelimiter = recordDelimiter;
		}

		public List<DelimitedColumn> getColumns() {
			return columns;
		}

		public void setColumns(List<DelimitedColumn> columns) {
			this.columns = columns;
		}

		@Override
		protected String render(Table table) {
			StringBuilder result = new StringBuilder();
			for (Record record : table.records) {
				int iCell = 0;
				for (Cell cell : record.cells) {
					result.append(cell.value.toString());
					if ((iCell + 1) < record.cells.size()) {
						result.append(columnSeparator);
					}
					iCell++;
				}
				result.append(recordDelimiter.getStringValue());
			}
			return result.toString();
		}

		@Override
		protected Table parse(String s) {
			Table result = new Table();
			String[] recordStrings = s.split(MiscUtils.escapeRegex(recordDelimiter.getStringValue()), -1);
			for (String recordString : recordStrings) {
				Record resultRecord = new Record();
				result.records.add(resultRecord);
				String[] cellStrings = recordString.split(MiscUtils.escapeRegex(columnSeparator), -1);
				if (cellStrings.length != columns.size()) {
					throw new PotentialError("Number of record cells (" + cellStrings.length
							+ ") is different from number of text format defined columns (" + columns.size() + ")");
				}
				int iColumn = 0;
				for (String cellString : cellStrings) {
					Cell cell = new Cell();
					resultRecord.cells.add(cell);
					DelimitedColumn column = columns.get(iColumn);
					if (column.type == Character.class) {
						cellString = cellString.trim();
						if (cellString.length() != 1) {
							throw new PotentialError("Invalid value: '" + cellString + "'. 1 character is expected");
						}
						cell.value = cellString.charAt(0);
					} else {
						try {
							cell.value = column.type.getConstructor(new Class[] { String.class })
									.newInstance(cellString);
						} catch (Exception e) {
							throw new PotentialError(e);
						}
					}
					iColumn++;
				}
			}
			return result;
		}

		@Override
		public void validate(boolean recursively, Solution solutionInstance) throws ValidationError {
			if (columns.size() == 0) {
				throw new ValidationError("Schema not specified (no column defined)");
			}
			if (columnSeparator.length() == 0) {
				throw new ValidationError("Column separator not specified");
			}
		}

		@Override
		public String toString() {
			return "DelimitedKind [columnSeparator=" + columnSeparator + ", recordDelimiter=" + recordDelimiter
					+ ", columns=" + columns + "]";
		}

	}

	public static class FixedKind extends Kind {
		private char fillCharacter = ' ';
		private List<FixedColumn> columns = new ArrayList<FixedColumn>();

		public char getFillCharacter() {
			return fillCharacter;
		}

		public void setFillCharacter(char fillCharacter) {
			this.fillCharacter = fillCharacter;
		}

		public List<FixedColumn> getColumns() {
			return columns;
		}

		public void setColumns(List<FixedColumn> columns) {
			this.columns = columns;
		}

		public int calculateRecordLength() {
			int result = 0;
			for (FixedColumn column : columns) {
				result += column.getSize();
			}
			return result;
		}

		@Override
		protected String render(Table table) {
			StringBuilder result = new StringBuilder();
			for (Record record : table.records) {
				int iColumn = 0;
				for (Cell cell : record.cells) {
					FixedColumn column = columns.get(iColumn);
					String cellValueString = cell.value.toString();
					if (cellValueString.length() < column.getSize()) {
						int paddingLength = column.getSize() - cellValueString.length();
						for (int iPadding = 0; iPadding < paddingLength; iPadding++) {
							cellValueString += fillCharacter;
						}
					} else if (column.getSize() < cellValueString.length()) {
						/* If the column size is too small for the cell value then truncate. */
						cellValueString = cellValueString.substring(0, column.getSize());
					}
					result.append(cellValueString);
					iColumn++;
				}
			}
			return result.toString();
		}

		@Override
		protected Table parse(String s) {
			Table result = new Table();
			int recordLength = calculateRecordLength();
			if ((s.length() % recordLength) != 0) {
				throw new PotentialError("The length of the input string (" + s.length()
						+ ") is not a multiple of the record length (" + recordLength + ")");
			}
			int recordCount = s.length() / recordLength;
			for (int iRecord = 0; iRecord < recordCount; iRecord++) {
				Record resultRecord = new Record();
				result.records.add(resultRecord);
				for (int iColumn = 0; iColumn < columns.size(); iColumn++) {
					FixedColumn column = columns.get(iColumn);
					Cell cell = new Cell();
					resultRecord.cells.add(cell);
					int cellValueStart = iRecord * recordLength;
					for (int iColumn2 = 0; iColumn2 < iColumn; iColumn2++) {
						cellValueStart += columns.get(iColumn2).getSize();
					}
					int cellValueEnd = cellValueStart + columns.get(iColumn).getSize();
					String cellValueString = s.substring(cellValueStart, cellValueEnd);
					// trimming
					while (cellValueString.endsWith(Character.toString(fillCharacter))) {
						cellValueString = cellValueString.substring(0, cellValueString.length() - 1);
					}
					if (column.type == Character.class) {
						if (cellValueString.length() != 1) {
							throw new PotentialError(
									"Invalid value: '" + cellValueString + "'. 1 character is expected");
						}
						cell.value = cellValueString.charAt(0);
					} else {
						try {
							cell.value = column.type.getConstructor(new Class[] { String.class })
									.newInstance(cellValueString);
						} catch (Exception e) {
							throw new PotentialError(e);
						}
					}
				}
			}
			return result;
		}

		@Override
		public void validate(boolean recursively, Solution solutionInstance) throws ValidationError {
			if (columns.size() == 0) {
				throw new ValidationError("Schema not specified (no column defined)");
			}
		}

		@Override
		public String toString() {
			return "FixedKind [fillCharacter=" + fillCharacter + ", columns=" + columns + "]";
		}

	}

	public static class DelimitedColumn {
		private String name;
		private Class<?> type = String.class;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Class<?> getType() {
			return type;
		}

		public void setType(Class<?> type) {
			this.type = type;
		}

		public List<Class<?>> getTypeOptions() {
			List<Class<?>> result = new ArrayList<Class<?>>();
			result.add(String.class);
			for (Class<?> clazz : ClassUtils.PRIMITIVE_CLASSES) {
				result.add(ClassUtils.primitiveToWrapperClass(clazz));
			}
			return result;
		}

		@Override
		public String toString() {
			return "DelimitedColumn [name=" + name + ", type=" + type + "]";
		}
	}

	public static class FixedColumn {
		private String name;
		private Class<?> type = String.class;
		private int size = 10;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Class<?> getType() {
			return type;
		}

		public void setType(Class<?> type) {
			this.type = type;
		}

		public List<Class<?>> getTypeOptions() {
			List<Class<?>> result = new ArrayList<Class<?>>();
			result.add(String.class);
			for (Class<?> clazz : ClassUtils.PRIMITIVE_CLASSES) {
				result.add(ClassUtils.primitiveToWrapperClass(clazz));
			}
			return result;
		}

		public int getSize() {
			return size;
		}

		public void setSize(int size) {
			this.size = size;
		}

		@Override
		public String toString() {
			return "FixedColumn [name=" + name + ", type=" + type + ", size=" + size + "]";
		}
	}

	public static class Metadata implements ResourceMetadata {

		@Override
		public ResourcePath getResourceIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(TextFormat.class.getName().replace(".", "/") + ".png"));
		}

		@Override
		public Class<? extends Resource> getResourceClass() {
			return TextFormat.class;
		}

		@Override
		public String getResourceTypeName() {
			return "Text Format";
		}

	}

}
