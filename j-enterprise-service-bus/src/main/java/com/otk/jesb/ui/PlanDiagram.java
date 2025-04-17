package com.otk.jesb.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JPopupMenu;
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
				Step step = (Step) node.getValue();
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
			public void selectionChanged() {
				if (selectionListeningEnabled) {
					selectionListeningEnabled = false;
					try {
						ListControl stepsControl = getStepsControl();
						stepsControl.setSelection(PlanDiagram.this.getSelection().stream()
								.filter(diagramObject -> diagramObject instanceof JNode)
								.map(diagramObject -> stepsControl
										.findItemPositionByReference(diagramObject.getValue()))
								.collect(Collectors.toList()));
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
		setSelection(getStepsControl().getSelection().stream()
				.map(itemPosition -> (JDiagramObject) findNode(itemPosition.getItem())).collect(Collectors.toSet()));
	}

	protected void onStepInsertionRequest(Step newStep, int x, int y) {
		newStep.setDiagramX(x);
		newStep.setDiagramY(y);
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
									result.add(new JDiagramAction() {

										@Override
										public void perform(int x, int y) {
											Step newStep = new Step(metadata);
											onStepInsertionRequest(newStep, x, y);
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
						result.add(new JDiagramAction() {

							@Override
							public void perform(int x, int y) {
								LoopCompositeStep newComposite = new LoopCompositeStep();
								onStepInsertionRequest(newComposite, x, y);

							}

							@Override
							public String getLabel() {
								return metadata.getActivityTypeName();
							}

							@Override
							public Icon getIcon() {
								return SwingRendererUtils
										.getIcon(
												SwingRendererUtils.scalePreservingRatio(
														SwingRendererUtils.loadImageThroughCache(
																metadata.getActivityIconImagePath(),
																ReflectionUIUtils.getDebugLogListener(
																		swingRenderer.getReflectionUI())),
														32, 32, Image.SCALE_SMOOTH));
							}
						});
						return result;
					}
				});
				return result;
			}
		});
	}

	@Override
	protected JPopupMenu createContextMenu(MouseEvent mouseEvent) {
		JPopupMenu result = super.createContextMenu(mouseEvent);
		Set<JDiagramObject> selection = getSelection();
		if(selection.size() == 1) {
			if(selection.iterator().next() instanceof JConnection) {
				JConnection selectedConnection = (JConnection) selection.iterator().next();
				Transition selectedTransition = (Transition) selectedConnection.getValue();
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
				Rectangle compositeBounds = ((CompositeStep) step).getChildrenBounds(plan,
						STEP_ICON_WIDTH + horizontalPadding, STEP_ICON_HEIGHT + verticalPadding + (headerHeight * 2));
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