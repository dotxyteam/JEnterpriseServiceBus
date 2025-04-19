package com.otk.jesb;

import java.util.ArrayList;
import java.util.List;

public class ValidationContext {

	private List<ValidationContext.VariableDeclaration> variableDeclarations = new ArrayList<ValidationContext.VariableDeclaration>();

	public ValidationContext() {
	}

	public ValidationContext(List<ValidationContext.VariableDeclaration> variableDeclarations) {
		this.variableDeclarations = variableDeclarations;
	}

	public ValidationContext(ValidationContext parentContext, ValidationContext.VariableDeclaration newDeclaration) {
		variableDeclarations.addAll(parentContext.getVariableDeclarations());
		variableDeclarations.add(newDeclaration);
	}

	public List<ValidationContext.VariableDeclaration> getVariableDeclarations() {
		return variableDeclarations;
	}

	public interface VariableDeclaration {

		Class<?> getVariableType();

		String getVariableName();

	}

}