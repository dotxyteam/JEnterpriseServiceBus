package com.otk.jesb.diagram;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class JDiagram extends JPanel implements MouseListener, MouseMotionListener {

	private static final long serialVersionUID = 1L;

	private List<JNode> nodes = new ArrayList<JNode>();
	private List<JConnection> connections = new ArrayList<JConnection>();
	private List<JDiagramListener> listeners = new ArrayList<JDiagramListener>();

	private JNode newDraggedConnectionStartNode;
	private JNode newDraggedConnectionEndNode;

	public JDiagram() {
		addMouseListener(this);
		addMouseMotionListener(this);
	}

	public void clear() {
		nodes.clear();
		connections.clear();
	}

	public JNode addNode(Object object, int x, int y) {
		JNode newNode = new JNode();
		newNode.setX(x);
		newNode.setY(y);
		newNode.setObject(object);
		nodes.add(newNode);
		return newNode;
	}

	public JNode getNode(Object object) {
		for (JNode node : nodes) {
			if (object.equals(node.getObject())) {
				return node;
			}
		}
		return null;
	}

	public JConnection addConnection(JNode node1, JNode node2) {
		JConnection newConn = new JConnection();
		newConn.setStartNode(node1);
		newConn.setEndNode(node2);
		connections.add(newConn);
		return newConn;
	}

	public void addListener(JDiagramListener listener) {
		listeners.add(listener);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		for (JNode node : nodes) {
			node.paint(g);
		}
		for (JConnection conn : connections) {
			conn.paint(g);
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e)) {
			for (JNode node : nodes) {
				if (node.isSelected()) {
					node.setX(e.getX());
					node.setY(e.getY());
					repaint();
					for (JDiagramListener l : listeners) {
						l.nodeMoved(node);
					}
				}
			}
		}
		if (SwingUtilities.isRightMouseButton(e)) {
			for (JNode node : nodes) {
				if (node.isSelected()) {
					for (JNode otherNode : nodes) {
						if (node != otherNode) {
							if (otherNode.containsPoint(e.getX(), e.getY())) {
								newDraggedConnectionStartNode = node;
								newDraggedConnectionEndNode = otherNode;
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		for (JNode node : nodes) {
			if (node.containsPoint(e.getX(), e.getY())) {
				select(node);
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (newDraggedConnectionStartNode != null) {
			if (newDraggedConnectionEndNode != null) {
				JConnection conn = new JConnection();
				conn.setStartNode(newDraggedConnectionStartNode);
				conn.setEndNode(newDraggedConnectionEndNode);
				connections.add(conn);
				newDraggedConnectionStartNode = null;
				newDraggedConnectionEndNode = null;
				repaint();
				for(JDiagramListener l: listeners) {
					l.connectionAdded(conn);
				}
			}
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}
	
	public void select(JNode node) {
		for (JNode otherNode : nodes) {
			if (node != otherNode) {
				otherNode.setSelected(false);
			}
			node.setSelected(true);
			repaint();
			for(JDiagramListener l: listeners) {
				l.nodeSelected(node);
			}
		}
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension result = new Dimension(0,0);
		for (JNode node : nodes) {
			result.width = Math.max(result.width,node.getX());
			result.height = Math.max(result.width,node.getY());
		}
		return result;
	}

}
