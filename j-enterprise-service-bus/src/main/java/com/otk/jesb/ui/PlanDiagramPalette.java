package com.otk.jesb.ui;

import java.awt.BorderLayout;
import javax.swing.BoxLayout;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import com.otk.jesb.solution.Plan;

import xy.reflect.ui.control.FieldControlDataProxy;
import xy.reflect.ui.control.FieldControlInputProxy;
import xy.reflect.ui.control.IAdvancedFieldControl;
import xy.reflect.ui.control.IFieldControlData;
import xy.reflect.ui.control.IFieldControlInput;
import xy.reflect.ui.control.swing.renderer.FieldControlPlaceHolder;
import xy.reflect.ui.control.swing.renderer.Form;
import xy.reflect.ui.control.swing.renderer.SwingRenderer;
import xy.reflect.ui.control.swing.util.ControlPanel;
import xy.reflect.ui.control.swing.util.SwingRendererUtils;
import xy.reflect.ui.info.menu.MenuModel;

public class PlanDiagramPalette extends ControlPanel implements IAdvancedFieldControl {

	private static final long serialVersionUID = 1L;

	private SwingRenderer swingRenderer;
	private IFieldControlInput input;

	public PlanDiagramPalette(SwingRenderer swingRenderer, IFieldControlInput input) {
		this.swingRenderer = swingRenderer;
		this.input = input;
		setLayout(new BorderLayout());
		refreshUI(true);
	}

	protected PlanDiagram getDiagram() {
		Form planEditor = getPlanEditor();
		if (planEditor == null) {
			return null;
		}
		FieldControlPlaceHolder fielControlPlaceHolder = SwingRendererUtils
				.findDescendantFieldControlPlaceHolder(planEditor, "diagram", swingRenderer);
		if (fielControlPlaceHolder == null) {
			return null;
		}
		return (PlanDiagram) fielControlPlaceHolder.getFieldControl();
	}

	protected Form getPlanEditor() {
		return SwingRendererUtils.findAncestorFormOfType(this, Plan.class.getName(), swingRenderer);
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
				add(BorderLayout.CENTER,
						getDiagram().createActionPalette(JTabbedPane.TOP, JTabbedPane.RIGHT, BoxLayout.Y_AXIS));
				SwingRendererUtils.handleComponentSizeChange(PlanDiagramPalette.this);
			} else {
				add(BorderLayout.CENTER, new PlanDiagram(swingRenderer, new FieldControlInputProxy(input) {
					@Override
					public IFieldControlData getControlData() {
						return new FieldControlDataProxy(super.getControlData()) {
							@Override
							public Object getValue() {
								return new PlanDiagram.Source(((Source) super.getValue()).getPlan());
							}
						};
					}
				}) {
					private static final long serialVersionUID = 1L;

					@Override
					protected void updateExternalComponentsOnInternalEvents() {
					}

					@Override
					protected void updateInternalComponentsOnExternalEvents() {
					}

					@Override
					public boolean refreshUI(boolean refreshStructure) {
						return true;
					}
				}.createActionPalette(JTabbedPane.TOP, JTabbedPane.RIGHT, BoxLayout.Y_AXIS));
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						PlanDiagramPalette.this.refreshUI(true);
					}
				});
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
	public boolean displayError(Throwable error) {
		return false;
	}

	public static class Source {
		private Plan plan;

		public Source(Plan plan) {
			this.plan = plan;
		}

		public Plan getPlan() {
			return plan;
		}

	}
}