package com.otk.jesb.ui;

import java.awt.Color;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.otk.jesb.Plan;
import com.otk.jesb.Step;
import com.otk.jesb.Transition;
import com.otk.jesb.activity.ActivityMetadata;
import com.otk.jesb.diagram.DragIntent;
import com.otk.jesb.diagram.JConnection;
import com.otk.jesb.diagram.JDiagram;
import com.otk.jesb.diagram.JDiagramAction;
import com.otk.jesb.diagram.JDiagramActionCategory;
import com.otk.jesb.diagram.JDiagramActionScheme;
import com.otk.jesb.diagram.JDiagramListener;
import com.otk.jesb.diagram.JNode;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.ReflectionUI;
import xy.reflect.ui.control.DefaultFieldControlData;
import xy.reflect.ui.control.IAdvancedFieldControl;
import xy.reflect.ui.control.swing.ListControl;
import xy.reflect.ui.control.swing.customizer.CustomizingForm;
import xy.reflect.ui.control.swing.renderer.FieldControlPlaceHolder;
import xy.reflect.ui.control.swing.renderer.Form;
import xy.reflect.ui.control.swing.renderer.SwingRenderer;
import xy.reflect.ui.control.swing.util.SwingRendererUtils;
import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.info.menu.MenuModel;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.iterable.item.BufferedItemPosition;
import xy.reflect.ui.info.type.iterable.item.ItemPositionFactory;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;
import xy.reflect.ui.undo.IModification;
import xy.reflect.ui.undo.ListModificationFactory;
import xy.reflect.ui.undo.UndoOrder;
import xy.reflect.ui.util.Listener;
import xy.reflect.ui.util.ReflectionUIUtils;

public class PlanDiagram extends JDiagram implements IAdvancedFieldControl {

	private static final long serialVersionUID = 1L;

	protected SwingRenderer swingRenderer;
	protected CustomizingForm parentForm;
	protected boolean selectionListeningEnabled = true;

	public PlanDiagram(SwingRenderer swingRenderer, CustomizingForm parentForm) {
		this.swingRenderer = swingRenderer;
		this.parentForm = parentForm;
		setActionScheme(createActionScheme());
		updateExternalComponentsOnInternalEvents();
		updateInternalComponentsOnExternalEvents();
		setBackground(Color.WHITE);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				selectionListeningEnabled = false;
				try {
					PlanDiagram.this.refreshUI(true);
					updateStepSelection();
				} finally {
					selectionListeningEnabled = true;
				}
			}
		});
	}

	public Plan getPlan() {
		return (Plan) getPlanEditor().getObject();
	}

	protected ListControl getStepsControl() {
		List<Form> forms = new ArrayList<Form>();
		forms.add(getPlanEditor());
		forms.addAll(SwingRendererUtils.findDescendantForms(getPlanEditor(), swingRenderer));
		for (Form form : forms) {
			FieldControlPlaceHolder stepsFieldControlPlaceHolder = form.getFieldControlPlaceHolder("steps");
			if (stepsFieldControlPlaceHolder != null) {
				return (ListControl) stepsFieldControlPlaceHolder.getFieldControl();
			}
		}
		throw new AssertionError();
	}

	protected Form getPlanEditor() {
		if (parentForm.getObject() instanceof Plan) {
			return parentForm;
		}
		return SwingRendererUtils.findAncestorFormOfType(parentForm, Plan.class.getName(), swingRenderer);
	}

	protected void updateExternalComponentsOnInternalEvents() {
		addListener(new JDiagramListener() {

			@Override
			public void nodeMoved(JNode node) {
				Step step = (Step) node.getObject();
				ReflectionUI reflectionUI = swingRenderer.getReflectionUI();
				ITypeInfo stepType = reflectionUI.getTypeInfo(new JavaTypeInfoSource(Step.class, null));
				parentForm.getModificationStack().insideComposite("Change Step Position", UndoOrder.getNormal(),
						new xy.reflect.ui.util.Accessor<Boolean>() {
							@Override
							public Boolean get() {
								ReflectionUIUtils
										.setFieldValueThroughModificationStack(
												new DefaultFieldControlData(reflectionUI, step,
														ReflectionUIUtils.findInfoByName(stepType.getFields(),
																"diagramX")),
												node.getCenterX(), parentForm.getModificationStack(),
												ReflectionUIUtils.getDebugLogListener(reflectionUI));
								ReflectionUIUtils
										.setFieldValueThroughModificationStack(
												new DefaultFieldControlData(reflectionUI, step,
														ReflectionUIUtils.findInfoByName(stepType.getFields(),
																"diagramY")),
												node.getCenterY(), parentForm.getModificationStack(),
												ReflectionUIUtils.getDebugLogListener(reflectionUI));
								return true;
							}
						}, false);
			}

			@Override
			public void nodeSelected(JNode node) {
				if (selectionListeningEnabled) {
					selectionListeningEnabled = false;
					try {
						if (node == null) {
							getStepsControl().setSingleSelection(null);
						} else {
							Step step = (Step) node.getObject();
							ListControl stepsControl = getStepsControl();
							getStepsControl().setSingleSelection(
									stepsControl.getRootListItemPosition(getPlan().getSteps().indexOf(step)));
						}
					} finally {
						selectionListeningEnabled = true;
					}
				}
			}

			@Override
			public void connectionSelected(JConnection connection) {
			}

			@Override
			public void connectionAdded(JConnection conn) {
				Transition newTransition = new Transition();
				newTransition.setStartStep((Step) conn.getStartNode().getObject());
				newTransition.setEndStep((Step) conn.getEndNode().getObject());
				ReflectionUI reflectionUI = swingRenderer.getReflectionUI();
				ITypeInfo planType = reflectionUI.getTypeInfo(new JavaTypeInfoSource(Plan.class, null));
				DefaultFieldControlData transitionsData = new DefaultFieldControlData(reflectionUI, getPlan(),
						ReflectionUIUtils.findInfoByName(planType.getFields(), "transitions"));
				IModification modification = new ListModificationFactory(
						new ItemPositionFactory(transitionsData).getRootItemPosition(-1)).add(0, newTransition);
				parentForm.getModificationStack().apply(modification);
			}
		});
	}

	protected void updateInternalComponentsOnExternalEvents() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				getStepsControl().addListControlSelectionListener(new Listener<List<BufferedItemPosition>>() {
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

	protected void updateStepSelection() {
		ListControl stepsControl = getStepsControl();
		BufferedItemPosition selection = stepsControl.getSingleSelection();
		if (selection != null) {
			select(getNode(selection.getItem()));
		} else {
			select((JNode) null);
		}
	}

	protected JDiagramActionScheme createActionScheme() {
		return new JDiagramActionScheme() {

			@Override
			public String getTitle() {
				return "Add";
			}

			@Override
			public List<JDiagramActionCategory> getActionCategories() {
				List<String> activityCategoryNames = new ArrayList<String>();
				for (ActivityMetadata metadata : JESBReflectionUI.ACTIVITY_METADATAS) {
					if (!activityCategoryNames.contains(metadata.getCategoryName())) {
						activityCategoryNames.add(metadata.getCategoryName());
					}
				}
				List<JDiagramActionCategory> result = new ArrayList<JDiagramActionCategory>();
				for (String name : activityCategoryNames) {
					result.add(new JDiagramActionCategory() {

						@Override
						public String getName() {
							return name;
						}

						@Override
						public List<JDiagramAction> getActions() {
							List<JDiagramAction> result = new ArrayList<JDiagramAction>();
							for (ActivityMetadata metadata : JESBReflectionUI.ACTIVITY_METADATAS) {
								if (name.equals(metadata.getCategoryName())) {
									result.add(new JDiagramAction() {

										@Override
										public void perform(int x, int y) {
											Step newStep = new Step(metadata);
											newStep.setDiagramX(x);
											newStep.setDiagramY(y);
											ReflectionUI reflectionUI = swingRenderer.getReflectionUI();
											ITypeInfo planType = reflectionUI
													.getTypeInfo(new JavaTypeInfoSource(Plan.class, null));
											DefaultFieldControlData stepsData = new DefaultFieldControlData(
													reflectionUI, getPlan(),
													ReflectionUIUtils.findInfoByName(planType.getFields(), "steps"));
											IModification modification = new ListModificationFactory(
													new ItemPositionFactory(stepsData).getRootItemPosition(-1))
															.add(getPlan().getSteps().size(), newStep);
											parentForm.getModificationStack().apply(modification);
											refreshUI(false);
										}

										@Override
										public String getLabel() {
											return metadata.getActivityTypeName();
										}

										@Override
										public Icon getIcon() {
											return SwingRendererUtils.getIcon(SwingRendererUtils.scalePreservingRatio(
													SwingRendererUtils.loadImageThroughCache(
															metadata.getActivityIconImagePath(),
															ReflectionUIUtils.getDebugLogListener(
																	swingRenderer.getReflectionUI())),
													32, 32, Image.SCALE_SMOOTH));
										}
									});
								}
							}
							return result;
						}
					});
				}
				return result;
			}
		};
	}

	@Override
	protected JPopupMenu createContextMenu(MouseEvent mouseEvent) {
		JPopupMenu result = super.createContextMenu(mouseEvent);
		JConnection selectedConnection = null;
		for (JConnection connection : getConnections()) {
			if (connection.isSelected()) {
				selectedConnection = connection;
				break;
			}
		}
		if (selectedConnection != null) {
			Transition selectedTransition = (Transition) selectedConnection.getObject();
			final int selectedTransitionIndex = getPlan().getTransitions().indexOf(selectedTransition);
			result.insert(new AbstractAction("Remove Transition") {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					ReflectionUI reflectionUI = swingRenderer.getReflectionUI();
					ITypeInfo planType = reflectionUI.getTypeInfo(new JavaTypeInfoSource(Plan.class, null));
					DefaultFieldControlData transitionsData = new DefaultFieldControlData(reflectionUI, getPlan(),
							ReflectionUIUtils.findInfoByName(planType.getFields(), "transitions"));
					IModification modification = new ListModificationFactory(
							new ItemPositionFactory(transitionsData).getRootItemPosition(-1))
									.remove(selectedTransitionIndex);
					parentForm.getModificationStack().apply(modification);
					refreshUI(false);
				}
			}, 0);
		}
		return result;
	}

	@Override
	public boolean showsCaption() {
		return false;
	}

	@Override
	public boolean refreshUI(boolean refreshStructure) {
		Plan plan = getPlan();
		setDragIntent(JESBReflectionUI.diagramDragIntentByPlan.getOrDefault(plan, DragIntent.MOVE));
		JNode selectedNode = getSelectedNode();
		Step selectedStep = (selectedNode != null) ? (Step) selectedNode.getObject() : null;
		clear();
		for (Step step : plan.getSteps()) {
			JNode node = addNode(step, step.getDiagramX(), step.getDiagramY());
			ResourcePath iconImagePath = MiscUtils.getIconImagePath(step);
			if (iconImagePath != null) {
				node.setImage(SwingRendererUtils.loadImageThroughCache(iconImagePath,
						ReflectionUIUtils.getDebugLogListener(swingRenderer.getReflectionUI())));
			}
		}
		select((selectedStep != null) ? getNode(selectedStep) : null);
		for (Transition t : plan.getTransitions()) {
			JNode node1 = getNode(t.getStartStep());
			JNode node2 = getNode(t.getEndStep());
			if ((node1 != null) && (node2 != null)) {
				addConnection(node1, node2, t);
			}
		}
		SwingRendererUtils.handleComponentSizeChange(this);
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

	public static class Source {
	}

	public static class PaletteSource {
	}
}