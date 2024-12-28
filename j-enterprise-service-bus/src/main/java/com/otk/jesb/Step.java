package com.otk.jesb;

import com.otk.jesb.activity.ActivityBuilder;

public class Step {

	private String name = "";
	private ActivityBuilder activityBuilder;
	private int diagramX = 0;
	private int diagramY = 0;

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

	@Override
	public String toString() {
		return name;
	}
	
	

}
