package com.otk.jesb.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.JXTreeTable;

import com.otk.jesb.PathExplorer;
import com.otk.jesb.PathExplorer.FieldNode;
import com.otk.jesb.PathExplorer.ListItemNode;
import com.otk.jesb.PathExplorer.PathNode;
import com.otk.jesb.PathExplorer.RelativePathNode;
import com.otk.jesb.Plan;
import com.otk.jesb.instantiation.CompilationContext;
import com.otk.jesb.instantiation.Facade;
import com.otk.jesb.instantiation.FacadeOutline;
import com.otk.jesb.instantiation.FieldInitializerFacade;
import com.otk.jesb.instantiation.Function;
import com.otk.jesb.instantiation.InstanceBuilderFacade;
import com.otk.jesb.instantiation.ListItemInitializerFacade;
import com.otk.jesb.instantiation.ListItemReplication;
import com.otk.jesb.instantiation.ListItemReplicationFacade;
import com.otk.jesb.instantiation.ParameterInitializerFacade;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.instantiation.RootInstanceBuilderFacade;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.Listener;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.Pair;

import xy.reflect.ui.control.IAdvancedFieldControl;
import xy.reflect.ui.control.IFieldControlInput;
import xy.reflect.ui.control.swing.ListControl;
import xy.reflect.ui.control.swing.renderer.FieldControlPlaceHolder;
import xy.reflect.ui.control.swing.renderer.Form;
import xy.reflect.ui.control.swing.renderer.SwingRenderer;
import xy.reflect.ui.control.swing.util.SwingRendererUtils;
import xy.reflect.ui.info.menu.MenuModel;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.iterable.item.BufferedItemPosition;
import xy.reflect.ui.undo.ListModificationFactory;
import xy.reflect.ui.undo.ModificationStack;

public class MappingsControl extends JPanel implements IAdvancedFieldControl {

	private static final long serialVersionUID = 1L;

	private InstanceBuilderVariableTreeControl foundSourceControl;
	private InstanceBuilderInitializerTreeControl foundTargetControl;

	private Set<Pair<BufferedItemPosition, BufferedItemPosition>> mappingsCache;

	@Override
	public boolean showsCaption() {
		return false;
	}

	@Override
	public boolean refreshUI(boolean refreshStructure) {
		return true;
	}

	@Override
	public void validateSubForms() throws Exception {
	}

	@Override
	public void addMenuContributions(MenuModel menuModel) {
	}

	@Override
	public boolean requestCustomFocus() {
		return false;
	}

	@Override
	public boolean isAutoManaged() {
		return false;
	}

	@Override
	public boolean displayError(String msg) {
		return false;
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		List<Pair<BufferedItemPosition, BufferedItemPosition>> allMappings = new ArrayList<Pair<BufferedItemPosition, BufferedItemPosition>>(
				estimateVisibleMappings());
		final Set<Pair<BufferedItemPosition, BufferedItemPosition>> highlightedMappings = estimateHighlightedVisibleMappings();
		Collections.sort(allMappings, new Comparator<Pair<BufferedItemPosition, BufferedItemPosition>>() {
			@Override
			public int compare(Pair<BufferedItemPosition, BufferedItemPosition> o1,
					Pair<BufferedItemPosition, BufferedItemPosition> o2) {
				return ((Boolean) highlightedMappings.contains(o1))
						.compareTo(((Boolean) highlightedMappings.contains(o2)));
			}
		});
		for (Pair<BufferedItemPosition, BufferedItemPosition> mapping : allMappings) {
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
			g.setColor(
					highlightedMappings.contains(mapping) ? getHighlightedMappingLinesColor() : getMappingLinesColor());
			((Graphics2D) g).setStroke(new BasicStroke(getMappingLinesThickness()));
			g.drawLine(0, sourceY, MappingsControl.this.getWidth(), targetY);
		}

	}

	public static int getMappingLinesThickness() {
		return 2;
	}

	public static Color getMappingLinesColor() {
		return new Color(190, 190, 255);
	}

	public static Color getHighlightedMappingLinesColor() {
		return new Color(0, 0, 230);
	}

	public static Color getConcreteElementTextColor() {
		return Color.BLACK;
	}

	public static Color getAbstractElementTextColor() {
		return Color.LIGHT_GRAY;
	}

	private static PathNode relativizePathNode(PathNode pathNode, Facade initializerFacade) {
		if (initializerFacade.getParent() != null) {
			pathNode = relativizePathNode(pathNode, initializerFacade.getParent());
		}
		if (initializerFacade instanceof ListItemInitializerFacade) {
			ListItemInitializerFacade listItemInitializerFacade = (ListItemInitializerFacade) initializerFacade;
			if (listItemInitializerFacade.getItemReplicationFacade() != null) {
				ListItemReplicationFacade itemReplicationFacade = listItemInitializerFacade.getItemReplicationFacade();
				if (itemReplicationFacade.getIterationListValue() instanceof Function) {
					Function replicationFunction = (Function) itemReplicationFacade.getIterationListValue();
					PathNode pathNodeOrAncestor = pathNode;
					while (pathNodeOrAncestor != null) {
						if (unrelativizePathNode(pathNodeOrAncestor) instanceof ListItemNode) {
							String functionBodyPattern = "^\\s*return\\s+"
									+ pathNodeOrAncestor.getParent().getExpressionPattern() + "\\s*;\\s*$";
							if (Pattern.compile(functionBodyPattern, Pattern.DOTALL)
									.matcher(replicationFunction.getFunctionBody()).matches()) {
								return new RelativePathNode(pathNode, pathNodeOrAncestor.getTypicalExpression(),
										pathNodeOrAncestor.getExpressionPattern(),
										itemReplicationFacade.getIterationVariableName());
							}
						}
						pathNodeOrAncestor = pathNodeOrAncestor.getParent();
					}
				}
			}
		}
		return pathNode;
	}

	private static PathNode unrelativizePathNode(PathNode pathNode) {
		if (pathNode instanceof RelativePathNode) {
			return unrelativizePathNode(((RelativePathNode) pathNode).getUnderlying());
		}
		return pathNode;
	}

	private static BufferedItemPosition getMappingItemPosition(Pair<BufferedItemPosition, BufferedItemPosition> pair,
			Side side) {
		if (side == Side.SOURCE) {
			return pair.getFirst();
		} else if (side == Side.TARGET) {
			return pair.getSecond();
		} else {
			throw new AssertionError();
		}
	}

	public Set<Pair<BufferedItemPosition, BufferedItemPosition>> estimateHighlightedVisibleMappings() {
		InstanceBuilderVariableTreeControl sourceControl = findSourceControl();
		if (sourceControl == null) {
			return Collections.emptySet();
		}
		InstanceBuilderInitializerTreeControl targetControl = findTargetControl();
		if (targetControl == null) {
			return Collections.emptySet();
		}
		List<BufferedItemPosition> sourceSelection = sourceControl.getSelection();
		List<BufferedItemPosition> targetSelection = targetControl.getSelection();
		return estimateVisibleMappings().stream()
				.filter(pair -> sourceSelection.contains(getMappingItemPosition(pair, Side.SOURCE))
						|| targetSelection.contains(getMappingItemPosition(pair, Side.TARGET)))
				.collect(Collectors.toSet());
	}

	public Set<Pair<BufferedItemPosition, BufferedItemPosition>> estimateVisibleMappings() {
		InstanceBuilderVariableTreeControl sourceControl = findSourceControl();
		if (sourceControl == null) {
			return Collections.emptySet();
		}
		InstanceBuilderInitializerTreeControl targetControl = findTargetControl();
		if (targetControl == null) {
			return Collections.emptySet();
		}
		Set<Pair<BufferedItemPosition, BufferedItemPosition>> result = estimateMappings();
		return result.stream().map(pair -> {
			BufferedItemPosition sourceItemPosition = pair.getFirst();
			BufferedItemPosition targetItemPosition = pair.getSecond();
			while ((sourceItemPosition.getParentItemPosition() != null)
					&& !sourceControl.isItemPositionExpanded(sourceItemPosition.getParentItemPosition())) {
				sourceItemPosition = sourceItemPosition.getParentItemPosition();
			}
			while ((targetItemPosition.getParentItemPosition() != null)
					&& !targetControl.isItemPositionExpanded(targetItemPosition.getParentItemPosition())) {
				targetItemPosition = targetItemPosition.getParentItemPosition();
			}
			return new Pair<BufferedItemPosition, BufferedItemPosition>(sourceItemPosition, targetItemPosition);
		}).collect(Collectors.toSet());
	}

	public Set<Pair<BufferedItemPosition, BufferedItemPosition>> estimateMappings() {
		if (mappingsCache == null) {
			Set<Pair<BufferedItemPosition, BufferedItemPosition>> result = new HashSet<Pair<BufferedItemPosition, BufferedItemPosition>>();
			InstanceBuilderVariableTreeControl sourceControl = findSourceControl();
			if (sourceControl == null) {
				return Collections.emptySet();
			}
			InstanceBuilderInitializerTreeControl targetControl = findTargetControl();
			if (targetControl == null) {
				return Collections.emptySet();
			}
			sourceControl.visitItems(new ListControl.IItemsVisitor() {
				boolean mappingFound;

				@Override
				public VisitStatus visitItem(BufferedItemPosition sourceItemPosition) {
					mappingFound = false;
					if (sourceItemPosition.getItem() instanceof PathNode) {
						PathNode pathNode = (PathNode) sourceItemPosition.getItem();
						targetControl.visitItems(new ListControl.IItemsVisitor() {
							@Override
							public VisitStatus visitItem(BufferedItemPosition targetItemPosition) {
								if (!(targetItemPosition.getItem() instanceof Facade)) {
									return VisitStatus.BRANCH_VISIT_INTERRUPTED;
								}
								final Facade initializerFacade = (Facade) targetItemPosition.getItem();
								if (!initializerFacade.isConcrete()) {
									return VisitStatus.BRANCH_VISIT_INTERRUPTED;
								}
								List<Function> functions = new ArrayList<Function>();
								if (initializerFacade instanceof ParameterInitializerFacade) {
									if (((ParameterInitializerFacade) initializerFacade)
											.getParameterValue() instanceof Function) {
										functions.add((Function) ((ParameterInitializerFacade) initializerFacade)
												.getParameterValue());
									}
								} else if (initializerFacade instanceof FieldInitializerFacade) {
									if (((FieldInitializerFacade) initializerFacade)
											.getFieldValue() instanceof Function) {
										functions.add((Function) ((FieldInitializerFacade) initializerFacade)
												.getFieldValue());
									}
									if (((FieldInitializerFacade) initializerFacade).getCondition() != null) {
										functions.add(((FieldInitializerFacade) initializerFacade).getCondition());
									}
								} else if (initializerFacade instanceof ListItemInitializerFacade) {
									if (((ListItemInitializerFacade) initializerFacade)
											.getItemValue() instanceof Function) {
										functions.add((Function) ((ListItemInitializerFacade) initializerFacade)
												.getItemValue());
									}
									if (((ListItemInitializerFacade) initializerFacade)
											.getItemReplicationFacade() != null) {
										ListItemReplicationFacade replication = ((ListItemInitializerFacade) initializerFacade)
												.getItemReplicationFacade();
										if (replication.getIterationListValue() instanceof Function) {
											functions.add((Function) replication.getIterationListValue());
										}
									}
								}
								for (Function function : functions) {
									if (Pattern
											.compile(".*" + relativizePathNode(pathNode, initializerFacade)
													.getExpressionPattern() + ".*", Pattern.DOTALL)
											.matcher(function.getFunctionBody()).matches()) {
										result.add(new Pair<BufferedItemPosition, BufferedItemPosition>(
												sourceItemPosition, targetItemPosition));
										mappingFound = true;
									}

								}
								return VisitStatus.VISIT_NOT_INTERRUPTED;
							}
						});
					}
					return mappingFound ? VisitStatus.VISIT_NOT_INTERRUPTED : VisitStatus.BRANCH_VISIT_INTERRUPTED;
				}
			});
			mappingsCache = result.stream()
					.filter(pair -> result.stream()
							.noneMatch(otherPair -> pair.getSecond().equals(otherPair.getSecond())
									&& pair.getFirst().equals(otherPair.getFirst().getParentItemPosition())))
					.collect(Collectors.toSet());
		}
		return mappingsCache;
	}

	public void resetMappingsCache() {
		mappingsCache = null;
	}

	public InstanceBuilderInitializerTreeControl findTargetControl() {
		return findControl(this, InstanceBuilderInitializerTreeControl.class,
				new Accessor<InstanceBuilderInitializerTreeControl>() {

					@Override
					public InstanceBuilderInitializerTreeControl get() {
						return foundTargetControl;
					}

					@Override
					public void set(InstanceBuilderInitializerTreeControl t) {
						foundTargetControl = t;
					}

				}, null);
	}

	public InstanceBuilderVariableTreeControl findSourceControl() {
		return findControl(this, InstanceBuilderVariableTreeControl.class,
				new Accessor<InstanceBuilderVariableTreeControl>() {

					@Override
					public InstanceBuilderVariableTreeControl get() {
						return foundSourceControl;
					}

					@Override
					public void set(InstanceBuilderVariableTreeControl t) {
						foundSourceControl = t;
					}

				}, null);
	}

	public SideControl findSideControl(Side side) {
		if (side == Side.SOURCE) {
			return findSourceControl();
		} else if (side == Side.TARGET) {
			return findTargetControl();
		} else {
			throw new AssertionError();
		}
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

	public enum Side {
		SOURCE, TARGET;

		public static Side getOther(Side side) {
			if (side == SOURCE) {
				return TARGET;
			} else if (side == TARGET) {
				return SOURCE;
			} else {
				throw new AssertionError();
			}
		}
	}

	public static abstract class SideControl extends ListControl {

		private static final long serialVersionUID = 1L;

		private MappingsControl foundMappingsControl;

		protected abstract Side getSide();

		public SideControl(SwingRenderer swingRenderer, IFieldControlInput input) {
			super(swingRenderer, input);
		}

		public JXTreeTable getTreeTableComponent() {
			return treeTableComponent;
		}

		protected void reestimateAndRepaintMappings() {
			MappingsControl mappingsControl = findMappingsControl();
			if (mappingsControl != null) {
				mappingsControl.repaint();
			} else {
				return;
			}
			mappingsControl.resetMappingsCache();
			InstanceBuilderVariableTreeControl sourceControl = mappingsControl.findSourceControl();
			if (sourceControl != null) {
				sourceControl.repaint();
			}
			InstanceBuilderInitializerTreeControl targetControl = mappingsControl.findTargetControl();
			if (targetControl != null) {
				targetControl.repaint();
			}
		}

		@Override
		protected void initializeTreeTableModelAndControl() {
			super.initializeTreeTableModelAndControl();
			treeTableComponent.addTreeExpansionListener(new TreeExpansionListener() {

				@Override
				public void treeExpanded(TreeExpansionEvent event) {
					reestimateAndRepaintMappings();
				}

				@Override
				public void treeCollapsed(TreeExpansionEvent event) {
					reestimateAndRepaintMappings();
				}
			});
			((JScrollPane) treeTableComponent.getParent().getParent()).getVerticalScrollBar()
					.addAdjustmentListener(new AdjustmentListener() {
						@Override
						public void adjustmentValueChanged(AdjustmentEvent e) {
							reestimateAndRepaintMappings();
						}
					});
			treeTableComponent.addTreeSelectionListener(new TreeSelectionListener() {
				@Override
				public void valueChanged(TreeSelectionEvent e) {
					preventSelectionOnBothSides();
					reestimateAndRepaintMappings();
				}

				private void preventSelectionOnBothSides() {
					if (getSelection().size() == 0) {
						return;
					}
					MappingsControl mappingsControl = findMappingsControl();
					if (mappingsControl == null) {
						return;
					}
					SideControl otherSideControl = mappingsControl.findSideControl(Side.getOther(getSide()));
					if (otherSideControl == null) {
						return;
					}
					otherSideControl.setSelection(Collections.emptyList());
				}
			});
		}

		@Override
		protected JPopupMenu createPopupMenu() {
			JPopupMenu result = super.createPopupMenu();
			result.add(new AbstractAction() {

				private static final long serialVersionUID = 1L;

				@Override
				public Object getValue(String key) {
					if (Action.NAME.equals(key)) {
						return "Reveal Mappings";
					} else {
						return super.getValue(key);
					}
				}

				@Override
				public boolean isEnabled() {
					BufferedItemPosition selectedItemPosition = getSingleSelection();
					if (selectedItemPosition == null) {
						return false;
					}
					TreePath treePath = getTreePath(selectedItemPosition);
					int rowIndex = treeTableComponent.getRowForPath(treePath);
					return isVisiblyMapped(rowIndex);
				}

				@Override
				public void actionPerformed(ActionEvent e) {
					BufferedItemPosition selectedItemPosition = getSingleSelection();
					MappingsControl mappingsControl = findMappingsControl();
					SideControl otherSideControl = mappingsControl.findSideControl(Side.getOther(getSide()));
					if (mappingsControl != null) {
						for (Pair<BufferedItemPosition, BufferedItemPosition> pair : mappingsControl
								.estimateMappings()) {
							BufferedItemPosition thisSideMappedItemPosition = getMappingItemPosition(pair, getSide());
							final boolean mappingSelected;
							if (isItemPositionExpanded(selectedItemPosition)) {
								mappingSelected = thisSideMappedItemPosition.equals(selectedItemPosition);
							} else {
								mappingSelected = thisSideMappedItemPosition.equals(selectedItemPosition)
										|| thisSideMappedItemPosition.getAncestors().contains(selectedItemPosition);
							}
							if (mappingSelected) {
								BufferedItemPosition otherSideItemPosition = getMappingItemPosition(pair,
										Side.getOther(getSide()));
								if (otherSideItemPosition.getParentItemPosition() != null) {
									otherSideControl.expandItemPosition(otherSideItemPosition.getParentItemPosition());
									otherSideControl.scrollTo(otherSideItemPosition);
								}
							}
						}
					}
				}

			});
			return result;
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
			label.setBorder(
					isVisiblyMapped(rowIndex)
							? BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1),
									BorderFactory
											.createLineBorder(isHighlighted(node) ? getHighlightedMappingLinesColor()
													: getMappingLinesColor(), getMappingLinesThickness()))
							: BorderFactory.createEmptyBorder());
			label.setText(getCellValue(node, columnIndex));
		}

		@Override
		protected String getCellValue(ItemNode node, int columnIndex) {
			String result = super.getCellValue(node, columnIndex);
			if (result == null) {
				return null;
			}
			result += "   ";
			for (int i = 0; i < getMappingLinesThickness(); i++) {
				result += "  ";
			}
			return result;
		}

		protected boolean isVisiblyMapped(int rowIndex) {
			if (rowIndex == -1) {
				return false;
			}
			MappingsControl mappingsControl = findMappingsControl();
			if (mappingsControl != null) {
				for (Pair<BufferedItemPosition, BufferedItemPosition> pair : mappingsControl
						.estimateVisibleMappings()) {
					BufferedItemPosition itemPosition = getMappingItemPosition(pair, getSide());
					TreePath treePath = getTreePath(itemPosition);
					int row = treeTableComponent.getRowForPath(treePath);
					if (rowIndex == row) {
						return true;
					}
				}
			}
			return false;
		}

		protected boolean isHighlighted(ItemNode node) {
			BufferedItemPosition itemPosition = getItemPositionByNode(node);
			if (itemPosition == null) {
				return false;
			}
			MappingsControl mappingsControl = findMappingsControl();
			if (mappingsControl != null) {
				for (Pair<BufferedItemPosition, BufferedItemPosition> pair : mappingsControl
						.estimateHighlightedVisibleMappings()) {
					BufferedItemPosition highlightedMappingItemPosition = getMappingItemPosition(pair, getSide());
					if (highlightedMappingItemPosition.equals(itemPosition)) {
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
							InstanceBuilderInitializerTreeControl initializerTreeControl = (InstanceBuilderInitializerTreeControl) ((JXTreeTable) component)
									.getClientProperty(InstanceBuilderInitializerTreeControl.class);
							if (initializerTreeControl != null) {
								Point dropPoint = support.getDropLocation().getDropPoint();
								TreePath treePath = ((JXTreeTable) component).getPathForLocation(dropPoint.x,
										dropPoint.y);
								if (treePath != null) {
									BufferedItemPosition initializerPosition = (BufferedItemPosition) ((DefaultMutableTreeNode) treePath
											.getLastPathComponent()).getUserObject();
									if (initializerPosition.getItem() instanceof Facade) {
										Facade initializerFacade = (Facade) initializerPosition.getItem();
										RootInstanceBuilder rootInstanceBuilder = ((RootInstanceBuilderFacade) Facade
												.getRoot(initializerFacade)).getUnderlying();
										JESBReflectionUI.backupRootInstanceBuilderState(rootInstanceBuilder);
										PathNode pathNode = (PathNode) data;
										pathNode = relativizePathNode(pathNode, initializerFacade);
										try {
											accept = map(pathNode, initializerPosition, initializerTreeControl);
										} catch (CancellationException e) {
											JESBReflectionUI
													.getRootInstanceBuilderStateRestorationJob(rootInstanceBuilder)
													.run();
											initializerTreeControl.refreshUI(false);
										}
										if (accept) {
											ModificationStack modifStack = SwingRendererUtils
													.findParentFormModificationStack(initializerTreeControl,
															GUI.INSTANCE);
											modifStack.apply(new ListModificationFactory(initializerPosition)
													.set(initializerPosition.getIndex(), initializerFacade));
										}
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

		private void beforeMpping(BufferedItemPosition initializerPosition,
				InstanceBuilderInitializerTreeControl initializerTreeControl) {
			initializerTreeControl.setSingleSelection(initializerPosition);
			initializerTreeControl.findMappingsControl().resetMappingsCache();
			for (JComponent component : new JComponent[] { initializerTreeControl,
					initializerTreeControl.findMappingsControl(),
					initializerTreeControl.findMappingsControl().findSourceControl() })
				component.paintImmediately(0, 0, component.getWidth(), component.getHeight());
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				throw new AssertionError(e);
			}
		}

		private boolean map(PathNode pathNode, BufferedItemPosition initializerPosition,
				InstanceBuilderInitializerTreeControl initializerTreeControl) throws CancellationException {
			boolean accept = false;
			Facade initializerFacade = (Facade) initializerPosition.getItem();
			if (initializerFacade instanceof ParameterInitializerFacade) {
				beforeMpping(initializerPosition, initializerTreeControl);
				accept = map(pathNode, initializerPosition, new Supplier<ITypeInfo>() {
					@Override
					public ITypeInfo get() {
						return ((ParameterInitializerFacade) initializerFacade).getParameterInfo().getType();
					}
				}, new Accessor<Object>() {
					@Override
					public Object get() {
						return ((ParameterInitializerFacade) initializerFacade).getParameterValue();
					}

					@Override
					public void set(Object value) {
						((ParameterInitializerFacade) initializerFacade).setParameterValue(value);
					}
				}, initializerTreeControl);
			} else if (initializerFacade instanceof FieldInitializerFacade) {
				beforeMpping(initializerPosition, initializerTreeControl);
				accept = map(pathNode, initializerPosition, new Supplier<ITypeInfo>() {
					@Override
					public ITypeInfo get() {
						return ((FieldInitializerFacade) initializerFacade).getFieldInfo().getType();
					}
				}, new Accessor<Object>() {
					@Override
					public Object get() {
						return ((FieldInitializerFacade) initializerFacade).getFieldValue();
					}

					@Override
					public void set(Object value) {
						((FieldInitializerFacade) initializerFacade).setFieldValue(value);
					}
				}, initializerTreeControl);
			} else if (initializerFacade instanceof ListItemInitializerFacade) {
				beforeMpping(initializerPosition, initializerTreeControl);
				accept = mapListItemReplication(pathNode, (ListItemInitializerFacade) initializerFacade,
						initializerTreeControl);
				ListItemReplicationFacade replicationFacade = ((ListItemInitializerFacade) initializerFacade)
						.getItemReplicationFacade();
				accept = map((replicationFacade != null)
						? new PathExplorer.RelativePathNode(pathNode, pathNode.getTypicalExpression(),
								pathNode.getExpressionPattern(), replicationFacade.getIterationVariableName())
						: pathNode, initializerPosition, new Supplier<ITypeInfo>() {
							@Override
							public ITypeInfo get() {
								return ((ListItemInitializerFacade) initializerFacade).getItemType();
							}
						}, new Accessor<Object>() {
							@Override
							public Object get() {
								return ((ListItemInitializerFacade) initializerFacade).getItemValue();
							}

							@Override
							public void set(Object value) {
								((ListItemInitializerFacade) initializerFacade).setItemValue(value);
							}
						}, initializerTreeControl) || accept;
			}
			return accept;
		}

		private boolean map(PathNode pathNode, BufferedItemPosition initializerPosition,
				Supplier<ITypeInfo> targetTypeSupplier, Accessor<Object> targetValueAccessor,
				InstanceBuilderInitializerTreeControl initializerTreeControl) throws CancellationException {
			boolean accept = false;
			Facade initializerFacade = (Facade) initializerPosition.getItem();
			if (targetValueAccessor.get() instanceof Function) {
				int choice = openMappingOptionSelectionDialog(
						Arrays.asList("Rewrite the existing function", "Do not rewrite the existing function"),
						pathNode, initializerFacade, initializerTreeControl);
				if (choice == 0) {
					targetValueAccessor.set(new Function("return " + pathNode.getTypicalExpression() + ";"));
					accept = true;
				} else if (choice == 1) {
					// do nothing
				} else {
					throw new CancellationException();
				}
			} else {
				if (isLeafType(pathNode.getExpressionType()) || isLeafType(targetTypeSupplier.get())) {
					targetValueAccessor.set(new Function("return " + pathNode.getTypicalExpression() + ";"));
					accept = true;
				} else {
					int choice = openMappingOptionSelectionDialog(
							Arrays.asList("Assign source value to target",
									"Map corresponding children (same name) of source and target values", "Do not map"),
							pathNode, initializerFacade, initializerTreeControl);
					if (choice == 0) {
						targetValueAccessor.set(new Function("return " + pathNode.getTypicalExpression() + ";"));
						accept = true;
					} else if (choice == 1) {
						initializerFacade.setConcrete(true);
						for (BufferedItemPosition subInitializerPosition : initializerPosition.getSubItemPositions()) {
							Facade initializerFacadeChild = (Facade) subInitializerPosition.getItem();
							for (PathNode pathNodeChild : pathNode.getChildren()) {
								if (initializerFacadeChild.toString().replaceAll("^\\[([0-9]+)\\]$", "[i]")
										.replaceAll("[^0-9a-zA-Z]", "").toLowerCase().equals(pathNodeChild.toString()
												.replaceAll("[^0-9a-zA-Z]", "").toLowerCase())) {
									accept = map(pathNodeChild, subInitializerPosition, initializerTreeControl)
											|| accept;
									break;
								}
							}
						}
						accept = true;
					} else if (choice == 2) {
						// do nothing
					} else {
						throw new CancellationException();
					}
				}
			}
			return accept;
		}

		private boolean mapListItemReplication(PathNode pathNode, ListItemInitializerFacade listItemInitializerFacade,
				InstanceBuilderInitializerTreeControl initializerTreeControl) {
			boolean accept = false;
			if (unrelativizePathNode(pathNode) instanceof ListItemNode) {
				int choice = openMappingOptionSelectionDialog(
						Arrays.asList("Replicate the target value for each source value",
								"Do not replicate the target value"),
						pathNode, listItemInitializerFacade, initializerTreeControl);
				if (choice == 0) {
					ListItemReplication itemReplication = new ListItemReplication();
					itemReplication.setIterationListValue(
							new Function("return " + pathNode.getParent().getTypicalExpression() + ";"));
					itemReplication.setIterationListValueTypeName(pathNode.getParent().getExpressionType().getName());
					itemReplication.setIterationVariableTypeName(pathNode.getExpressionType().getName());
					((ListItemInitializerFacade) listItemInitializerFacade).setConcrete(true);
					((ListItemInitializerFacade) listItemInitializerFacade).getUnderlying()
							.setItemReplication(itemReplication);

					if (unrelativizePathNode(pathNode.getParent()) instanceof FieldNode) {
						String parentFieldName = ((FieldNode) unrelativizePathNode(pathNode.getParent()))
								.getFieldName();
						itemReplication
								.setIterationVariableName("current" + parentFieldName.substring(0, 1).toUpperCase()
										+ parentFieldName.substring(1) + "Item");
					}
					CompilationContext compilationContext = ((InstanceBuilderFacade) Facade
							.getRoot(listItemInitializerFacade)).findFunctionCompilationContext(
									(Function) itemReplication.getIterationListValue(),
									new Plan.ValidationContext(new Plan()));
					final String NUMBERED_NAME_PATTERN = "^(.*)([0-9]+)$";
					while (true) {
						boolean nameConflictDetected = compilationContext.getValidationContext()
								.getVariableDeclarations().stream().anyMatch(variableDeclaration -> variableDeclaration
										.getVariableName().equals(itemReplication.getIterationVariableName()));
						if (!nameConflictDetected) {
							break;
						}
						if (!itemReplication.getIterationVariableName().matches(NUMBERED_NAME_PATTERN)) {
							itemReplication.setIterationVariableName(itemReplication.getIterationVariableName() + "1");
						} else {
							int number = Integer.valueOf(
									itemReplication.getIterationVariableName().replaceAll(NUMBERED_NAME_PATTERN, "$2"));
							itemReplication.setIterationVariableName(
									itemReplication.getIterationVariableName().replaceAll(NUMBERED_NAME_PATTERN, "$1")
											+ (number + 1));
						}
					}
					accept = true;
				} else if (choice == 1) {
					// do nothing
				} else {
					throw new CancellationException();
				}
			}
			return accept;
		}

		private int openMappingOptionSelectionDialog(List<String> options, PathNode pathNode, Facade initializerFacade,
				InstanceBuilderInitializerTreeControl initializerTreeControl) {
			String choice = GUI.INSTANCE.openSelectionDialog(initializerTreeControl, options, null,
					"Choose a mapping option for: " + pathNode.toString() + " => " + initializerFacade.toString(),
					"Mapping");
			return options.indexOf(choice);
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
