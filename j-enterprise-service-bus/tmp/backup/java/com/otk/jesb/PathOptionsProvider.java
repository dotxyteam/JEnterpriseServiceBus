package com.otk.jesb;

import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.PathExplorer.PathNode;

import xy.reflect.ui.info.type.ITypeInfo;

public class PathOptionsProvider {

	protected Plan currentPlan;
	protected Step currentStep;

	public PathOptionsProvider(Plan currentPlan, Step currentStep) {
		this.currentPlan = currentPlan;
		this.currentStep = currentStep;
	}

	public List<PathNode> getRootPathNodes() {
		List<PathNode> result = new ArrayList<PathExplorer.PathNode>();
		for (VariableDeclaration declaration : getVariableDeclarations()) {
			result.add(new RootPathNode(
					new PathExplorer(declaration.getVariableType().getName(), declaration.getVariableName())));
		}
		return result;
	}

	protected List<VariableDeclaration> getVariableDeclarations() {
		return currentPlan.getValidationContext(currentStep).getVariableDeclarations();
	}

	private static class RootPathNode implements PathNode {

		private PathExplorer pathExplorer;

		public RootPathNode(PathExplorer pathExplorer) {
			this.pathExplorer = pathExplorer;
		}

		@Override
		public PathNode getParent() {
			return null;
		}

		@Override
		public List<PathNode> getChildren() {
			return pathExplorer.explore();
		}

		@Override
		public String getTypicalExpression() {
			return pathExplorer.getTypicalRootExpression();
		}

		@Override
		public String getExpressionPattern() {
			return pathExplorer.getRootExpressionPattern();
		}

		@Override
		public ITypeInfo getExpressionType() {
			return pathExplorer.getRootExpressionType();
		}

		@Override
		public String toString() {
			return pathExplorer.getTypicalRootExpression();
		}

	}
}
