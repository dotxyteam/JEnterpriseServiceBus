package com.otk.jesb.ui;

import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.jdesktop.swingx.JXTreeTable;

import com.otk.jesb.UnexpectedError;
import com.otk.jesb.instantiation.FacadeOutline;
import com.otk.jesb.instantiation.RootInstanceBuilder;

import xy.reflect.ui.control.IFieldControlInput;
import xy.reflect.ui.control.swing.ListControl;
import xy.reflect.ui.control.swing.renderer.Form;
import xy.reflect.ui.control.swing.renderer.SwingRenderer;
import xy.reflect.ui.control.swing.util.SwingRendererUtils;
import xy.reflect.ui.info.ValidationSession;
import xy.reflect.ui.info.type.iterable.item.BufferedItemPosition;

public class InstanceBuilderOutlineTreeControl extends ListControl {

	private static final long serialVersionUID = 1L;

	private ListControl facadeTreeControl;

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
	public void validateControl(ValidationSession session) throws Exception {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					facadeTreeControl = createFacadeTreeControl();
				}
			});
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return;
		} catch (InvocationTargetException e) {
			throw new UnexpectedError(e);
		}
		try {
			facadeTreeControl.validateControl(session);
		} finally {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					refreshRendrers();
					treeTableComponent.repaint();
				}
			});
		}
	}

	@Override
	public Exception getValidationError(BufferedItemPosition itemPosition) {
		return swingRenderer.getReflectionUI().getValidationErrorRegistry()
				.getValidationError(((FacadeOutline) itemPosition.getItem()).getFacade(), null);
	}

	private ListControl createFacadeTreeControl() {
		Form rootInstanceBuilderForm = SwingRendererUtils.findAncestorFormOfType(this,
				RootInstanceBuilder.class.getName(), swingRenderer);
		Form rootInstanceBuilderFacadeForm = swingRenderer
				.createForm(((RootInstanceBuilder) rootInstanceBuilderForm.getObject()).getFacade());
		return (ListControl) SwingRendererUtils
				.findDescendantFieldControlPlaceHolder(rootInstanceBuilderFacadeForm, "children", swingRenderer)
				.getFieldControl();
	}

}