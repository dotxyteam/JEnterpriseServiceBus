package com.otk.jesb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;

import com.otk.jesb.PathExplorer.PathNode;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import groovy.transform.TypeChecked;

public class ExpressionEditor {

	private String expression;
	private Plan currentPlan;
	private Step currentStep;
	private PathNode selectedPathNode;

	public ExpressionEditor(String expression, Plan currentPlan, Step currentStep) {
		this.expression = expression;
		this.currentPlan = currentPlan;
		this.currentStep = currentStep;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
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
			if (step.getActivityBuilder().getActivityResultClass() != null) {
				result.add(new RootPathNode(step));
			}
		}
		return result;
	}

	public void validateExpression() {
		CompilerConfiguration config = new CompilerConfiguration();
		config.addCompilationCustomizers(new ASTTransformationCustomizer(TypeChecked.class));
		try (GroovyClassLoader gcl = new GroovyClassLoader(this.getClass().getClassLoader(), config)) {
			new GroovyShell().parse(expression);
		} catch (IOException e) {
			throw new AssertionError(e);
		}
		new GroovyShell(config).parse(expression);
	}

	public void insertSelectedPathNodeExpression(int insertStartPosition, int insertEndPosition) {
		if (selectedPathNode == null) {
			throw new IllegalStateException("Select a path node");
		}
		if (expression == null) {
			expression = selectedPathNode.getExpression();
		} else {
			expression = expression.substring(0, insertStartPosition) + selectedPathNode.getExpression()
					+ expression.substring(insertEndPosition);
		}
	}

	private static class RootPathNode implements PathNode {

		private Step step;
		private PathExplorer pathExplorer;

		public RootPathNode(Step step) {
			this.step = step;
			this.pathExplorer = new PathExplorer(step.getActivityBuilder().getActivityResultClass().getName(),
					step.getName());
		}

		@Override
		public List<PathNode> getChildren() {
			return Collections.singletonList(pathExplorer.getRootNode());
		}

		@Override
		public String getExpression() {
			return step.getName();
		}

		@Override
		public String toString() {
			return step.getName();
		}

	}

}
