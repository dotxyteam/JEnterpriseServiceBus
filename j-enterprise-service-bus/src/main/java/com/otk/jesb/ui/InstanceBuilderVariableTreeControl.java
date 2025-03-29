package com.otk.jesb.ui;

import java.awt.Color;
import java.awt.Component;
import javax.swing.ListSelectionModel;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.otk.jesb.util.Pair;

import xy.reflect.ui.control.IFieldControlInput;
import xy.reflect.ui.control.swing.renderer.SwingRenderer;
import xy.reflect.ui.info.type.iterable.item.BufferedItemPosition;

public class InstanceBuilderVariableTreeControl extends MappingsControl.SideControl {

	private static final long serialVersionUID = 1L;

	public InstanceBuilderVariableTreeControl(SwingRenderer swingRenderer, IFieldControlInput input) {
		super(swingRenderer, input);
	}

	@Override
	protected void initializeTreeTableModelAndControl() {
		super.initializeTreeTableModelAndControl();
		treeTableComponent.putClientProperty(InstanceBuilderVariableTreeControl.class, this);
		treeTableComponent.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		treeTableComponent.setDragEnabled(true);
		treeTableComponent.setTransferHandler(new MappingsControl.PathExportTransferHandler());
		treeTableComponent.addHighlighter(new ColorHighlighter(new HighlightPredicate() {
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
				return isMapped(adapter.row);
			}
		}, new Color(215, 230, 255), Color.BLACK));
	}
	
	private boolean isMapped(int rowIndex) {
		MappingsControl mappingsControl = findMappingsControl();
		if (mappingsControl != null) {
			for (Pair<BufferedItemPosition, BufferedItemPosition> pair : mappingsControl
					.listVisibleMappings()) {
				BufferedItemPosition itemPosition = pair.getFirst();
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