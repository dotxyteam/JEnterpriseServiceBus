package com.otk.jesb.ui;

import java.awt.Color;
import java.awt.Component;
import javax.swing.DropMode;
import javax.swing.JLabel;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.otk.jesb.instantiation.Facade;
import com.otk.jesb.util.Pair;

import xy.reflect.ui.control.IFieldControlInput;
import xy.reflect.ui.control.swing.renderer.SwingRenderer;
import xy.reflect.ui.info.type.iterable.item.BufferedItemPosition;

public class InstanceBuilderInitializerTreeControl extends MappingsControl.SideControl {

	private static final long serialVersionUID = 1L;

	public InstanceBuilderInitializerTreeControl(SwingRenderer swingRenderer, IFieldControlInput input) {
		super(swingRenderer, input);
		expandItemPositions(2);
	}

	@Override
	protected void initializeTreeTableModelAndControl() {
		super.initializeTreeTableModelAndControl();
		treeTableComponent.putClientProperty(InstanceBuilderInitializerTreeControl.class, this);
		treeTableComponent.setTransferHandler(new MappingsControl.PathImportTransferHandler());
		treeTableComponent.setDropMode(DropMode.ON);
		treeTableComponent.addHighlighter(new ColorHighlighter(new HighlightPredicate() {
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
				MappingsControl mappingsControl = findMappingsControl();
				if (mappingsControl != null) {
					for (Pair<BufferedItemPosition, BufferedItemPosition> pair : mappingsControl.getMappings()) {
						BufferedItemPosition itemPosition = pair.getSecond();
						TreePath treePath = getTreePath(itemPosition);
						int row = treeTableComponent.getRowForPath(treePath);
						if (adapter.row == row) {
							return true;
						}
					}
				}
				return false;
			}
		}, Color.LIGHT_GRAY, Color.BLACK));
	}

	@Override
	protected void customizeCellRendererComponent(JLabel label, ItemNode node, int rowIndex, int columnIndex,
			boolean isSelected, boolean hasFocus) {
		super.customizeCellRendererComponent(label, node, rowIndex, columnIndex, isSelected, hasFocus);
		BufferedItemPosition itemPosition = getItemPositionByNode(node);
		if (itemPosition != null) {
			if (itemPosition.getItem() instanceof Facade) {
				Facade facade = (Facade) itemPosition.getItem();
				if (!facade.isConcrete()) {
					label.setForeground(Color.LIGHT_GRAY);
				}
			}
		}
	}

}