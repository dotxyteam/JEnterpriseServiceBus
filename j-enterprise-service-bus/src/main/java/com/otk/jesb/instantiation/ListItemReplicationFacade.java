package com.otk.jesb.instantiation;

import java.util.ArrayList;

import com.otk.jesb.Function;
import com.otk.jesb.meta.TypeInfoProvider;
import com.otk.jesb.util.InstantiationUtils;

public class ListItemReplicationFacade {

	private ListItemReplication listItemReplication;

	public ListItemReplicationFacade() {
		this.listItemReplication = new ListItemReplication();
	}

	public ListItemReplicationFacade(ListItemReplication listItemReplication) {
		this.listItemReplication = listItemReplication;
	}

	public ListItemReplication getUnderlying() {
		return listItemReplication;
	}

	public String getIterationVariableName() {
		return listItemReplication.getIterationVariableName();
	}

	public void setIterationVariableName(String iterationVariableName) {
		listItemReplication.setIterationVariableName(iterationVariableName);
	}

	public Object getIterationListValue() {
		return InstantiationUtils.maintainInterpretableValue(listItemReplication.getIterationListValue(),
				TypeInfoProvider.getTypeInfo(Object.class.getName()));
	}

	public void setIterationListValue(Object iterationListValue) {
		listItemReplication.setIterationListValue(iterationListValue);
	}

	public String getIterationListValueTypeName() {
		return listItemReplication.getIterationListValueTypeName();
	}

	public void setIterationListValueTypeName(String iterationListValueTypeName) {
		listItemReplication.setIterationListValueTypeName(iterationListValueTypeName);
	}

	public String getIterationVariableTypeName() {
		return listItemReplication.getIterationVariableTypeName();
	}

	public void setIterationVariableTypeName(String iterationVariableTypeName) {
		listItemReplication.setIterationVariableTypeName(iterationVariableTypeName);
	}

	public ValueMode getIterationListValueMode() {
		return InstantiationUtils.getValueMode(listItemReplication.getIterationListValue());
	}

	public void setIterationListValueMode(ValueMode valueMode) {
		Object iterationListValue;
		if (valueMode == ValueMode.FUNCTION) {
			String functionBody;
			if (getIterationListValueTypeName() != null) {
				Class<?> listClass = TypeInfoProvider.getClass(getIterationListValueTypeName());
				if (listClass.isArray()) {
					functionBody = "return new " + listClass.getComponentType().getName() + "[]{};";
				} else {
					functionBody = "return new " + listClass.getName() + "();";
				}
			} else {
				functionBody = "return new " + ArrayList.class.getName() + "<Object>();";
			}
			iterationListValue = new Function(functionBody);
		} else {
			iterationListValue = new ArrayList<Object>();
		}
		listItemReplication.setIterationListValue(iterationListValue);
	}

	public Class<?> getIterationListValueClass() {
		return (getIterationListValueTypeName() != null) ? TypeInfoProvider.getClass(getIterationListValueTypeName())
				: Object.class;
	}

	public Class<?> getIterationVariableClass() {
		Class<?> listClass = getIterationListValueClass();
		return (getIterationVariableTypeName() != null) ? TypeInfoProvider.getClass(getIterationVariableTypeName())
				: (listClass.isArray() ? listClass.getComponentType() : Object.class);
	}

}