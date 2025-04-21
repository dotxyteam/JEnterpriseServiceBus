package com.otk.jesb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.otk.jesb.Plan.ExecutionContext;
import com.otk.jesb.Plan.ExecutionContext.Variable;
import com.otk.jesb.Plan.ExecutionInspector;
import com.otk.jesb.ValidationContext.VariableDeclaration;
import com.otk.jesb.activity.Activity;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;
import com.otk.jesb.instantiation.CompilationContext;
import com.otk.jesb.instantiation.EvaluationContext;
import com.otk.jesb.util.InstantiationUtils;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.info.ResourcePath;

public class LoopCompositeStep extends CompositeStep {

	public LoopCompositeStep() {
		super(new LoopActivity.Metadata());
	}

	@Override
	public LoopActivity.Builder getActivityBuilder() {
		return (LoopActivity.Builder) super.getActivityBuilder();
	}

	@Override
	public void setActivityBuilder(ActivityBuilder activityBuilder) {
		if (!(activityBuilder instanceof LoopActivity.Builder)) {
			throw new AssertionError();
		}
		super.setActivityBuilder(activityBuilder);
	}

	@Override
	protected List<VariableDeclaration> getContextualVariableDeclarations() {
		List<VariableDeclaration> result = new ArrayList<ValidationContext.VariableDeclaration>();
		result.add(new VariableDeclaration() {

			@Override
			public String getVariableName() {
				return getActivityBuilder().getIterationIndexVariableName();
			}

			@Override
			public Class<?> getVariableType() {
				return int.class;
			}

		});
		return result;
	}

	public static class LoopActivity implements Activity {

		private ExecutionContext context;
		private ExecutionInspector executionInspector;
		private Function loopEndCondition;
		private String iterationIndexVariableName;

		public LoopActivity(ExecutionContext context, ExecutionInspector executionInspector, Function loopEndCondition,
				String iterationIndexVariableName) {
			this.context = context;
			this.executionInspector = executionInspector;
			this.loopEndCondition = loopEndCondition;
			this.iterationIndexVariableName = iterationIndexVariableName;
		}

		@Override
		public Object execute() throws Exception {
			LoopCompositeStep loopCompositeStep = (LoopCompositeStep) context.getCurrentStep();
			List<Step> insideLoopSteps = loopCompositeStep.getChildren(context.getPlan());
			final int[] index = new int[] { 0 };
			Variable iterationIndexVariable = new ExecutionContext.Variable() {

				@Override
				public String getName() {
					return iterationIndexVariableName;
				}

				@Override
				public Object getValue() {
					return index[0];
				}
			};
			context.getVariables().add(iterationIndexVariable);
			try {
				List<ExecutionContext.Variable> initialVariables = new ArrayList<ExecutionContext.Variable>(
						context.getVariables());
				for (Step descendantStep : MiscUtils.getDescendants(loopCompositeStep, context.getPlan())) {
					if (descendantStep.getActivityBuilder().getActivityResultClass() != null) {
						context.getVariables().add(new Variable() {

							@Override
							public String getName() {
								return descendantStep.getName();
							}

							@Override
							public Object getValue() {
								return null;
							}

						});
					}
				}
				while (true) {
					EvaluationContext evaluationContext = new EvaluationContext(context, null);
					if ((Boolean) InstantiationUtils.executeFunction(loopEndCondition, evaluationContext)) {
						break;
					}
					context.getVariables().clear();
					context.getVariables().addAll(initialVariables);
					try {
						context.getPlan().execute(insideLoopSteps, context, executionInspector);
					} catch (Exception e) {
						throw e;
					} catch (Throwable t) {
						throw new AssertionError(t);
					}
					context.setCutrrentStep(loopCompositeStep);
					index[0]++;
				}
			} finally {
				context.getVariables().remove(iterationIndexVariable);
			}
			return null;
		}

		public static class Metadata implements ActivityMetadata {

			@Override
			public String getActivityTypeName() {
				return "Loop";
			}

			@Override
			public String getCategoryName() {
				return "Composite";
			}

			@Override
			public Class<? extends ActivityBuilder> getActivityBuilderClass() {
				return Builder.class;
			}

			@Override
			public ResourcePath getActivityIconImagePath() {
				return new ResourcePath(ResourcePath.specifyClassPathResourceLocation(
						LoopCompositeStep.class.getPackage().getName().replace(".", "/") + "/Loop.png"));
			}
		}

		public static class Builder implements ActivityBuilder {

			private String iterationIndexVariableName = "iterationIndex";
			private Function loopEndCondition = new Function("return " + iterationIndexVariableName + "==3;");
			private Set<String> resultsCollectionTargetedStepNames = new HashSet<String>();

			public String getIterationIndexVariableName() {
				return iterationIndexVariableName;
			}

			public void setIterationIndexVariableName(String iterationIndexVariableName) {
				this.iterationIndexVariableName = iterationIndexVariableName;
			}

			public Function getLoopEndCondition() {
				return loopEndCondition;
			}

			public void setLoopEndCondition(Function loopEndCondition) {
				this.loopEndCondition = loopEndCondition;
			}

			public Set<String> getResultsCollectionTargetedStepNames() {
				return resultsCollectionTargetedStepNames;
			}

			public void setResultsCollectionTargetedStepNames(Set<String> resultsCollectionTargetedStepNames) {
				this.resultsCollectionTargetedStepNames = resultsCollectionTargetedStepNames;
			}

			public List<ResultsCollectionConfigurationEntry> retrieveResultsCollectionConfigurationEntries(
					Plan currentPlan, Step currentStep) {
				List<ResultsCollectionConfigurationEntry> result = new ArrayList<LoopCompositeStep.LoopActivity.Builder.ResultsCollectionConfigurationEntry>();
				LoopCompositeStep loopCompositeStep = (LoopCompositeStep) currentStep;
				for (Step descendantStep : MiscUtils.getDescendants(loopCompositeStep, currentPlan)) {
					if (descendantStep.getActivityBuilder().getActivityResultClass() != null) {
						result.add(new ResultsCollectionConfigurationEntry(descendantStep.getName()));
					}
				}
				return result;
			}

			@Override
			public Activity build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
				return new LoopActivity(context, executionInspector, loopEndCondition, iterationIndexVariableName);
			}

			@Override
			public Class<?> getActivityResultClass() {
				return null;
			}

			@Override
			public CompilationContext findFunctionCompilationContext(Function function, Step currentStep,
					Plan currentPlan) {
				if (function != loopEndCondition) {
					throw new AssertionError();
				}
				ValidationContext validationContext = currentPlan.getValidationContext(currentStep);
				validationContext = new ValidationContext(validationContext,
						new ValidationContext.VariableDeclaration() {
							@Override
							public String getVariableName() {
								return iterationIndexVariableName;
							}

							@Override
							public Class<?> getVariableType() {
								return int.class;
							}

						});
				LoopCompositeStep loopCompositeStep = (LoopCompositeStep) currentStep;
				for (Step descendantStep : MiscUtils.getDescendants(loopCompositeStep, currentPlan)) {
					if (descendantStep.getActivityBuilder().getActivityResultClass() != null) {
						validationContext.getVariableDeclarations().add(new StepEventuality(descendantStep));
					}
				}
				return new CompilationContext(validationContext, null, boolean.class);
			}

			public class ResultsCollectionConfigurationEntry {
				private String stepName;

				public ResultsCollectionConfigurationEntry(String stepName) {
					this.stepName = stepName;
				}

				public String getStepName() {
					return stepName;
				}

				public boolean isResultsCollectionEnabled() {
					return resultsCollectionTargetedStepNames.contains(stepName);
				}

				public void setResultsCollectionEnabled(boolean resultsCollectionEnabled) {
					if (resultsCollectionEnabled) {
						resultsCollectionTargetedStepNames.add(stepName);
					} else {
						resultsCollectionTargetedStepNames.remove(stepName);
					}
				}

			}

		}

	}

}
