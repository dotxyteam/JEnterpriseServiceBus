package com.otk.jesb.instantiation;

import com.otk.jesb.Variable;

/**
 * Allows to specify a range of item values.
 * 
 * @author olitank
 *
 */
public class ListItemReplication {

	private Object iterationListValue = new InstantiationFunction("return ?;");
	private String iterationVariableName = "current";
	private String iterationVariableTypeName;

	public Object getIterationListValue() {
		return iterationListValue;
	}

	public void setIterationListValue(Object iterationListValue) {
		this.iterationListValue = iterationListValue;
	}

	public String getIterationVariableName() {
		return iterationVariableName;
	}

	public void setIterationVariableName(String iterationVariableName) {
		this.iterationVariableName = iterationVariableName;
	}

	public String getIterationVariableTypeName() {
		return iterationVariableTypeName;
	}

	public void setIterationVariableTypeName(String iterationVariableTypeName) {
		this.iterationVariableTypeName = iterationVariableTypeName;
	}

	public static class IterationVariable implements Variable {

		private ListItemReplication itemReplication;
		private Object iterationVariableValue;

		public IterationVariable(ListItemReplication itemReplication, Object iterationVariableValue) {
			this.itemReplication = itemReplication;
			this.iterationVariableValue = iterationVariableValue;
		}

		@Override
		public Object getValue() {
			return iterationVariableValue;
		}

		@Override
		public String getName() {
			return itemReplication.getIterationVariableName();
		}

	}

}