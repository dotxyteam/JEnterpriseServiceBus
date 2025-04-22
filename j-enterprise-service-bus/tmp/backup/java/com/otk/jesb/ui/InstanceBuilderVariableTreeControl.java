package com.otk.jesb.ui;

import java.util.Collections;

import javax.swing.ListSelectionModel;

import com.otk.jesb.ui.MappingsControl.Side;
import xy.reflect.ui.control.IFieldControlInput;
import xy.reflect.ui.control.swing.renderer.SwingRenderer;

public class InstanceBuilderVariableTreeControl extends MappingsControl.SideControl {

	private static final long serialVersionUID = 1L;

	public InstanceBuilderVariableTreeControl(SwingRenderer swingRenderer, IFieldControlInput input) {
		super(swingRenderer, input);
		setSelection(Collections.emptyList());
	}

	@Override
	protected Side getSide() {
		return Side.SOURCE;
	}

	@Override
	protected void initializeTreeTableModelAndControl() {
		super.initializeTreeTableModelAndControl();
		treeTableComponent.putClientProperty(InstanceBuilderVariableTreeControl.class, this);
		treeTableComponent.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		treeTableComponent.setDragEnabled(true);
		treeTableComponent.setTransferHandler(new MappingsControl.PathExportTransferHandler());
	}

	@Override
	public boolean requestCustomFocus() {
		return false;
	}

}