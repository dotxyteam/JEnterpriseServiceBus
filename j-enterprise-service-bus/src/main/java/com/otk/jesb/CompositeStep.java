package com.otk.jesb;

import java.awt.Rectangle;
import java.util.List;
import java.util.stream.Collectors;

import com.otk.jesb.ValidationContext.VariableDeclaration;
import com.otk.jesb.activity.ActivityMetadata;

public abstract class CompositeStep extends Step {

	protected abstract List<VariableDeclaration> getChildrenVariableDeclarations();

	public CompositeStep(ActivityMetadata activityMetadata) {
		super(activityMetadata);
	}

	public List<Step> getChildren(Plan plan) {
		return plan.getSteps().stream().filter(step -> this.equals(step.getParent())).collect(Collectors.toList());
	}

	public Rectangle getChildrenBounds(Plan plan, int stepIconWidth, int stepIconHeight, int horizontalPadding,
			int verticalPadding) {
		Rectangle result = null;
		for (Step child : getChildren(plan)) {
			Rectangle childBounds;
			if (child instanceof CompositeStep) {
				childBounds = ((CompositeStep) child).getChildrenBounds(plan, stepIconWidth, stepIconHeight,
						horizontalPadding, verticalPadding);
			} else {
				childBounds = new Rectangle(child.getDiagramX() - (stepIconWidth / 2),
						child.getDiagramY() - (stepIconHeight / 2), stepIconWidth, stepIconHeight);
			}
			childBounds.grow(horizontalPadding, verticalPadding);
			if (result == null) {
				result = childBounds;
			} else {
				result.add(childBounds);
			}
		}
		if (result == null) {
			result = new Rectangle(getDiagramX() - (stepIconWidth / 2), getDiagramY() - (stepIconHeight / 2),
					stepIconWidth, stepIconHeight);
		}
		return result;
	}

}
