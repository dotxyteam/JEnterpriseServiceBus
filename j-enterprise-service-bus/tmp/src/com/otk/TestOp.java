package com.otk;

public class TestOp implements com.otk.jesb.operation.Operation {
	public final java.lang.String param2;
	public final int param3;
	public final Param4Structure param4;
	public final Param5Structure param5;

	public TestOp(java.lang.String param2, int param3, Param4Structure param4, Param5Structure param5) {
		this.param2 = param2;
		this.param3 = param3;
		this.param4 = param4;
		this.param5 = param5;
	}

	@Override
	public String toString() {
		return "TestOp [param2=" + param2 + ", param3=" + param3 + ", param4=" + param4 + ", param5=" + param5 + "]";
	}

	@Override
	public Object execute() throws Throwable {
		return null;
	}

	public class Builder implements com.otk.jesb.operation.OperationBuilder<TestOp> {
		public java.lang.String param2 = "bla bla bla";
		public com.otk.jesb.Variant<java.lang.Integer> param3Variant = new com.otk.jesb.Variant<java.lang.Integer>(
				java.lang.Integer.class);
		public com.otk.jesb.instantiation.RootInstanceBuilder param4Builder = new com.otk.jesb.instantiation.RootInstanceBuilder(
				"param4Input", Param4Structure.class.getName());
		public com.otk.jesb.instantiation.RootInstanceBuilder param5Builder = new com.otk.jesb.instantiation.RootInstanceBuilder(
				"param5Input", new Param5InputClassNameAccessor() {
				});

		public Builder() {

		}

		@Override
		public String toString() {
			return "Builder [param2=" + param2 + ", param3Variant=" + param3Variant + ", param4Builder=" + param4Builder
					+ ", param5Builder=" + param5Builder + "]";
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
			return new TestOp(param2, param3, param4, param5);
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

	abstract public static class ResultStructure {
		public final java.lang.String element;

		public ResultStructure(java.lang.String element) {
			this.element = element;
		}

		@Override
		public String toString() {
			return "ResultStructure [element=" + element + "]";
		}

	}

	public static class R1ResultStructure extends ResultStructure {
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

	public static class R2ResultStructure extends ResultStructure {
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

	public static class Param4Structure {
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

		public enum Param4StructureElement2Structure {
			ITEM1, ITEM2;
		}
	}

	abstract public static class Param5Structure {
		public final java.lang.String element;

		public Param5Structure(java.lang.String element) {
			this.element = element;
		}

		@Override
		public String toString() {
			return "Param5Structure [element=" + element + "]";
		}

	}

	public static class A1Param5Structure extends Param5Structure {
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

	public static class A2Param5Structure extends Param5Structure {
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
}