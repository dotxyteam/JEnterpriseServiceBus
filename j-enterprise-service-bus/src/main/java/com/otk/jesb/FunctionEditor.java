package com.otk.jesb;

import java.util.List;

import com.otk.jesb.Function.Precompiler;
import com.otk.jesb.PathExplorer.PathNode;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.solution.Solution;

public class FunctionEditor extends PathOptionsProvider {

	private Function function;
	private Precompiler precompiler;
	private Class<?> returnType;

	private PathNode selectedPathNode;

	public FunctionEditor(Function function, Precompiler precompiler, List<VariableDeclaration> variableDeclarations,
			Class<?> returnType, Solution solutionInstance) {
		super(variableDeclarations, solutionInstance);
		this.function = function;
		this.precompiler = precompiler;
		this.returnType = returnType;
	}

	public String getFunctionBody() {
		return function.getFunctionBody();
	}

	public void setFunctionBody(String functionBody) {
		this.function.setFunctionBody(functionBody);
	}

	public PathNode getSelectedPathNode() {
		return selectedPathNode;
	}

	public void setSelectedPathNode(PathNode selectedPathNode) {
		this.selectedPathNode = selectedPathNode;
	}

	public String getReturnTypeName() {
		return returnType.getName();
	}

	public void validate() throws CompilationError {
		function.getCompiledVersion(precompiler, getVariableDeclarations(), returnType);
	}

	public void insertSelectedPathNodeExpression(int insertStartPosition, int insertEndPosition) {
		if (selectedPathNode == null) {
			throw new IllegalStateException("Select a path node");
		}
		if (getFunctionBody() == null) {
			setFunctionBody(selectedPathNode.getTypicalExpression());
		} else {
			setFunctionBody(getFunctionBody().substring(0, insertStartPosition)
					+ selectedPathNode.getTypicalExpression() + getFunctionBody().substring(insertEndPosition));
		}
	}

}
