package com.otk;

import com.otk.jesb.instantiation.InstantiationContext;

public class TestOp implements com.otk.jesb.operation.Operation {
	private final Param1Structure param1;
	private java.lang.String param2 = "bla bla bla";
	private final java.lang.String param3;

	public TestOp(Param1Structure param1, java.lang.String param3) {
		this.param1 = param1;
		this.param3 = param3;
	}

	public Param1Structure getParam1() {
		return param1;
	}

	public java.lang.String getParam2() {
		return param2;
	}

	public void setParam2(java.lang.String param2) {
		this.param2 = param2;
	}

	public java.lang.String getParam3() {
		return param3;
	}

	@Override
	public Object execute() throws Throwable {
		return null;
	}

	public class Builder implements com.otk.jesb.operation.OperationBuilder<TestOp> {
		private com.otk.jesb.instantiation.RootInstanceBuilder param1Builder = new com.otk.jesb.instantiation.RootInstanceBuilder(
				"param1Input", Param1Structure.class.getName());
		private com.otk.jesb.Variant<java.lang.String> param2Variant;
		private java.lang.String param3;

		public Builder() {

		}

		public com.otk.jesb.instantiation.RootInstanceBuilder getParam1Builder() {
			return param1Builder;
		}

		public void setParam1Builder(com.otk.jesb.instantiation.RootInstanceBuilder param1Builder) {
			this.param1Builder = param1Builder;
		}

		public com.otk.jesb.Variant<java.lang.String> getParam2Variant() {
			return param2Variant;
		}

		public void setParam2Variant(com.otk.jesb.Variant<java.lang.String> param2Variant) {
			this.param2Variant = param2Variant;
		}

		public java.lang.String getParam3() {
			return param3;
		}

		public void setParam3(java.lang.String param3) {
			this.param3 = param3;
		}

		@Override
		public TestOp build(com.otk.jesb.solution.Plan.ExecutionContext context,
				com.otk.jesb.solution.Plan.ExecutionInspector executionInspector) throws Exception {
			Param1Structure param1 =  (Param1Structure) this.param1Builder.build(new InstantiationContext(context.getVariables(),
					context.getPlan().getValidationContext(context.getCurrentStep()).getVariableDeclarations()));
			String param2 = this.param2Variant.getValue();
			String param3 = this.param3;
			TestOp result = new TestOp(param1, param3);
			result.setParam2(param2);
			return result;
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

		@Override
		public String toString() {
			return "Builder [param1Builder=" + param1Builder + ", param2Variant=" + param2Variant + ", param3=" + param3
					+ "]";
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

	@Override
	public String toString() {
		return "TestOp [param1=" + param1 + ", param2=" + param2 + ", param3=" + param3 + "]";
	}

	public static class Param1Structure {
		private final java.lang.String element;
		private final Param1StructureElement2Structure element2;

		public Param1Structure(java.lang.String element, Param1StructureElement2Structure element2) {
			this.element = element;
			this.element2 = element2;
		}

		public java.lang.String getElement() {
			return element;
		}

		public Param1StructureElement2Structure getElement2() {
			return element2;
		}

		@Override
		public String toString() {
			return "Param1Structure [element=" + element + ", element2=" + element2 + "]";
		}

		public enum Param1StructureElement2Structure {
			ITEM1, ITEM2;
		}
	}
}