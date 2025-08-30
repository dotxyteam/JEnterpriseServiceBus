package com.otk;

public class TestOp implements com.otk.jesb.operation.Operation {
	public final java.lang.String param2;
	public final int param3;
	public final Param4Structure param4;
	public final Param5Structure param5;
	public final Param6Structure param6;
	public final Param7Structure param7;

	public TestOp(java.lang.String param2, int param3, Param4Structure param4, Param5Structure param5,
			Param6Structure param6, Param7Structure param7) {
		this.param2 = param2;
		this.param3 = param3;
		this.param4 = param4;
		this.param5 = param5;
		this.param6 = param6;
		this.param7 = param7;
	}

	@Override
	public String toString() {
		return "TestOp [param2=" + param2 + ", param3=" + param3 + ", param4=" + param4 + ", param5=" + param5
				+ ", param6=" + param6 + ", param7=" + param7 + "]";
	}

	@Override
	public Object execute() throws Throwable {
		return null;
	}

	public class Builder implements com.otk.jesb.operation.OperationBuilder<TestOp> {
		public java.lang.String param2;
		public com.otk.jesb.Variant<java.lang.Integer> param3Variant = new com.otk.jesb.Variant<java.lang.Integer>(
				java.lang.Integer.class);
		public com.otk.jesb.instantiation.RootInstanceBuilder param4Builder = new com.otk.jesb.instantiation.RootInstanceBuilder(
				"param4Input", Param4Structure.class.getName());
		public com.otk.jesb.instantiation.RootInstanceBuilder param5Builder = new com.otk.jesb.instantiation.RootInstanceBuilder(
				"param5Input", new Param5InputClassNameAccessor() {
				});
		public Param6Structure.GroupBuilder param6GroupBuilder = new Param6Structure.GroupBuilder();
		public com.otk.jesb.Variant<Param7Structure> param7Variant = new com.otk.jesb.Variant<Param7Structure>(
				Param7Structure.class, Param7Structure.ITEM1);

		public Builder() {

		}

		@Override
		public String toString() {
			return "Builder [param2=" + param2 + ", param3Variant=" + param3Variant + ", param4Builder=" + param4Builder
					+ ", param5Builder=" + param5Builder + ", param6GroupBuilder=" + param6GroupBuilder
					+ ", param7Variant=" + param7Variant + "]";
		}

		@Override
		public TestOp build(com.otk.jesb.solution.Plan.ExecutionContext context,
				com.otk.jesb.solution.Plan.ExecutionInspector executionInspector) throws Exception {
			java.lang.String param2 = this.param2;
			int param3 = this.param3Variant.getValue();
			Param4Structure param4 = (Param4Structure) this.param4Builder
					.build(new com.otk.jesb.instantiation.InstantiationContext(context.getVariables(), context.getPlan()
							.getValidationContext(context.getCurrentStep()).getVariableDeclarations()));
			Param5Structure param5 = (Param5Structure) this.param5Builder
					.build(new com.otk.jesb.instantiation.InstantiationContext(context.getVariables(), context.getPlan()
							.getValidationContext(context.getCurrentStep()).getVariableDeclarations()));
			Param6Structure param6 = this.param6GroupBuilder.build(context, executionInspector);
			Param7Structure param7 = this.param7Variant.getValue();
			return new TestOp(param2, param3, param4, param5, param6, param7);
		}

		@Override
		public Class<?> getOperationResultClass(com.otk.jesb.solution.Plan currentPlan,
				com.otk.jesb.solution.Step currentStep) {
			if (param2 == null) {
				return R1ResultStructure.class;
			}
			if (param2 != null) {
				return R2ResultStructure.class;
			}
			return null;
		}

		@Override
		public void validate(boolean recursively, com.otk.jesb.solution.Plan currentPlan,
				com.otk.jesb.solution.Step currentStep) {

		}

		private class Param5InputClassNameAccessor extends com.otk.jesb.util.Accessor<String> {
			@Override
			public String get() {
				if (param2 != null) {
					return A1Param5Structure.class.getName();
				}
				if (param2 == null) {
					return A2Param5Structure.class.getName();
				}
				return null;
			}
		}
	}

	public class Metadata implements com.otk.jesb.operation.OperationMetadata<TestOp> {
		@Override
		public String getOperationTypeName() {
			return "Test Op";
		}

		@Override
		public String getCategoryName() {
			return "General";
		}

		@Override
		public Class<? extends com.otk.jesb.operation.OperationBuilder<TestOp>> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public xy.reflect.ui.info.ResourcePath getOperationIconImagePath() {
			return new xy.reflect.ui.info.ResourcePath(xy.reflect.ui.info.ResourcePath
					.specifyClassPathResourceLocation(TestOp.class.getName().replace(".", "/") + ".png"));
		}
	}

	abstract static public class ResultStructure {
		public final java.lang.String element;

		public ResultStructure(java.lang.String element) {
			this.element = element;
		}

		@Override
		public String toString() {
			return "ResultStructure [element=" + element + "]";
		}

	}

	static public class R1ResultStructure extends ResultStructure {
		public final java.lang.String r1Element;

		public R1ResultStructure(java.lang.String element, java.lang.String r1Element) {
			super(element);
			this.r1Element = r1Element;
		}

		@Override
		public String toString() {
			return "R1ResultStructure [r1Element=" + r1Element + "]";
		}

	}

	static public class R2ResultStructure extends ResultStructure {
		public final java.lang.String r2Element;

		public R2ResultStructure(java.lang.String element, java.lang.String r2Element) {
			super(element);
			this.r2Element = r2Element;
		}

		@Override
		public String toString() {
			return "R2ResultStructure [r2Element=" + r2Element + "]";
		}

	}

	static public class Param4Structure {
		public final java.lang.String element;
		public final Param4StructureElement2Structure element2;

		public Param4Structure(java.lang.String element, Param4StructureElement2Structure element2) {
			this.element = element;
			this.element2 = element2;
		}

		@Override
		public String toString() {
			return "Param4Structure [element=" + element + ", element2=" + element2 + "]";
		}

		static public enum Param4StructureElement2Structure {
			ITEM1, ITEM2;
		}
	}

	abstract static public class Param5Structure {
		public final java.lang.String element;

		public Param5Structure(java.lang.String element) {
			this.element = element;
		}

		@Override
		public String toString() {
			return "Param5Structure [element=" + element + "]";
		}

	}

	static public class A1Param5Structure extends Param5Structure {
		public final java.lang.String a1Element;

		public A1Param5Structure(java.lang.String element, java.lang.String a1Element) {
			super(element);
			this.a1Element = a1Element;
		}

		@Override
		public String toString() {
			return "A1Param5Structure [a1Element=" + a1Element + "]";
		}

	}

	static public class A2Param5Structure extends Param5Structure {
		public final java.lang.String a2Element;

		public A2Param5Structure(java.lang.String element, java.lang.String a2Element) {
			super(element);
			this.a2Element = a2Element;
		}

		@Override
		public String toString() {
			return "A2Param5Structure [a2Element=" + a2Element + "]";
		}

	}

	static public class Param6Structure {
		public final java.lang.Byte subParam1;
		public final com.otk.jesb.meta.Date subParam2;
		public final Param6StructureSubParam3Structure subParam3;
		public final Param6StructureSubParam4Structure subParam4;

		public Param6Structure(java.lang.Byte subParam1, com.otk.jesb.meta.Date subParam2,
				Param6StructureSubParam3Structure subParam3, Param6StructureSubParam4Structure subParam4) {
			this.subParam1 = subParam1;
			this.subParam2 = subParam2;
			this.subParam3 = subParam3;
			this.subParam4 = subParam4;
		}

		@Override
		public String toString() {
			return "Param6Structure [subParam1=" + subParam1 + ", subParam2=" + subParam2 + ", subParam3=" + subParam3
					+ ", subParam4=" + subParam4 + "]";
		}

		static public class GroupBuilder {
			public java.lang.Byte subParam1;
			public com.otk.jesb.Variant<com.otk.jesb.meta.Date> subParam2Variant = new com.otk.jesb.Variant<com.otk.jesb.meta.Date>(
					com.otk.jesb.meta.Date.class);
			public com.otk.jesb.instantiation.RootInstanceBuilder subParam3Builder = new com.otk.jesb.instantiation.RootInstanceBuilder(
					"subParam3Input", Param6StructureSubParam3Structure.class.getName());
			public Param6StructureSubParam4Structure.GroupBuilder subParam4GroupBuilder = new Param6StructureSubParam4Structure.GroupBuilder();

			public GroupBuilder() {

			}

			@Override
			public String toString() {
				return "GroupBuilder [subParam1=" + subParam1 + ", subParam2Variant=" + subParam2Variant
						+ ", subParam3Builder=" + subParam3Builder + ", subParam4GroupBuilder=" + subParam4GroupBuilder
						+ "]";
			}

			public Param6Structure build(com.otk.jesb.solution.Plan.ExecutionContext context,
					com.otk.jesb.solution.Plan.ExecutionInspector executionInspector) throws Exception {
				java.lang.Byte subParam1 = this.subParam1;
				com.otk.jesb.meta.Date subParam2 = this.subParam2Variant.getValue();
				Param6StructureSubParam3Structure subParam3 = (Param6StructureSubParam3Structure) this.subParam3Builder
						.build(new com.otk.jesb.instantiation.InstantiationContext(context.getVariables(), context
								.getPlan().getValidationContext(context.getCurrentStep()).getVariableDeclarations()));
				Param6StructureSubParam4Structure subParam4 = this.subParam4GroupBuilder.build(context,
						executionInspector);
				return new Param6Structure(subParam1, subParam2, subParam3, subParam4);
			}

			public void validate(boolean recursively, com.otk.jesb.solution.Plan currentPlan,
					com.otk.jesb.solution.Step currentStep) {

			}

		}

		static public class Param6StructureSubParam3Structure {
			public final byte[] element;

			public Param6StructureSubParam3Structure(byte[] element) {
				this.element = element;
			}

			@Override
			public String toString() {
				return "Param6StructureSubParam3Structure [element=" + element + "]";
			}

		}

		static public class Param6StructureSubParam4Structure {
			public final java.lang.String subSubParam1;
			public final java.lang.String subSubParam2;

			public Param6StructureSubParam4Structure(java.lang.String subSubParam1, java.lang.String subSubParam2) {
				this.subSubParam1 = subSubParam1;
				this.subSubParam2 = subSubParam2;
			}

			@Override
			public String toString() {
				return "Param6StructureSubParam4Structure [subSubParam1=" + subSubParam1 + ", subSubParam2="
						+ subSubParam2 + "]";
			}

			static public class GroupBuilder {
				public java.lang.String subSubParam1;
				public java.lang.String subSubParam2;

				public GroupBuilder() {

				}

				@Override
				public String toString() {
					return "GroupBuilder [subSubParam1=" + subSubParam1 + ", subSubParam2=" + subSubParam2 + "]";
				}

				public Param6StructureSubParam4Structure build(com.otk.jesb.solution.Plan.ExecutionContext context,
						com.otk.jesb.solution.Plan.ExecutionInspector executionInspector) throws Exception {
					java.lang.String subSubParam1 = this.subSubParam1;
					java.lang.String subSubParam2 = this.subSubParam2;
					return new Param6StructureSubParam4Structure(subSubParam1, subSubParam2);
				}

				public void validate(boolean recursively, com.otk.jesb.solution.Plan currentPlan,
						com.otk.jesb.solution.Step currentStep) {

				}

			}

		}
	}

	static public enum Param7Structure {
		ITEM1, ITEM2;
	}
}