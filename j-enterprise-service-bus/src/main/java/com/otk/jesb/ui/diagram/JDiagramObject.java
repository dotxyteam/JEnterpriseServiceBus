package com.otk.jesb.ui.diagram;

import java.awt.Graphics;
import java.awt.Rectangle;

public abstract class JDiagramObject {

	public abstract Rectangle getBounds(JDiagram jDiagram);

	public abstract boolean containsPoint(int x, int y, JDiagram diagram);

	public abstract void paint(Graphics g, JDiagram diagram);

	protected Object value;

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

}