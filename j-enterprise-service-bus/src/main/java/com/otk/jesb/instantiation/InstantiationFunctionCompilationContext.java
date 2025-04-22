package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.util.InstantiationUtils;

public class InstantiationFunctionCompilationContext {

	private Facade parentFacade;
	private List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
	private Class<?> functionReturnType;

	public InstantiationFunctionCompilationContext(List<VariableDeclaration> variableDeclarations, Facade parentFacade,
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

	public Function<String, String> getPrecompiler() {
		return new Function<String, String>() {
			@Override
			public String apply(String functionBody) {
				return InstantiationUtils.makeTypeNamesAbsolute(functionBody,
						InstantiationUtils.getAncestorStructureInstanceBuilders(parentFacade));
			}
		};
	}

}