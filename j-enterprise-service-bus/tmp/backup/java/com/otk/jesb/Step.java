package com.otk.jesb;

import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;

public class Step {

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
				throw new AssertionError(e);
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

	public void validate() throws Exception {
		String NAME_PATTERN = "[a-zA-Z_][a-zA-Z0-9_]*";
		if (!name.matches(NAME_PATTERN)) {
			throw new Exception("The step name must match the following regular expression: " + NAME_PATTERN);
		}
	}

	@Override
	public String toString() {
		return name;
	}

}
