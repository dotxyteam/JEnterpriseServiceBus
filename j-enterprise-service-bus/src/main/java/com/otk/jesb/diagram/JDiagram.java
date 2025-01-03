package com.otk.jesb.diagram;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

public class JDiagram extends JPanel implements MouseListener, MouseMotionListener {

	private static final long serialVersionUID = 1L;

	private static Image DRAGGING_IMAGE;
	static {
		try {
			DRAGGING_IMAGE = ImageIO.read(JDiagram.class.getResource("Dragging.png"));
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}
	private List<JNode> nodes = new ArrayList<JNode>();
	private List<JConnection> connections = new ArrayList<JConnection>();
	private List<JDiagramListener> listeners = new ArrayList<JDiagramListener>();

	private JNode newDraggedConnectionStartNode;
	private JNode newDraggedConnectionEndNode;
	private JNode draggedNode;
	private Point draggingPoint;
	private JDiagramActionScheme actionScheme;

	public JDiagram() {
		addMouseListener(this);
		addMouseMotionListener(this);
	}

	public JDiagramActionScheme getActionScheme() {
		return actionScheme;
	}

	public void setActionScheme(JDiagramActionScheme actionScheme) {
		this.actionScheme = actionScheme;
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
	public void mouseDragged(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
			for (JNode node : nodes) {
				if (node.isSelected()) {
					draggingPoint = new Point(e.getX(), e.getY());
					repaint();
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
		if (draggedNode != null) {
			draggingPoint = new Point(e.getX(), e.getY());
			repaint();
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mousePressed(final MouseEvent mouseEvent) {
		for (JNode node : nodes) {
			if (node.containsPoint(mouseEvent.getX(), mouseEvent.getY())) {
				select(node);
				if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
					draggedNode = node;
				}
				break;
			}
		}
		if (SwingUtilities.isRightMouseButton(mouseEvent)) {
			JPopupMenu popupMenu = new JPopupMenu();
			{
				if (actionScheme != null) {
					JMenu addMenu = new JMenu(actionScheme.getTitle());
					{
						popupMenu.add(addMenu);
						for (JDiagramActionCategory category : actionScheme.getActionCategories()) {
							JMenu categoryMenu = new JMenu(category.getName());
							{
								addMenu.add(categoryMenu);
								for (JDiagramAction action : category.getActions()) {
									categoryMenu
											.add(new JMenuItem(new AbstractAction(action.getLabel(), action.getIcon()) {
												private static final long serialVersionUID = 1L;

												@Override
												public void actionPerformed(ActionEvent e) {
													action.perform(mouseEvent.getX(), mouseEvent.getY());
												}
											}));
								}
							}
						}
					}
				}

			}
			popupMenu.show(this, mouseEvent.getX(), mouseEvent.getY());
		}

	}

	@Override
	public void mouseReleased(MouseEvent e) {
		draggingPoint = null;
		repaint();
		if (SwingUtilities.isRightMouseButton(e)) {
			if (newDraggedConnectionStartNode != null) {
				if (newDraggedConnectionEndNode != null) {
					JConnection conn = new JConnection();
					conn.setStartNode(newDraggedConnectionStartNode);
					conn.setEndNode(newDraggedConnectionEndNode);
					connections.add(conn);
					newDraggedConnectionStartNode = null;
					newDraggedConnectionEndNode = null;
					repaint();
					for (JDiagramListener l : listeners) {
						l.connectionAdded(conn);
					}
				}
			}
		}
		if (SwingUtilities.isLeftMouseButton(e)) {
			if (draggedNode != null) {
				draggedNode.setX(e.getX());
				draggedNode.setY(e.getY());
				repaint();
				for (JDiagramListener l : listeners) {
					l.nodeMoved(draggedNode);
				}
				draggedNode = null;
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
			if (node != null) {
				node.setSelected(true);
			}
			repaint();
			for (JDiagramListener l : listeners) {
				l.nodeSelected(node);
			}
		}
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension result = new Dimension(0, 0);
		for (JNode node : nodes) {
			result.width = Math.max(result.width, node.getX());
			result.height = Math.max(result.width, node.getY());
		}
		return result;
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		for (JNode node : nodes) {
			paintNode(g, node);
		}
		for (JConnection conn : connections) {
			paintConnection(g, conn);
		}
		if (draggingPoint != null) {
			paintDraggingPoint(g, draggingPoint);
		}
	}

	protected void paintDraggingPoint(Graphics g, Point draggingPoint2) {
		g.drawImage(DRAGGING_IMAGE, draggingPoint.x - DRAGGING_IMAGE.getWidth(null) / 2,
				draggingPoint.y - DRAGGING_IMAGE.getHeight(null) / 2, null);
	}

	protected void paintConnection(Graphics g, JConnection conn) {
		conn.paint(g);
	}

	protected void paintNode(Graphics g, JNode node) {
		node.paint(g);
	}

	public Component createActionPalette() {
		if (actionScheme == null) {
			return null;
		}
		JTabbedPane result = new JTabbedPane(JTabbedPane.LEFT);
		result.setBorder(BorderFactory.createTitledBorder(actionScheme.getTitle()));
		for (JDiagramActionCategory category : actionScheme.getActionCategories()) {
			JPanel categoryPanel = new JPanel();
			categoryPanel.setBackground(getBackground());
			categoryPanel.setLayout(new BoxLayout(categoryPanel, BoxLayout.X_AXIS));
			for (JDiagramAction action : category.getActions()) {
				JButton button = new JButton(new AbstractAction(action.getLabel(), action.getIcon()) {

					private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed(ActionEvent e) {
						action.perform(0, 0);
					}
				});
				button.setHorizontalTextPosition(JButton.CENTER);
				button.setVerticalTextPosition(JButton.BOTTOM);
				button.setContentAreaFilled(false);
				button.setBorderPainted(false);
				categoryPanel.add(button);
			}
			result.addTab(category.getName(), categoryPanel);
		}
		return result;
	}

}
