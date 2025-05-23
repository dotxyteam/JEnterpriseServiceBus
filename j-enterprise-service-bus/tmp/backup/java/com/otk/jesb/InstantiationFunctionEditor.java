package com.otk.jesb;

import java.util.List;

import com.otk.jesb.PathExplorer.PathNode;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.instantiation.CompilationContext;
import com.otk.jesb.instantiation.InstantiationFunction;
import com.otk.jesb.util.InstantiationUtils;

public class InstantiationFunctionEditor extends PathOptionsProvider {

	private InstantiationFunction currentFunction;
	private PathNode selectedPathNode;

	public InstantiationFunctionEditor(Plan currentPlan, Step currentStep, InstantiationFunction currentFunction) {
		super(currentPlan, currentStep);
		this.currentFunction = currentFunction;
	}

	public String getFunctionBody() {
		return currentFunction.getFunctionBody();
	}

	public void setFunctionBody(String functionBody) {
		this.currentFunction.setFunctionBody(functionBody);
	}

	public PathNode getSelectedPathNode() {
		return selectedPathNode;
	}

	public void setSelectedPathNode(PathNode selectedPathNode) {
		this.selectedPathNode = selectedPathNode;
	}

	@Override
	protected List<VariableDeclaration> getVariableDeclarations() {
		return getCompilationContext().getVariableDeclarations();
	}

	private CompilationContext getCompilationContext() {
		ValidationContext validationContext;
		CompilationContext result;
		result = currentStep.getOperationBuilder().findFunctionCompilationContext(currentFunction, currentStep,
				currentPlan);
		if (result != null) {
			return result;
		}
		validationContext = currentPlan.getValidationContext(null);
		result = currentPlan.getOutputBuilder().getFacade().findFunctionCompilationContext(currentFunction,
				validationContext);
		if (result != null) {
			return result;
		}
		throw new AssertionError();
	}

	public void validate() throws CompilationError {
		InstantiationUtils.validateFunction(getFunctionBody(), getCompilationContext());
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
