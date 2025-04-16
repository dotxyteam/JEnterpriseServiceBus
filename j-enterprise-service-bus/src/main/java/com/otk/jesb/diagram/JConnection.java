package com.otk.jesb.diagram;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.otk.jesb.util.MiscUtils;

public class JConnection {

	private JNode startNode;
	private JNode endNode;
	private Object object;
	private boolean selected = false;

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

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public void paint(Graphics g, JDiagram diagram) {
		Color selectionColor = diagram.getSelectionColor();
		g.setColor(selected ? selectionColor : Color.BLACK);
		for (Polygon polygon : computePolygons(2, diagram.getConnectionArrowSize())) {
			g.fillPolygon(polygon);
		}
	}

	public boolean containsPoint(int x, int y, JDiagram diagram) {
		for (Polygon polygon : computePolygons(4, diagram.getConnectionArrowSize() + 2)) {
			if (polygon.contains(x, y)) {
				return true;
			}
		}
		return false;
	}

	private List<Polygon> computePolygons(int lineThickness, int arrowSize) {
		List<Polygon> result = new ArrayList<Polygon>();
		Point startPoint = new Point(startNode.getCenterX(), startNode.getCenterY());
		Point endPoint = new Point(endNode.getCenterX(), endNode.getCenterY());
		if (startNode.getImage() != null) {
			startPoint = MiscUtils.getRectangleBorderContactOfLineToExternalPoint(startPoint.x, startPoint.y,
					startNode.getImage().getWidth(null), startNode.getImage().getHeight(null), endPoint.x, endPoint.y);
		}
		if (startPoint == null) {
			return Collections.emptyList();
		}
		if (endNode.getImage() != null) {
			endPoint = MiscUtils.getRectangleBorderContactOfLineToExternalPoint(endPoint.x, endPoint.y,
					endNode.getImage().getWidth(null), endNode.getImage().getHeight(null), startPoint.x, startPoint.y);
		}
		if (endPoint == null) {
			return Collections.emptyList();
		}
		Polygon linePolygon = lineToPolygon(startPoint, endPoint, lineThickness);
		result.add(linePolygon);
		{
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
			int thirdTrianglePointX = (int) Math
					.round(firstTrianglePointX + Math.cos(thirdTrianglePointAngle) * arrowSize);
			int thirdTrianglePointY = (int) Math
					.round(firstTrianglePointY + Math.sin(thirdTrianglePointAngle) * arrowSize);
			Polygon arrowPolygon = new Polygon();
			arrowPolygon.addPoint(firstTrianglePointX, firstTrianglePointY);
			arrowPolygon.addPoint(secondTrianglePointX, secondTrianglePointY);
			arrowPolygon.addPoint(thirdTrianglePointX, thirdTrianglePointY);
			result.add(arrowPolygon);
		}
		return result;
	}

	private static Polygon lineToPolygon(Point p1, Point p2, double thickness) {
		// Direction vector
		double dx = p2.x - p1.x;
		double dy = p2.y - p1.y;

		// Normalize perpendicular vector
		double length = Math.hypot(dx, dy);
		double ux = -dy / length;
		double uy = dx / length;

		// Half thickness offset
		double offsetX = (ux * thickness) / 2.0;
		double offsetY = (uy * thickness) / 2.0;

		// 4 points of the polygon
		int[] xPoints = { (int) Math.round(p1.x + offsetX), (int) Math.round(p2.x + offsetX),
				(int) Math.round(p2.x - offsetX), (int) Math.round(p1.x - offsetX) };

		int[] yPoints = { (int) Math.round(p1.y + offsetY), (int) Math.round(p2.y + offsetY),
				(int) Math.round(p2.y - offsetY), (int) Math.round(p1.y - offsetY) };

		return new Polygon(xPoints, yPoints, 4);
	}

}
