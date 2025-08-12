package com.otk.jesb.operation.builtin;

import java.io.IOException;
import java.util.List;

import com.otk.jesb.ValidationError;
import com.otk.jesb.PotentialError;
import com.otk.jesb.Structure;
import com.otk.jesb.Structure.SimpleElement;
import com.otk.jesb.Structure.StructuredElement;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.InstantiationUtils;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.Pair;
import com.otk.jesb.util.UpToDate;
import com.otk.jesb.util.UpToDate.VersionAccessException;

import xy.reflect.ui.info.ResourcePath;

public class Evaluate implements Operation {

	private Object value;

	public Evaluate(Object value) {
		this.value = value;
	}

	@Override
	public Object execute() throws IOException {
		return value;
	}

	public static class Metadata implements OperationMetadata<Evaluate> {

		@Override
		public String getOperationTypeName() {
			return "Evaluate";
		}

		@Override
		public String getCategoryName() {
			return "General";
		}

		@Override
		public Class<? extends OperationBuilder<Evaluate>> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(
					ResourcePath.specifyClassPathResourceLocation(Evaluate.class.getName().replace(".", "/") + ".png"));
		}
	}

	public static class Builder implements OperationBuilder<Evaluate> {

		private ValueKind valueKind = new SimpleValueKind();
		private boolean multiple = false;

		private final UpToDate<Class<?>> upToDateValueClass = new UpToDateValueClass();
		private RootInstanceBuilder valueBuilder = new RootInstanceBuilder("Value", new ValueClassNameAccessor());

		public RootInstanceBuilder getValueBuilder() {
			return valueBuilder;
		}

		public ValueKind getValueKind() {
			return valueKind;
		}

		public void setValueKind(ValueKind valueKind) {
			this.valueKind = valueKind;
		}

		public boolean isMultiple() {
			return multiple;
		}

		public void setMultiple(boolean multiple) {
			this.multiple = multiple;
		}

		public void setValueBuilder(RootInstanceBuilder valueBuilder) {
			this.valueBuilder = valueBuilder;
		}

		@Override
		public Evaluate build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			return new Evaluate(valueBuilder.build(new InstantiationContext(context.getVariables(),
					context.getPlan().getValidationContext(context.getCurrentStep()).getVariableDeclarations())));
		}

		@Override
		public Class<?> getOperationResultClass(Plan currentPlan, Step currentStep) {
			try {
				return upToDateValueClass.get();
			} catch (VersionAccessException e) {
				throw new PotentialError(e);
			}
		}

		@Override
		public void validate(boolean recursively, Plan plan, Step step) throws ValidationError {
			if (valueKind == null) {
				throw new ValidationError("Value kind not provided");
			}
			try {
				valueKind.validate(true);
			} catch (ValidationError e) {
				throw new ValidationError("Failed to validate the value kind", e);
			}
			if (recursively) {
				try {
					valueBuilder.getFacade().validate(recursively,
							plan.getValidationContext(step).getVariableDeclarations());
				} catch (ValidationError e) {
					throw new ValidationError("Failed to validate the value builder", e);
				}
			}
		}

		private class UpToDateValueClass extends UpToDate<Class<?>> {
			@Override
			protected Object retrieveLastVersionIdentifier() {
				return new Pair<String, Boolean>((valueKind != null) ? MiscUtils.serialize(valueKind) : null, multiple);
			}

			@Override
			protected Class<?> obtainLatest(Object versionIdentifier) {
				if (valueKind == null) {
					return null;
				} else {
					Class<?> result = valueKind.obtainClass();
					if (multiple) {
						result = MiscUtils.getArrayType(result);
					}
					return result;
				}
			}
		}

		private class ValueClassNameAccessor extends Accessor<String> {
			@Override
			public String get() {
				Class<?> valueClass;
				try {
					valueClass = upToDateValueClass.get();
				} catch (VersionAccessException e) {
					throw new PotentialError(e);
				}
				if (valueClass == null) {
					return null;
				}
				return valueClass.getName();
			}
		}

		public static abstract class ValueKind {

			public abstract Class<?> obtainClass();

			public abstract void validate(boolean recursively) throws ValidationError;

		}

		public static class SimpleValueKind extends ValueKind {

			private SimpleElement internalElement = new SimpleElement();

			public String getTypeNameOrAlias() {
				return internalElement.getTypeNameOrAlias();
			}

			public void setTypeNameOrAlias(String typeNameOrAlias) {
				internalElement.setTypeNameOrAlias(typeNameOrAlias);
			}

			public List<String> getTypeNameOrAliasOptions() {
				return internalElement.getTypeNameOrAliasOptions();
			}

			@Override
			public Class<?> obtainClass() {
				return MiscUtils.getJESBClass(internalElement.getTypeName(null));
			}

			public void validate(boolean recursively) throws ValidationError {
				internalElement.validate(recursively);
			}

		}

		public static class StructuredValueKind extends ValueKind {
			private StructuredElement internalElement = new StructuredElement();

			public Structure getStructure() {
				return internalElement.getStructure();
			}

			public void setStructure(Structure structure) {
				internalElement.setStructure(structure);
			}

			public void validate(boolean recursively) throws ValidationError {
				internalElement.validate(recursively);
			}

			@Override
			public Class<?> obtainClass() {
				try {
					String className = Evaluate.class.getName() + "Result" + InstantiationUtils
							.toRelativeTypeNameVariablePart(MiscUtils.toDigitalUniqueIdentifier(this));
					return MiscUtils.IN_MEMORY_COMPILER.compile(className,
							getStructure().generateJavaTypeSourceCode(className));
				} catch (CompilationError e) {
					throw new PotentialError(e);
				}
			}

		}

	}

}
