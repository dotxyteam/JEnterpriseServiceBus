package com.otk.jesb;

import java.util.List;

import com.otk.jesb.PathExplorer.PathNode;
import com.otk.jesb.Plan.ValidationContext.VariableDeclaration;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.instantiation.CompilationContext;
import com.otk.jesb.instantiation.Function;
import com.otk.jesb.util.MiscUtils;

public class FunctionEditor extends PathOptionsProvider {

	private Function currentFunction;
	private PathNode selectedPathNode;

	public FunctionEditor(Plan currentPlan, Step currentStep, Function currentFunction) {
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
		return getCompilationContext().getValidationContext().getVariableDeclarations();
	}

	private CompilationContext getCompilationContext() {
		Plan.ValidationContext validationContext;
		CompilationContext result;
		validationContext = currentPlan.getValidationContext(currentStep);
		result = currentStep.getActivityBuilder().findFunctionCompilationContext(currentFunction, validationContext);
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
		MiscUtils.validateFunction(getFunctionBody(), getCompilationContext());
	}

	public void insertSelectedPathNodeExpression(int insertStartPosition, int insertEndPosition) {
		if (selectedPathNode == null) {
			throw new IllegalStateException("Select a path node");
		}
		if (getFunctionBody() == null) {
			setFunctionBody(selectedPathNode.getTypicalExpression());
		} else {
			setFunctionBody(getFunctionBody().substring(0, insertStartPosition) + selectedPathNode.getTypicalExpression()
					+ getFunctionBody().substring(insertEndPosition));
		}
	}

}
