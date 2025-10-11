package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Allows to specify a case related to a parent {@link InitializationSwitch}.
 * 
 * @author olitank
 *
 */
public class InitializationCase {
	private List<ParameterInitializer> parameterInitializers = new ArrayList<ParameterInitializer>();
	private List<FieldInitializer> fieldInitializers = new ArrayList<FieldInitializer>();
	private List<ListItemInitializer> listItemInitializers = new ArrayList<ListItemInitializer>();
	private List<InitializationSwitch> initializationSwitches = new ArrayList<InitializationSwitch>();

	public List<ParameterInitializer> getParameterInitializers() {
		return parameterInitializers;
	}

	public void setParameterInitializers(List<ParameterInitializer> parameterInitializers) {
		this.parameterInitializers = parameterInitializers;
	}

	public List<FieldInitializer> getFieldInitializers() {
		return fieldInitializers;
	}

	public void setFieldInitializers(List<FieldInitializer> fieldInitializers) {
		this.fieldInitializers = fieldInitializers;
	}

	public List<ListItemInitializer> getListItemInitializers() {
		return listItemInitializers;
	}

	public void setListItemInitializers(List<ListItemInitializer> listItemInitializers) {
		this.listItemInitializers = listItemInitializers;
	}

	public List<InitializationSwitch> getInitializationSwitches() {
		return initializationSwitches;
	}

	public void setInitializationSwitches(List<InitializationSwitch> initializationSwitches) {
		this.initializationSwitches = initializationSwitches;
	}

	public ParameterInitializer getParameterInitializer(int parameterPosition) {
		for (ParameterInitializer parameterInitializer : parameterInitializers) {
			if (parameterInitializer.getParameterPosition() == parameterPosition) {
				return parameterInitializer;
			}
		}
		return null;
	}

	public void removeParameterInitializer(int parameterPosition) {
		for (Iterator<ParameterInitializer> it = parameterInitializers.iterator(); it.hasNext();) {
			ParameterInitializer parameterInitializer = it.next();
			if (parameterInitializer.getParameterPosition() == parameterPosition) {
				it.remove();
			}
		}
	}

	public FieldInitializer getFieldInitializer(String fieldName) {
		for (FieldInitializer fieldInitializer : fieldInitializers) {
			if (fieldInitializer.getFieldName().equals(fieldName)) {
				return fieldInitializer;
			}
		}
		return null;
	}

	public void removeFieldInitializer(String fieldName) {
		for (Iterator<FieldInitializer> it = fieldInitializers.iterator(); it.hasNext();) {
			FieldInitializer fieldInitializer = it.next();
			if (fieldInitializer.getFieldName().equals(fieldName)) {
				it.remove();
			}
		}
	}

	public ListItemInitializer getListItemInitializer(int index) {
		for (ListItemInitializer listItemInitializer : listItemInitializers) {
			if (listItemInitializer.getIndex() == index) {
				return listItemInitializer;
			}
		}
		return null;
	}

	public void removeListItemInitializer(int index) {
		for (Iterator<ListItemInitializer> it = listItemInitializers.iterator(); it.hasNext();) {
			ListItemInitializer listItemInitializer = it.next();
			if (listItemInitializer.getIndex() == index) {
				it.remove();
			}
		}
	}

	public static InstantiationFunction createDefaultCondition() {
		return new InstantiationFunction("return false;");
	}

}