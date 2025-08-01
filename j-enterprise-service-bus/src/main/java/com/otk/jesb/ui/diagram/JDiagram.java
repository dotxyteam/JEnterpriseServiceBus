package com.otk.jesb.ui.diagram;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
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

import com.otk.jesb.UnexpectedError;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.control.swing.util.ImagePanel;
import xy.reflect.ui.control.swing.util.SwingRendererUtils;

public class JDiagram extends ImagePanel implements MouseListener, MouseMotionListener {

	private static final long serialVersionUID = 1L;

	private List<JNode> nodes = new ArrayList<JNode>();
	private List<JConnection> connections = new ArrayList<JConnection>();
	private List<JDiagramListener> listeners = new ArrayList<JDiagramListener>();

	private JNode newConnectionStartNode;
	private JNode newConnectionEndNode;
	private JNode draggedNode;
	private Point draggedNodeCenterOffset;
	private List<JDiagramActionScheme> actionSchemes;
	private DragIntent dragIntent = DragIntent.MOVE;
	private int connectionArrowSize = 25;
	private int connectionLineThickness = 2;

	private Color nodeColor = Color.BLACK;
	private Color connectionColor = Color.BLACK;
	private Color textColor = Color.BLACK;
	private Color selectionColor = new Color(184, 207, 229);

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

	public List<JDiagramActionScheme> getActionSchemes() {
		return actionSchemes;
	}

	public void setActionSchemes(List<JDiagramActionScheme> actionSchemes) {
		this.actionSchemes = actionSchemes;
	}

	public int getConnectionArrowSize() {
		return connectionArrowSize;
	}

	public void setConnectionArrowSize(int connectionArrowSize) {
		this.connectionArrowSize = connectionArrowSize;
	}

	public int getConnectionLineThickness() {
		return connectionLineThickness;
	}

	public void setConnectionLineThickness(int connectionLineThickness) {
		this.connectionLineThickness = connectionLineThickness;
	}

	public Color getNodeColor() {
		return nodeColor;
	}

	public void setNodeColor(Color nodeColor) {
		this.nodeColor = nodeColor;
	}

	public Color getConnectionColor() {
		return connectionColor;
	}

	public void setConnectionColor(Color connectionColor) {
		this.connectionColor = connectionColor;
	}

	public Color getTextColor() {
		return textColor;
	}

	public void setTextColor(Color textColor) {
		this.textColor = textColor;
	}

	public Color getSelectionColor() {
		return selectionColor;
	}

	public void setSelectionColor(Color selectionColor) {
		this.selectionColor = selectionColor;
	}

	public void clear() {
		nodes.clear();
		connections.clear();
	}

	public void scrollTo(JDiagramObject diagramObject) {
		scrollRectToVisible(diagramObject.getBounds(this));
	}

	public JNode addNode(Object object, int centerX, int centerY) {
		JNode newNode = new JNode();
		newNode.setCenterX(centerX);
		newNode.setCenterY(centerY);
		newNode.setValue(object);
		nodes.add(newNode);
		return newNode;
	}

	public JNode findNode(Object value) {
		for (JNode node : nodes) {
			if (value.equals(node.getValue())) {
				return node;
			}
		}
		return null;
	}

	public JConnection findConnection(Object value) {
		for (JConnection connection : connections) {
			if (value.equals(connection.getValue())) {
				return connection;
			}
		}
		return null;
	}

	public JDiagramObject findDiagramObject(Object value) {
		JDiagramObject result;
		result = findNode(value);
		if (result != null) {
			return result;
		}
		result = findConnection(value);
		if (result != null) {
			return result;
		}
		return null;
	}

	public JConnection addConnection(JNode node1, JNode node2, Object object) {
		JConnection newConn = new JConnection();
		newConn.setStartNode(node1);
		newConn.setEndNode(node2);
		newConn.setValue(object);
		connections.add(newConn);
		return newConn;
	}

	public void addListener(JDiagramListener listener) {
		listeners.add(listener);
	}

	@Override
	public void mouseDragged(MouseEvent mouseEvent) {
		if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
			if (draggedNode != null) {
				if (dragIntent == DragIntent.CONNECT) {
					JDiagramObject pointedDiagramObject = getPointedDiagramObject(mouseEvent.getX(), mouseEvent.getY());
					if (pointedDiagramObject instanceof JNode) {
						if (draggedNode != pointedDiagramObject) {
							if (pointedDiagramObject.containsPoint(mouseEvent.getX(), mouseEvent.getY(), this)) {
								newConnectionStartNode = draggedNode;
								newConnectionEndNode = (JNode) pointedDiagramObject;
							}
						}
					}
					setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
				} else {
					setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
				}
				repaint();
			}
		}
	}

	@Override
	public void mouseMoved(MouseEvent mouseEvent) {
		JDiagramObject pointedDiagramObject = getPointedDiagramObject(mouseEvent.getX(), mouseEvent.getY());
		setToolTipText((pointedDiagramObject != null) ? pointedDiagramObject.getTooltipText() : null);
	}

	@Override
	public void mouseClicked(MouseEvent mouseEvent) {
	}

	@Override
	public void mousePressed(final MouseEvent mouseEvent) {
		if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
			JDiagramObject pointedDiagramObject = getPointedDiagramObject(mouseEvent.getX(), mouseEvent.getY());
			if (pointedDiagramObject instanceof JNode) {
				draggedNode = (JNode) pointedDiagramObject;
				draggedNodeCenterOffset = new Point(draggedNode.getCenterX() - mouseEvent.getX(),
						draggedNode.getCenterY() - mouseEvent.getY());
			}
		}
		JDiagramObject pointedDiagramObject = getPointedDiagramObject(mouseEvent.getX(), mouseEvent.getY());
		if (pointedDiagramObject != null) {
			if ((mouseEvent.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK) {
				Set<JDiagramObject> newSelection = new HashSet<JDiagramObject>(getSelection());
				if (newSelection.contains(pointedDiagramObject)) {
					newSelection.remove(pointedDiagramObject);
				} else {
					newSelection.add(pointedDiagramObject);
				}
				setSelection(newSelection, true);
			} else {
				if (!(SwingUtilities.isRightMouseButton(mouseEvent) && getSelection().contains(pointedDiagramObject))) {
					setSelection(Collections.singleton(pointedDiagramObject), true);
				}
			}
		} else {
			setSelection(Collections.emptySet(), true);
		}
		if (SwingUtilities.isRightMouseButton(mouseEvent)) {
			JPopupMenu popupMenu = createContextMenu(mouseEvent);
			popupMenu.show(this, mouseEvent.getX(), mouseEvent.getY());
		}
	}

	@Override
	public void mouseReleased(MouseEvent mouseEvent) {
		setCursor(Cursor.getDefaultCursor());
		repaint();
		if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
			try {
				if (dragIntent == DragIntent.CONNECT) {
					if ((newConnectionStartNode != null) && (newConnectionEndNode != null)) {
						JConnection connection = new JConnection();
						connection.setStartNode(newConnectionStartNode);
						connection.setEndNode(newConnectionEndNode);
						connections.add(connection);
						newConnectionStartNode = null;
						newConnectionEndNode = null;
						repaint();
						for (JDiagramListener l : listeners) {
							l.connectionAdded(connection);
						}
						return;
					}
				}
				if (dragIntent == DragIntent.MOVE) {
					if (draggedNode != null) {
						Point draggedNodeMove = new Point(
								mouseEvent.getX() + draggedNodeCenterOffset.x - draggedNode.getCenterX(),
								mouseEvent.getY() + draggedNodeCenterOffset.y - draggedNode.getCenterY());
						if ((draggedNodeMove.x != 0) || (draggedNodeMove.y != 0)) {
							Set<JNode> nodesToMove = new HashSet<JNode>();
							nodesToMove.add(draggedNode);
							if (draggedNode.isSelected()) {
								nodesToMove.addAll(getSelection().stream()
										.filter(diagramObject -> diagramObject instanceof JNode)
										.map(diagramObject -> (JNode) diagramObject).collect(Collectors.toSet()));
							}
							for (JNode node : nodesToMove) {
								node.setCenterX(node.getCenterX() + draggedNodeMove.x);
								node.setCenterY(node.getCenterY() + draggedNodeMove.y);
							}
							repaint();
							for (JDiagramListener l : listeners) {
								l.nodesMoved(nodesToMove);
							}
							return;
						}
					}
				}
			} finally {
				draggedNode = null;
				draggedNodeCenterOffset = null;
			}
		}
	}

	public JDiagramObject getPointedDiagramObject(int x, int y) {
		List<JDiagramObject> diagramObjects = new ArrayList<JDiagramObject>();
		diagramObjects.addAll(MiscUtils.getReverse(connections));
		diagramObjects.addAll(MiscUtils.getReverse(nodes));
		for (JDiagramObject diagramObject : diagramObjects) {
			if (diagramObject.containsPoint(x, y, this)) {
				return diagramObject;
			}
		}
		return null;
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	protected JPopupMenu createContextMenu(MouseEvent mouseEvent) {
		JPopupMenu popupMenu = new JPopupMenu();
		{
			if (actionSchemes != null) {
				for (JDiagramActionScheme actionScheme : actionSchemes) {
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
		}
		return popupMenu;
	}

	public Set<JDiagramObject> getSelection() {
		Set<JDiagramObject> result = new HashSet<JDiagramObject>();
		for (JNode node : nodes) {
			if (node.isSelected()) {
				result.add(node);
			}
		}
		for (JConnection connection : connections) {
			if (connection.isSelected()) {
				result.add(connection);
			}
		}
		return result;
	}

	public void setSelection(Set<JDiagramObject> selection) {
		setSelection(selection, false);
	}

	private void setSelection(Set<JDiagramObject> selection, boolean paintBeforeNotifications) {
		for (JNode eachNode : nodes) {
			eachNode.setSelected(selection.contains(eachNode));
		}
		for (JConnection eachConnection : connections) {
			eachConnection.setSelected(selection.contains(eachConnection));
		}
		if (paintBeforeNotifications) {
			paintImmediately(getBounds());
		}
		for (JDiagramListener l : listeners) {
			l.selectionChanged();
		}
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension result = new Dimension(0, 0);
		Graphics g = getGraphics();
		for (JNode node : nodes) {
			Rectangle nodeBounds = node.getBounds(this);
			result.width = Math.max(result.width, nodeBounds.x + nodeBounds.width);
			result.height = Math.max(result.height, nodeBounds.y + nodeBounds.height);
		}
		if (g != null) {
			result.width += g.getFontMetrics().getHeight();
			result.height += g.getFontMetrics().getHeight();
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
	}

	protected void paintConnection(Graphics g, JConnection connection) {
		connection.paint(g, this);
	}

	protected void paintNode(Graphics g, JNode node) {
		node.paint(g, this);
	}

	public Component createActionPalette(int tabPlacement1, int tabPlacement2, int itemsBoxAxis) {
		if (actionSchemes == null) {
			return null;
		}
		JTabbedPane result = new JTabbedPane(tabPlacement1);
		for (JDiagramActionScheme actionScheme : actionSchemes) {
			JTabbedPane actionSchemeTabbedPane = new JTabbedPane(tabPlacement2);
			for (JDiagramActionCategory category : actionScheme.getActionCategories()) {
				JPanel categoryPanel = new JPanel();
				categoryPanel.setBackground(getBackground());
				categoryPanel.setLayout(new BoxLayout(categoryPanel, itemsBoxAxis));
				for (JDiagramAction action : category.getActions()) {
					JButton button = new JButton(action.getLabel(), action.getIcon());
					button.setHorizontalTextPosition(JButton.CENTER);
					button.setVerticalTextPosition(JButton.BOTTOM);
					button.setBackground(getBackground());
					button.setBorderPainted(false);
					button.setTransferHandler(new ActionExportTransferHandler(action));
					button.addMouseMotionListener(new MouseAdapter() {
						@Override
						public void mouseDragged(MouseEvent e) {
							TransferHandler handle = button.getTransferHandler();
							handle.exportAsDrag(button, e, TransferHandler.COPY);
						}
					});
					JPanel buttonBox = SwingRendererUtils.flowInLayout(button, GridBagConstraints.CENTER);
					buttonBox.setMaximumSize(buttonBox.getPreferredSize());
					categoryPanel.add(buttonBox);
				}
				actionSchemeTabbedPane.addTab(category.getName(), categoryPanel);
			}
			result.addTab(actionScheme.getTitle(), actionSchemeTabbedPane);
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
					throw new UnexpectedError(e);
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
