package com.otk.jesb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.otk.jesb.PathExplorer.PathNode;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.instantiation.Function;
import com.otk.jesb.instantiation.Function.CompilationContext;
import com.otk.jesb.util.MiscUtils;

public class FunctionEditor {

	private Plan currentPlan;
	private Step currentStep;
	private Function currentFunction;
	private PathNode selectedPathNode;

	public FunctionEditor(Plan currentPlan, Step currentStep, Function currentFunction) {
		this.currentPlan = currentPlan;
		this.currentStep = currentStep;
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

	public List<PathNode> getRootPathNodes() {
		List<PathNode> result = new ArrayList<PathExplorer.PathNode>();
		for (Plan.ValidationContext.VariableDeclaration declaration : getCompilationContext().getVerificationContext()
				.getValidationContext().getVariableDeclarations()) {
			result.add(new RootPathNode(
					new PathExplorer(declaration.getVariableClass().getName(), declaration.getVariableName())));
		}
		return result;
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
			setFunctionBody(selectedPathNode.getExpression());
		} else {
			setFunctionBody(getFunctionBody().substring(0, insertStartPosition) + selectedPathNode.getExpression()
					+ getFunctionBody().substring(insertEndPosition));
		}
	}

	private static class RootPathNode implements PathNode {

		private PathExplorer pathExplorer;

		public RootPathNode(PathExplorer pathExplorer) {
			this.pathExplorer = pathExplorer;
		}

		@Override
		public List<PathNode> getChildren() {
			return Collections.singletonList(pathExplorer.getRootNode());
		}

		@Override
		public String getExpression() {
			return pathExplorer.getRootExpression();
		}

		@Override
		public String toString() {
			return pathExplorer.getRootExpression();
		}

	}

}
