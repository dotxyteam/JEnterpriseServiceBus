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
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;
import java.util.regex.Pattern;
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
import com.otk.jesb.PathExplorer.RelativePathNode;
import com.otk.jesb.instantiation.Facade;
import com.otk.jesb.instantiation.FacadeOutline;
import com.otk.jesb.instantiation.FieldInitializerFacade;
import com.otk.jesb.instantiation.Function;
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
				if (!(targetItemPosition.getItem() instanceof Facade)) {
					return VisitStatus.BRANCH_VISIT_INTERRUPTED;
				}
				final Facade initializerFacade = (Facade) targetItemPosition.getItem();
				if (!initializerFacade.isConcrete()) {
					return VisitStatus.BRANCH_VISIT_INTERRUPTED;
				}
				List<Function> functions = new ArrayList<Function>();
				if (initializerFacade instanceof ParameterInitializerFacade) {
					if (((ParameterInitializerFacade) initializerFacade).getParameterValue() instanceof Function) {
						functions.add((Function) ((ParameterInitializerFacade) initializerFacade).getParameterValue());
					}
				} else if (initializerFacade instanceof FieldInitializerFacade) {
					if (((FieldInitializerFacade) initializerFacade).getFieldValue() instanceof Function) {
						functions.add((Function) ((FieldInitializerFacade) initializerFacade).getFieldValue());
					}
					if (((FieldInitializerFacade) initializerFacade).getCondition() != null) {
						functions.add(((FieldInitializerFacade) initializerFacade).getCondition());
					}
				} else if (initializerFacade instanceof ListItemInitializerFacade) {
					if (((ListItemInitializerFacade) initializerFacade).getItemValue() instanceof Function) {
						functions.add((Function) ((ListItemInitializerFacade) initializerFacade).getItemValue());
					}
					if (((ListItemInitializerFacade) initializerFacade).getItemReplicationFacade() != null) {
						ListItemReplicationFacade replication = ((ListItemInitializerFacade) initializerFacade)
								.getItemReplicationFacade();
						if (replication.getIterationListValue() instanceof Function) {
							functions.add((Function) replication.getIterationListValue());
						}
					}
				}
				Set<BufferedItemPosition> mappedSourceItemPositions = new HashSet<BufferedItemPosition>();
				for (Function function : functions) {
					sourceControl.visitItems(new ListControl.IItemsVisitor() {
						@Override
						public VisitStatus visitItem(BufferedItemPosition sourceItemPosition) {
							if (sourceItemPosition.getItem() instanceof PathNode) {
								PathNode pathNode = (PathNode) sourceItemPosition.getItem();
								List<String> pathExpressionPatterns = new ArrayList<String>();
								pathExpressionPatterns.add(pathNode.getExpressionPattern());
								for (RelativePathNode relativePathNode : relativizePathNode(pathNode,
										initializerFacade)) {
									pathExpressionPatterns.add(relativePathNode.getExpressionPattern());
								}
								for (String pathExpressionPattern : pathExpressionPatterns) {
									if (Pattern.compile(".*" + pathExpressionPattern + ".*", Pattern.DOTALL)
											.matcher(function.getFunctionBody()).matches()) {
										mappedSourceItemPositions.add(sourceItemPosition);
										break;
									}
								}
							}
							return (!sourceControl.isItemPositionExpanded(sourceItemPosition))
									? VisitStatus.BRANCH_VISIT_INTERRUPTED
									: VisitStatus.VISIT_NOT_INTERRUPTED;
						}

						private List<RelativePathNode> relativizePathNode(PathNode pathNode, Facade initializerFacade) {
							List<RelativePathNode> result = new ArrayList<PathExplorer.RelativePathNode>();
							List<Facade> initializerFacadeAndAncestors = new ArrayList<Facade>();
							initializerFacadeAndAncestors.add(initializerFacade);
							initializerFacadeAndAncestors.addAll(Facade.getAncestors(initializerFacade));
							for (Facade initializerFacadeAncestor : initializerFacadeAndAncestors) {
								if (initializerFacadeAncestor instanceof ListItemInitializerFacade) {
									ListItemInitializerFacade listItemInitializerFacade = (ListItemInitializerFacade) initializerFacadeAncestor;
									if (listItemInitializerFacade.getItemReplicationFacade() != null) {
										ListItemReplicationFacade itemReplicationFacade = listItemInitializerFacade
												.getItemReplicationFacade();
										if (itemReplicationFacade.getIterationListValue() instanceof Function) {
											Function listFunction = (Function) itemReplicationFacade
													.getIterationListValue();
											PathNode pathNodeOrAncestor = pathNode;
											while (pathNodeOrAncestor != null) {
												if (pathNodeOrAncestor instanceof ListItemNode) {
													String functionBodyPattern = "^\\s*return\\s+"
															+ pathNodeOrAncestor.getParent().getExpressionPattern()
															+ "\\s*;\\s*$";
													if (Pattern.compile(functionBodyPattern, Pattern.DOTALL)
															.matcher(listFunction.getFunctionBody()).matches()) {
														result.add(new RelativePathNode(pathNode,
																pathNodeOrAncestor.getTypicalExpression(),
																pathNodeOrAncestor.getExpressionPattern(),
																itemReplicationFacade.getIterationVariableName()));
														break;
													}
												}
												pathNodeOrAncestor = pathNodeOrAncestor.getParent();
											}
										}
									}
								}
							}
							return result;
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
							InstanceBuilderInitializerTreeControl initializerTreeControl = (InstanceBuilderInitializerTreeControl) ((JXTreeTable) component)
									.getClientProperty(InstanceBuilderInitializerTreeControl.class);
							if (initializerTreeControl != null) {
								Point dropPoint = support.getDropLocation().getDropPoint();
								TreePath treePath = ((JXTreeTable) component).getPathForLocation(dropPoint.x,
										dropPoint.y);
								if (treePath != null) {
									BufferedItemPosition itemPosition = (BufferedItemPosition) ((DefaultMutableTreeNode) treePath
											.getLastPathComponent()).getUserObject();
									Object item = itemPosition.getItem();
									if (item instanceof Facade) {
										Facade initializerFacade = (Facade) item;
										PathNode pathNode = (PathNode) data;
										RootInstanceBuilder rootInstanceBuilder = ((RootInstanceBuilderFacade) Facade
												.getRoot(initializerFacade)).getUnderlying();
										JESBReflectionUI.backupRootInstanceBuilderState(rootInstanceBuilder);
										try {
											accept = map(pathNode, initializerFacade, component);
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
											modifStack.apply(new ListModificationFactory(itemPosition)
													.set(itemPosition.getIndex(), initializerFacade));
											initializerTreeControl.setSingleSelection(itemPosition);
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

		private boolean map(PathNode pathNode, Facade initializerFacade, Component targetComponent)
				throws CancellationException {
			boolean accept = false;
			if (initializerFacade instanceof ParameterInitializerFacade) {
				accept = map(pathNode, initializerFacade, new Supplier<ITypeInfo>() {
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
				}, targetComponent);
			} else if (initializerFacade instanceof FieldInitializerFacade) {
				accept = map(pathNode, initializerFacade, new Supplier<ITypeInfo>() {
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
				}, targetComponent);
			} else if (initializerFacade instanceof ListItemInitializerFacade) {
				final ListItemReplication itemReplication;
				if (pathNode instanceof ListItemNode) {
					List<String> options = Arrays.asList("Replicate the target value for each source value",
							"Do not replicate the target value");
					String choice = GUI.INSTANCE.openSelectionDialog(targetComponent, options, null,
							"Choose a mapping option for: " + pathNode.toString() + " => "
									+ initializerFacade.toString(),
							"Mapping");
					if (choice == options.get(0)) {
						itemReplication = new ListItemReplication();
						itemReplication.setIterationListValue(new Function(
								"return " + ((ListItemNode) pathNode).getParent().getTypicalExpression() + ";"));
						itemReplication.setIterationListValueTypeName(
								((ListItemNode) pathNode).getParent().getExpressionType().getName());
						itemReplication
								.setIterationVariableTypeName(((ListItemNode) pathNode).getExpressionType().getName());
						((ListItemInitializerFacade) initializerFacade).setConcrete(true);
						((ListItemInitializerFacade) initializerFacade).getUnderlying()
								.setItemReplication(itemReplication);
						accept = true;
					} else if (choice == options.get(1)) {
						itemReplication = null;
					} else {
						throw new CancellationException();
					}
				} else {
					itemReplication = null;
				}
				accept = map((itemReplication != null)
						? new PathExplorer.RelativePathNode(pathNode, pathNode.getTypicalExpression(),
								pathNode.getExpressionPattern(), itemReplication.getIterationVariableName())
						: pathNode, initializerFacade, new Supplier<ITypeInfo>() {
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
						}, targetComponent);
			}
			return accept;
		}

		private boolean map(PathNode pathNode, Facade initializerFacade, Supplier<ITypeInfo> targetTypeSupplier,
				Accessor<Object> targetValueAccessor, Component targetComponent) throws CancellationException {
			/*
			 * Any source node can be mapped to a target node, even if the types are
			 * incompatible. Actually the intent of the developer may be to generate a base
			 * expression and modify it. The role of the UI is to automate common tasks,
			 * provide simpler representations of data, and warn the developer early when
			 * something is wrong. If a specific mapping is likely to be done, the it should
			 * be done systematically. If there is a doubt, then the UI should propose the
			 * common options. Note that if a function is complex, the function editor
			 * should be used to compose it rather than the mappings editor. Thus there is
			 * no need to allow to specify all the possible logics in the mappings editor.
			 */
			boolean accept = false;
			if (targetValueAccessor.get() instanceof Function) {
				if (GUI.INSTANCE.openQuestionDialog(targetComponent, "Rewrite the existing function to map: "
						+ pathNode.toString() + " => " + initializerFacade.toString() + "?", "Mapping")) {
					targetValueAccessor.set(new Function("return " + pathNode.getTypicalExpression() + ";"));
					accept = true;
				}
			} else {
				if (isLeafType(pathNode.getExpressionType()) || isLeafType(targetTypeSupplier.get())) {
					targetValueAccessor.set(new Function("return " + pathNode.getTypicalExpression() + ";"));
					accept = true;
				} else {
					List<String> options = Arrays.asList("Assign source value to target",
							"Map corresponding children (same name) of source and target values", "Do not map");
					String choice = GUI.INSTANCE.openSelectionDialog(targetComponent, options, null,
							"Choose a mapping option for: " + pathNode.toString() + " => "
									+ initializerFacade.toString(),
							"Mapping");
					if (choice == options.get(0)) {
						targetValueAccessor.set(new Function("return " + pathNode.getTypicalExpression() + ";"));
						accept = true;
					} else if (choice == options.get(1)) {
						initializerFacade.setConcrete(true);
						for (Facade initializerFacadeChild : initializerFacade.getChildren()) {
							for (PathNode pathNodeChild : pathNode.getChildren()) {
								if (initializerFacadeChild.toString().replaceAll("^\\[([0-9]+)\\]$", "[i]")
										.replaceAll("[^0-9a-zA-Z]", "").toLowerCase().equals(pathNodeChild.toString()
												.replaceAll("[^0-9a-zA-Z]", "").toLowerCase())) {
									accept = map(pathNodeChild, initializerFacadeChild, targetComponent) || accept;
									break;
								}
							}
						}
						accept = true;
					} else if (choice == options.get(2)) {
						// do nothing
					} else {
						throw new CancellationException();
					}
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
