package com.otk.jesb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.otk.jesb.InstanceSpecification.DynamicValue;
import com.otk.jesb.PathExplorer.PathNode;
import com.otk.jesb.Plan.ValidationContext;
import com.otk.jesb.util.MiscUtils;

public class ExpressionEditor {

	private Plan currentPlan;
	private Step currentStep;
	private DynamicValue currentDynamicValue;
	private PathNode selectedPathNode;

	public ExpressionEditor(String expression, Plan currentPlan, Step currentStep, DynamicValue currentDynamicValue) {
		this.currentPlan = currentPlan;
		this.currentStep = currentStep;
		this.currentDynamicValue = currentDynamicValue;
	}

	public String getExpression() {
		return currentDynamicValue.getScript();
	}

	public void setExpression(String expression) {
		this.currentDynamicValue.setScript(expression);
	}

	public PathNode getSelectedPathNode() {
		return selectedPathNode;
	}

	public void setSelectedPathNode(PathNode selectedPathNode) {
		this.selectedPathNode = selectedPathNode;
	}

	public List<PathNode> getRootPathNodes() {
		List<PathNode> result = new ArrayList<PathExplorer.PathNode>();
		for (Plan.ValidationContext.Declaration declaration : getValidationContext().getDeclarations()) {
			result.add(new RootPathNode(
					new PathExplorer(declaration.getPropertyClass().getName(), declaration.getPropertyName())));
		}
		return result;
	}

	private ValidationContext getValidationContext() {
		Plan.ValidationContext context = currentPlan.getValidationContext(currentStep);
		currentStep.getActivityBuilder().completeValidationContext(context, currentDynamicValue);
		return context;
	}

	public void validateExpression() {
		MiscUtils.validateScript(getExpression(), getValidationContext());
	}

	public void insertSelectedPathNodeExpression(int insertStartPosition, int insertEndPosition) {
		if (selectedPathNode == null) {
			throw new IllegalStateException("Select a path node");
		}
		if (getExpression() == null) {
			setExpression(selectedPathNode.getExpression());
		} else {
			setExpression(getExpression().substring(0, insertStartPosition) + selectedPathNode.getExpression()
					+ getExpression().substring(insertEndPosition));
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
