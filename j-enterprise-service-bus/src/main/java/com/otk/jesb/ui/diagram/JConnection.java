package com.otk.jesb.ui.diagram;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.Pair;

public class JConnection extends JDiagramObject {

	private JNode startNode;
	private JNode endNode;
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

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public void paint(Graphics g, JDiagram diagram) {
		MiscUtils.improveRenderingQuality((Graphics2D) g);
		g.setColor(selected ? diagram.getSelectionColor() : diagram.getConnectionColor());
		for (Polygon polygon : computePolygons(diagram.getConnectionLineThickness(),
				diagram.getConnectionArrowSize())) {
			g.fillPolygon(polygon);
		}
		if (value != null) {
			g.setColor(diagram.getTextColor());
			Rectangle labelBounds = getLabelBounds(g);
			if (labelBounds != null) {
				double rotationAngle = getLabelRotationAngleRadians();
				Point2D rotationCenter = getLabelRotationCenter();
				Graphics2D g2D = (Graphics2D) g.create();
				g2D.rotate(rotationAngle, rotationCenter.getX(), rotationCenter.getY());
				g2D.drawString(value.toString(), labelBounds.x, labelBounds.y + labelBounds.height);
				g2D.dispose();
			}
		}
	}

	public Rectangle getLabelBounds(Graphics g) {
		if ((value == null) || (value.toString().length() == 0)) {
			return null;
		}
		Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(value.toString(), g);
		Point2D center = getCenter();
		if (center == null) {
			return null;
		}
		return new Rectangle((int) Math.round(center.getX() - (stringBounds.getWidth() / 2)),
				(int) Math.round(center.getY() - stringBounds.getHeight() * 1.3),
				(int) Math.round(stringBounds.getWidth()), (int) Math.round(stringBounds.getHeight()));
	}

	public Point2D getLabelRotationCenter() {
		return getCenter();
	}

	public double getLabelRotationAngleRadians() {
		Pair<Point, Point> lineSegment = getLineSegment();
		if (lineSegment == null) {
			return 0.0;
		}
		double result = Math.atan2(lineSegment.getSecond().y - lineSegment.getFirst().y,
				lineSegment.getSecond().x - lineSegment.getFirst().x);
		double resultBetweenMinusPiAndPi = Math.atan2(Math.sin(result), Math.cos(result));
		boolean upsideDown = Math.abs(resultBetweenMinusPiAndPi) > (Math.PI / 2);
		if (upsideDown) {
			result += Math.PI;
		}
		return result;
	}

	public Point2D getCenter() {
		Pair<Point, Point> lineSegment = getLineSegment();
		if (lineSegment == null) {
			return null;
		}
		double centerX = (lineSegment.getFirst().x + lineSegment.getSecond().x) / 2.0;
		double centerY = (lineSegment.getFirst().y + lineSegment.getSecond().y) / 2.0;
		return new Point2D.Double(centerX, centerY);
	}

	public boolean containsPoint(int x, int y, JDiagram diagram) {
		Rectangle labelBounds = getLabelBounds(diagram.getGraphics());
		if (labelBounds != null) {
			double rotationAngle = getLabelRotationAngleRadians();
			Point2D rotationCenter = getLabelRotationCenter();
			AffineTransform rotation = AffineTransform.getRotateInstance(rotationAngle, rotationCenter.getX(),
					rotationCenter.getY());
			Shape rotatedLabelBounds = rotation.createTransformedShape(labelBounds);
			if (rotatedLabelBounds.contains(x, y)) {
				return true;
			}
		}
		for (Polygon polygon : computePolygons(diagram.getConnectionLineThickness() + 2,
				diagram.getConnectionArrowSize() + 2)) {
			if (polygon.contains(x, y)) {
				return true;
			}
		}
		return false;
	}

	public List<Polygon> computePolygons(int lineThickness, int arrowSize) {
		List<Polygon> result = new ArrayList<Polygon>();
		Pair<Point, Point> lineSegment = getLineSegment();
		if (lineSegment == null) {
			return Collections.emptyList();
		}
		Point startPoint = lineSegment.getFirst();
		Point endPoint = lineSegment.getSecond();
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

	public Pair<Point, Point> getLineSegment() {
		Point startPoint = new Point(startNode.getCenterX(), startNode.getCenterY());
		Point endPoint = new Point(endNode.getCenterX(), endNode.getCenterY());
		if (startNode.getImage() != null) {
			startPoint = MiscUtils.getRectangleBorderContactOfLineToExternalPoint(startPoint.x, startPoint.y,
					startNode.getImage().getWidth(null), startNode.getImage().getHeight(null), endPoint.x, endPoint.y);
		}
		if (startPoint == null) {
			return null;
		}
		if (endNode.getImage() != null) {
			endPoint = MiscUtils.getRectangleBorderContactOfLineToExternalPoint(endPoint.x, endPoint.y,
					endNode.getImage().getWidth(null), endNode.getImage().getHeight(null), startPoint.x, startPoint.y);
		}
		if (endPoint == null) {
			return null;
		}
		return new Pair<Point, Point>(startPoint, endPoint);
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
