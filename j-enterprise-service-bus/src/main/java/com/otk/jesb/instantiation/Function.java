package com.otk.jesb.instantiation;

public class Function {
	private String functionBody;

	public Function() {
	}

	public Function(String functionBody) {
		this.functionBody = functionBody;
	}

	public String getFunctionBody() {
		return functionBody;
	}

	public void setFunctionBody(String functionBody) {
		this.functionBody = functionBody;
	}

}