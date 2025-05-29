package com.otk.jesb.ui;

import java.awt.Color;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.jdesktop.swingx.JXTreeTable;

import com.otk.jesb.instantiation.FacadeOutline;
import com.otk.jesb.instantiation.RootInstanceBuilder;

import xy.reflect.ui.control.IFieldControlInput;
import xy.reflect.ui.control.swing.ListControl;
import xy.reflect.ui.control.swing.renderer.Form;
import xy.reflect.ui.control.swing.renderer.SwingRenderer;
import xy.reflect.ui.control.swing.util.SwingRendererUtils;
import xy.reflect.ui.info.ValidationSession;
import xy.reflect.ui.info.type.iterable.item.BufferedItemPosition;
import xy.reflect.ui.util.ReflectionUIError;

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
	public void validateControl(ValidationSession session) throws Exception {
		Form rootInstanceBuilderForm = SwingRendererUtils.findAncestorFormOfType(this,
				RootInstanceBuilder.class.getName(), swingRenderer);
		Form[] rootInstanceBuilderFacadeForm = new Form[1];
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					rootInstanceBuilderFacadeForm[0] = swingRenderer
							.createForm(((RootInstanceBuilder) rootInstanceBuilderForm.getObject()).getFacade());
				}
			});
		} catch (InvocationTargetException e) {
			throw new ReflectionUIError(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return;
		}
		ListControl facadeTreeControl = (ListControl) SwingRendererUtils
				.findDescendantFieldControlPlaceHolder(rootInstanceBuilderFacadeForm[0], "children", swingRenderer)
				.getFieldControl();
		validitionErrorByItemPosition.clear();
		visitItems(new IItemsVisitor() {
			@Override
			public VisitStatus visitItem(BufferedItemPosition itemPosition) {
				if (Thread.currentThread().isInterrupted()) {
					return VisitStatus.TREE_VISIT_INTERRUPTED;
				}
				if (!itemPosition.getContainingListType().isItemNodeValidityDetectionEnabled(itemPosition)) {
					return VisitStatus.SUBTREE_VISIT_INTERRUPTED;
				}
				BufferedItemPosition facadeItemPosition = getFacadeItemPosition(itemPosition);
				Form[] itemForm = new Form[1];
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						@Override
						public void run() {
							itemForm[0] = facadeTreeControl.new ItemUIBuilder(facadeItemPosition)
									.createEditorForm(false, false);
						}
					});
				} catch (InvocationTargetException e) {
					throw new ReflectionUIError(e);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return VisitStatus.TREE_VISIT_INTERRUPTED;
				}
				try {
					itemForm[0].validateForm(session);
				} catch (Exception e) {
					validitionErrorByItemPosition.put(itemPosition, e);
				}
				return VisitStatus.VISIT_NOT_INTERRUPTED;
			}

			BufferedItemPosition getFacadeItemPosition(BufferedItemPosition outlineItemPosition) {
				if (outlineItemPosition.isRoot()) {
					return facadeTreeControl.getRootListItemPosition(outlineItemPosition.getIndex());
				}
				BufferedItemPosition parentResult = getFacadeItemPosition(outlineItemPosition.getParentItemPosition());
				return parentResult.getSubItemPosition(outlineItemPosition.getIndex());
			}
		});
		if (Thread.currentThread().isInterrupted()) {
			return;
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				refreshRendrers();
				treeTableComponent.repaint();
			}
		});
		if (validitionErrorByItemPosition.size() > 0) {
			throw new ListValidationError("Invalid element(s) detected", validitionErrorByItemPosition);
		}
	}

}