package com.otk.jesb.activity.builtin;

import java.io.IOException;

import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.Structure.ClassicStructure;
import com.otk.jesb.Structure.SimpleElement;
import com.otk.jesb.activity.Activity;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.UpToDate;
import com.otk.jesb.util.UpToDate.VersionAccessException;

import xy.reflect.ui.info.ResourcePath;

public class EvaluateActivity implements Activity {

	private Object value;

	public EvaluateActivity(Object value) {
		this.value = value;
	}

	@Override
	public Object execute() throws IOException {
		return value;
	}

	public static class Metadata implements ActivityMetadata {

		@Override
		public String getActivityTypeName() {
			return "Evaluate";
		}

		@Override
		public String getCategoryName() {
			return "General";
		}

		@Override
		public Class<? extends ActivityBuilder> getActivityBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getActivityIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(EvaluateActivity.class.getName().replace(".", "/") + ".png"));
		}
	}

	public static class Builder implements ActivityBuilder {

		private ClassicStructure valueStructure = new ClassicStructure();
		{
			valueStructure.getElements().add(new SimpleElement());
		}
		private final UpToDate<Class<?>> upToDateValueClass = new UpToDate<Class<?>>() {
			@Override
			protected Object retrieveLastVersionIdentifier() {
				return (valueStructure != null) ? MiscUtils.serialize(valueStructure) : null;
			}

			@Override
			protected Class<?> obtainLatest(Object versionIdentifier) {
				if (valueStructure == null) {
					return null;
				} else {
					try {
						String className = EvaluateActivity.class.getPackage().getName() + "." + "Value"
								+ MiscUtils.toDigitalUniqueIdentifier(Builder.this);
						return MiscUtils.IN_MEMORY_COMPILER.compile(className,
								valueStructure.generateJavaTypeSourceCode(className));
					} catch (CompilationError e) {
						throw new UnexpectedError(e);
					}
				}
			}
		};
		private RootInstanceBuilder valueBuilder = new RootInstanceBuilder("Value", new Accessor<String>() {
			@Override
			public String get() {
				Class<?> valueClass;
				try {
					valueClass = upToDateValueClass.get();
				} catch (VersionAccessException e) {
					throw new UnexpectedError(e);
				}
				if (valueClass == null) {
					return null;
				}
				return valueClass.getName();
			}
		});

		public ClassicStructure getValueStructure() {
			return valueStructure;
		}

		public void setValueStructure(ClassicStructure valueStructure) {
			this.valueStructure = valueStructure;
		}

		public RootInstanceBuilder getValueBuilder() {
			return valueBuilder;
		}

		public void setValueBuilder(RootInstanceBuilder valueBuilder) {
			this.valueBuilder = valueBuilder;
		}

		@Override
		public Activity build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			return (EvaluateActivity) valueBuilder.build(new InstantiationContext(context.getVariables(),
					context.getPlan().getValidationContext(context.getCurrentStep()).getVariableDeclarations()));
		}

		@Override
		public Class<?> getActivityResultClass(Plan currentPlan, Step currentStep) {
			try {
				return upToDateValueClass.get();
			} catch (VersionAccessException e) {
				throw new UnexpectedError(e);
			}
		}

		@Override
		public void validate(boolean recursively, Plan plan, Step step) throws ValidationError {
			if (recursively) {
				try {
					valueStructure.validate(recursively);
				} catch (ValidationError e) {
					throw new ValidationError("Failed to validate the value structure", e);
				}
				try {
					valueBuilder.getFacade().validate(recursively,
							plan.getValidationContext(step).getVariableDeclarations());
				} catch (ValidationError e) {
					throw new ValidationError("Failed to validate the value builder", e);
				}
			}
		}
	}

}
