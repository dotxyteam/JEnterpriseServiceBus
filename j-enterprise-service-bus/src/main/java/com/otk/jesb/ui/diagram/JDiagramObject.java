package com.otk.jesb.ui.diagram;

import java.awt.Graphics;
import java.awt.Rectangle;

public abstract class JDiagramObject {

	public abstract Rectangle getBounds(JDiagram diagram);

	public abstract boolean containsPoint(int x, int y, JDiagram diagram);

	public abstract void paint(Graphics g, JDiagram diagram);

	protected Object value;
	protected String tooltipText;

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public String getTooltipText() {
		return tooltipText;
	}

	public void setTooltipText(String tooltipText) {
		this.tooltipText = tooltipText;
	}

}