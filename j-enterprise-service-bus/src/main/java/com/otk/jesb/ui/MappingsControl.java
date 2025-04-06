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
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.JXTreeTable;

import com.otk.jesb.PathExplorer;
import com.otk.jesb.PathExplorer.ListItemNode;
import com.otk.jesb.PathExplorer.PathNode;
import com.otk.jesb.instantiation.Facade;
import com.otk.jesb.instantiation.FacadeOutline;
import com.otk.jesb.instantiation.FieldInitializerFacade;
import com.otk.jesb.instantiation.Function;
import com.otk.jesb.instantiation.ListItemInitializerFacade;
import com.otk.jesb.instantiation.ListItemReplication;
import com.otk.jesb.instantiation.ListItemReplicationFacade;
import com.otk.jesb.instantiation.ParameterInitializerFacade;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.Listener;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.Pair;

import xy.reflect.ui.control.IFieldControlInput;
import xy.reflect.ui.control.swing.ListControl;
import xy.reflect.ui.control.swing.renderer.FieldControlPlaceHolder;
import xy.reflect.ui.control.swing.renderer.Form;
import xy.reflect.ui.control.swing.renderer.SwingRenderer;
import xy.reflect.ui.control.swing.util.SwingRendererUtils;
import xy.reflect.ui.info.type.ITypeInfo;
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
		for (Pair<BufferedItemPosition, BufferedItemPosition> mapping : listVisibleMappings()) {
			BufferedItemPosition sourceItemPosition = mapping.getFirst();
			BufferedItemPosition targetItemPosition = mapping.getSecond();
			int sourceY;
			{
				TreePath treePath = foundSourceControl.getTreePath(sourceItemPosition);
				int row = foundSourceControl.getTreeTableComponent().getRowForPath(treePath);
				Rectangle cellRect = foundSourceControl.getTreeTableComponent().getCellRect(row, 0, false);
				sourceY = cellRect.y + Math.round(cellRect.height / 2f);
				sourceY = SwingUtilities.convertPoint(foundSourceControl.getTreeTableComponent(), 0, sourceY,
						MappingsControl.this).y;
			}
			int targetY;
			{
				TreePath treePath = foundTargetControl.getTreePath(targetItemPosition);
				int row = foundTargetControl.getTreeTableComponent().getRowForPath(treePath);
				Rectangle cellRect = foundTargetControl.getTreeTableComponent().getCellRect(row, 0, false);
				targetY = cellRect.y + Math.round(cellRect.height / 2f);
				targetY = SwingUtilities.convertPoint(foundTargetControl.getTreeTableComponent(), 0, targetY,
						MappingsControl.this).y;
			}
			g.setColor(getMappingColor());
			g.drawLine(0, sourceY, MappingsControl.this.getWidth(), targetY);
		}

	}

	private Color getMappingColor() {
		return new Color(0, 0, 255);
	}

	public Set<Pair<BufferedItemPosition, BufferedItemPosition>> listVisibleMappings() {
		Set<Pair<BufferedItemPosition, BufferedItemPosition>> result = new HashSet<Pair<BufferedItemPosition, BufferedItemPosition>>();
		InstanceBuilderVariableTreeControl sourceControl = findControl(this, InstanceBuilderVariableTreeControl.class,
				new Accessor<InstanceBuilderVariableTreeControl>() {

					@Override
					public InstanceBuilderVariableTreeControl get() {
						return foundSourceControl;
					}

					@Override
					public void set(InstanceBuilderVariableTreeControl t) {
						foundSourceControl = t;
					}

				}, new Listener<InstanceBuilderVariableTreeControl>() {
					@Override
					public void handle(InstanceBuilderVariableTreeControl control) {
						control.getTreeTableComponent().addTreeExpansionListener(new TreeExpansionListener() {

							@Override
							public void treeExpanded(TreeExpansionEvent event) {
								MappingsControl.this.repaint();
							}

							@Override
							public void treeCollapsed(TreeExpansionEvent event) {
								MappingsControl.this.repaint();
							}
						});
						((JScrollPane) control.getTreeTableComponent().getParent().getParent()).getVerticalScrollBar()
								.addAdjustmentListener(new AdjustmentListener() {
									@Override
									public void adjustmentValueChanged(AdjustmentEvent e) {
										MappingsControl.this.repaint();
									}
								});
					}
				});
		if (sourceControl == null) {
			return Collections.emptySet();
		}
		InstanceBuilderInitializerTreeControl targetControl = findControl(this,
				InstanceBuilderInitializerTreeControl.class, new Accessor<InstanceBuilderInitializerTreeControl>() {

					@Override
					public InstanceBuilderInitializerTreeControl get() {
						return foundTargetControl;
					}

					@Override
					public void set(InstanceBuilderInitializerTreeControl t) {
						foundTargetControl = t;
					}

				}, new Listener<InstanceBuilderInitializerTreeControl>() {
					@Override
					public void handle(InstanceBuilderInitializerTreeControl control) {
						control.getTreeTableComponent().addTreeExpansionListener(new TreeExpansionListener() {

							@Override
							public void treeExpanded(TreeExpansionEvent event) {
								MappingsControl.this.repaint();
							}

							@Override
							public void treeCollapsed(TreeExpansionEvent event) {
								MappingsControl.this.repaint();
							}
						});
						((JScrollPane) control.getTreeTableComponent().getParent().getParent()).getVerticalScrollBar()
								.addAdjustmentListener(new AdjustmentListener() {
									@Override
									public void adjustmentValueChanged(AdjustmentEvent e) {
										MappingsControl.this.repaint();
									}
								});
					}
				});
		if (targetControl == null) {
			return Collections.emptySet();
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
				while ((targetItemPosition.getParentItemPosition() != null)
						&& !targetControl.isItemPositionExpanded(targetItemPosition.getParentItemPosition())) {
					targetItemPosition = targetItemPosition.getParentItemPosition();
				}
				for (BufferedItemPosition sourceItemPosition : filteredMappedSourceItemPositions) {
					result.add(new Pair<BufferedItemPosition, BufferedItemPosition>(sourceItemPosition,
							targetItemPosition));
				}
				return VisitStatus.VISIT_NOT_INTERRUPTED;
			}
		});
		return result;
	}

	@SuppressWarnings("unchecked")
	private static <T extends Component> T findControl(Component fromComponent, Class<T> controlClass,
			Accessor<T> alreadyFoundControlAccessor, Listener<T> controlConfigurator) {
		if (alreadyFoundControlAccessor.get() != null) {
			return alreadyFoundControlAccessor.get();
		}
		Form facadeOutlineForm = SwingRendererUtils.findAncestorFormOfType(fromComponent, FacadeOutline.class.getName(),
				GUI.INSTANCE);
		if (facadeOutlineForm == null) {
			return null;
		}
		List<Form> forms = new ArrayList<Form>();
		forms.add(facadeOutlineForm);
		forms.addAll(SwingRendererUtils.findDescendantForms(facadeOutlineForm, GUI.INSTANCE));
		for (Form form : forms) {
			for (List<FieldControlPlaceHolder> fieldControlPlaceHolders : form.getFieldControlPlaceHoldersByCategory()
					.values()) {
				for (FieldControlPlaceHolder fieldControlPlaceHolder : fieldControlPlaceHolders) {
					if (controlClass.isInstance(fieldControlPlaceHolder.getFieldControl())) {
						alreadyFoundControlAccessor.set((T) fieldControlPlaceHolder.getFieldControl());
						if (controlConfigurator != null) {
							controlConfigurator.handle((T) fieldControlPlaceHolder.getFieldControl());
						}
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

		private MappingsControl foundMappingsControl;

		protected abstract BufferedItemPosition getSideItemPosition(
				Pair<BufferedItemPosition, BufferedItemPosition> pair);

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

		public MappingsControl findMappingsControl() {
			return findControl(this, MappingsControl.class, new Accessor<MappingsControl>() {

				@Override
				public MappingsControl get() {
					return foundMappingsControl;
				}

				@Override
				public void set(MappingsControl t) {
					foundMappingsControl = t;
				}
			}, null);
		}

		@Override
		protected void customizeCellRendererComponent(JLabel label, ItemNode node, int rowIndex, int columnIndex,
				boolean isSelected, boolean hasFocus) {
			super.customizeCellRendererComponent(label, node, rowIndex, columnIndex, isSelected, hasFocus);
			label.setBorder(isMapped(rowIndex)
					? BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1),
							BorderFactory.createLineBorder(foundMappingsControl.getMappingColor()))
					: BorderFactory.createEmptyBorder());
		}

		@Override
		protected String getCellValue(ItemNode node, int columnIndex) {
			String result = super.getCellValue(node, columnIndex);
			if (result == null) {
				return null;
			}
			result += "     ";
			return result;
		}

		private boolean isMapped(int rowIndex) {
			MappingsControl mappingsControl = findMappingsControl();
			if (mappingsControl != null) {
				for (Pair<BufferedItemPosition, BufferedItemPosition> pair : mappingsControl.listVisibleMappings()) {
					BufferedItemPosition itemPosition = getSideItemPosition(pair);
					TreePath treePath = getTreePath(itemPosition);
					int row = treeTableComponent.getRowForPath(treePath);
					if (rowIndex == row) {
						return true;
					}
				}
			}
			return false;
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
									PathNode pathNode = (PathNode) data;
									if (item instanceof ParameterInitializerFacade) {
										if (isLeafType(((ParameterInitializerFacade) item).getParameterInfo().getType())
												&& isLeafType(pathNode.getExpressionType())) {
											((ParameterInitializerFacade) item).setParameterValue(
													new Function("return " + pathNode.getExpression() + ";"));
											accept = true;
										}
										if (!isLeafType(
												((ParameterInitializerFacade) item).getParameterInfo().getType())
												&& !isLeafType(pathNode.getExpressionType())) {
											List<String> options = Arrays.asList("Assign source value to target");
											String choice = GUI.INSTANCE.openSelectionDialog(component, options, null,
													"Choose an option", "Mapping");
											if (choice == options.get(0)) {
												((ParameterInitializerFacade) item).setParameterValue(
														new Function("return " + pathNode.getExpression() + ";"));
												accept = true;
											}
										}
									} else if (item instanceof FieldInitializerFacade) {
										if (isLeafType(((FieldInitializerFacade) item).getFieldInfo().getType())
												&& isLeafType(pathNode.getExpressionType())) {
											((FieldInitializerFacade) item).setFieldValue(
													new Function("return " + pathNode.getExpression() + ";"));
											accept = true;
										}
										if (!isLeafType(((FieldInitializerFacade) item).getFieldInfo().getType())
												&& !isLeafType(pathNode.getExpressionType())) {
											List<String> options = Arrays.asList("Assign source value to target");
											String choice = GUI.INSTANCE.openSelectionDialog(component, options, null,
													"Choose an option", "Mapping");
											if (choice == options.get(0)) {
												((FieldInitializerFacade) item).setFieldValue(
														new Function("return " + pathNode.getExpression() + ";"));
												accept = true;
											}
										}
									} else if (item instanceof ListItemInitializerFacade) {
										ListItemReplication itemReplication = null;
										boolean cancelled = false;
										if (pathNode instanceof ListItemNode) {
											List<String> options = Arrays
													.asList("Replicate the target value for each source value");
											String choice = GUI.INSTANCE.openSelectionDialog(component, options, null,
													"Choose an option", "Mapping");
											if (choice == options.get(0)) {
												itemReplication = new ListItemReplication();
												itemReplication.setIterationListValue(new Function("return "
														+ ((ListItemNode) pathNode).getParent().getExpression() + ";"));
												itemReplication.setIterationListValueTypeName(((ListItemNode) pathNode)
														.getParent().getExpressionType().getName());
												itemReplication.setIterationVariableTypeName(
														((ListItemNode) pathNode).getExpressionType().getName());
												((ListItemInitializerFacade) item).setConcrete(true);
												((ListItemInitializerFacade) item).getUnderlying()
														.setItemReplication(itemReplication);
												accept = true;
											} else {
												cancelled = true;
											}
										}
										if (!cancelled) {
											if (isLeafType(((ListItemInitializerFacade) item).getItemType())
													&& isLeafType(pathNode.getExpressionType())) {
												((ListItemInitializerFacade) item).setItemValue(
														new Function("return " + ((itemReplication != null)
																? itemReplication.getIterationVariableName()
																: pathNode.getExpression()) + ";"));
												accept = true;
											}
											if (!isLeafType(((ListItemInitializerFacade) item).getItemType())
													&& !isLeafType(pathNode.getExpressionType())) {
												List<String> options = Arrays.asList("Assign source value to target");
												String choice = GUI.INSTANCE.openSelectionDialog(component, options,
														null, "Choose an option", "Mapping");
												if (choice == options.get(0)) {
													((ListItemInitializerFacade) item).setItemValue(
															new Function("return " + pathNode.getExpression() + ";"));
													accept = true;
												}
											}
										}
									}
									if (accept) {
										ModificationStack modifStack = SwingRendererUtils
												.findParentFormModificationStack(listControl, GUI.INSTANCE);
										modifStack.apply(new ListModificationFactory(itemPosition)
												.set(itemPosition.getIndex(), item));
										listControl.setSingleSelection(itemPosition);
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

		private boolean isLeafType(ITypeInfo type) {
			if (!MiscUtils.isComplexType(type)) {
				return true;
			}
			if (type.getName().equals(Object.class.getName())) {
				return true;
			}
			return false;
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
