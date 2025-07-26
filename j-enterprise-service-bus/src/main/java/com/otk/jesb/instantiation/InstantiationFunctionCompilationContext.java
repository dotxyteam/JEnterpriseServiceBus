package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.List;
import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.Function.Precompiler;
import com.otk.jesb.util.InstantiationUtils;

public class InstantiationFunctionCompilationContext {

	private Facade parentFacade;
	private List<VariableDeclaration> baseVariableDeclarations = new ArrayList<VariableDeclaration>();

	public InstantiationFunctionCompilationContext(List<VariableDeclaration> baseVariableDeclarations,
			Facade parentFacade) {
		this.parentFacade = parentFacade;
		this.baseVariableDeclarations = baseVariableDeclarations;
	}

	public List<VariableDeclaration> getVariableDeclarations(InstantiationFunction function) {
		List<VariableDeclaration> result = new ArrayList<VariableDeclaration>(baseVariableDeclarations);
		result.addAll(parentFacade.getAdditionalVariableDeclarations(function, baseVariableDeclarations));
		return result;
	}

	public Facade getParentFacade() {
		return parentFacade;
	}

	public Class<?> getFunctionReturnType(InstantiationFunction function) {
		return parentFacade.getFunctionReturnType(function, baseVariableDeclarations);
	}

	public Precompiler getPrecompiler() {
		return new Precompiler() {
			@Override
			public String apply(String functionBody) {
				return InstantiationUtils.makeTypeNamesAbsolute(functionBody,
						InstantiationUtils.getAncestorInstanceBuilders(parentFacade));
			}

			@Override
			public int unprecompileFunctionBodyPosition(int position, String precompiledFunctionBody) {
				return InstantiationUtils.positionBeforeTypeNamesMadeAbsolute(position, precompiledFunctionBody,
						InstantiationUtils.getAncestorInstanceBuilders(parentFacade));
			}
		};
	}

}