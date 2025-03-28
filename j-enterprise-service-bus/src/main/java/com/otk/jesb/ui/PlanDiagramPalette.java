package com.otk.jesb.ui;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import com.otk.jesb.Plan;

import xy.reflect.ui.control.IAdvancedFieldControl;
import xy.reflect.ui.control.swing.customizer.CustomizingForm;
import xy.reflect.ui.control.swing.renderer.FieldControlPlaceHolder;
import xy.reflect.ui.control.swing.renderer.Form;
import xy.reflect.ui.control.swing.renderer.SwingRenderer;
import xy.reflect.ui.control.swing.util.ControlPanel;
import xy.reflect.ui.control.swing.util.SwingRendererUtils;
import xy.reflect.ui.info.menu.MenuModel;

public class PlanDiagramPalette extends ControlPanel implements IAdvancedFieldControl {

	private static final long serialVersionUID = 1L;

	private CustomizingForm parentForm;
	private SwingRenderer swingRenderer;

	public PlanDiagramPalette(SwingRenderer swingRenderer, CustomizingForm parentForm) {
		this.swingRenderer = swingRenderer;
		this.parentForm = parentForm;
		setLayout(new BorderLayout());
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				PlanDiagramPalette.this.refreshUI(true);
			}
		});
	}

	protected PlanDiagram getDiagram() {
		List<Form> forms = new ArrayList<Form>();
		forms.add(getPlanEditor());
		forms.addAll(SwingRendererUtils.findDescendantForms(getPlanEditor(), swingRenderer));
		for (Form form : forms) {
			FieldControlPlaceHolder diagramFieldControlPlaceHolder = form.getFieldControlPlaceHolder("diagram");
			if (diagramFieldControlPlaceHolder != null) {
				if (diagramFieldControlPlaceHolder.getFieldControl() instanceof PlanDiagram) {
					return (PlanDiagram) diagramFieldControlPlaceHolder.getFieldControl();
				}
			}
		}
		return null;
	}

	protected Form getPlanEditor() {
		if (parentForm.getObject() instanceof Plan) {
			return parentForm;
		}
		return SwingRendererUtils.findAncestorFormOfType(parentForm, Plan.class.getName(), swingRenderer);
	}

	@Override
	public boolean showsCaption() {
		return false;
	}

	@Override
	public boolean refreshUI(boolean refreshStructure) {
		if (refreshStructure) {
			removeAll();
			if (getDiagram() != null) {
				add(BorderLayout.CENTER, getDiagram().createActionPalette(JTabbedPane.RIGHT, BoxLayout.Y_AXIS));
				SwingRendererUtils.handleComponentSizeChange(PlanDiagramPalette.this);
			}
		}
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
}