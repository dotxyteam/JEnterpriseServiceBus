package com.otk.jesb.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.StepCrossing;
import com.otk.jesb.CompositeStep;
import com.otk.jesb.Debugger.PlanExecutor;
import com.otk.jesb.diagram.JConnection;
import com.otk.jesb.diagram.JDiagramListener;
import com.otk.jesb.diagram.JDiagramObject;
import com.otk.jesb.diagram.JNode;
import com.otk.jesb.util.Pair;

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

	protected ListControl getStepCrossingsControl() {
		List<Form> forms = new ArrayList<Form>();
		forms.add(getPlanExecutorView());
		forms.addAll(SwingRendererUtils.findDescendantForms(getPlanExecutorView(), swingRenderer));
		for (Form form : forms) {
			FieldControlPlaceHolder stepCrossingsFieldControlPlaceHolder = form
					.getFieldControlPlaceHolder("stepCrossings");
			if (stepCrossingsFieldControlPlaceHolder != null) {
				return (ListControl) stepCrossingsFieldControlPlaceHolder.getFieldControl();
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
						ListControl stepCrossingsControl = getStepCrossingsControl();
						stepCrossingsControl.setSelection(DebugPlanDiagram.this.getSelection().stream()
								.filter(diagramObject -> diagramObject instanceof JNode).map(diagramObject -> {
									Step step = (Step) diagramObject.getValue();
									for (int i = getPlanExecutor().getStepCrossings().size() - 1; i >= 0; i--) {
										StepCrossing stepCrossing = getPlanExecutor().getStepCrossings().get(i);
										if (stepCrossing.getStep() == step) {
											return stepCrossing;
										}
									}
									throw new AssertionError();
								}).map(stepCrossing -> stepCrossingsControl
										.findItemPositionByReference(stepCrossing))
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
				getStepCrossingsControl().addListControlSelectionListener(new Listener<List<BufferedItemPosition>>() {
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
		setSelection(getStepCrossingsControl().getSelection().stream()
				.map(itemPosition -> (JDiagramObject) findNode(((StepCrossing) itemPosition.getItem()).getStep()))
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
		StepCrossing currentStepCrossing = getPlanExecutor().getCurrentStepCrossing();
		if (currentStepCrossing != null) {
			if (currentStepCrossing.getStep() == node.getValue()) {
				highlightNode(g, node, (currentStepCrossing.getOperationError() == null) ? new Color(175, 255, 200)
						: new Color(255, 173, 173));
			}
		}
		super.paintNode(g, node);
	}

	@Override
	protected void paintConnection(Graphics g, JConnection conn) {
		super.paintConnection(g, conn);
		int transitionOccurrenceCount = 0;
		Step startStep = (Step) conn.getStartNode().getValue();
		Step endStep = (Step) conn.getEndNode().getValue();
		CompositeStep parent = startStep.getParent();
		List<StepCrossing> stepCrossings = getPlanExecutor().getStepCrossings().stream()
				.filter(stepCrossing -> (stepCrossing.getStep().getParent() == parent))
				.collect(Collectors.toList());
		for (int i = 0; i < stepCrossings.size(); i++) {
			if (i > 0) {
				if (stepCrossings.get(i - 1).getStep() == startStep) {
					if (stepCrossings.get(i).getStep() == endStep) {
						transitionOccurrenceCount++;
					}
				}
			}
		}
		if (transitionOccurrenceCount > 0) {
			annotateConnection(g, conn, "(" + transitionOccurrenceCount + ")");
		}
	}

	private void annotateConnection(Graphics g, JConnection conn, String annotation) {
		g.setColor(Color.BLUE);
		Pair<Point, Point> lineSegment = conn.getLineSegment();
		if (lineSegment == null) {
			return;
		}
		int x = (lineSegment.getFirst().x + lineSegment.getSecond().x) / 2;
		int y = (lineSegment.getFirst().y + lineSegment.getSecond().y) / 2;
		Rectangle2D annotationBounds = g.getFontMetrics().getStringBounds(annotation, g);
		g.drawString(annotation, x - (int) (annotationBounds.getWidth() / 2d), y - getConnectionArrowSize());
	}

	private void highlightNode(Graphics g, JNode node, Color color) {
		g.setColor(color);
		int width = (node.getImage().getWidth(null) * 3) / 2;
		int height = (node.getImage().getHeight(null) * 3) / 2;
		g.fillRoundRect(node.getCenterX() - (width / 2), node.getCenterY() - (height / 2), width, height, width / 10,
				height / 10);
	}

	public static class Source {
	}
}