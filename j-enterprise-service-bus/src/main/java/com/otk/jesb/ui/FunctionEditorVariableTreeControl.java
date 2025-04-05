package com.otk.jesb.ui;

import java.awt.Component;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import javax.swing.text.JTextComponent;

import org.jdesktop.swingx.JXTreeTable;

import com.otk.jesb.FunctionEditor;
import com.otk.jesb.PathExplorer;
import com.otk.jesb.PathExplorer.PathNode;
import com.otk.jesb.util.TextTransferHandler;

import xy.reflect.ui.control.IFieldControlInput;
import xy.reflect.ui.control.swing.ListControl;
import xy.reflect.ui.control.swing.renderer.Form;
import xy.reflect.ui.control.swing.renderer.SwingRenderer;
import xy.reflect.ui.control.swing.util.SwingRendererUtils;
import xy.reflect.ui.info.type.iterable.item.BufferedItemPosition;

public class FunctionEditorVariableTreeControl extends ListControl {

	private static final long serialVersionUID = 1L;

	public FunctionEditorVariableTreeControl(SwingRenderer swingRenderer, IFieldControlInput input) {
		super(swingRenderer, input);
	}

	@Override
	protected void initializeTreeTableModelAndControl() {
		super.initializeTreeTableModelAndControl();
		treeTableComponent.putClientProperty(FunctionEditorVariableTreeControl.class, this);
		treeTableComponent.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		treeTableComponent.setDragEnabled(true);
		treeTableComponent.setTransferHandler(new PathExportTransferHandler());
	}

	public static class PathExportTransferHandler extends TransferHandler {

		private static final long serialVersionUID = 1L;

		@Override
		public int getSourceActions(JComponent c) {
			return DnDConstants.ACTION_COPY_OR_MOVE;
		}

		@Override
		protected Transferable createTransferable(JComponent c) {
			FunctionEditorVariableTreeControl listControl = (FunctionEditorVariableTreeControl) ((JXTreeTable) c)
					.getClientProperty(FunctionEditorVariableTreeControl.class);
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

	public static class PathImportTransferHandler extends TextTransferHandler {

		private static final long serialVersionUID = 1L;

		public PathImportTransferHandler() {
		}

		@Override
		public boolean canImport(TransferHandler.TransferSupport support) {
			if (support.isDataFlavorSupported(TransferablePath.DATA_FLAVOR)) {
				return true;
			}
			return super.canImport(support);
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
						if (component instanceof JTextComponent) {
							JTextComponent textComponent = (JTextComponent) component;
							Form functionEditorForm = SwingRendererUtils.findAncestorFormOfType(textComponent,
									FunctionEditor.class.getName(), GUI.INSTANCE);
							if (functionEditorForm != null) {
								Point dropPoint = support.getDropLocation().getDropPoint();
								int textInsertPosition = textComponent.viewToModel(dropPoint);
								if (textInsertPosition != -1) {
									PathNode pathNode = (PathNode) data;
									String expression = pathNode.getExpression();
									textComponent.getDocument().insertString(textInsertPosition, expression, null);
									accept = true;
								}
							}
						}
					}
				} catch (UnsupportedFlavorException e) {
				} catch (Exception e) {
					throw new AssertionError(e);
				}
			}
			if (accept) {
				return true;
			}
			return super.importData(support);
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