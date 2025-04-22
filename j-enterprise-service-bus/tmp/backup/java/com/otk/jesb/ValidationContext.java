package com.otk.jesb;

import java.util.ArrayList;
import java.util.List;

public class ValidationContext {

	private List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();

	public ValidationContext() {
	}

	public ValidationContext(List<VariableDeclaration> variableDeclarations) {
		this.variableDeclarations = variableDeclarations;
	}

	public ValidationContext(ValidationContext parentContext, VariableDeclaration newDeclaration) {
		variableDeclarations.addAll(parentContext.getVariableDeclarations());
		variableDeclarations.add(newDeclaration);
	}

	public List<VariableDeclaration> getVariableDeclarations() {
		return variableDeclarations;
	}

}