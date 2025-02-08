package com.otk.jesb.instantiation;

import java.util.ArrayList;

import com.otk.jesb.Plan.ExecutionContext.Variable;

public class ListItemReplication {

	private String iterationVariableName = "current";
	private Object iterationListValue = new ArrayList<Object>();

	public String getIterationVariableName() {
		return iterationVariableName;
	}

	public void setIterationVariableName(String iterationVariableName) {
		this.iterationVariableName = iterationVariableName;
	}

	public Object getIterationListValue() {
		return iterationListValue;
	}

	public void setIterationListValue(Object iterationListValue) {
		this.iterationListValue = iterationListValue;
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