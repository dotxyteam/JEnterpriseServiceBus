package com.otk.jesb.diagram;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

import com.otk.jesb.util.MiscUtils;

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
		Point startPoint = new Point(startNode.getX(), startNode.getY());
		Point endPoint = new Point(endNode.getX(), endNode.getY());
		if (startNode.getImage() != null) {
			startPoint = MiscUtils.getRectangleBorderContactOfLineToExternalPoint(startPoint.x, startPoint.y,
					startNode.getImage().getWidth(null), startNode.getImage().getHeight(null), endPoint.x, endPoint.y);
		}
		if (startPoint == null) {
			return;
		}
		if (endNode.getImage() != null) {
			endPoint = MiscUtils.getRectangleBorderContactOfLineToExternalPoint(endPoint.x, endPoint.y,
					endNode.getImage().getWidth(null), endNode.getImage().getHeight(null), startPoint.x, startPoint.y);
		}
		if (endPoint == null) {
			return;
		}
		int arrowSize = 10;
		if (endNode.getImage() != null) {
			arrowSize = (endNode.getImage().getWidth(null) + endNode.getImage().getHeight(null)) / 10;
		}
		if ((startPoint != null) && (endPoint != null)) {
			g.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
			paintArrow(g, startPoint, endPoint, arrowSize);
		}
	}

	private void paintArrow(Graphics g, Point startPoint, Point endPoint, int arrowSize) {
		int firstTrianglePointX = endPoint.x;
		int firstTrianglePointY = endPoint.y;
		double lineAngle = Math.atan2(startPoint.y - endPoint.y, startPoint.x - endPoint.x);
		double triangleHeadAngle = Math.PI / 4;
		double secondTrianglePointAngle = lineAngle - triangleHeadAngle / 2;
		int secondTrianglePointX = (int) Math
				.round(firstTrianglePointX + Math.cos(secondTrianglePointAngle) * arrowSize);
		int secondTrianglePointY = (int) Math
				.round(firstTrianglePointY + Math.sin(secondTrianglePointAngle) * arrowSize);
		double thirdTrianglePointAngle = lineAngle + triangleHeadAngle / 2;
		int thirdTrianglePointX = (int) Math.round(firstTrianglePointX + Math.cos(thirdTrianglePointAngle) * arrowSize);
		int thirdTrianglePointY = (int) Math.round(firstTrianglePointY + Math.sin(thirdTrianglePointAngle) * arrowSize);
		g.setColor(Color.BLACK);
		g.fillPolygon(new int[] { firstTrianglePointX, secondTrianglePointX, thirdTrianglePointX },
				new int[] { firstTrianglePointY, secondTrianglePointY, thirdTrianglePointY }, 3);
	}

}
