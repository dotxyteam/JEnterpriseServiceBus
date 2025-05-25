package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.util.InstantiationUtils;

public class InstantiationFunctionCompilationContext {

	private Facade parentFacade;
	private List<VariableDeclaration> baseVariableDeclarations = new ArrayList<VariableDeclaration>();

	public InstantiationFunctionCompilationContext(List<VariableDeclaration> baseVariableDeclarations,
			Facade parentFacade) {
		this.parentFacade = parentFacade;
		this.baseVariableDeclarations = baseVariableDeclarations;
	}

	public List<VariableDeclaration> getVariableDeclarations() {
		List<VariableDeclaration> result = new ArrayList<VariableDeclaration>(baseVariableDeclarations);
		result.addAll(parentFacade.getAdditionalVariableDeclarations(baseVariableDeclarations));
		return result;
	}

	public Facade getParentFacade() {
		return parentFacade;
	}

	public Class<?> getFunctionReturnType(InstantiationFunction function) {
		return parentFacade.getFunctionReturnType(function, baseVariableDeclarations);
	}

	public Function<String, String> getPrecompiler() {
		return new Function<String, String>() {
			@Override
			public String apply(String functionBody) {
				return InstantiationUtils.makeTypeNamesAbsolute(functionBody,
						InstantiationUtils.getAncestorStructuredInstanceBuilders(parentFacade));
			}
		};
	}

}