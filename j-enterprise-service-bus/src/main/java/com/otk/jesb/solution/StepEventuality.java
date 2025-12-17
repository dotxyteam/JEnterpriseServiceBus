package com.otk.jesb.solution;

import com.otk.jesb.VariableDeclaration;

import xy.reflect.ui.util.ClassUtils;

public class StepEventuality implements VariableDeclaration {

	private Step step;
	private Plan currentPlan;
	private Solution solutionInstance;

	public StepEventuality(Step step, Plan currentPlan, Solution solutionInstance) {
		this.step = step;
		this.currentPlan = currentPlan;
		this.solutionInstance = solutionInstance;
	}

	public Solution getSolutionInstance() {
		return solutionInstance;
	}

	@Override
	public Class<?> getVariableType() {
		Class<?> result = step.getOperationBuilder().getOperationResultClass(solutionInstance, currentPlan, step);
		if (result != null) {
			if (result.isPrimitive()) {
				result = ClassUtils.primitiveToWrapperClass(result);
			}
		}
		return result;
	}

	@Override
	public String getVariableName() {
		return step.getName();
	}
}
