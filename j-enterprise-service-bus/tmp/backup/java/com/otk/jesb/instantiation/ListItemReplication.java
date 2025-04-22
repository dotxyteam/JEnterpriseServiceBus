package com.otk.jesb.instantiation;

import java.util.ArrayList;

import com.otk.jesb.Variable;

public class ListItemReplication {

	private Object iterationListValue = new ArrayList<Object>();
	private String iterationListValueTypeName;
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

	public String getIterationListValueTypeName() {
		return iterationListValueTypeName;
	}

	public void setIterationListValueTypeName(String iterationListValueTypeName) {
		this.iterationListValueTypeName = iterationListValueTypeName;
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