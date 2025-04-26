package com.otk.jesb.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import com.otk.jesb.CompositeStep;
import com.otk.jesb.LoopCompositeStep;
import com.otk.jesb.LoopCompositeStep.LoopActivity.Metadata;
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
import com.otk.jesb.diagram.JDiagramObject;
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
import xy.reflect.ui.info.menu.CustomActionMenuItemInfo;
import xy.reflect.ui.info.menu.MenuInfo;
import xy.reflect.ui.info.menu.MenuItemCategory;
import xy.reflect.ui.info.menu.MenuModel;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.iterable.item.BufferedItemPosition;
import xy.reflect.ui.info.type.iterable.item.ItemPositionFactory;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;
import xy.reflect.ui.undo.IModification;
import xy.reflect.ui.undo.ListModificationFactory;
import xy.reflect.ui.undo.UndoOrder;
import xy.reflect.ui.util.Accessor;
import xy.reflect.ui.util.Listener;
import xy.reflect.ui.util.ReflectionUIUtils;

public class PlanDiagram extends JDiagram implements IAdvancedFieldControl {

	private static final long serialVersionUID = 1L;

	private static final int STEP_ICON_WIDTH = 64;
	private static final int STEP_ICON_HEIGHT = 64;

	protected SwingRenderer swingRenderer;
	protected CustomizingForm parentForm;
	protected boolean selectionListeningEnabled = true;
	private Map<ResourcePath, Image> adaptedIconImageByPath = new HashMap<ResourcePath, Image>();

	public PlanDiagram(SwingRenderer swingRenderer, CustomizingForm parentForm) {
		this.swingRenderer = swingRenderer;
		this.parentForm = parentForm;
		setActionSchemes(createActionSchemes());
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

	protected ListControl getCurrentPlanElementsControl() {
		List<Form> forms = new ArrayList<Form>();
		forms.add(getPlanEditor());
		forms.addAll(SwingRendererUtils.findDescendantForms(getPlanEditor(), swingRenderer));
		for (Form form : forms) {
			FieldControlPlaceHolder fieldControlPlaceHolder = form
					.getFieldControlPlaceHolder("focusedStepOrTransitionSurroundings");
			if (fieldControlPlaceHolder != null) {
				return (ListControl) fieldControlPlaceHolder.getFieldControl();
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
			public void nodesMoved(Set<JNode> nodes) {
				ReflectionUI reflectionUI = swingRenderer.getReflectionUI();
				ITypeInfo stepType = reflectionUI.getTypeInfo(new JavaTypeInfoSource(Step.class, null));
				parentForm.getModificationStack().insideComposite("Move Step(s)", UndoOrder.getNormal(),
						new xy.reflect.ui.util.Accessor<Boolean>() {
							@Override
							public Boolean get() {
								for (JNode node : nodes) {
									Step step = (Step) node.getValue();
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
								}
								return true;
							}
						}, false);
			}

			@Override
			public void selectionChanged() {
				if (selectionListeningEnabled) {
					selectionListeningEnabled = false;
					try {
						Set<JDiagramObject> selection = PlanDiagram.this.getSelection();
						getPlan().setFocusedStepOrTransition(
								(selection.size() == 1) ? selection.iterator().next().getValue() : null);
						ListControl currentPlanElementsControl = getCurrentPlanElementsControl();
						currentPlanElementsControl.refreshUI(false);
						currentPlanElementsControl.setSelection((selection.size() == 1) ? selection.stream()
								.map(diagramObject -> currentPlanElementsControl
										.findItemPositionByReference(diagramObject.getValue()))
								.collect(Collectors.toList()) : Collections.emptyList());
						SwingRendererUtils.updateWindowMenu(PlanDiagram.this, swingRenderer);
					} finally {
						selectionListeningEnabled = true;
					}
				}
			}

			@Override
			public void connectionAdded(JConnection conn) {
				Transition newTransition = new Transition();
				newTransition.setStartStep((Step) conn.getStartNode().getValue());
				newTransition.setEndStep((Step) conn.getEndNode().getValue());
				onTransitionInsertionRequest(newTransition);
				JESBReflectionUI.diagramDragIntentByPlan.put(getPlan(), DragIntent.MOVE);
			}
		});
	}

	protected void updateInternalComponentsOnExternalEvents() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				getCurrentPlanElementsControl()
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

	protected void updateStepSelection() {
		setSelection(getCurrentPlanElementsControl().getSelection().stream().map(itemPosition -> {
			if (itemPosition.getItem() instanceof Step) {
				return (JDiagramObject) findNode(itemPosition.getItem());
			} else if (itemPosition.getItem() instanceof Transition) {
				return (JDiagramObject) findConnection(itemPosition.getItem());
			} else {
				throw new AssertionError();
			}
		}).collect(Collectors.toSet()));
	}

	protected void onStepInsertionRequest(Step newStep) {
		Set<JDiagramObject> selection = getSelection();
		if ((selection.size() == 1) && (selection.iterator().next() instanceof JNode)) {
			JNode selectedNode = (JNode) selection.iterator().next();
			if (selectedNode.getValue() instanceof CompositeStep) {
				newStep.setParent((CompositeStep) selectedNode.getValue());
			}
		}
		ReflectionUI reflectionUI = swingRenderer.getReflectionUI();
		ITypeInfo planType = reflectionUI.getTypeInfo(new JavaTypeInfoSource(Plan.class, null));
		DefaultFieldControlData stepsData = new DefaultFieldControlData(reflectionUI, getPlan(),
				ReflectionUIUtils.findInfoByName(planType.getFields(), "steps"));
		IModification modification = new ListModificationFactory(
				new ItemPositionFactory(stepsData).getRootItemPosition(-1)).add(getPlan().getSteps().size(), newStep);
		parentForm.getModificationStack().apply(modification);
	}

	protected void onTransitionInsertionRequest(Transition newTransition) {
		ReflectionUI reflectionUI = swingRenderer.getReflectionUI();
		ITypeInfo planType = reflectionUI.getTypeInfo(new JavaTypeInfoSource(Plan.class, null));
		DefaultFieldControlData transitionsData = new DefaultFieldControlData(reflectionUI, getPlan(),
				ReflectionUIUtils.findInfoByName(planType.getFields(), "transitions"));
		IModification modification = new ListModificationFactory(
				new ItemPositionFactory(transitionsData).getRootItemPosition(-1)).add(0, newTransition);
		parentForm.getModificationStack().apply(modification);
	}

	protected void onDeletionRequest() {
		Set<Object> selectedStepAndTransitions = getSelection().stream()
				.map(selectedObject -> selectedObject.getValue()).collect(Collectors.toSet());
		Plan plan = getPlan();
		Set<Step> stepsToDelete = new HashSet<Step>();
		Set<Transition> transitionsToDelete = new HashSet<Transition>();
		for (Object object : selectedStepAndTransitions) {
			if (object instanceof Step) {
				stepsToDelete.add((Step) object);
				if (object instanceof CompositeStep) {
					stepsToDelete.addAll(MiscUtils.getDescendants((CompositeStep) object, plan));
				}
			} else if (object instanceof Transition) {
				transitionsToDelete.add((Transition) object);
			}
		}
		for (Transition transition : plan.getTransitions()) {
			if (stepsToDelete.contains(transition.getStartStep()) || stepsToDelete.contains(transition.getEndStep())) {
				transitionsToDelete.add(transition);
			}
		}
		ReflectionUI reflectionUI = swingRenderer.getReflectionUI();
		ITypeInfo planType = reflectionUI.getTypeInfo(new JavaTypeInfoSource(Plan.class, null));
		DefaultFieldControlData stepsData = new DefaultFieldControlData(reflectionUI, plan,
				ReflectionUIUtils.findInfoByName(planType.getFields(), "steps"));
		DefaultFieldControlData transitionsData = new DefaultFieldControlData(reflectionUI, plan,
				ReflectionUIUtils.findInfoByName(planType.getFields(), "transitions"));
		ListModificationFactory stepsModificationFactory = new ListModificationFactory(
				new ItemPositionFactory(stepsData).getRootItemPosition(-1));
		ListModificationFactory transitionsModificationFactory = new ListModificationFactory(
				new ItemPositionFactory(transitionsData).getRootItemPosition(-1));
		parentForm.getModificationStack().insideComposite("Delete", UndoOrder.getNormal(), new Accessor<Boolean>() {
			@Override
			public Boolean get() {
				for (Step step : stepsToDelete) {
					IModification modification = stepsModificationFactory.remove(plan.getSteps().indexOf(step));
					parentForm.getModificationStack().apply(modification);
				}
				for (Transition transition : transitionsToDelete) {
					IModification modification = transitionsModificationFactory
							.remove(plan.getTransitions().indexOf(transition));
					parentForm.getModificationStack().apply(modification);
				}
				return true;
			}
		}, false);
	}

	protected List<JDiagramActionScheme> createActionSchemes() {
		return Arrays.asList(new JDiagramActionScheme() {

			@Override
			public String getTitle() {
				return "Add Activity";
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
									result.add(createStepInsertionDiagramAction(new Supplier<Step>() {
										@Override
										public Step get() {
											return new Step(metadata);
										}
									}, metadata.getActivityTypeName(), metadata.getActivityIconImagePath()));
								}
							}
							return result;
						}

					});
				}
				return result;
			}
		}, new JDiagramActionScheme() {

			@Override
			public String getTitle() {
				return "Add Composite";
			}

			@Override
			public List<JDiagramActionCategory> getActionCategories() {
				List<JDiagramActionCategory> result = new ArrayList<JDiagramActionCategory>();
				result.add(new JDiagramActionCategory() {

					final Metadata metadata = new Metadata();

					@Override
					public String getName() {
						return "Flow Control";
					}

					@Override
					public List<JDiagramAction> getActions() {
						List<JDiagramAction> result = new ArrayList<JDiagramAction>();
						result.add(createStepInsertionDiagramAction(new Supplier<Step>() {
							@Override
							public Step get() {
								return new LoopCompositeStep();
							}
						}, metadata.getActivityTypeName(), metadata.getActivityIconImagePath()));
						return result;
					}
				});
				return result;
			}
		});
	}

	private JDiagramAction createStepInsertionDiagramAction(Supplier<Step> newStepSupplier, String label,
			ResourcePath iconResourcePath) {
		return new JDiagramAction() {

			@Override
			public void perform(int x, int y) {
				parentForm.getModificationStack().insideComposite("Add '" + label + "'", UndoOrder.getNormal(),
						new Accessor<Boolean>() {
							@Override
							public Boolean get() {
								Step newStep = newStepSupplier.get();
								newStep.setDiagramX(x);
								newStep.setDiagramY(y);
								Plan plan = getPlan();
								while (plan.getSteps().stream()
										.anyMatch(step -> step.getName().equals(newStep.getName()))) {
									newStep.setName(MiscUtils.nextNumbreredName(newStep.getName()));
								}
								newStep.setParent(getDestinationCompositeStep(x, y));
								if (newStep instanceof CompositeStep) {
									Set<Step> selectedSteps = getSelection().stream()
											.filter(diagramObject -> diagramObject.getValue() instanceof Step)
											.map(diagramObject -> (Step) diagramObject.getValue())
											.collect(Collectors.toSet());
									if (selectedSteps.size() > 0) {
										CompositeStep selectedStepsCommonParent = selectedSteps.iterator().next()
												.getParent();
										ReflectionUI reflectionUI = swingRenderer.getReflectionUI();
										ITypeInfo stepType = reflectionUI
												.getTypeInfo(new JavaTypeInfoSource(Step.class, null));
										for (Step selectedStep : selectedSteps) {
											ReflectionUIUtils.setFieldValueThroughModificationStack(
													new DefaultFieldControlData(reflectionUI, selectedStep,
															ReflectionUIUtils.findInfoByName(stepType.getFields(),
																	"parent")),
													(CompositeStep) newStep, parentForm.getModificationStack(),
													ReflectionUIUtils.getDebugLogListener(reflectionUI));
											if ((selectedStep.getParent() == null)
													|| MiscUtils.getDescendants(selectedStep.getParent(), plan)
															.contains(selectedStepsCommonParent)) {
												selectedStepsCommonParent = selectedStep.getParent();
											}
										}
										ITypeInfo transitionType = reflectionUI
												.getTypeInfo(new JavaTypeInfoSource(Transition.class, null));
										Set<Transition> transitionsToDelete = new HashSet<Transition>();
										for (Transition transition : plan.getTransitions()) {
											if (!selectedSteps.contains(transition.getStartStep())
													&& selectedSteps.contains(transition.getEndStep())) {
												if (plan.getTransitions().stream()
														.anyMatch(otherTransition -> (otherTransition != transition)
																&& (otherTransition.getStartStep() == transition
																		.getStartStep())
																&& (otherTransition.getEndStep() == newStep))) {
													transitionsToDelete.add(transition);
												} else {
													ReflectionUIUtils.setFieldValueThroughModificationStack(
															new DefaultFieldControlData(reflectionUI, transition,
																	ReflectionUIUtils.findInfoByName(
																			transitionType.getFields(), "endStep")),
															(CompositeStep) newStep, parentForm.getModificationStack(),
															ReflectionUIUtils.getDebugLogListener(reflectionUI));
												}
											}
											if (selectedSteps.contains(transition.getStartStep())
													&& !selectedSteps.contains(transition.getEndStep())) {
												if (plan.getTransitions().stream()
														.anyMatch(otherTransition -> (otherTransition != transition)
																&& (otherTransition.getStartStep() == newStep)
																&& (otherTransition.getEndStep() == transition
																		.getEndStep()))) {
													transitionsToDelete.add(transition);
												} else {
													ReflectionUIUtils.setFieldValueThroughModificationStack(
															new DefaultFieldControlData(reflectionUI, transition,
																	ReflectionUIUtils.findInfoByName(
																			transitionType.getFields(), "startStep")),
															(CompositeStep) newStep, parentForm.getModificationStack(),
															ReflectionUIUtils.getDebugLogListener(reflectionUI));
												}
											}
										}
										ITypeInfo planType = reflectionUI
												.getTypeInfo(new JavaTypeInfoSource(Plan.class, null));
										for (Transition transition : transitionsToDelete) {
											DefaultFieldControlData transitionsData = new DefaultFieldControlData(
													reflectionUI, plan, ReflectionUIUtils
															.findInfoByName(planType.getFields(), "transitions"));
											ListModificationFactory transitionsModificationFactory = new ListModificationFactory(
													new ItemPositionFactory(transitionsData).getRootItemPosition(-1));
											IModification modification = transitionsModificationFactory
													.remove(plan.getTransitions().indexOf(transition));
											parentForm.getModificationStack().apply(modification);
										}
										newStep.setParent(selectedStepsCommonParent);
									}
								}
								onStepInsertionRequest(newStep);
								return true;
							}
						}, false);
			}

			@Override
			public String getLabel() {
				return label;
			}

			@Override
			public Icon getIcon() {
				return SwingRendererUtils
						.getIcon(
								SwingRendererUtils
										.scalePreservingRatio(
												SwingRendererUtils.loadImageThroughCache(iconResourcePath,
														ReflectionUIUtils
																.getDebugLogListener(swingRenderer.getReflectionUI())),
												32, 32, Image.SCALE_SMOOTH));
			}
		};
	}

	@Override
	protected JPopupMenu createContextMenu(MouseEvent mouseEvent) {
		JPopupMenu result = super.createContextMenu(mouseEvent);
		result.add(new JSeparator());
		result.add(createCopyAction());
		result.add(createCutAction());
		result.add(createPasteAction(mouseEvent.getX(), mouseEvent.getY()));
		result.add(createDeleteAction());
		result.add(new JSeparator());
		result.add(createSelectAllAction());
		return result;
	}

	private AbstractAction createCopyAction() {
		return new AbstractAction("Copy") {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isEnabled() {
				return Clipboard.canCopy(PlanDiagram.this);
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				Clipboard.copy(PlanDiagram.this);
				SwingRendererUtils.updateWindowMenu(PlanDiagram.this, swingRenderer);
			}
		};
	}

	private AbstractAction createCutAction() {
		return new AbstractAction("Cut") {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isEnabled() {
				return (getSelection().size() > 0) && Clipboard.canCopy(PlanDiagram.this);
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				Clipboard.copy(PlanDiagram.this);
				onDeletionRequest();
			}
		};
	}

	private AbstractAction createPasteAction(int x, int y) {
		return new AbstractAction("Paste") {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isEnabled() {
				return Clipboard.canPaste(PlanDiagram.this);
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				Clipboard.paste(PlanDiagram.this, x, y);
			}
		};
	}

	private AbstractAction createDeleteAction() {
		return new AbstractAction("Delete") {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isEnabled() {
				return getSelection().size() > 0;
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				if (!swingRenderer.openQuestionDialog(PlanDiagram.this, "Remove the selected element(s)?", null, "OK",
						"Cancel")) {
					return;
				}
				onDeletionRequest();
			}
		};
	}

	private AbstractAction createSelectAllAction() {
		return new AbstractAction("Select All") {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isEnabled() {
				return true;
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				Set<JDiagramObject> all = new HashSet<JDiagramObject>();
				all.addAll(getNodes());
				all.addAll(getConnections());
				setSelection(all);
			}
		};
	}

	private CompositeStep getDestinationCompositeStep(int x, int y) {
		for (JNode node : MiscUtils.getReverse(getNodes())) {
			if (node.getValue() instanceof CompositeStep) {
				if (node.containsPoint(x, y, this)) {
					return (CompositeStep) node.getValue();
				}
			}
		}
		return null;
	}

	@Override
	public boolean showsCaption() {
		return false;
	}

	@Override
	public boolean refreshUI(boolean refreshStructure) {
		Plan plan = getPlan();
		setDragIntent(JESBReflectionUI.diagramDragIntentByPlan.getOrDefault(plan, DragIntent.MOVE));
		Set<JDiagramObject> selection = getSelection();
		List<Object> selectedStepAndTransitions = selection.stream().map(selectedObject -> selectedObject.getValue())
				.collect(Collectors.toList());
		clear();
		List<Step> sortedSteps = new ArrayList<Step>(plan.getSteps());
		Collections.sort(sortedSteps, new Comparator<Step>() {
			@Override
			public int compare(Step o1, Step o2) {
				if ((o1.getParent() == null) && (o2.getParent() != null)) {
					return -1;
				}
				if ((o1.getParent() != null) && (o2.getParent() == null)) {
					return 1;
				}
				if ((o1.getParent() != null) && (o2.getParent() != null)) {
					return compare(o1.getParent(), o2.getParent());
				}
				return 0;
			}
		});
		for (Step step : sortedSteps) {
			JNode node = addNode(step, step.getDiagramX(), step.getDiagramY());
			ResourcePath iconImagePath = MiscUtils.getIconImagePath(step);
			if (iconImagePath != null) {
				Image iconImage = adaptedIconImageByPath.get(iconImagePath);
				if (iconImage == null) {
					iconImage = SwingRendererUtils.loadImageThroughCache(iconImagePath,
							ReflectionUIUtils.getDebugLogListener(swingRenderer.getReflectionUI()));
					iconImage = SwingRendererUtils.scalePreservingRatio(iconImage, STEP_ICON_WIDTH, STEP_ICON_HEIGHT,
							Image.SCALE_SMOOTH);
					adaptedIconImageByPath.put(iconImagePath, iconImage);
				}
				node.setImage(iconImage);
			}
			if (step instanceof CompositeStep) {
				int headerHeight = 16;
				int horizontalPadding = (int) (STEP_ICON_WIDTH * 0.75);
				int verticalPadding = (int) (STEP_ICON_HEIGHT * 0.75) - headerHeight;
				Rectangle compositeBounds = ((CompositeStep) step).getChildrenBounds(plan, STEP_ICON_WIDTH,
						STEP_ICON_HEIGHT, (horizontalPadding / 2), (verticalPadding / 2) + headerHeight);
				BufferedImage compositeImage = new BufferedImage(compositeBounds.width, compositeBounds.height,
						BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = compositeImage.createGraphics();
				g.setColor(Color.BLACK);
				g.drawRect(0, 0, compositeImage.getWidth() - 1, headerHeight);
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				g.drawImage(node.getImage(), 0, 0, headerHeight, headerHeight, 0, 0, node.getImage().getWidth(null),
						node.getImage().getHeight(null), null);
				g.setColor(Color.BLACK);
				g.drawRect(0, headerHeight, compositeBounds.width - 1, compositeBounds.height - headerHeight - 1);
				g.dispose();
				node.setCenterX((int) Math.round(compositeBounds.getCenterX()));
				node.setCenterY((int) Math.round(compositeBounds.getCenterY()));
				node.setImage(compositeImage);
			}
		}
		selectionListeningEnabled = false;
		try {
			setSelection(selectedStepAndTransitions.stream().map(selectedStepOrTransition -> {
				if (selectedStepOrTransition instanceof Step) {
					return findNode(selectedStepOrTransition);
				} else if (selectedStepOrTransition instanceof Transition) {
					return findConnection(selectedStepOrTransition);
				} else {
					throw new AssertionError();
				}
			}).collect(Collectors.toSet()));
		} finally {
			selectionListeningEnabled = true;
		}
		for (Transition t : plan.getTransitions()) {
			JNode node1 = findNode(t.getStartStep());
			JNode node2 = findNode(t.getEndStep());
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
		MenuInfo editMenu = menuModel.getMenus().stream().filter(menu -> menu.getCaption().equals("Edit")).findFirst()
				.orElse(null);
		{
			MenuItemCategory category = new MenuItemCategory();
			{
				editMenu.addItemCategory(category);
				for (AbstractAction action : Arrays.asList(createCopyAction(), createCutAction(),
						createPasteAction(getWidth() / 2, getHeight() / 2), createDeleteAction(), null,
						createSelectAllAction())) {
					if (action == null) {
						category = new MenuItemCategory();
						editMenu.addItemCategory(category);
						continue;
					}
					category.addItem(new CustomActionMenuItemInfo(swingRenderer.getReflectionUI(),
							(String) action.getValue(AbstractAction.NAME), null, new Supplier<Boolean>() {
								@Override
								public Boolean get() {
									return action.isEnabled();
								}
							}, new Runnable() {
								@Override
								public void run() {
									action.actionPerformed(null);
								}
							}));
				}
			}

		}

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

	private static class Clipboard {

		private static Clipboard current;

		private ByteArrayOutputStream planStore = new ByteArrayOutputStream();
		private Set<Integer> selectedStepIndexes = new HashSet<Integer>();
		private Set<Integer> selectedTransitionIndexes = new HashSet<Integer>();

		public static boolean canCopy(PlanDiagram planDiagram) {
			return planDiagram.getSelection().size() > 0;
		}

		public static boolean canPaste(PlanDiagram planDiagram) {
			return current != null;
		}

		public static void copy(PlanDiagram planDiagram) {
			current = new Clipboard();
			Plan plan = planDiagram.getPlan();
			Set<Object> selectedStepAndTransitions = planDiagram.getSelection().stream()
					.map(selectedObject -> selectedObject.getValue()).collect(Collectors.toSet());
			try {
				MiscUtils.serialize(plan, current.planStore);
			} catch (IOException e) {
				throw new AssertionError(e);
			}
			for (Object object : selectedStepAndTransitions) {
				if (object instanceof Step) {
					current.selectedStepIndexes.add(plan.getSteps().indexOf(object));
				} else if (object instanceof Transition) {
					current.selectedTransitionIndexes.add(plan.getTransitions().indexOf(object));
				} else {
					throw new AssertionError();
				}
			}
		}

		public static void paste(PlanDiagram planDiagram, int x, int y) {
			Plan planCopy;
			try {
				planCopy = (Plan) MiscUtils.deserialize(new ByteArrayInputStream(current.planStore.toByteArray()));
			} catch (IOException e) {
				throw new AssertionError(e);
			}
			planDiagram.parentForm.getModificationStack().insideComposite("Paste", UndoOrder.getNormal(),
					new Accessor<Boolean>() {
						@Override
						public Boolean get() {
							Set<Step> allStepsToPaste = new HashSet<Step>();
							for (int stepIndex : current.selectedStepIndexes) {
								Step stepCopy = planCopy.getSteps().get(stepIndex);
								allStepsToPaste.add(stepCopy);
								if (stepCopy instanceof CompositeStep) {
									allStepsToPaste
											.addAll(MiscUtils.getDescendants((CompositeStep) stepCopy, planCopy));
								}
							}
							CompositeStep destinationCompositeStep = planDiagram.getDestinationCompositeStep(x, y);
							Plan destinationPlan = planDiagram.getPlan();
							Rectangle boundsOfAllStepsToPaste = null;
							for (Step stepCopy : allStepsToPaste) {
								if ((stepCopy.getParent() == null) || !allStepsToPaste.contains(stepCopy.getParent())) {
									stepCopy.setParent(destinationCompositeStep);
								}
								while (destinationPlan.getSteps().stream().anyMatch(
										destinationStep -> destinationStep.getName().equals(stepCopy.getName()))
										|| allStepsToPaste.stream()
												.anyMatch(otherStepCopy -> (otherStepCopy != stepCopy)
														&& otherStepCopy.getName().equals(stepCopy.getName()))) {
									stepCopy.setName(MiscUtils.nextNumbreredName(stepCopy.getName()));
								}
								Rectangle stepCopyBounds = (stepCopy instanceof CompositeStep)
										? ((CompositeStep) stepCopy).getChildrenBounds(planCopy, STEP_ICON_WIDTH,
												STEP_ICON_HEIGHT, 0, 0)
										: new Rectangle(stepCopy.getDiagramX() - (STEP_ICON_WIDTH / 2),
												stepCopy.getDiagramY() - (STEP_ICON_HEIGHT / 2), STEP_ICON_WIDTH,
												STEP_ICON_HEIGHT);
								if (boundsOfAllStepsToPaste == null) {
									boundsOfAllStepsToPaste = stepCopyBounds;
								} else {
									boundsOfAllStepsToPaste.add(stepCopyBounds);
								}
							}
							Point stepCopyMoveAtDestination = new Point(x - (int) boundsOfAllStepsToPaste.getCenterX(),
									y - (int) boundsOfAllStepsToPaste.getCenterY());
							for (Step stepCopy : allStepsToPaste) {
								stepCopy.setDiagramX(stepCopy.getDiagramX() + stepCopyMoveAtDestination.x);
								stepCopy.setDiagramY(stepCopy.getDiagramY() + stepCopyMoveAtDestination.y);
								planDiagram.onStepInsertionRequest(stepCopy);
							}
							for (Transition transitionCopy : planCopy.getTransitions()) {
								if (allStepsToPaste.contains(transitionCopy.getStartStep())
										&& allStepsToPaste.contains(transitionCopy.getEndStep())) {
									planDiagram.onTransitionInsertionRequest(transitionCopy);
								}
							}
							planDiagram.refreshUI(false);
							planDiagram.setSelection(new HashSet<JDiagramObject>(
									allStepsToPaste.stream().map(step -> (JDiagramObject) planDiagram.findNode(step))
											.collect(Collectors.toSet())));
							planDiagram.getCurrentPlanElementsControl().refreshUI(false);
							return true;
						}
					}, false);
		}

	}

}