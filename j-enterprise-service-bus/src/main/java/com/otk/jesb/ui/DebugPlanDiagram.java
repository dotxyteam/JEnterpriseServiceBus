package com.otk.jesb.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.otk.jesb.Plan;
import com.otk.jesb.Step;
import com.otk.jesb.StepOccurrence;
import com.otk.jesb.Debugger.PlanExecutor;
import com.otk.jesb.diagram.JConnection;
import com.otk.jesb.diagram.JDiagramListener;
import com.otk.jesb.diagram.JDiagramObject;
import com.otk.jesb.diagram.JNode;

import xy.reflect.ui.control.swing.ListControl;
import xy.reflect.ui.control.swing.customizer.CustomizingForm;
import xy.reflect.ui.control.swing.renderer.FieldControlPlaceHolder;
import xy.reflect.ui.control.swing.renderer.Form;
import xy.reflect.ui.control.swing.renderer.SwingRenderer;
import xy.reflect.ui.control.swing.util.SwingRendererUtils;
import xy.reflect.ui.info.menu.MenuModel;
import xy.reflect.ui.info.type.iterable.item.BufferedItemPosition;
import xy.reflect.ui.util.Listener;

public class DebugPlanDiagram extends PlanDiagram {

	private static final long serialVersionUID = 1L;

	public DebugPlanDiagram(SwingRenderer swingRenderer, CustomizingForm parentForm) {
		super(swingRenderer, parentForm);
	}

	protected ListControl getStepOccurrencesControl() {
		List<Form> forms = new ArrayList<Form>();
		forms.add(getPlanExecutorView());
		forms.addAll(SwingRendererUtils.findDescendantForms(getPlanExecutorView(), swingRenderer));
		for (Form form : forms) {
			FieldControlPlaceHolder stepOccurrencesFieldControlPlaceHolder = form
					.getFieldControlPlaceHolder("stepOccurrences");
			if (stepOccurrencesFieldControlPlaceHolder != null) {
				return (ListControl) stepOccurrencesFieldControlPlaceHolder.getFieldControl();
			}
		}
		throw new AssertionError();
	}

	protected Form getPlanExecutorView() {
		if (parentForm.getObject() instanceof PlanExecutor) {
			return parentForm;
		}
		return SwingRendererUtils.findAncestorFormOfType(parentForm, PlanExecutor.class.getName(), swingRenderer);
	}

	@Override
	protected void updateExternalComponentsOnInternalEvents() {
		addListener(new JDiagramListener() {

			@Override
			public void nodesMoved(Set<JNode> nodes) {
				refreshUI(false);
			}

			@Override
			public void selectionChanged() {
				if (selectionListeningEnabled) {
					selectionListeningEnabled = false;
					try {
						ListControl stepOccurrencesControl = getStepOccurrencesControl();
						stepOccurrencesControl.setSelection(DebugPlanDiagram.this.getSelection().stream()
								.filter(diagramObject -> diagramObject instanceof JNode).map(diagramObject -> {
									Step step = (Step) diagramObject.getValue();
									for (int i = getPlanExecutor().getStepOccurrences().size() - 1; i >= 0; i--) {
										StepOccurrence stepOccurrence = getPlanExecutor().getStepOccurrences().get(i);
										if (stepOccurrence.getStep() == step) {
											return stepOccurrence;
										}
									}
									throw new AssertionError();
								}).map(stepOccurrence -> stepOccurrencesControl
										.findItemPositionByReference(stepOccurrence))
								.collect(Collectors.toList()));
					} finally {
						selectionListeningEnabled = true;
					}
				}
			}

			@Override
			public void connectionAdded(JConnection conn) {
				refreshUI(false);
			}
		});
	}

	@Override
	protected void updateInternalComponentsOnExternalEvents() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				getStepOccurrencesControl().addListControlSelectionListener(new Listener<List<BufferedItemPosition>>() {
					@Override
					public void handle(List<BufferedItemPosition> event) {
						if (selectionListeningEnabled) {
							selectionListeningEnabled = false;
							try {
								updateStepSelection();
							} finally {
								selectionListeningEnabled = true;
							}
						}
					}
				});
			}
		});
	}

	@Override
	protected void updateStepSelection() {
		setSelection(getStepOccurrencesControl().getSelection().stream()
				.map(itemPosition -> (JDiagramObject) findNode(((StepOccurrence) itemPosition.getItem()).getStep()))
				.collect(Collectors.toSet()));
	}

	@Override
	protected JPopupMenu createContextMenu(MouseEvent mouseEvent) {
		JPopupMenu result = super.createContextMenu(mouseEvent);
		result.removeAll();
		return result;
	}

	@Override
	public void addMenuContributions(MenuModel menuModel) {
	}

	public PlanExecutor getPlanExecutor() {
		return (PlanExecutor) getPlanExecutorView().getObject();
	}

	@Override
	public Plan getPlan() {
		return getPlanExecutor().getPlan();
	}

	@Override
	protected void paintNode(Graphics g, JNode node) {
		StepOccurrence currentStepOccurrence = getPlanExecutor().getCurrentStepOccurrence();
		if (currentStepOccurrence != null) {
			if (currentStepOccurrence.getStep() == node.getValue()) {
				highlightNode(g, node, (currentStepOccurrence.getActivityError() == null) ? new Color(175, 255, 200)
						: new Color(255, 173, 173));
			}
		}
		super.paintNode(g, node);
	}

	@Override
	protected void paintConnection(Graphics g, JConnection conn) {
		super.paintConnection(g, conn);
		int transitionOccurrenceCount = 0;
		List<StepOccurrence> stepOccurrences = getPlanExecutor().getStepOccurrences();
		for (int i = 0; i < stepOccurrences.size(); i++) {
			if (i > 0) {
				if (stepOccurrences.get(i - 1).getStep() == conn.getStartNode().getValue()) {
					if (stepOccurrences.get(i).getStep() == conn.getEndNode().getValue()) {
						transitionOccurrenceCount++;
					}
				}
			}
		}
		if (transitionOccurrenceCount > 0) {
			annotateConnection(g, conn, "(" + transitionOccurrenceCount + ")");
		}
	}

	void annotateConnection(Graphics g, JConnection conn, String annotation) {
		g.setColor(Color.BLUE);
		int x = (conn.getStartNode().getCenterX() + conn.getEndNode().getCenterX()) / 2;
		int y = (conn.getStartNode().getCenterY() + conn.getEndNode().getCenterY()) / 2;
		g.drawString(annotation, x, y);
	}

	void highlightNode(Graphics g, JNode node, Color color) {
		g.setColor(color);
		int width = (node.getImage().getWidth(null) * 3) / 2;
		int height = (node.getImage().getHeight(null) * 3) / 2;
		g.fillRoundRect(node.getCenterX() - (width / 2), node.getCenterY() - (height / 2), width, height, width / 10,
				height / 10);
	}

	public static class Source {
	}
}