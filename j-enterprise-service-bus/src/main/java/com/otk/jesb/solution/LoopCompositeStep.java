package com.otk.jesb.solution;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.otk.jesb.Function;
import com.otk.jesb.PotentialError;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.Variable;
import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.Structure.ClassicStructure;
import com.otk.jesb.Structure.SimpleElement;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.Pair;
import com.otk.jesb.util.UpToDate;
import com.otk.jesb.util.UpToDate.VersionAccessException;

import xy.reflect.ui.info.ResourcePath;

public class LoopCompositeStep extends CompositeStep<LoopCompositeStep.LoopOperation> {

	public LoopCompositeStep() {
		super(new LoopOperation.Builder());
	}

	@Override
	public LoopOperation.Builder getOperationBuilder() {
		return (LoopOperation.Builder) super.getOperationBuilder();
	}

	@Override
	public void setOperationBuilder(OperationBuilder<?> operationBuilder) {
		if (!(operationBuilder instanceof LoopOperation.Builder)) {
			throw new UnexpectedError();
		}
		super.setOperationBuilder(operationBuilder);
	}

	@Override
	public List<VariableDeclaration> getContextualVariableDeclarations() {
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

	public List<VariableDeclaration> getLoopEndConditionVariableDeclarations(Solution solutionInstance, Plan plan) {
		List<VariableDeclaration> loopEndConditionVariableDeclarations = new ArrayList<VariableDeclaration>(
				plan.getValidationContext(this, solutionInstance).getVariableDeclarations());
		loopEndConditionVariableDeclarations.addAll(((LoopCompositeStep) this).getContextualVariableDeclarations());
		for (Step descendantStep : ((LoopCompositeStep) this).getDescendantResultProducingSteps(solutionInstance,
				plan)) {
			loopEndConditionVariableDeclarations.add(new StepEventuality(descendantStep, plan, solutionInstance));
		}
		return loopEndConditionVariableDeclarations;
	}

	private List<Step> getDescendantResultProducingSteps(Solution solutionInstance, Plan currentPlan) {
		List<Step> result = new ArrayList<Step>();
		for (Step descendantStep : MiscUtils.getDescendants(this, currentPlan)) {
			if (descendantStep.getOperationBuilder().getOperationResultClass(solutionInstance, currentPlan,
					descendantStep) != null) {
				result.add(descendantStep);
			}
		}
		return result;
	}

	public static class LoopOperation implements Operation {

		private ExecutionContext context;
		private ExecutionInspector executionInspector;
		private Function loopEndCondition;
		private String iterationIndexVariableName;
		private int currentIndex = -1;

		public LoopOperation(ExecutionContext context, ExecutionInspector executionInspector, Function loopEndCondition,
				String iterationIndexVariableName) {
			this.context = context;
			this.executionInspector = executionInspector;
			this.loopEndCondition = loopEndCondition;
			this.iterationIndexVariableName = iterationIndexVariableName;
		}

		public int getCurrentIndex() {
			return currentIndex;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object execute(Solution solutionInstance) throws Exception {
			LoopCompositeStep loopCompositeStep = (LoopCompositeStep) context.getCurrentStep();
			List<Step> insideLoopSteps = loopCompositeStep.getChildren(context.getPlan());
			currentIndex = 0;
			Class<?> resultClass = loopCompositeStep.getOperationBuilder().getOperationResultClass(
					context.getSession().getSolutionInstance(), context.getPlan(), loopCompositeStep);
			List<Object>[] resultLists = (resultClass != null) ? IntStream
					.range(0, resultClass.getDeclaredFields().length).mapToObj(ArrayList::new).toArray(List[]::new)
					: null;
			Variable iterationIndexVariable = new Variable() {

				@Override
				public String getName() {
					return iterationIndexVariableName;
				}

				@Override
				public Object getValue() {
					return currentIndex;
				}
			};
			List<Variable> initialVariables = new ArrayList<Variable>(context.getVariables());
			try {
				context.getVariables().add(iterationIndexVariable);
				for (Step descendantStep : ((LoopCompositeStep) context.getCurrentStep())
						.getDescendantResultProducingSteps(context.getSession().getSolutionInstance(),
								context.getPlan())) {
					context.getVariables().add(new StepSkipping(descendantStep, context.getPlan(), solutionInstance));
				}
				List<VariableDeclaration> loopEndConditionVariableDeclarations = ((LoopCompositeStep) context
						.getCurrentStep()).getLoopEndConditionVariableDeclarations(
								context.getSession().getSolutionInstance(), context.getPlan());
				while (true) {
					if (executionInspector.isExecutionInterrupted()) {
						break;
					}
					MiscUtils.checkVariables(loopEndConditionVariableDeclarations, context.getVariables());
					if ((Boolean) loopEndCondition
							.getCompiledVersion(null, loopEndConditionVariableDeclarations, boolean.class)
							.call(context.getVariables())) {
						break;
					}
					context.getVariables().clear();
					context.getVariables().addAll(initialVariables);
					context.getVariables().add(iterationIndexVariable);
					try {
						context.getPlan().execute(insideLoopSteps, context, executionInspector);
					} catch (Exception e) {
						throw e;
					} catch (Throwable t) {
						throw new PotentialError(t);
					}
					context.setCutrrentStep(loopCompositeStep);
					if (resultLists != null) {
						for (Variable variable : context.getVariables()) {
							int resultFieldIndex = IntStream.range(0, resultClass.getDeclaredFields().length).filter(
									i -> variable.getName().equals(resultClass.getDeclaredFields()[i].getName()))
									.findFirst().orElse(-1);
							if (resultFieldIndex != -1) {
								if (variable.getValue() != null) {
									List<Object> resultList = resultLists[resultFieldIndex];
									resultList.add(variable.getValue());
								}
							}
						}
					}
					currentIndex++;
				}
			} finally {
				context.getVariables().clear();
				context.getVariables().addAll(initialVariables);
			}
			if (resultLists != null) {
				Object[] resultConstructorArguments = IntStream.range(0, resultLists.length).mapToObj(i -> {
					List<Object> resultList = resultLists[i];
					Object resultArray = Array.newInstance(
							resultClass.getDeclaredFields()[i].getType().getComponentType(), resultList.size());
					for (int resultArrayIndex = 0; resultArrayIndex < resultList.size(); resultArrayIndex++) {
						Object object = resultList.get(resultArrayIndex);
						Array.set(resultArray, resultArrayIndex, object);
					}
					return resultArray;
				}).toArray();
				return resultClass.getConstructors()[0].newInstance(resultConstructorArguments);
			} else {
				return null;
			}
		}

		public static class Builder implements OperationBuilder<LoopOperation> {

			private String iterationIndexVariableName = "iterationIndex";
			private Function loopEndCondition = new Function("return " + iterationIndexVariableName + "==3;");
			private Set<String> resultsCollectionTargetedStepNames = new HashSet<String>();
			private UpToDate<Pair<Solution, Pair<Plan, Step>>, Class<?>> upToDateResultClass = new UpToDateResultClass();

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
					Solution solutionInstance, Plan currentPlan, Step currentStep) {
				if (resultsCollectionTargetedStepNames == null) {
					return null;
				} else {
					List<ResultsCollectionConfigurationEntry> result = new ArrayList<LoopCompositeStep.LoopOperation.Builder.ResultsCollectionConfigurationEntry>();
					for (Step descendantStep : ((LoopCompositeStep) currentStep)
							.getDescendantResultProducingSteps(solutionInstance, currentPlan)) {
						result.add(new ResultsCollectionConfigurationEntry(descendantStep, true));
					}
					resultsCollectionTargetedStepNames.stream()
							.filter(targetedStepName -> result.stream()
									.noneMatch(entry -> entry.getStepName().equals(targetedStepName)))
							.forEach(notFoundStepName -> result
									.add(new InvalidResultsCollectionConfigurationEntry(notFoundStepName)));
					if (result.size() == 0) {
						result.add(new InformativeResultsCollectionConfigurationEntry("No compatible steps found!"));
					}
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

			private Class<?> obtainResultClass(Solution solutionInstance, Plan currentPlan, Step currentStep) {
				List<ResultsCollectionConfigurationEntry> resultsCollectionEntries = retrieveResultsCollectionConfigurationEntries(
						solutionInstance, currentPlan, currentStep);
				if (resultsCollectionEntries == null) {
					return null;
				}
				String resultClassName = LoopCompositeStep.class.getName() + "Result"
						+ MiscUtils.toDigitalUniqueIdentifier(LoopOperation.Builder.this);
				ClassicStructure structure = new ClassicStructure();
				{
					for (int i = 0; i < resultsCollectionEntries.size(); i++) {
						ResultsCollectionConfigurationEntry resultsCollectionEntry = resultsCollectionEntries.get(i);
						if (!resultsCollectionEntry.isValid()) {
							continue;
						}
						if (!resultsCollectionEntry.isResultsCollectionEnabled()) {
							continue;
						}
						SimpleElement element = new SimpleElement();
						structure.getElements().add(element);
						element.setName(resultsCollectionEntry.getStepName());
						element.setTypeNameOrAlias(resultsCollectionEntry
								.getOperationResultClass(solutionInstance, currentPlan).getName());
						element.setMultiple(true);
					}
				}
				try {
					return solutionInstance.getRuntime().getInMemoryCompiler().compile(resultClassName,
							structure.generateJavaTypeSourceCode(resultClassName, solutionInstance));
				} catch (CompilationError e) {
					throw new PotentialError(e);
				}
			}

			@Override
			public void validate(boolean recursively, Solution solutionInstance, Plan plan, Step step)
					throws ValidationError {
				if (!MiscUtils.VARIABLE_NAME_PATTERN.matcher(iterationIndexVariableName).matches()) {
					throw new ValidationError("Invalid iteration index variable name: '" + iterationIndexVariableName
							+ "' (should match the following regular expression: "
							+ MiscUtils.VARIABLE_NAME_PATTERN.pattern() + ")");
				}
				for (VariableDeclaration variableDeclaration : plan.getValidationContext(step, solutionInstance)
						.getVariableDeclarations()) {
					if (variableDeclaration.getVariableName().equals(iterationIndexVariableName)) {
						throw new ValidationError(
								"Iteration index variable name already used: " + iterationIndexVariableName + "'");
					}
				}
				List<ResultsCollectionConfigurationEntry> resultsCollectionEntries = retrieveResultsCollectionConfigurationEntries(
						solutionInstance, plan, step);
				if (resultsCollectionEntries != null) {
					List<ResultsCollectionConfigurationEntry> invalidEntries = resultsCollectionEntries.stream()
							.filter(entry -> !entry.isValid()).collect(Collectors.toList());
					if (invalidEntries.size() > 0) {
						throw new ValidationError("Invalid results collection step names: " + MiscUtils.stringJoin(
								invalidEntries.stream().map(entry -> entry.getStepName()).collect(Collectors.toList()),
								", "));
					}
				}
				try {
					loopEndCondition.getCompiledVersion(null,
							((LoopCompositeStep) step).getLoopEndConditionVariableDeclarations(solutionInstance, plan),
							boolean.class);
				} catch (CompilationError e) {
					throw new ValidationError("Failed to validate the loop end condition", e);
				}

			}

			@Override
			public LoopOperation build(ExecutionContext context, ExecutionInspector executionInspector)
					throws Exception {
				return new LoopOperation(context, executionInspector, loopEndCondition, iterationIndexVariableName);
			}

			@Override
			public Class<?> getOperationResultClass(Solution solutionInstance, Plan currentPlan, Step currentStep) {
				try {
					return upToDateResultClass.get(new Pair<Solution, Pair<Plan, Step>>(solutionInstance,
							new Pair<Plan, Step>(currentPlan, currentStep)));
				} catch (VersionAccessException e) {
					throw new PotentialError(e);
				}
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

				public Class<?> getOperationResultClass(Solution solutionInstance, Plan currentPlan) {
					if (targetedStep == null) {
						return null;
					}
					return targetedStep.getOperationBuilder().getOperationResultClass(solutionInstance, currentPlan,
							targetedStep);
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

				@Override
				public int hashCode() {
					final int prime = 31;
					int result = 1;
					result = prime * result + getEnclosingInstance().hashCode();
					result = prime * result + ((targetedStep == null) ? 0 : targetedStep.hashCode());
					result = prime * result + (valid ? 1231 : 1237);
					return result;
				}

				@Override
				public boolean equals(Object obj) {
					if (this == obj)
						return true;
					if (obj == null)
						return false;
					if (getClass() != obj.getClass())
						return false;
					ResultsCollectionConfigurationEntry other = (ResultsCollectionConfigurationEntry) obj;
					if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
						return false;
					if (targetedStep == null) {
						if (other.targetedStep != null)
							return false;
					} else if (!targetedStep.equals(other.targetedStep))
						return false;
					if (valid != other.valid)
						return false;
					return true;
				}

				private Builder getEnclosingInstance() {
					return Builder.this;
				}

				@Override
				public String toString() {
					return "ResultsCollectionConfigurationEntry [targetedStep=" + targetedStep + ", valid=" + valid
							+ "]";
				}

			}

			public class InvalidResultsCollectionConfigurationEntry extends ResultsCollectionConfigurationEntry {

				private String stepName;

				public InvalidResultsCollectionConfigurationEntry(String stepName) {
					super(null, false);
					this.stepName = stepName;
				}

				@Override
				public String getStepName() {
					return stepName;
				}

				@Override
				public int hashCode() {
					final int prime = 31;
					int result = super.hashCode();
					result = prime * result + getEnclosingInstance().hashCode();
					result = prime * result + ((stepName == null) ? 0 : stepName.hashCode());
					return result;
				}

				@Override
				public boolean equals(Object obj) {
					if (this == obj)
						return true;
					if (!super.equals(obj))
						return false;
					if (getClass() != obj.getClass())
						return false;
					InvalidResultsCollectionConfigurationEntry other = (InvalidResultsCollectionConfigurationEntry) obj;
					if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
						return false;
					if (stepName == null) {
						if (other.stepName != null)
							return false;
					} else if (!stepName.equals(other.stepName))
						return false;
					return true;
				}

				private Builder getEnclosingInstance() {
					return Builder.this;
				}

				@Override
				public String toString() {
					return "InvalidResultsCollectionConfigurationEntry [stepName=" + stepName + "]";
				}

			}

			public class InformativeResultsCollectionConfigurationEntry extends ResultsCollectionConfigurationEntry {

				private String message;

				public InformativeResultsCollectionConfigurationEntry(String message) {
					super(null, true);
					this.message = message;
				}

				@Override
				public boolean isResultsCollectionEnabled() {
					return false;
				}

				@Override
				public String getStepName() {
					return message;
				}

				@Override
				public int hashCode() {
					final int prime = 31;
					int result = super.hashCode();
					result = prime * result + getEnclosingInstance().hashCode();
					result = prime * result + ((message == null) ? 0 : message.hashCode());
					return result;
				}

				@Override
				public boolean equals(Object obj) {
					if (this == obj)
						return true;
					if (!super.equals(obj))
						return false;
					if (getClass() != obj.getClass())
						return false;
					InformativeResultsCollectionConfigurationEntry other = (InformativeResultsCollectionConfigurationEntry) obj;
					if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
						return false;
					if (message == null) {
						if (other.message != null)
							return false;
					} else if (!message.equals(other.message))
						return false;
					return true;
				}

				private Builder getEnclosingInstance() {
					return Builder.this;
				}

				@Override
				public String toString() {
					return "InformativeResultsCollectionConfigurationEntry [message=" + message + "]";
				}

			}

			private class UpToDateResultClass extends UpToDate<Pair<Solution, Pair<Plan, Step>>, Class<?>> {

				@Override
				protected Object retrieveLastVersionIdentifier(Pair<Solution, Pair<Plan, Step>> context) {
					Solution solutionInstance = context.getFirst();
					Plan currentPlan = context.getSecond().getFirst();
					Step currentStep = context.getSecond().getSecond();
					List<ResultsCollectionConfigurationEntry> resultsCollectionConfigurationEntries = retrieveResultsCollectionConfigurationEntries(
							solutionInstance, currentPlan, currentStep);
					return resultsCollectionConfigurationEntries.stream()
							.filter(entry -> entry.isValid() && entry.isResultsCollectionEnabled())
							.map(targetedStep -> targetedStep.getOperationResultClass(solutionInstance, currentPlan))
							.collect(Collectors.toList());
				}

				@Override
				protected Class<?> obtainLatest(Pair<Solution, Pair<Plan, Step>> context, Object versionIdentifier) {
					Solution solutionInstance = context.getFirst();
					Plan currentPlan = context.getSecond().getFirst();
					Step currentStep = context.getSecond().getSecond();
					return obtainResultClass(solutionInstance, currentPlan, currentStep);
				}

			}

		}

	}

	public static class Metadata implements CompositeStepMetadata {

		@Override
		public String getCompositeStepTypeName() {
			return "Loop";
		}

		@Override
		public Class<? extends CompositeStep<?>> getCompositeStepClass() {
			return LoopCompositeStep.class;
		}

		@Override
		public ResourcePath getCompositeStepIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(LoopCompositeStep.class.getName().replace(".", "/") + ".png"));
		}
	}

}
