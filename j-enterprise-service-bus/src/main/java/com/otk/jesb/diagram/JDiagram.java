package com.otk.jesb.diagram;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
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
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.control.swing.util.SwingRendererUtils;

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
	private Point draggedNodeOffset;
	private Point draggingPoint;
	private JDiagramActionScheme actionScheme;
	private DragIntent dragIntent = DragIntent.MOVE;
	private int connectionArrowSize = 10;

	public JDiagram() {
		addMouseListener(this);
		addMouseMotionListener(this);
		setTransferHandler(new ActionImportTransferHandler());
	}

	public List<JNode> getNodes() {
		return nodes;
	}

	public List<JConnection> getConnections() {
		return connections;
	}

	public DragIntent getDragIntent() {
		return dragIntent;
	}

	public void setDragIntent(DragIntent dragIntent) {
		this.dragIntent = dragIntent;
	}

	public JDiagramActionScheme getActionScheme() {
		return actionScheme;
	}

	public void setActionScheme(JDiagramActionScheme actionScheme) {
		this.actionScheme = actionScheme;
	}

	public int getConnectionArrowSize() {
		return connectionArrowSize;
	}

	public void setConnectionArrowSize(int connectionArrowSize) {
		this.connectionArrowSize = connectionArrowSize;
	}

	public void clear() {
		nodes.clear();
		connections.clear();
	}

	protected Color getSelectionColor() {
		return new Color(184, 207, 229);
	}

	public JNode addNode(Object object, int centerX, int centerY) {
		JNode newNode = new JNode();
		newNode.setCenterX(centerX);
		newNode.setCenterY(centerY);
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

	public JConnection addConnection(JNode node1, JNode node2, Object object) {
		JConnection newConn = new JConnection();
		newConn.setStartNode(node1);
		newConn.setEndNode(node2);
		newConn.setObject(object);
		connections.add(newConn);
		return newConn;
	}

	public void addListener(JDiagramListener listener) {
		listeners.add(listener);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e)) {
			if (draggedNode != null) {
				draggingPoint = new Point(e.getX(), e.getY());
				repaint();
				for (JNode otherNode : MiscUtils.getReverse(nodes)) {
					if (draggedNode != otherNode) {
						if (otherNode.containsPoint(e.getX(), e.getY(), this)) {
							newDraggedConnectionStartNode = draggedNode;
							newDraggedConnectionEndNode = otherNode;
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
	public void mousePressed(final MouseEvent mouseEvent) {
		List<Object> diagramObjects = new ArrayList<Object>();
		diagramObjects.addAll(MiscUtils.getReverse(nodes));
		diagramObjects.addAll(MiscUtils.getReverse(connections));
		for (Object object : diagramObjects) {
			if (object instanceof JNode) {
				JNode node = (JNode) object;
				if (node.containsPoint(mouseEvent.getX(), mouseEvent.getY(), this)) {
					select(node);
					if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
						draggedNode = node;
						draggedNodeOffset = new Point(draggedNode.getCenterX() - mouseEvent.getX(),
								draggedNode.getCenterY() - mouseEvent.getY());
					}
					break;
				}
			}
			if (object instanceof JConnection) {
				JConnection connection = (JConnection) object;
				if (connection.containsPoint(mouseEvent.getX(), mouseEvent.getY(), this)) {
					select(connection);
					break;
				}
			}
		}
		if (SwingUtilities.isRightMouseButton(mouseEvent)) {
			JPopupMenu popupMenu = createContextMenu(mouseEvent);
			popupMenu.show(this, mouseEvent.getX(), mouseEvent.getY());
		}

	}

	protected JPopupMenu createContextMenu(MouseEvent mouseEvent) {
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
								categoryMenu.add(new JMenuItem(new AbstractAction(action.getLabel(), action.getIcon()) {
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
		return popupMenu;
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		draggingPoint = null;
		repaint();
		if (SwingUtilities.isLeftMouseButton(e)) {
			if (dragIntent == DragIntent.CONNECT) {
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
			if (dragIntent == DragIntent.MOVE) {
				if (draggedNode != null) {
					int newNodeCenterX = e.getX() + draggedNodeOffset.x;
					int newNodeCenterY = e.getY() + draggedNodeOffset.y;
					if ((newNodeCenterX != draggedNode.getCenterX()) || (newNodeCenterY != draggedNode.getCenterY())) {
						draggedNode.setCenterX(newNodeCenterX);
						draggedNode.setCenterY(newNodeCenterY);
						repaint();
						for (JDiagramListener l : listeners) {
							l.nodeMoved(draggedNode);
						}
					}
					draggedNode = null;
					draggedNodeOffset = null;
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
		for (JNode eachNode : nodes) {
			eachNode.setSelected(node == eachNode);
		}
		for (JConnection eachConnection : connections) {
			eachConnection.setSelected(false);
		}
		repaint();
		for (JDiagramListener l : listeners) {
			l.nodeSelected(node);
		}
	}

	public void select(JConnection connection) {
		for (JNode eachNode : nodes) {
			eachNode.setSelected(false);
		}
		for (JConnection eachConnection : connections) {
			eachConnection.setSelected(connection == eachConnection);
		}
		repaint();
		for (JDiagramListener l : listeners) {
			l.connectionSelected(connection);
		}
	}

	public JNode getSelectedNode() {
		for (JNode node : nodes) {
			if (node.isSelected()) {
				return node;
			}
		}
		return null;
	}

	public JConnection getSelectedConnection() {
		for (JConnection connection : connections) {
			if (connection.isSelected()) {
				return connection;
			}
		}
		return null;
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension result = new Dimension(0, 0);
		Graphics g = getGraphics();
		for (JNode node : nodes) {
			result.width = Math.max(result.width, node.getCenterX() + (node.getWidth() / 2));
			result.height = Math.max(result.height, node.getCenterY() + (node.getHeight() / 2));
			Rectangle labelBounds = node.getLabelBounds(g);
			result.width = Math.max(result.width, labelBounds.x + labelBounds.width);
			result.height = Math.max(result.height, labelBounds.y + labelBounds.height);
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
		conn.paint(g, this);
	}

	protected void paintNode(Graphics g, JNode node) {
		node.paint(g, this);
	}

	public Component createActionPalette(int tabPlacement, int itemsBoxAxis) {
		if (actionScheme == null) {
			return null;
		}
		JTabbedPane result = new JTabbedPane(tabPlacement);
		result.setBorder(BorderFactory.createTitledBorder(actionScheme.getTitle()));
		for (JDiagramActionCategory category : actionScheme.getActionCategories()) {
			JPanel categoryPanel = new JPanel();
			categoryPanel.setBackground(getBackground());
			categoryPanel.setLayout(new BoxLayout(categoryPanel, itemsBoxAxis));
			for (JDiagramAction action : category.getActions()) {
				JButton button = new JButton(action.getLabel(), action.getIcon());
				button.setHorizontalTextPosition(JButton.CENTER);
				button.setVerticalTextPosition(JButton.BOTTOM);
				button.setContentAreaFilled(false);
				button.setBorderPainted(false);
				button.setTransferHandler(new ActionExportTransferHandler(action));
				button.addMouseMotionListener(new MouseAdapter() {
					@Override
					public void mouseDragged(MouseEvent e) {
						TransferHandler handle = button.getTransferHandler();
						handle.exportAsDrag(button, e, TransferHandler.COPY);
					}
				});
				categoryPanel.add(SwingRendererUtils.flowInLayout(button, GridBagConstraints.CENTER));
			}
			result.addTab(category.getName(), categoryPanel);
		}
		return result;
	}

	private static class ActionExportTransferHandler extends TransferHandler {

		private static final long serialVersionUID = 1L;

		private JDiagramAction action;

		public ActionExportTransferHandler(JDiagramAction action) {
			this.action = action;
		}

		@Override
		public int getSourceActions(JComponent c) {
			return DnDConstants.ACTION_COPY_OR_MOVE;
		}

		@Override
		protected Transferable createTransferable(JComponent c) {
			return new TransferableAction(action);
		}

	}

	private static class ActionImportTransferHandler extends TransferHandler {

		private static final long serialVersionUID = 1L;

		public ActionImportTransferHandler() {
		}

		@Override
		public boolean canImport(TransferHandler.TransferSupport support) {
			return support.isDataFlavorSupported(TransferableAction.DATA_FLAVOR);
		}

		@Override
		public boolean importData(TransferHandler.TransferSupport support) {
			boolean accept = false;
			if (canImport(support)) {
				try {
					Transferable t = support.getTransferable();
					Object data = t.getTransferData(TransferableAction.DATA_FLAVOR);
					if (data instanceof JDiagramAction) {
						Component component = support.getComponent();
						if (component instanceof JDiagram) {
							Point dropPoint = support.getDropLocation().getDropPoint();
							((JDiagramAction) data).perform(dropPoint.x, dropPoint.y);
							accept = true;
						}
					}
				} catch (Exception e) {
					throw new AssertionError(e);
				}
			}
			return accept;
		}
	}

	private static class TransferableAction implements Transferable {

		public static DataFlavor DATA_FLAVOR = new DataFlavor(JDiagramAction.class,
				JDiagramAction.class.getSimpleName());

		private JDiagramAction action;

		public TransferableAction(JDiagramAction action) {
			this.action = action;
		}

		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[] { DATA_FLAVOR };
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return flavor.equals(DATA_FLAVOR);
		}

		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if (flavor.equals(DATA_FLAVOR))
				return action;
			else
				throw new UnsupportedFlavorException(flavor);
		}
	}

}
