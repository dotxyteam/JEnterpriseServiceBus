package com.otk.jesb.solution;

import com.otk.jesb.CompositeStep;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.util.MiscUtils;

public class Step extends PlanElement{

	private String name = "";
	private OperationBuilder operationBuilder;
	private int diagramX = 0;
	private int diagramY = 0;
	private CompositeStep parent;

	public Step(OperationMetadata operationMetadata) {
		if (operationMetadata != null) {
			name = operationMetadata.getOperationTypeName();
			name = name.replace(" ", "");
			name = name.substring(0, 1).toLowerCase() + name.substring(1);
			try {
				operationBuilder = operationMetadata.getOperationBuilderClass().newInstance();
			} catch (Exception e) {
				throw new UnexpectedError(e);
			}
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public OperationBuilder getOperationBuilder() {
		return operationBuilder;
	}

	public void setOperationBuilder(OperationBuilder operationBuilder) {
		this.operationBuilder = operationBuilder;
	}

	public int getDiagramX() {
		return diagramX;
	}

	public void setDiagramX(int diagramX) {
		this.diagramX = diagramX;
	}

	public int getDiagramY() {
		return diagramY;
	}

	public void setDiagramY(int diagramY) {
		this.diagramY = diagramY;
	}

	public CompositeStep getParent() {
		return parent;
	}

	public void setParent(CompositeStep parent) {
		this.parent = parent;
	}

	@Override
	public String getSummary() {
		return name;
	}

	@Override
	public void validate(boolean recursively, Plan plan) throws ValidationError {
		if (!MiscUtils.VARIABLE_NAME_PATTERN.matcher(name).matches()) {
			throw new ValidationError("The step name must match the following regular expression: "
					+ MiscUtils.VARIABLE_NAME_PATTERN.pattern());
		}
		if (plan.isPreceding(this, this)) {
			throw new ValidationError("Cycle detected");
		}
		if (recursively) {
			if (operationBuilder != null) {
				operationBuilder.validate(recursively, plan, this);
			}
		}
	}

	@Override
	public String toString() {
		return name;
	}

}
