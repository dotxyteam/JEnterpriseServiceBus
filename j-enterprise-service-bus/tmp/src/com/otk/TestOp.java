package com.otk;

public class TestOp implements com.otk.jesb.operation.Operation {
	public final Param1Structure param1;
	public final java.lang.String param2;
	public final int param3;
	public final Param4Structure param4;
	public final Param5Structure param5;

	public TestOp(Param1Structure param1, java.lang.String param2, int param3, Param4Structure param4,
			Param5Structure param5) {
		this.param1 = param1;
		this.param2 = param2;
		this.param3 = param3;
		this.param4 = param4;
		this.param5 = param5;
	}

	@Override
	public String toString() {
		return "TestOp [param1=" + param1 + ", param2=" + param2 + ", param3=" + param3 + ", param4=" + param4
				+ ", param5=" + param5 + "]";
	}

	@Override
	public Object execute() throws Throwable {
		return null;
	}

	public class Builder implements com.otk.jesb.operation.OperationBuilder<TestOp> {
		public Param1Structure param1;
		public java.lang.String param2 = "bla bla bla";
		public com.otk.jesb.Variant<java.lang.Integer> param3Variant = new com.otk.jesb.Variant<java.lang.Integer>(
				java.lang.Integer.class);
		public com.otk.jesb.instantiation.RootInstanceBuilder param4Builder = new com.otk.jesb.instantiation.RootInstanceBuilder(
				"param4Input", Param4Structure.class.getName());
		public Param5Structure param5;

		public Builder() {

		}

		@Override
		public String toString() {
			return "Builder [param1=" + param1 + ", param2=" + param2 + ", param3Variant=" + param3Variant
					+ ", param4Builder=" + param4Builder + ", param5=" + param5 + "]";
		}

		@Override
		public TestOp build(com.otk.jesb.solution.Plan.ExecutionContext context,
				com.otk.jesb.solution.Plan.ExecutionInspector executionInspector) throws Exception {
			Param1Structure param1 = this.param1;
			java.lang.String param2 = this.param2;
			int param3 = this.param3Variant.getValue();
			Param4Structure param4 = (Param4Structure) this.param4Builder
					.build(new com.otk.jesb.instantiation.InstantiationContext(context.getVariables(), context.getPlan()
							.getValidationContext(context.getCurrentStep()).getVariableDeclarations()));
			Param5Structure param5 = this.param5;
			return new TestOp(param1, param2, param3, param4, param5);
		}

		@Override
		public Class<?> getOperationResultClass(com.otk.jesb.solution.Plan currentPlan,
				com.otk.jesb.solution.Step currentStep) {
			return null;
		}

		@Override
		public void validate(boolean recursively, com.otk.jesb.solution.Plan currentPlan,
				com.otk.jesb.solution.Step currentStep) {

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

	public static class Param1Structure {
		public final java.lang.String element;
		public final Param1StructureElement2Structure element2;

		public Param1Structure(java.lang.String element, Param1StructureElement2Structure element2) {
			this.element = element;
			this.element2 = element2;
		}

		@Override
		public String toString() {
			return "Param1Structure [element=" + element + ", element2=" + element2 + "]";
		}

		public enum Param1StructureElement2Structure {
			ITEM1, ITEM2;
		}
	}

	public static class Param4Structure {
		public final java.lang.String element;

		public Param4Structure(java.lang.String element) {
			this.element = element;
		}

		@Override
		public String toString() {
			return "Param4Structure [element=" + element + "]";
		}

	}

	abstract public static class Param5Structure {

		public Param5Structure() {

		}

		@Override
		public String toString() {
			return "Param5Structure []";
		}

	}

	public static class A1Param5Structure extends Param5Structure {

		public A1Param5Structure() {

		}

		@Override
		public String toString() {
			return "A1Param5Structure []";
		}

	}

	public static class A2Param5Structure extends Param5Structure {

		public A2Param5Structure() {

		}

		@Override
		public String toString() {
			return "A2Param5Structure []";
		}

	}
}