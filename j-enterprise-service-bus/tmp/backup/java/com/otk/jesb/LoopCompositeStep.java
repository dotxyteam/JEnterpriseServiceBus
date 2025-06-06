package com.otk.jesb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.instantiation.CompilationContext;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.InstantiationFunction;
import com.otk.jesb.util.InstantiationUtils;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.info.ResourcePath;

public class LoopCompositeStep extends CompositeStep {

	public LoopCompositeStep() {
		super(new LoopOperation.Metadata());
	}

	@Override
	public LoopOperation.Builder getOperationBuilder() {
		return (LoopOperation.Builder) super.getOperationBuilder();
	}

	@Override
	public void setOperationBuilder(OperationBuilder operationBuilder) {
		if (!(operationBuilder instanceof LoopOperation.Builder)) {
			throw new AssertionError();
		}
		super.setOperationBuilder(operationBuilder);
	}

	@Override
	protected List<VariableDeclaration> getContextualVariableDeclarations() {
		List<VariableDeclaration> result = new ArrayList<VariableDeclaration>();
		result.add(new VariableDeclaration() {

			@Override
			public String getVariableName() {
				return getOperationBuilder().getIterationIndexVariableName();
			}

			@Override
			public Class<?> getVariableType() {
				return int.class;
			}

		});
		return result;
	}

	public static class LoopOperation implements Operation {

		private ExecutionContext context;
		private ExecutionInspector executionInspector;
		private InstantiationFunction loopEndCondition;
		private String iterationIndexVariableName;

		public LoopOperation(ExecutionContext context, ExecutionInspector executionInspector, InstantiationFunction loopEndCondition,
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
				List<Variable> initialVariables = new ArrayList<Variable>(
						context.getVariables());
				for (Step descendantStep : MiscUtils.getDescendants(loopCompositeStep, context.getPlan())) {
					if (descendantStep.getOperationBuilder().getOperationResultClass() != null) {
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

		public static class Metadata implements OperationMetadata {

			@Override
			public String getOperationTypeName() {
				return "Loop";
			}

			@Override
			public String getCategoryName() {
				return "Composite";
			}

			@Override
			public Class<? extends OperationBuilder> getOperationBuilderClass() {
				return Builder.class;
			}

			@Override
			public ResourcePath getOperationIconImagePath() {
				return new ResourcePath(ResourcePath.specifyClassPathResourceLocation(
						LoopCompositeStep.class.getPackage().getName().replace(".", "/") + "/Loop.png"));
			}
		}

		public static class Builder implements OperationBuilder {

			private String iterationIndexVariableName = "iterationIndex";
			private InstantiationFunction loopEndCondition = new InstantiationFunction("return " + iterationIndexVariableName + "==3;");
			private Set<String> resultsCollectionTargetedStepNames = new HashSet<String>();

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
				List<ResultsCollectionConfigurationEntry> result = new ArrayList<LoopCompositeStep.LoopOperation.Builder.ResultsCollectionConfigurationEntry>();
				LoopCompositeStep loopCompositeStep = (LoopCompositeStep) currentStep;
				for (Step descendantStep : MiscUtils.getDescendants(loopCompositeStep, currentPlan)) {
					if (descendantStep.getOperationBuilder().getOperationResultClass() != null) {
						result.add(new ResultsCollectionConfigurationEntry(descendantStep.getName()));
					}
				}
				return result;
			}

			@Override
			public Operation build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
				return new LoopOperation(context, executionInspector, loopEndCondition, iterationIndexVariableName);
			}

			@Override
			public Class<?> getOperationResultClass() {
				return null;
			}

			@Override
			public CompilationContext findFunctionCompilationContext(InstantiationFunction function, Step currentStep,
					Plan currentPlan) {
				if (function != loopEndCondition) {
					throw new AssertionError();
				}
				ValidationContext validationContext = currentPlan.getValidationContext(currentStep);
				validationContext = new ValidationContext(validationContext,
						new VariableDeclaration() {
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
					if (descendantStep.getOperationBuilder().getOperationResultClass() != null) {
						validationContext.getVariableDeclarations().add(new StepEventuality(descendantStep));
					}
				}
				return new CompilationContext(validationContext.getVariableDeclarations(), null, boolean.class);
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
