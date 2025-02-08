package com.otk.jesb.instantiation;

public class FieldInitializer {

	private String fieldName;
	private Object fieldValue;
	private Function condition;

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

	public Function getCondition() {
		return condition;
	}

	public void setCondition(Function condition) {
		this.condition = condition;
	}

	public Object getFieldValue() {
		return fieldValue;
	}

	public void setFieldValue(Object fieldValue) {
		this.fieldValue = fieldValue;
	}

}