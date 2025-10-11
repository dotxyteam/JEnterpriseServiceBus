package com.otk.jesb.instantiation;

/**
 * Allows to specify an object field value.
 * 
 * @author olitank
 *
 */
public class FieldInitializer {

	private String fieldName;
	private Object fieldValue;
	private InstantiationFunction condition;

	public FieldInitializer() {
	}

	public FieldInitializer(String fieldName, Object fieldValue) {
		this.fieldName = fieldName;
		this.fieldValue = fieldValue;
	}

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public InstantiationFunction getCondition() {
		return condition;
	}

	public void setCondition(InstantiationFunction condition) {
		this.condition = condition;
	}

	public Object getFieldValue() {
		return fieldValue;
	}

	public void setFieldValue(Object fieldValue) {
		this.fieldValue = fieldValue;
	}

}