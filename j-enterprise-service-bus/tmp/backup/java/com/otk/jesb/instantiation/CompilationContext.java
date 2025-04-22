package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.VariableDeclaration;

public class CompilationContext {

	private Facade parentFacade;
	private List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
	private Class<?> functionReturnType;

	public CompilationContext(List<VariableDeclaration> variableDeclarations, Facade parentFacade,
			Class<?> functionReturnType) {
		this.parentFacade = parentFacade;
		this.variableDeclarations = variableDeclarations;
		this.functionReturnType = functionReturnType;
	}

	public List<VariableDeclaration> getVariableDeclarations() {
		return variableDeclarations;
	}

	public Facade getParentFacade() {
		return parentFacade;
	}

	public Class<?> getFunctionReturnType() {
		return functionReturnType;
	}

}