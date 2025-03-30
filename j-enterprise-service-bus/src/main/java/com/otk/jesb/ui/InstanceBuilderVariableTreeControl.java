package com.otk.jesb.ui;

import javax.swing.ListSelectionModel;

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
	protected BufferedItemPosition getSideItemPosition(Pair<BufferedItemPosition, BufferedItemPosition> pair) {
		return pair.getFirst();
	}

	@Override
	protected void initializeTreeTableModelAndControl() {
		super.initializeTreeTableModelAndControl();
		treeTableComponent.putClientProperty(InstanceBuilderVariableTreeControl.class, this);
		treeTableComponent.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		treeTableComponent.setDragEnabled(true);
		treeTableComponent.setTransferHandler(new MappingsControl.PathExportTransferHandler());
	}

}