package com.otk.jesb.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
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
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import com.otk.jesb.UnexpectedError;
import com.otk.jesb.operation.Experiment;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.solution.PlanElement;
import com.otk.jesb.solution.CompositeStep;
import com.otk.jesb.solution.CompositeStep.CompositeStepMetadata;
import com.otk.jesb.solution.LoopCompositeStep;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Transition;
import com.otk.jesb.ui.diagram.DragIntent;
import com.otk.jesb.ui.diagram.JConnection;
import com.otk.jesb.ui.diagram.JDiagram;
import com.otk.jesb.ui.diagram.JDiagramAction;
import com.otk.jesb.ui.diagram.JDiagramActionCategory;
import com.otk.jesb.ui.diagram.JDiagramActionScheme;
import com.otk.jesb.ui.diagram.JDiagramListener;
import com.otk.jesb.ui.diagram.JDiagramObject;
import com.otk.jesb.ui.diagram.JNode;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.Pair;

import xy.reflect.ui.ReflectionUI;
import xy.reflect.ui.control.DefaultFieldControlData;
import xy.reflect.ui.control.IAdvancedFieldControl;
import xy.reflect.ui.control.IFieldControlInput;
import xy.reflect.ui.control.swing.ListControl;
import xy.reflect.ui.control.swing.renderer.Form;
import xy.reflect.ui.control.swing.renderer.SwingRenderer;
import xy.reflect.ui.control.swing.util.HyperlinkTooltip;
import xy.reflect.ui.control.swing.util.SwingRendererUtils;
import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.info.ValidationSession;
import xy.reflect.ui.info.menu.CustomActionMenuItemInfo;
import xy.reflect.ui.info.menu.MenuInfo;
import xy.reflect.ui.info.menu.MenuItemCategory;
import xy.reflect.ui.info.menu.MenuModel;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.iterable.item.BufferedItemPosition;
import xy.reflect.ui.info.type.iterable.item.ItemPositionFactory;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;
import xy.reflect.ui.undo.AbstractSimpleModificationListener;
import xy.reflect.ui.undo.IModification;
import xy.reflect.ui.undo.ListModificationFactory;
import xy.reflect.ui.undo.UndoOrder;
import xy.reflect.ui.util.Accessor;
import xy.reflect.ui.util.KeyboardShortcut;
import xy.reflect.ui.util.Listener;
import xy.reflect.ui.util.ReflectionUIUtils;
import xy.reflect.ui.util.ValidationErrorWrapper;

public class PlanDiagram extends JDiagram implements IAdvancedFieldControl {

	private static final long serialVersionUID = 1L;

	private static final int STEP_ICON_WIDTH = 64;
	private static final int STEP_ICON_HEIGHT = 64;
	private static final Color OBJECTS_COLOR = new Color(95, 99, 104);
	private static final Image BACKGROUND_IMAGE;
	static {
		try {
			BACKGROUND_IMAGE = ImageIO.read(PlanDiagram.class.getResourceAsStream("diagram-background.png"));
		} catch (IOException e) {
			throw new UnexpectedError(e);
		}
	}

	protected SwingRenderer swingRenderer;
	protected IFieldControlInput input;
	protected boolean selectionListeningEnabled = true;
	private Map<ResourcePath, Image> adaptedIconImageByPath = new HashMap<ResourcePath, Image>();

	public PlanDiagram(SwingRenderer swingRenderer, IFieldControlInput input) {
		this.swingRenderer = swingRenderer;
		this.input = input;
		setConnectionColor(OBJECTS_COLOR);
		setNodeColor(OBJECTS_COLOR);
		setActionSchemes(createActionSchemes());
		updateExternalComponentsOnInternalEvents();
		updateInternalComponentsOnExternalEvents();
		setImage(BACKGROUND_IMAGE);
		setPreservingRatio(true);
		setFillingAreaWhenPreservingRatio(true);
		setScalingQualitHigh(false);
		setFont(new Font(Font.SANS_SERIF, Font.PLAIN, getFont().getSize()));
		refreshUI(true);
	}

	public Plan getPlan() {
		return (Plan) ((Source) input.getControlData().getValue()).getPlan();
	}

	protected ListControl getFocusedPlanElementsControl() {
		return (ListControl) SwingRendererUtils
				.findDescendantFieldControlPlaceHolder(getPlanEditor(), "focusedElementSurroundings", swingRenderer)
				.getFieldControl();
	}

	protected Form getPlanEditor() {
		return SwingRendererUtils.findAncestorFormOfType(this, Plan.class.getName(), swingRenderer);
	}

	protected void updateExternalComponentsOnInternalEvents() {
		addListener(new JDiagramListener() {

			@Override
			public void nodesMoved(Set<JNode> nodes) {
				ReflectionUI reflectionUI = swingRenderer.getReflectionUI();
				ITypeInfo stepType = reflectionUI.getTypeInfo(new JavaTypeInfoSource(Step.class, null));
				input.getModificationStack().insideComposite("Move Step(s)", UndoOrder.getNormal(),
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
													node.getCenterX(), input.getModificationStack(),
													ReflectionUIUtils.getDebugLogListener(reflectionUI));
									ReflectionUIUtils
											.setFieldValueThroughModificationStack(
													new DefaultFieldControlData(reflectionUI, step,
															ReflectionUIUtils.findInfoByName(stepType.getFields(),
																	"diagramY")),
													node.getCenterY(), input.getModificationStack(),
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
						getPlan().setSelectedElements(
								selection.stream().map(diagramObject -> (PlanElement) diagramObject.getValue())
										.collect(Collectors.toSet()));
						getPlan().setFocusedElementSelectedSurrounding(null);
						updateFocusedPlanElementsControl();
						SwingRendererUtils.updateWindowMenu(PlanDiagram.this, swingRenderer);
					} finally {
						selectionListeningEnabled = true;
					}
				}
			}

			@Override
			public void connectionAdded(JConnection connection) {
				Step startStep = (Step) connection.getStartNode().getValue();
				Step endStep = (Step) connection.getEndNode().getValue();
				if (startStep.getParent() == endStep.getParent()) {
					Transition newTransition = new Transition();
					newTransition.setStartStep((Step) connection.getStartNode().getValue());
					newTransition.setEndStep((Step) connection.getEndNode().getValue());
					onTransitionInsertionRequest(newTransition);
				} else {
					PlanDiagram.this.getConnections().remove(connection);
				}
			}
		});
	}

	protected void updateInternalComponentsOnExternalEvents() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (!isShowing()) {
					return;
				}
				getFocusedPlanElementsControl()
						.addListControlSelectionListener(new Listener<List<BufferedItemPosition>>() {
							@Override
							public void handle(List<BufferedItemPosition> event) {
								ListControl focusedPlanElementsControl = getFocusedPlanElementsControl();
								BufferedItemPosition singleSelection = focusedPlanElementsControl.getSingleSelection();
								getPlan().setFocusedElementSelectedSurrounding(
										(singleSelection != null) ? (PlanElement) singleSelection.getItem() : null);
								if (selectionListeningEnabled) {
									selectionListeningEnabled = false;
									try {
										updateSelection();
									} finally {
										selectionListeningEnabled = true;
									}
								}
							}
						});
			}
		});
	}

	protected void updateFocusedPlanElementsControl() {
		ListControl focusedPlanElementsControl = getFocusedPlanElementsControl();
		Set<JDiagramObject> selection = getSelection();
		focusedPlanElementsControl.refreshUI(false);
		focusedPlanElementsControl.setSelection((selection.size() == 1)
				? selection.stream()
						.map(diagramObject -> focusedPlanElementsControl
								.findItemPositionByReference(diagramObject.getValue()))
						.collect(Collectors.toList())
				: Collections.emptyList());
	}

	protected void updateSelection() {
		Plan plan = getPlan();
		if (plan.getFocusedElementSelectedSurrounding() != null) {
			setSelection(Collections.singleton(findDiagramObject(plan.getFocusedElementSelectedSurrounding())), false);
		} else {
			setSelection(plan.getSelectedElements().stream().map(element -> findDiagramObject(element))
					.collect(Collectors.toSet()), false);
		}
		if (getSelection().size() == 1) {
			scrollTo(getSelection().iterator().next());
		}
	}

	protected void onStepInsertionRequest(Step newStep) {
		Plan plan = getPlan();
		JDiagramObject pointedDiagramObject = getPointedDiagramObject(newStep.getDiagramX(), newStep.getDiagramY());
		if (pointedDiagramObject instanceof JNode) {
			JNode pointedNode = (JNode) pointedDiagramObject;
			if (pointedNode.getValue() instanceof CompositeStep) {
				CompositeStep<?> parent = (CompositeStep<?>) pointedNode.getValue();
				Rectangle parentBounds = getCompositeStepBounds(parent, plan);
				if (parentBounds.contains(newStep.getDiagramX(), newStep.getDiagramY())) {
					newStep.setParent(parent);
				}
			}
		}
		ReflectionUI reflectionUI = swingRenderer.getReflectionUI();
		ITypeInfo planType = reflectionUI.getTypeInfo(new JavaTypeInfoSource(Plan.class, null));
		DefaultFieldControlData stepsData = new DefaultFieldControlData(reflectionUI, plan,
				ReflectionUIUtils.findInfoByName(planType.getFields(), "steps"));
		IModification modification = new ListModificationFactory(
				new ItemPositionFactory(stepsData, this).getRootItemPosition(-1)).add(plan.getSteps().size(), newStep);
		input.getModificationStack().apply(modification);
	}

	protected void onTransitionInsertionRequest(Transition newTransition) {
		Plan plan = getPlan();
		plan.getTransitions().add(newTransition);
		if (plan.isPreceding(newTransition.getEndStep(), newTransition.getStartStep())) {
			plan.getTransitions().remove(newTransition);
			refreshUI(false);
			swingRenderer.handleException(this, new Plan.PlanificationError("Cycle detected"));
			return;
		} else {
			plan.getTransitions().remove(newTransition);
		}
		ReflectionUI reflectionUI = swingRenderer.getReflectionUI();
		ITypeInfo planType = reflectionUI.getTypeInfo(new JavaTypeInfoSource(Plan.class, null));
		DefaultFieldControlData transitionsData = new DefaultFieldControlData(reflectionUI, getPlan(),
				ReflectionUIUtils.findInfoByName(planType.getFields(), "transitions"));
		IModification modification = new ListModificationFactory(
				new ItemPositionFactory(transitionsData, this).getRootItemPosition(-1)).add(0, newTransition);
		input.getModificationStack().apply(modification);
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
					stepsToDelete.addAll(MiscUtils.getDescendants((CompositeStep<?>) object, plan));
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
				new ItemPositionFactory(stepsData, this).getRootItemPosition(-1));
		ListModificationFactory transitionsModificationFactory = new ListModificationFactory(
				new ItemPositionFactory(transitionsData, this).getRootItemPosition(-1));
		input.getModificationStack().insideComposite("Delete", UndoOrder.getNormal(), new Accessor<Boolean>() {
			@Override
			public Boolean get() {
				for (Step step : stepsToDelete) {
					IModification modification = stepsModificationFactory.remove(plan.getSteps().indexOf(step));
					input.getModificationStack().apply(modification);
				}
				for (Transition transition : transitionsToDelete) {
					IModification modification = transitionsModificationFactory
							.remove(plan.getTransitions().indexOf(transition));
					input.getModificationStack().apply(modification);
				}
				return true;
			}
		}, false);
	}

	protected List<JDiagramActionScheme> createActionSchemes() {
		return Arrays.asList(new JDiagramActionScheme() {

			@Override
			public String getTitle() {
				return "Add Operation";
			}

			@Override
			public List<JDiagramActionCategory> getActionCategories() {
				List<String> operationCategoryNames = new ArrayList<String>();
				for (OperationMetadata<?> metadata : MiscUtils.getAllOperationMetadatas()) {
					if (!operationCategoryNames.contains(metadata.getCategoryName())) {
						operationCategoryNames.add(metadata.getCategoryName());
					}
				}
				List<JDiagramActionCategory> result = new ArrayList<JDiagramActionCategory>();
				for (String name : operationCategoryNames) {
					result.add(new JDiagramActionCategory() {

						@Override
						public String getName() {
							return name;
						}

						@Override
						public List<JDiagramAction> getActions() {
							List<JDiagramAction> result = new ArrayList<JDiagramAction>();
							for (OperationMetadata<?> metadata : MiscUtils.getAllOperationMetadatas()) {
								if (name.equals(metadata.getCategoryName())) {
									result.add(createStepInsertionDiagramAction(new Supplier<Step>() {
										@Override
										public Step get() {
											try {
												return new Step(metadata);
											} catch (InstantiationException | IllegalAccessException e) {
												throw new UnexpectedError(e);
											}
										}
									}, metadata.getOperationTypeName(), metadata.getOperationIconImagePath()));
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

					@Override
					public String getName() {
						return "Flow Control";
					}

					@Override
					public List<JDiagramAction> getActions() {
						List<JDiagramAction> result = new ArrayList<JDiagramAction>();
						for (CompositeStepMetadata metadata : MiscUtils.BUILTIN_COMPOSITE_STEP_METADATAS) {
							result.add(createStepInsertionDiagramAction(new Supplier<Step>() {
								@Override
								public Step get() {
									return new LoopCompositeStep();
								}
							}, metadata.getCompositeStepTypeName(), metadata.getCompositeStepIconImagePath()));
						}
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
				input.getModificationStack().insideComposite("Add '" + label + "'", UndoOrder.getNormal(),
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
										CompositeStep<?> selectedStepsCommonParent = selectedSteps.iterator().next()
												.getParent();
										ReflectionUI reflectionUI = swingRenderer.getReflectionUI();
										ITypeInfo stepType = reflectionUI
												.getTypeInfo(new JavaTypeInfoSource(Step.class, null));
										for (Step selectedStep : selectedSteps) {
											ReflectionUIUtils.setFieldValueThroughModificationStack(
													new DefaultFieldControlData(reflectionUI, selectedStep,
															ReflectionUIUtils.findInfoByName(stepType.getFields(),
																	"parent")),
													(CompositeStep<?>) newStep, input.getModificationStack(),
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
															(CompositeStep<?>) newStep, input.getModificationStack(),
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
															(CompositeStep<?>) newStep, input.getModificationStack(),
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
													new ItemPositionFactory(transitionsData, this)
															.getRootItemPosition(-1));
											IModification modification = transitionsModificationFactory
													.remove(plan.getTransitions().indexOf(transition));
											input.getModificationStack().apply(modification);
										}
										newStep.setParent(selectedStepsCommonParent);
									}
								}
								onStepInsertionRequest(newStep);
								plan.setSelectedElements(Collections.singleton(newStep));
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
														ReflectionUIUtils.getDebugLogListener(
																swingRenderer.getReflectionUI()),
														swingRenderer),
												32, 32, Image.SCALE_SMOOTH));
			}
		};
	}

	@Override
	protected JPopupMenu createContextMenu(MouseEvent mouseEvent) {
		JPopupMenu result = super.createContextMenu(mouseEvent);
		Component dragIntentControl;
		{
			Form tmpPlanEditor = swingRenderer.createForm(getPlan());
			dragIntentControl = SwingRendererUtils
					.findDescendantFieldControlPlaceHolder(tmpPlanEditor, "diagramDragIntent", swingRenderer)
					.getFieldControl();
			tmpPlanEditor.getModificationStack().addListener(new AbstractSimpleModificationListener() {
				@Override
				protected void handleAnyEvent(IModification modification) {
					((IAdvancedFieldControl) dragIntentControl).refreshUI(false);
				}
			});
		}
		result.insert(dragIntentControl, 0);
		result.add(new JSeparator());
		result.add(createCopyAction());
		result.add(createCutAction());
		result.add(createPasteAction(mouseEvent.getX(), mouseEvent.getY()));
		result.add(createDeleteAction());
		result.add(new JSeparator());
		result.add(createSelectAllAction());
		result.add(new JSeparator());
		result.add(createExperimentAction());
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

	private AbstractAction createExperimentAction() {
		return new AbstractAction("Experiment...") {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isEnabled() {
				Set<JDiagramObject> selection = getSelection();
				if (selection.size() != 1) {
					return false;
				}
				Object planElement = selection.iterator().next().getValue();
				if (planElement.getClass() != Step.class) {
					return false;
				}
				return true;
			}

			@Override
			public void actionPerformed(ActionEvent event) {
				Step currentStep = (Step) getSelection().iterator().next().getValue();
				OperationBuilder<?> operationBuilder = MiscUtils.copy(currentStep.getOperationBuilder());
				try (Experiment experiment = new Experiment(operationBuilder)) {
					GUI.INSTANCE.openObjectDialog(PlanDiagram.this, experiment, null, null, false);
				} catch (Exception e) {
					throw new UnexpectedError(e);
				}
			}
		};
	}

	private CompositeStep<?> getDestinationCompositeStep(int x, int y) {
		for (JNode node : MiscUtils.getReverse(getNodes())) {
			if (node.getValue() instanceof CompositeStep) {
				if (node.containsPoint(x, y, this)) {
					return (CompositeStep<?>) node.getValue();
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
		if (refreshStructure) {
			if (input.getControlData().getEditorBackgroundColor() != null) {
				setBackground(SwingRendererUtils.getColor(input.getControlData().getEditorBackgroundColor()));
			} else {
				setBackground(Color.WHITE);
			}
		}
		refreshElementObjects();
		return true;
	}

	protected void refreshElementObjects() {
		Plan plan = getPlan();
		setDragIntent(GUI.getDiagramDragIntentByPlan().getOrDefault(plan, DragIntent.MOVE));
		Set<JDiagramObject> selection = getSelection();
		List<Object> selectedStepAndTransitions = selection.stream().map(selectedObject -> selectedObject.getValue())
				.collect(Collectors.toList());
		selectionListeningEnabled = false;
		try {
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
								ReflectionUIUtils.getDebugLogListener(swingRenderer.getReflectionUI()), swingRenderer);
						iconImage = SwingRendererUtils.scalePreservingRatio(iconImage, STEP_ICON_WIDTH,
								STEP_ICON_HEIGHT, Image.SCALE_SMOOTH);
						adaptedIconImageByPath.put(iconImagePath, iconImage);
					}
					node.setImage(iconImage);
				}
				if (step instanceof CompositeStep) {
					Rectangle compositeBounds = getCompositeStepBounds((CompositeStep<?>) step, plan);
					BufferedImage compositeImage = new BufferedImage(compositeBounds.width, compositeBounds.height,
							BufferedImage.TYPE_INT_ARGB);
					Graphics2D g = compositeImage.createGraphics();
					g.setColor(getNodeColor());
					MiscUtils.improveRenderingQuality(g);
					int headerHeight = getCompositeStepHeaderHeight();
					g.drawRect(0, 0, compositeImage.getWidth() - 1, headerHeight);
					g.drawImage(node.getImage(), 0, 0, headerHeight, headerHeight, 0, 0, node.getImage().getWidth(null),
							node.getImage().getHeight(null), null);
					g.drawRect(0, headerHeight, compositeBounds.width - 1, compositeBounds.height - headerHeight - 1);
					g.dispose();
					node.setCenterX((int) Math.round(compositeBounds.getCenterX()));
					node.setCenterY((int) Math.round(compositeBounds.getCenterY()));
					node.setImage(compositeImage);
				}
			}
			for (Transition t : plan.getTransitions()) {
				JNode node1 = findNode(t.getStartStep());
				JNode node2 = findNode(t.getEndStep());
				if ((node1 != null) && (node2 != null)) {
					addConnection(node1, node2, t);
				}
			}
			setSelection(selectedStepAndTransitions.stream().map(selectedStepOrTransition -> {
				if (selectedStepOrTransition instanceof Step) {
					return findNode(selectedStepOrTransition);
				} else if (selectedStepOrTransition instanceof Transition) {
					return findConnection(selectedStepOrTransition);
				} else {
					throw new UnexpectedError();
				}
			}).collect(Collectors.toSet()), false);
		} finally {
			selectionListeningEnabled = true;
		}
		SwingRendererUtils.handleComponentSizeChange(this);
		Runnable selectionUpdate = new Runnable() {
			@Override
			public void run() {
				selectionListeningEnabled = false;
				try {
					updateSelection();
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							if (!isShowing()) {
								return;
							}
							selectionListeningEnabled = false;
							try {
								updateFocusedPlanElementsControl();
							} finally {
								selectionListeningEnabled = true;
							}
						}
					});
				} finally {
					selectionListeningEnabled = true;
				}
			}
		};
		if (isShowing()) {
			selectionUpdate.run();
		} else {
			SwingUtilities.invokeLater(selectionUpdate);
		}
	}

	private Rectangle getCompositeStepBounds(CompositeStep<?> step, Plan plan) {
		int headerHeight = getCompositeStepHeaderHeight();
		int horizontalPadding = (int) (STEP_ICON_WIDTH * 0.75);
		int verticalPadding = (int) (STEP_ICON_HEIGHT * 0.75) - headerHeight;
		return ((CompositeStep<?>) step).getChildrenBounds(plan, STEP_ICON_WIDTH, STEP_ICON_HEIGHT,
				(horizontalPadding / 2), (verticalPadding / 2) + headerHeight);
	}

	private int getCompositeStepHeaderHeight() {
		return 16;
	}

	@Override
	protected void paintNode(Graphics g, JNode node) {
		super.paintNode(g, node);
		paintErrorMarker(g, node);
	}

	@Override
	protected void paintConnection(Graphics g, JConnection connection) {
		super.paintConnection(g, connection);
		paintErrorMarker(g, connection);
	}

	protected void paintErrorMarker(Graphics g, JDiagramObject diagramObject) {
		Throwable validationError;
		try {
			validationError = swingRenderer.getReflectionUI().getValidationErrorRegistry()
					.getValidationError(diagramObject.getValue(), null);
		} catch (Throwable t) {
			swingRenderer.getReflectionUI().logDebug("WARNING: Failed to retrieve validation error attributed to '"
					+ diagramObject.getValue() + "': " + MiscUtils.getPrintedStackTrace(t));
			return;
		}
		if (validationError != null) {
			Point errorMarkerLocation = getErrorMarkerLocation(diagramObject);
			if (errorMarkerLocation != null) {
				g.drawImage(getErrorMarker(), errorMarkerLocation.x, errorMarkerLocation.y, null);
			}
		}
	}

	private Image getErrorMarker() {
		return SwingRendererUtils.ERROR_OVERLAY_ICON.getImage();
	}

	private Point getErrorMarkerLocation(JDiagramObject diagramObject) {
		if (diagramObject instanceof JNode) {
			Rectangle nodeBounds = ((JNode) diagramObject).getImageBounds();
			return new Point(nodeBounds.x, nodeBounds.y);
		}
		if (diagramObject instanceof JConnection) {
			Pair<Point, Point> segment = ((JConnection) diagramObject).getLineSegment();
			if (segment == null) {
				return null;
			}
			return new Point((segment.getFirst().x + segment.getSecond().x) / 2,
					(segment.getFirst().y + segment.getSecond().y) / 2);
		}
		throw new UnexpectedError();
	}

	@Override
	public void validateControlData(ValidationSession session) throws Exception {
		Plan plan = getPlan();
		plan.validate(false);
		List<Pair<String, PlanElement>> titleAndElementPairs = new ArrayList<Pair<String, PlanElement>>();
		titleAndElementPairs.addAll(plan.getSteps().stream()
				.map(step -> new Pair<String, PlanElement>("step '" + step.getName() + "'", step))
				.collect(Collectors.toList()));
		titleAndElementPairs.addAll(plan.getTransitions().stream().map(
				transition -> new Pair<String, PlanElement>("transition '" + transition.getSummary() + "'", transition))
				.collect(Collectors.toList()));
		Map<Pair<String, PlanElement>, Exception> validitionErrorMap = new HashMap<Pair<String, PlanElement>, Exception>();
		for (Pair<String, PlanElement> toValidate : titleAndElementPairs) {
			if (Thread.currentThread().isInterrupted()) {
				return;
			}
			try {
				swingRenderer.getReflectionUI().getValidationErrorRegistry()
						.attributing(toValidate.getSecond(),
								(sessionArg) -> toValidate.getSecond().validate(true, plan),
								validationError -> swingRenderer.getReflectionUI().logDebug(validationError))
						.validate(session);
			} catch (Exception e) {
				validitionErrorMap.put(toValidate, e);
			}
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				repaint();
			}
		});
		if (Thread.currentThread().isInterrupted()) {
			return;
		}
		if (validitionErrorMap.size() > 0) {
			Entry<Pair<String, PlanElement>, Exception> firstErrorEntry = validitionErrorMap.entrySet().iterator()
					.next();
			Pair<String, PlanElement> titleAndObjectPair = firstErrorEntry.getKey();
			Exception validationError = firstErrorEntry.getValue();
			throw new ValidationErrorWrapper("Failed to validate the " + titleAndObjectPair.getFirst(),
					validationError);
		}
	}

	@Override
	public void mousePressed(MouseEvent mouseEvent) {
		requestFocus();
		super.mousePressed(mouseEvent);
	}

	@Override
	public void mouseMoved(MouseEvent mouseEvent) {
		super.mouseMoved(mouseEvent);
		manageErrorTooltipOnMouseMove(mouseEvent);
	}

	protected void manageErrorTooltipOnMouseMove(MouseEvent mouseEvent) {
		JDiagramObject pointedDiagramObject = getPointedDiagramObject(mouseEvent.getX(), mouseEvent.getY());
		Exception currentError = (pointedDiagramObject != null) ? swingRenderer.getReflectionUI()
				.getValidationErrorRegistry().getValidationError(pointedDiagramObject.getValue(), null) : null;
		if (currentError != null) {
			Pair<PlanElement, Exception> newTooltipId = new Pair<PlanElement, Exception>(
					(PlanElement) pointedDiagramObject.getValue(), currentError);
			@SuppressWarnings("unchecked")
			Pair<PlanElement, Exception> oldTooltipId = (HyperlinkTooltip.get(this) != null)
					? (Pair<PlanElement, Exception>) HyperlinkTooltip.get(this).getCustomValue()
					: null;
			if (!newTooltipId.equals(oldTooltipId)) {
				HyperlinkTooltip.set(this, xy.reflect.ui.util.MiscUtils.getPrettyErrorMessage(currentError),
						new Runnable() {
							@Override
							public void run() {
								swingRenderer.openErrorDetailsDialog(PlanDiagram.this, currentError);
							}
						});
				HyperlinkTooltip.get(this).setCustomComponentResponsiveBoundsMapper(component -> {
					Image errorMarker = getErrorMarker();
					Point location = getErrorMarkerLocation(pointedDiagramObject);
					return new Rectangle(location.x, location.y, errorMarker.getWidth(null),
							errorMarker.getHeight(null));
				});
				HyperlinkTooltip.get(this).setCustomValue(newTooltipId);
				HyperlinkTooltip.get(this).getMouseMotionListener().mouseMoved(mouseEvent);
			}
		}
	}

	@Override
	public void addMenuContributions(MenuModel menuModel) {
		MenuInfo editMenu = menuModel.getMenus().stream().filter(menu -> menu.getCaption().equals("Edit")).findFirst()
				.orElse(null);
		{
			MenuItemCategory category = new MenuItemCategory();
			{
				editMenu.addItemCategory(category);
				Pair<AbstractAction, KeyboardShortcut> SEPARATOR = null;
				for (Pair<AbstractAction, KeyboardShortcut> actionAndShortcut : Arrays.asList(
						new Pair<AbstractAction, KeyboardShortcut>(createCopyAction(),
								new KeyboardShortcut(KeyEvent.VK_C, false, true, false, false, false)),
						new Pair<AbstractAction, KeyboardShortcut>(createCutAction(),
								new KeyboardShortcut(KeyEvent.VK_X, false, true, false, false, false)),
						new Pair<AbstractAction, KeyboardShortcut>(createPasteAction(getWidth() / 2, getHeight() / 2),
								new KeyboardShortcut(KeyEvent.VK_V, false, true, false, false, false)),
						new Pair<AbstractAction, KeyboardShortcut>(createDeleteAction(),
								new KeyboardShortcut(KeyEvent.VK_DELETE, false, false, false, false, false)),
						null, new Pair<AbstractAction, KeyboardShortcut>(createSelectAllAction(),
								new KeyboardShortcut(KeyEvent.VK_A, false, true, false, false, false)))) {
					if (actionAndShortcut == SEPARATOR) {
						category = new MenuItemCategory();
						editMenu.addItemCategory(category);
						continue;
					}
					CustomActionMenuItemInfo menuItem = new CustomActionMenuItemInfo(swingRenderer.getReflectionUI(),
							(String) actionAndShortcut.getFirst().getValue(AbstractAction.NAME), null,
							new Supplier<Boolean>() {
								@Override
								public Boolean get() {
									return actionAndShortcut.getFirst().isEnabled();
								}
							}, new Runnable() {
								@Override
								public void run() {
									actionAndShortcut.getFirst().actionPerformed(null);
								}
							});
					menuItem.setKeyboardShortcut(actionAndShortcut.getSecond());
					category.addItem(menuItem);
				}
			}

		}

	}

	@Override
	public boolean requestCustomFocus() {
		return false;
	}

	@Override
	public boolean isModificationStackManaged() {
		return false;
	}

	@Override
	public boolean areValueAccessErrorsManaged() {
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
				throw new UnexpectedError(e);
			}
			for (Object object : selectedStepAndTransitions) {
				if (object instanceof Step) {
					current.selectedStepIndexes.add(plan.getSteps().indexOf(object));
				} else if (object instanceof Transition) {
					current.selectedTransitionIndexes.add(plan.getTransitions().indexOf(object));
				} else {
					throw new UnexpectedError();
				}
			}
		}

		public static void paste(PlanDiagram planDiagram, int x, int y) {
			Plan planCopy;
			try {
				planCopy = (Plan) MiscUtils.deserialize(new ByteArrayInputStream(current.planStore.toByteArray()));
			} catch (IOException e) {
				throw new UnexpectedError(e);
			}
			planDiagram.input.getModificationStack().insideComposite("Paste", UndoOrder.getNormal(),
					new Accessor<Boolean>() {
						@Override
						public Boolean get() {
							Set<Step> allStepsToPaste = new HashSet<Step>();
							for (int stepIndex : current.selectedStepIndexes) {
								Step stepCopy = planCopy.getSteps().get(stepIndex);
								allStepsToPaste.add(stepCopy);
								if (stepCopy instanceof CompositeStep) {
									allStepsToPaste
											.addAll(MiscUtils.getDescendants((CompositeStep<?>) stepCopy, planCopy));
								}
							}
							CompositeStep<?> destinationCompositeStep = planDiagram.getDestinationCompositeStep(x, y);
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
										? ((CompositeStep<?>) stepCopy).getChildrenBounds(planCopy, STEP_ICON_WIDTH,
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
							if (boundsOfAllStepsToPaste == null) {
								return false;
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
											.collect(Collectors.toSet())),
									false);
							planDiagram.getFocusedPlanElementsControl().refreshUI(false);
							return true;
						}
					}, false);
		}

	}

}