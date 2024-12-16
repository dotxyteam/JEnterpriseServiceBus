package com.otk.jesb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.otk.jesb.PathExplorer.PathNode;

public class PathNodeSelector {

	private PathNode selectedPathNode;
	private Plan currentPlan;
	private Step currentStep;

	public PathNodeSelector(Plan currentPlan, Step currentStep) {
		this.currentPlan = currentPlan;
		this.currentStep = currentStep;
	}

	public PathNode getSelectedPathNode() {
		return selectedPathNode;
	}

	public void setSelectedPathNode(PathNode selectedPathNode) {
		this.selectedPathNode = selectedPathNode;
	}

	public List<PathNode> getRootPathNodes() {
		List<PathNode> result = new ArrayList<PathExplorer.PathNode>();
		List<Step> previousSteps = currentPlan.getPreviousSteps(currentStep);
		for (Step step : previousSteps) {
			result.add(new RootPathNode(step));
		}
		return result;
	}

	public void validate() {
		if (selectedPathNode == null) {
			throw new IllegalStateException("No selected path node");
		}
	}

	private static class RootPathNode implements PathNode {

		private Step step;
		private PathExplorer pathExplorer;

		public RootPathNode(Step step) {
			this.step = step;
			this.pathExplorer = new PathExplorer(step.getActivityBuilder().getActivityResultClass().getName(), step.getName());
		}

		@Override
		public List<PathNode> getChildren() {
			return Collections.singletonList(pathExplorer.getRootNode());
		}

		@Override
		public String generateExpression() {
			return step.getName();
		}

	}

}
