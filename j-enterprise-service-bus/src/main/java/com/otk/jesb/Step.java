package com.otk.jesb;

import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;

public class Step {

	private String name = "";
	private ActivityBuilder activityBuilder;
	private int diagramX = 0;
	private int diagramY = 0;

	public Step(ActivityMetadata activityMetadata) {
		if (activityMetadata != null) {
			name = activityMetadata.getActivityTypeName();
			name = name.replace(" ", "");
			name = name.substring(0, 1).toLowerCase() + name.substring(1);
			try {
				activityBuilder = activityMetadata.getActivityBuilderClass().newInstance();
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

	public ActivityBuilder getActivityBuilder() {
		return activityBuilder;
	}

	public void setActivityBuilder(ActivityBuilder activityBuilder) {
		this.activityBuilder = activityBuilder;
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
	
	public void validate() throws Exception {
		if(!name.matches("[a-zA-Z][a-zA-Z0-9_]*")){
			throw new Exception("The step name must match the following regular expression: [a-zA-Z][a-zA-Z0-9_]*");
		}
	}

	@Override
	public String toString() {
		return name;
	}

}
