package com.otk.jesb.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.otk.jesb.CompositeStep;
import com.otk.jesb.Debugger.PlanExecutor;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.diagram.JConnection;
import com.otk.jesb.diagram.JDiagramListener;
import com.otk.jesb.diagram.JDiagramObject;
import com.otk.jesb.diagram.JNode;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.StepGoingThrough;
import com.otk.jesb.util.MiscUtils;
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

	protected ListControl getStepGoingThroughsControl() {
		List<Form> forms = new ArrayList<Form>();
		forms.add(getPlanExecutorView());
		forms.addAll(SwingRendererUtils.findDescendantForms(getPlanExecutorView(), swingRenderer));
		for (Form form : forms) {
			FieldControlPlaceHolder stepGoingThroughsFieldControlPlaceHolder = form
					.getFieldControlPlaceHolder("stepGoingThroughs");
			if (stepGoingThroughsFieldControlPlaceHolder != null) {
				return (ListControl) stepGoingThroughsFieldControlPlaceHolder.getFieldControl();
			}
		}
		throw new UnexpectedError();
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
						ListControl stepGoingThroughsControl = getStepGoingThroughsControl();
						stepGoingThroughsControl.setSelection(DebugPlanDiagram.this.getSelection().stream()
								.filter(diagramObject -> diagramObject instanceof JNode).map(diagramObject -> {
									Step step = (Step) diagramObject.getValue();
									for (int i = getPlanExecutor().getStepGoingThroughs().size() - 1; i >= 0; i--) {
										StepGoingThrough stepGoingThrough = getPlanExecutor().getStepGoingThroughs()
												.get(i);
										if (stepGoingThrough.getStep() == step) {
											return stepGoingThrough;
										}
									}
									throw new UnexpectedError();
								}).map(stepGoingThrough -> stepGoingThroughsControl
										.findItemPositionByReference(stepGoingThrough))
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
				getStepGoingThroughsControl()
						.addListControlSelectionListener(new Listener<List<BufferedItemPosition>>() {
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
		setSelection(getStepGoingThroughsControl().getSelection().stream()
				.map(itemPosition -> (JDiagramObject) findNode(((StepGoingThrough) itemPosition.getItem()).getStep()))
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
		StepGoingThrough lastStepGoingThrough = MiscUtils.getReverse(getPlanExecutor().getStepGoingThroughs()).stream()
				.filter(candidateStepGoingThrough -> (candidateStepGoingThrough.getStep() == node.getValue()))
				.findFirst().orElse(null);
		if ((lastStepGoingThrough != null) && (lastStepGoingThrough.getActivityError() != null)) {
			highlightNode(g, node, new Color(255, 173, 173));
		} else {
			StepGoingThrough currentStepGoingThrough = getPlanExecutor().getCurrentStepGoingThrough();
			if ((currentStepGoingThrough != null) && (currentStepGoingThrough.getStep() == node.getValue())) {
				highlightNode(g, node, new Color(175, 255, 200));
			}
		}
		super.paintNode(g, node);
	}

	@Override
	protected void paintConnection(Graphics g, JConnection connection) {
		int transitionOccurrenceCount = 0;
		Step startStep = (Step) connection.getStartNode().getValue();
		Step endStep = (Step) connection.getEndNode().getValue();
		CompositeStep parent = startStep.getParent();
		List<StepGoingThrough> stepGoingThroughs = getPlanExecutor().getStepGoingThroughs().stream()
				.filter(stepGoingThrough -> (stepGoingThrough.getStep().getParent() == parent))
				.collect(Collectors.toList());
		for (int i = 0; i < stepGoingThroughs.size(); i++) {
			if (i > 0) {
				if (stepGoingThroughs.get(i - 1).getStep() == startStep) {
					if (stepGoingThroughs.get(i).getStep() == endStep) {
						transitionOccurrenceCount++;
					}
				}
			}
		}
		if (transitionOccurrenceCount > 0) {
			Color connectionColorToRestore = getConnectionColor();
			try {
				setConnectionColor(new Color(115, 195, 140));
				super.paintConnection(g, connection);
			} finally {
				setConnectionColor(connectionColorToRestore);
			}
			annotateConnection(g, connection, "(" + transitionOccurrenceCount + ")");
		} else {
			super.paintConnection(g, connection);
		}
	}

	private void annotateConnection(Graphics g, JConnection connection, String annotation) {
		g.setColor(new Color(65, 145, 90));
		Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(annotation, g);
		Point2D center = connection.getCenter();
		if (center == null) {
			return;
		}
		Rectangle annotationBounds = new Rectangle((int) Math.round(center.getX() - (stringBounds.getWidth() / 2)),
				(int) Math.round(center.getY() - stringBounds.getHeight() * 2.3),
				(int) Math.round(stringBounds.getWidth()), (int) Math.round(stringBounds.getHeight()));
		double rotationAngle = connection.getLabelRotationAngleRadians();
		Point2D rotationCenter = connection.getLabelRotationCenter();
		Graphics2D g2D = (Graphics2D) g.create();
		g2D.rotate(rotationAngle, rotationCenter.getX(), rotationCenter.getY());
		g2D.drawString(annotation, annotationBounds.x, annotationBounds.y + annotationBounds.height);
		g2D.dispose();
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