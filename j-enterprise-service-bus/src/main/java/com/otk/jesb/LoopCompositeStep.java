package com.otk.jesb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.otk.jesb.Plan.ExecutionContext;
import com.otk.jesb.Plan.ExecutionInspector;
import com.otk.jesb.Plan.ValidationContext;
import com.otk.jesb.activity.Activity;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.instantiation.InstantiationFunctionCompilationContext;
import com.otk.jesb.instantiation.EvaluationContext;
import com.otk.jesb.instantiation.InstantiationFunction;
import com.otk.jesb.util.InstantiationUtils;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.Pair;
import com.otk.jesb.util.UpToDate;

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
		List<VariableDeclaration> result = new ArrayList<VariableDeclaration>();
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
		private InstantiationFunction loopEndCondition;
		private String iterationIndexVariableName;

		public LoopActivity(ExecutionContext context, ExecutionInspector executionInspector,
				InstantiationFunction loopEndCondition, String iterationIndexVariableName) {
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
			Variable iterationIndexVariable = new Variable() {

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
				List<Variable> initialVariables = new ArrayList<Variable>(context.getVariables());
				for (Step descendantStep : getLoopDescendantSteps(context.getPlan(), context.getCurrentStep())) {
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
				while (true) {
					EvaluationContext evaluationContext = new EvaluationContext(context.getVariables(), null,
							context.getComilationContextProvider());
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

		private static List<Step> getLoopDescendantSteps(Plan currentPlan, Step currentStep) {
			List<Step> result = new ArrayList<Step>();
			LoopCompositeStep loopCompositeStep = (LoopCompositeStep) currentStep;
			for (Step descendantStep : MiscUtils.getDescendants(loopCompositeStep, currentPlan)) {
				if (descendantStep.getActivityBuilder().getActivityResultClass(currentPlan, descendantStep) != null) {
					result.add(descendantStep);
				}
			}
			return result;
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
			private InstantiationFunction loopEndCondition = new InstantiationFunction(
					"return " + iterationIndexVariableName + "==3;");
			private Set<String> resultsCollectionTargetedStepNames = new HashSet<String>();
			private UpToDate<Class<?>> upToDateResultClass = new UpToDate<Class<?>>() {

				@Override
				protected Object retrieveLastModificationIdentifier() {
					@SuppressWarnings("unchecked")
					Pair<Plan, Step> pair = (Pair<Plan, Step>) getCustomValue();
					Plan currentPlan = pair.getFirst();
					Step currentStep = pair.getSecond();
					List<ResultsCollectionConfigurationEntry> resultsCollectionConfigurationEntries = retrieveResultsCollectionConfigurationEntries(
							currentPlan, currentStep);
					return resultsCollectionConfigurationEntries.stream()
							.filter(entry -> entry.isValid() && entry.isResultsCollectionEnabled())
							.map(targetedStep -> targetedStep.getActivityResultClass(currentPlan))
							.collect(Collectors.toList());
				}

				@Override
				protected Class<?> obtainLatest() {
					@SuppressWarnings("unchecked")
					Pair<Plan, Step> pair = (Pair<Plan, Step>) getCustomValue();
					Plan currentPlan = pair.getFirst();
					Step currentStep = pair.getSecond();
					return obtainResultClass(currentPlan, currentStep);
				}

			};

			public String getIterationIndexVariableName() {
				return iterationIndexVariableName;
			}

			public void setIterationIndexVariableName(String iterationIndexVariableName) {
				this.iterationIndexVariableName = iterationIndexVariableName;
			}

			public InstantiationFunction getLoopEndCondition() {
				return loopEndCondition;
			}

			public void setLoopEndCondition(InstantiationFunction loopEndCondition) {
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
				if (resultsCollectionTargetedStepNames == null) {
					return null;
				} else {
					List<ResultsCollectionConfigurationEntry> result = new ArrayList<LoopCompositeStep.LoopActivity.Builder.ResultsCollectionConfigurationEntry>();
					for (Step descendantStep : getLoopDescendantSteps(currentPlan, currentStep)) {
						result.add(new ResultsCollectionConfigurationEntry(descendantStep, true));
					}
					resultsCollectionTargetedStepNames.stream()
							.filter(targetedStepName -> result.stream()
									.noneMatch(entry -> entry.getStepName().equals(targetedStepName)))
							.forEach(notFoundStepName -> result
									.add(new ResultsCollectionConfigurationEntry(null, false) {
										@Override
										public String getStepName() {
											return notFoundStepName;
										}
									}));
					return result;
				}
			}

			public void updateResultsCollectionConfigurationEntries(List<ResultsCollectionConfigurationEntry> entries,
					Plan currentPlan, Step currentStep) {
				if (entries == null) {
					resultsCollectionTargetedStepNames = null;
				} else {
					Set<String> tmp = new HashSet<String>();
					for (ResultsCollectionConfigurationEntry entry : entries) {
						if (entry.isResultsCollectionEnabled()) {
							tmp.add(entry.getStepName());
						}
					}
					resultsCollectionTargetedStepNames = tmp;
				}
			}

			private Class<?> obtainResultClass(Plan currentPlan, Step currentStep) {
				List<ResultsCollectionConfigurationEntry> resultsCollectionEntries = retrieveResultsCollectionConfigurationEntries(
						currentPlan, currentStep);
				if (resultsCollectionEntries == null) {
					return null;
				}
				String resultClassName = LoopCompositeStep.class.getName() + "Result"
						+ MiscUtils.getDigitalUniqueIdentifier(this);
				StringBuilder javaSource = new StringBuilder();
				javaSource.append("package " + MiscUtils.extractPackageNameFromClassName(resultClassName) + ";" + "\n");
				javaSource.append(
						"public class " + MiscUtils.extractSimpleNameFromClassName(resultClassName) + "{" + "\n");
				StringBuilder privateFieldDeclarationsSource = new StringBuilder();
				StringBuilder getterDeclarationsSource = new StringBuilder();
				for (ResultsCollectionConfigurationEntry resultsCollectionEntry : resultsCollectionEntries) {
					if (!resultsCollectionEntry.isValid()) {
						continue;
					}
					if (!resultsCollectionEntry.isResultsCollectionEnabled()) {
						continue;
					}
					String fieldName = resultsCollectionEntry.getStepName();
					String fieldTypeName = List.class.getName() + "<" + MiscUtils.adaptClassNameToSourceCode(
							resultsCollectionEntry.getActivityResultClass(currentPlan).getName()) + ">";
					privateFieldDeclarationsSource.append("  private " + fieldTypeName + " " + fieldName + ";\n");
					String getterMethoName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
					getterDeclarationsSource
							.append("  public " + fieldTypeName + " " + getterMethoName + "() {" + "\n");
					getterDeclarationsSource.append("    return " + fieldName + ";" + "\n");
					getterDeclarationsSource.append("  }" + "\n");
				}
				javaSource.append(privateFieldDeclarationsSource.toString());
				javaSource.append(getterDeclarationsSource.toString());
				javaSource.append("}" + "\n");
				try {
					return MiscUtils.IN_MEMORY_JAVA_COMPILER.compile(resultClassName, javaSource.toString());
				} catch (CompilationError e) {
					throw new AssertionError(e);
				}
			}

			public void validate(Plan currentPlan, Step currentStep) throws ValidationError {
				List<ResultsCollectionConfigurationEntry> resultsCollectionEntries = retrieveResultsCollectionConfigurationEntries(
						currentPlan, currentStep);
				if (resultsCollectionEntries != null) {
					List<ResultsCollectionConfigurationEntry> invalidEntries = resultsCollectionEntries.stream()
							.filter(entry -> !entry.isValid()).collect(Collectors.toList());
					if (invalidEntries.size() > 0) {
						throw new ValidationError("Invalid results collection step names: " + MiscUtils.stringJoin(
								invalidEntries.stream().map(entry -> entry.getStepName()).collect(Collectors.toList()),
								", "));
					}
				}
			}

			@Override
			public Activity build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
				return new LoopActivity(context, executionInspector, loopEndCondition, iterationIndexVariableName);
			}

			@Override
			public Class<?> getActivityResultClass(Plan currentPlan, Step currentStep) {
				upToDateResultClass.setCustomValue(new Pair<Plan, Step>(currentPlan, currentStep));
				return upToDateResultClass.get();
			}

			@Override
			public InstantiationFunctionCompilationContext findFunctionCompilationContext(
					InstantiationFunction function, Step currentStep, Plan currentPlan) {
				if (function != loopEndCondition) {
					throw new AssertionError();
				}
				ValidationContext validationContext = currentPlan.getValidationContext(currentStep);
				validationContext = new ValidationContext(validationContext, new VariableDeclaration() {
					@Override
					public String getVariableName() {
						return iterationIndexVariableName;
					}

					@Override
					public Class<?> getVariableType() {
						return int.class;
					}

				});
				for (Step descendantStep : getLoopDescendantSteps(currentPlan, currentStep)) {
					validationContext.getVariableDeclarations().add(new StepEventuality(descendantStep, currentPlan));
				}
				return new InstantiationFunctionCompilationContext(validationContext.getVariableDeclarations(), null,
						boolean.class);
			}

			public class ResultsCollectionConfigurationEntry {
				private Step targetedStep;
				private boolean valid;

				public ResultsCollectionConfigurationEntry(Step targetedStep, boolean valid) {
					this.targetedStep = targetedStep;
					this.valid = valid;
				}

				public Step getTargetedStep() {
					return targetedStep;
				}

				public String getStepName() {
					if (targetedStep == null) {
						return null;
					}
					return targetedStep.getName();
				}

				public Class<?> getActivityResultClass(Plan currentPlan) {
					if (targetedStep == null) {
						return null;
					}
					return targetedStep.getActivityBuilder().getActivityResultClass(currentPlan, targetedStep);
				}

				public boolean isValid() {
					return valid;
				}

				public boolean isResultsCollectionEnabled() {
					return resultsCollectionTargetedStepNames.contains(getStepName());
				}

				public void setResultsCollectionEnabled(boolean resultsCollectionEnabled) {
					if (resultsCollectionEnabled) {
						resultsCollectionTargetedStepNames.add(getStepName());
					} else {
						resultsCollectionTargetedStepNames.remove(getStepName());
					}
				}

			}

		}

	}

}
