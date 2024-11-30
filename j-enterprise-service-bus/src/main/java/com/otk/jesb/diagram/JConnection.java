package com.otk.jesb.diagram;

import java.awt.Color;
import java.awt.Graphics;

public class JConnection {

	private JNode startNode;
	private JNode endNode;

	public JNode getStartNode() {
		return startNode;
	}

	public void setStartNode(JNode startNode) {
		this.startNode = startNode;
	}

	public JNode getEndNode() {
		return endNode;
	}

	public void setEndNode(JNode endNode) {
		this.endNode = endNode;
	}

	public void paint(Graphics g) {
		g.setColor(Color.BLACK);
		g.drawLine(startNode.getX(), startNode.getY(), endNode.getX(), endNode.getY());
		paintArrow(g);
	}

	private void paintArrow(Graphics g) {
		int firstTrianglePointX = endNode.getX();
		int firstTrianglePointY = endNode.getY();
		double lineAngle = Math.atan2(startNode.getY() - endNode.getY(), startNode.getX() - endNode.getX());
		int radius = 10;
		double triangleHeadAngle = Math.PI / 4;
		double secondTrianglePointAngle = lineAngle - triangleHeadAngle / 2;
		int secondTrianglePointX = (int) Math.round(firstTrianglePointX + Math.cos(secondTrianglePointAngle) * radius);
		int secondTrianglePointY = (int) Math.round(firstTrianglePointY + Math.sin(secondTrianglePointAngle) * radius);
		double thirdTrianglePointAngle = lineAngle + triangleHeadAngle / 2;
		int thirdTrianglePointX = (int) Math.round(firstTrianglePointX + Math.cos(thirdTrianglePointAngle) * radius);
		int thirdTrianglePointY = (int) Math.round(firstTrianglePointY + Math.sin(thirdTrianglePointAngle) * radius);
		g.setColor(Color.BLACK);
		g.fillPolygon(new int[] { firstTrianglePointX, secondTrianglePointX, thirdTrianglePointX },
				new int[] { firstTrianglePointY, secondTrianglePointY, thirdTrianglePointY }, 3);
	}

}
