package com.otk.jesb.instantiation;

import java.util.ArrayList;

import com.otk.jesb.meta.TypeInfoProvider;
import com.otk.jesb.util.MiscUtils;

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
		return MiscUtils.maintainInterpretableValue(listItemReplication.getIterationListValue(),
				TypeInfoProvider.getTypeInfo(Object.class.getName()));
	}

	public void setIterationListValue(Object iterationListValue) {
		listItemReplication.setIterationListValue(iterationListValue);
	}

	public ValueMode getIterationListValueMode() {
		return MiscUtils.getValueMode(listItemReplication.getIterationListValue());
	}

	public void setIterationListValueMode(ValueMode valueMode) {
		Object iterationListValue;
		if (valueMode == ValueMode.FUNCTION) {
			String functionBody = "return new " + ArrayList.class.getName() + "<Object>();";
			iterationListValue = new Function(functionBody);
		} else {
			iterationListValue = new ArrayList<Object>();
		}
		listItemReplication.setIterationListValue(iterationListValue);
	}
}