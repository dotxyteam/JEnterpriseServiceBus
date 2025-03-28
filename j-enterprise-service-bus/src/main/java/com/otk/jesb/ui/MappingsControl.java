package com.otk.jesb.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.JXTreeTable;

import com.otk.jesb.PathExplorer;
import com.otk.jesb.PathExplorer.PathNode;
import com.otk.jesb.instantiation.Facade;
import com.otk.jesb.instantiation.FieldInitializerFacade;
import com.otk.jesb.instantiation.Function;
import com.otk.jesb.instantiation.ListItemInitializerFacade;
import com.otk.jesb.instantiation.ListItemReplicationFacade;
import com.otk.jesb.instantiation.ParameterInitializerFacade;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.control.IFieldControlInput;
import xy.reflect.ui.control.swing.ListControl;
import xy.reflect.ui.control.swing.renderer.FieldControlPlaceHolder;
import xy.reflect.ui.control.swing.renderer.Form;
import xy.reflect.ui.control.swing.renderer.SwingRenderer;
import xy.reflect.ui.control.swing.util.SwingRendererUtils;
import xy.reflect.ui.info.type.iterable.item.BufferedItemPosition;
import xy.reflect.ui.undo.ListModificationFactory;
import xy.reflect.ui.undo.ModificationStack;

public class MappingsControl extends JPanel {

	private static final long serialVersionUID = 1L;

	private InstanceBuilderVariableTreeControl foundSourceControl;
	private InstanceBuilderInitializerTreeControl foundTargetControl;

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		InstanceBuilderVariableTreeControl sourceControl = findControl(InstanceBuilderVariableTreeControl.class,
				new Accessor<InstanceBuilderVariableTreeControl>() {

					@Override
					public InstanceBuilderVariableTreeControl get() {
						return foundSourceControl;
					}

					@Override
					public void set(InstanceBuilderVariableTreeControl t) {
						foundSourceControl = t;
					}

				});
		if (sourceControl == null) {
			return;
		}
		InstanceBuilderInitializerTreeControl targetControl = findControl(InstanceBuilderInitializerTreeControl.class,
				new Accessor<InstanceBuilderInitializerTreeControl>() {

					@Override
					public InstanceBuilderInitializerTreeControl get() {
						return foundTargetControl;
					}

					@Override
					public void set(InstanceBuilderInitializerTreeControl t) {
						foundTargetControl = t;
					}

				});
		if (targetControl == null) {
			return;
		}
		targetControl.visitItems(new ListControl.IItemsVisitor() {
			@Override
			public VisitStatus visitItem(BufferedItemPosition targetItemPosition) {
				Object item = targetItemPosition.getItem();
				if (!(item instanceof Facade)) {
					return VisitStatus.BRANCH_VISIT_INTERRUPTED;
				}
				if (!((Facade) item).isConcrete()) {
					return VisitStatus.BRANCH_VISIT_INTERRUPTED;
				}
				List<Function> functions = new ArrayList<Function>();
				if (item instanceof ParameterInitializerFacade) {
					if (((ParameterInitializerFacade) item).getParameterValue() instanceof Function) {
						functions.add((Function) ((ParameterInitializerFacade) item).getParameterValue());
					}
				} else if (item instanceof FieldInitializerFacade) {
					if (((FieldInitializerFacade) item).getFieldValue() instanceof Function) {
						functions.add((Function) ((FieldInitializerFacade) item).getFieldValue());
					}
					if (((FieldInitializerFacade) item).getCondition() != null) {
						functions.add(((FieldInitializerFacade) item).getCondition());
					}
				} else if (item instanceof ListItemInitializerFacade) {
					if (((ListItemInitializerFacade) item).getItemValue() instanceof Function) {
						functions.add((Function) ((ListItemInitializerFacade) item).getItemValue());
					}
					if (((ListItemInitializerFacade) item).getItemReplicationFacade() != null) {
						ListItemReplicationFacade replication = ((ListItemInitializerFacade) item)
								.getItemReplicationFacade();
						if (replication.getIterationListValue() instanceof Function) {
							functions.add((Function) replication.getIterationListValue());
						}
					}
				}
				List<BufferedItemPosition> mappedSourceItemPositions = new ArrayList<BufferedItemPosition>();
				for (Function function : functions) {
					sourceControl.visitItems(new ListControl.IItemsVisitor() {
						@Override
						public VisitStatus visitItem(BufferedItemPosition sourceItemPosition) {
							Object item = sourceItemPosition.getItem();
							if (item instanceof PathNode) {
								String pathExpression = ((PathNode) item).getExpression();
								String pathExpressionRegex = MiscUtils.escapeRegex(pathExpression);
								pathExpressionRegex = ".*" + pathExpressionRegex + ".*";
								pathExpressionRegex = pathExpressionRegex.replace("\\.", "\\s*\\.\\s*");
								pathExpressionRegex = pathExpressionRegex.replace("\\(", "\\s*\\(\\s*");
								pathExpressionRegex = pathExpressionRegex.replace("\\)", "\\s*\\)\\s*");
								if (function.getFunctionBody().matches(pathExpressionRegex)) {
									mappedSourceItemPositions.add(sourceItemPosition);
								}
							}
							return (!sourceControl.isItemPositionExpanded(sourceItemPosition))
									? VisitStatus.BRANCH_VISIT_INTERRUPTED
									: VisitStatus.VISIT_NOT_INTERRUPTED;
						}
					});
				}
				List<BufferedItemPosition> filteredMappedSourceItemPositions = mappedSourceItemPositions.stream()
						.filter(itemPosition1 -> !mappedSourceItemPositions.stream()
								.anyMatch(itemPosition2 -> itemPosition1.equals(itemPosition2.getParentItemPosition())))
						.collect(Collectors.toList());
				for (BufferedItemPosition sourceItemPosition : filteredMappedSourceItemPositions) {
					int sourceY;
					{
						TreePath treePath = sourceControl.getTreePath(sourceItemPosition);
						int row = sourceControl.getTreeTableComponent().getRowForPath(treePath);
						Rectangle cellRect = sourceControl.getTreeTableComponent().getCellRect(row, 0, false);
						sourceY = cellRect.y + Math.round(cellRect.height / 2f);
						sourceY = SwingUtilities.convertPoint(sourceControl.getTreeTableComponent(), 0, sourceY,
								MappingsControl.this).y;
					}
					int targetY;
					{
						TreePath treePath = targetControl.getTreePath(targetItemPosition);
						int row = targetControl.getTreeTableComponent().getRowForPath(treePath);
						Rectangle cellRect = targetControl.getTreeTableComponent().getCellRect(row, 0, false);
						targetY = cellRect.y + Math.round(cellRect.height / 2f);
						targetY = SwingUtilities.convertPoint(targetControl.getTreeTableComponent(), 0, targetY,
								MappingsControl.this).y;
					}
					g.setColor(Color.BLUE);
					g.drawLine(0, sourceY, MappingsControl.this.getWidth(), targetY);
				}
				return VisitStatus.VISIT_NOT_INTERRUPTED;
			}
		});
	}

	@SuppressWarnings("unchecked")
	private <T extends SideControl> T findControl(Class<T> controlClass, Accessor<T> alreadyFoundControlAccessor) {
		if (alreadyFoundControlAccessor.get() != null) {
			return alreadyFoundControlAccessor.get();
		}
		Form rootInstanceBuilderForm = SwingRendererUtils.findAncestorFormOfType(this,
				RootInstanceBuilder.class.getName(), GUI.INSTANCE);
		if (rootInstanceBuilderForm == null) {
			return null;
		}
		Form rootInstanceBuilderParentForm = SwingRendererUtils.findParentForm(rootInstanceBuilderForm, GUI.INSTANCE);
		if (rootInstanceBuilderParentForm == null) {
			return null;
		}
		List<Form> forms = new ArrayList<Form>();
		forms.add(rootInstanceBuilderParentForm);
		forms.addAll(SwingRendererUtils.findDescendantForms(rootInstanceBuilderParentForm, GUI.INSTANCE));
		for (Form form : forms) {
			for (List<FieldControlPlaceHolder> fieldControlPlaceHolders : form.getFieldControlPlaceHoldersByCategory()
					.values()) {
				for (FieldControlPlaceHolder fieldControlPlaceHolder : fieldControlPlaceHolders) {
					if (controlClass.isInstance(fieldControlPlaceHolder.getFieldControl())) {
						alreadyFoundControlAccessor.set((T) fieldControlPlaceHolder.getFieldControl());
						((T) fieldControlPlaceHolder.getFieldControl()).getTreeTableComponent()
								.addTreeExpansionListener(new TreeExpansionListener() {

									@Override
									public void treeExpanded(TreeExpansionEvent event) {
										MappingsControl.this.repaint();
									}

									@Override
									public void treeCollapsed(TreeExpansionEvent event) {
										MappingsControl.this.repaint();
									}
								});
						return (T) fieldControlPlaceHolder.getFieldControl();
					}
				}
			}
		}
		return null;
	}

	public static class Source {

	}

	public static abstract class SideControl extends ListControl {

		private static final long serialVersionUID = 1L;

		public SideControl(SwingRenderer swingRenderer, IFieldControlInput input) {
			super(swingRenderer, input);
		}

		public JXTreeTable getTreeTableComponent() {
			return treeTableComponent;
		}

		public TreePath getTreePath(BufferedItemPosition itemPosition) {
			ItemNode node = findNode(itemPosition);
			if (node == null) {
				return null;
			}
			return new TreePath(node.getPath());
		}
	}

	public static class PathExportTransferHandler extends TransferHandler {

		private static final long serialVersionUID = 1L;

		@Override
		public int getSourceActions(JComponent c) {
			return DnDConstants.ACTION_COPY_OR_MOVE;
		}

		@Override
		protected Transferable createTransferable(JComponent c) {
			InstanceBuilderVariableTreeControl listControl = (InstanceBuilderVariableTreeControl) ((JXTreeTable) c)
					.getClientProperty(InstanceBuilderVariableTreeControl.class);
			BufferedItemPosition selectedItemPosition = listControl.getSingleSelection();
			if (selectedItemPosition == null) {
				return null;
			}
			if (!(selectedItemPosition.getItem() instanceof PathExplorer.PathNode)) {
				return null;
			}
			return new TransferablePath((PathExplorer.PathNode) selectedItemPosition.getItem());
		}

	}

	public static class PathImportTransferHandler extends TransferHandler {

		private static final long serialVersionUID = 1L;

		public PathImportTransferHandler() {
		}

		@Override
		public boolean canImport(TransferHandler.TransferSupport support) {
			return support.isDataFlavorSupported(TransferablePath.DATA_FLAVOR);
		}

		@Override
		public boolean importData(TransferHandler.TransferSupport support) {
			boolean accept = false;
			if (canImport(support)) {
				try {
					Transferable t = support.getTransferable();
					Object data = t.getTransferData(TransferablePath.DATA_FLAVOR);
					if (data instanceof PathNode) {
						Component component = support.getComponent();
						if (component instanceof JXTreeTable) {
							InstanceBuilderInitializerTreeControl listControl = (InstanceBuilderInitializerTreeControl) ((JXTreeTable) component)
									.getClientProperty(InstanceBuilderInitializerTreeControl.class);
							if (listControl != null) {
								Point dropPoint = support.getDropLocation().getDropPoint();
								TreePath treePath = ((JXTreeTable) component).getPathForLocation(dropPoint.x,
										dropPoint.y);
								if (treePath != null) {
									BufferedItemPosition itemPosition = (BufferedItemPosition) ((DefaultMutableTreeNode) treePath
											.getLastPathComponent()).getUserObject();
									Object item = itemPosition.getItem();
									Function pathFunction = new Function(
											"return " + ((PathNode) data).getExpression() + ";");
									if (item instanceof ParameterInitializerFacade) {
										((ParameterInitializerFacade) item).setParameterValue(pathFunction);
										accept = true;
									} else if (item instanceof FieldInitializerFacade) {
										((FieldInitializerFacade) item).setFieldValue(pathFunction);
										accept = true;
									} else if (item instanceof ListItemInitializerFacade) {
										((ListItemInitializerFacade) item).setItemValue(pathFunction);
										accept = true;
									}
									if (accept) {
										ModificationStack modifStack = SwingRendererUtils
												.findParentFormModificationStack(listControl, GUI.INSTANCE);
										modifStack.apply(new ListModificationFactory(itemPosition)
												.set(itemPosition.getIndex(), item));
									}
								}
							}
						}
					}
				} catch (Exception e) {
					throw new AssertionError(e);
				}
			}
			return accept;
		}
	}

	public static class TransferablePath implements Transferable {

		public static DataFlavor DATA_FLAVOR = new DataFlavor(PathNode.class, PathNode.class.getSimpleName());

		private PathNode pathNode;

		public TransferablePath(PathNode pathNode) {
			this.pathNode = pathNode;
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
				return pathNode;
			else
				throw new UnsupportedFlavorException(flavor);
		}
	}

}
