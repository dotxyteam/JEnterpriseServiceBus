package com.otk.jesb.solution;

import com.otk.jesb.VariableDeclaration;

import xy.reflect.ui.util.ClassUtils;

public class StepEventuality implements VariableDeclaration {

	private Step step;
	private Plan currentPlan;

	public StepEventuality(Step step, Plan currentPlan) {
		this.step = step;
		this.currentPlan = currentPlan;
	}

	@Override
	public Class<?> getVariableType() {
		Class<?> result = step.getOperationBuilder().getOperationResultClass(currentPlan, step);
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
