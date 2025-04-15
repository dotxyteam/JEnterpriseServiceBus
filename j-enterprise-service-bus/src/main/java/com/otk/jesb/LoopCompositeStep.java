package com.otk.jesb;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.otk.jesb.Plan.ExecutionContext;
import com.otk.jesb.Plan.ExecutionInspector;
import com.otk.jesb.Plan.ValidationContext;
import com.otk.jesb.Plan.ValidationContext.VariableDeclaration;
import com.otk.jesb.activity.Activity;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;
import com.otk.jesb.instantiation.CompilationContext;
import com.otk.jesb.instantiation.EvaluationContext;
import com.otk.jesb.instantiation.Function;
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
	protected List<VariableDeclaration> getChildrenVariableDeclarations() {
		List<VariableDeclaration> result = new ArrayList<Plan.ValidationContext.VariableDeclaration>();
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
			List<Step> insideLoopSteps = context.getPlan().getSteps().stream()
					.filter(step -> loopCompositeStep.equals(step.getParent())).collect(Collectors.toList());
			int index = 0;
			while (true) {
				final int finalIndex = index;
				ExecutionContext iterationContext = new ExecutionContext(context, new ExecutionContext.Variable() {

					@Override
					public String getName() {
						return iterationIndexVariableName;
					}

					@Override
					public Object getValue() {
						return finalIndex;
					}
				});
				EvaluationContext evaluationContext = new EvaluationContext(iterationContext, null);
				if ((Boolean) MiscUtils.executeFunction(loopEndCondition, evaluationContext)) {
					break;
				}
				try {
					context.getPlan().execute(insideLoopSteps, iterationContext, executionInspector);
				} catch (Exception e) {
					throw e;
				} catch (Throwable t) {
					throw new AssertionError(t);
				}
				index++;
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
			private Function loopEndCondition = new Function("return " + iterationIndexVariableName + "==3");

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

			@Override
			public Activity build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
				return new LoopActivity(context, executionInspector, loopEndCondition, iterationIndexVariableName);
			}

			@Override
			public Class<?> getActivityResultClass() {
				return null;
			}

			@Override
			public CompilationContext findFunctionCompilationContext(Function function,
					ValidationContext validationContext) {
				if (function != loopEndCondition) {
					throw new AssertionError();
				}
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
				return new CompilationContext(validationContext, null, boolean.class);
			}

		}

	}

}
