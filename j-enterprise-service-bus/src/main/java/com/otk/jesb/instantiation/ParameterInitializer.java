package com.otk.jesb.instantiation;

public class ParameterInitializer {

	private int parameterPosition;
	private Object parameterValue;

	public ParameterInitializer() {
	}

	public ParameterInitializer(int parameterPosition, Object parameterValue) {
		this.parameterPosition = parameterPosition;
		this.parameterValue = parameterValue;
	}

	public int getParameterPosition() {
		return parameterPosition;
	}

	public void setParameterPosition(int parameterPosition) {
		this.parameterPosition = parameterPosition;
	}

	public Object getParameterValue() {
		return parameterValue;
	}

	public void setParameterValue(Object parameterValue) {
		this.parameterValue = parameterValue;
	}

}