package com.otk.jesb.instantiation;

public class ParameterInitializer {

	private int parameterPosition;
	private String parameterTypeName;
	private Object parameterValue;

	public ParameterInitializer() {
	}

	public ParameterInitializer(int parameterPosition, String parameterTypeName, Object parameterValue) {
		super();
		this.parameterPosition = parameterPosition;
		this.parameterTypeName = parameterTypeName;
		this.parameterValue = parameterValue;
	}

	public int getParameterPosition() {
		return parameterPosition;
	}

	public void setParameterPosition(int parameterPosition) {
		this.parameterPosition = parameterPosition;
	}

	public String getParameterTypeName() {
		return parameterTypeName;
	}

	public void setParameterTypeName(String parameterTypeName) {
		this.parameterTypeName = parameterTypeName;
	}

	public Object getParameterValue() {
		return parameterValue;
	}

	public void setParameterValue(Object parameterValue) {
		this.parameterValue = parameterValue;
	}

}