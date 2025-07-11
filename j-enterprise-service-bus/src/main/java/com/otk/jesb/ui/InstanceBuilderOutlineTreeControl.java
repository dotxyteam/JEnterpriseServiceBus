package com.otk.jesb.ui;

import java.awt.Color;
import javax.swing.JLabel;
import org.jdesktop.swingx.JXTreeTable;

import com.otk.jesb.instantiation.FacadeOutline;
import xy.reflect.ui.control.IFieldControlInput;
import xy.reflect.ui.control.swing.ListControl;
import xy.reflect.ui.control.swing.renderer.SwingRenderer;
import xy.reflect.ui.info.type.iterable.item.BufferedItemPosition;

public class InstanceBuilderOutlineTreeControl extends ListControl {

	private static final long serialVersionUID = 1L;

	public InstanceBuilderOutlineTreeControl(SwingRenderer swingRenderer, IFieldControlInput input) {
		super(swingRenderer, input);
		treeTableComponent.putClientProperty(JXTreeTable.USE_DTCR_COLORMEMORY_HACK, false);
		expandItemPositions(2);
	}

	@Override
	protected void customizeCellRendererComponent(JLabel label, ItemNode node, int rowIndex, int columnIndex,
			boolean isSelected, boolean hasFocus) {
		super.customizeCellRendererComponent(label, node, rowIndex, columnIndex, isSelected, hasFocus);
		BufferedItemPosition itemPosition = getItemPositionByNode(node);
		if (itemPosition != null) {
			if (itemPosition.getItem() instanceof FacadeOutline) {
				FacadeOutline facadeOutline = (FacadeOutline) itemPosition.getItem();
				label.setForeground(
						facadeOutline.getFacade().isConcrete() ? MappingsControl.getConcreteElementTextColor()
								: MappingsControl.getAbstractElementTextColor());
				label.setOpaque(columnIndex == 1);
				if (!isSelected) {
					label.setBackground((columnIndex == 1)
							? ((facadeOutline.getFacade().express() != null) ? getExpressionBackgroudColor()
									: getNoExpressionBackgroundColor())
							: null);
				}
			}
		}
	}

	private Color getNoExpressionBackgroundColor() {
		return new Color(240, 240, 240);
	}

	private Color getExpressionBackgroudColor() {
		return new Color(245, 245, 255);
	}

	@Override
	protected ItemDialogBuilder createItemDialogBuilder(BufferedItemPosition bufferedItemPosition) {
		return new ItemDialogBuilder(bufferedItemPosition) {

			@Override
			protected void copyValidationErrorFromCapsuleToItem(Object capsule) {
				if (bufferedItemPosition.getContainingListType()
						.getListItemAbstractFormValidationJob(bufferedItemPosition) != null) {
					/*
					 * Do not copy the eventual abstract form validation error because it may be
					 * structurally different (even if it represents the same incoherence) from the
					 * error that would be generated from the concrete form.
					 */
					return;
				}
				super.copyValidationErrorFromCapsuleToItem(capsule);
			}

			@Override
			protected void copyValidationErrorFromItemToCapsule(Object capsule) {
				if (bufferedItemPosition.getContainingListType()
						.getListItemAbstractFormValidationJob(bufferedItemPosition) != null) {
					/*
					 * Do not copy the eventual abstract form validation error because it may be
					 * structurally different (even if it represents the same incoherence) from the
					 * error that would be generated from the concrete form.
					 */
					return;
				}
				super.copyValidationErrorFromItemToCapsule(capsule);
			}
			
		};
	}

}