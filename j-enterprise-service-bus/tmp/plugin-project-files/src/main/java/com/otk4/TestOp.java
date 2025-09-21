package com.otk4;

public class TestOp implements com.otk.jesb.operation.Operation {
	private final java.lang.String param2;
	private final int param3;
	private final Param4Structure param4;
	private final Param5Structure param5;
	private final Param6Structure param6;
	private final Param7Structure param7;
	private final com.otk.jesb.resource.builtin.JDBCConnection param8;

	public TestOp(java.lang.String param2, int param3, Param4Structure param4, Param5Structure param5,
			Param6Structure param6, Param7Structure param7, com.otk.jesb.resource.builtin.JDBCConnection param8) {
		this.param2 = param2;
		this.param3 = param3;
		this.param4 = param4;
		this.param5 = param5;
		this.param6 = param6;
		this.param7 = param7;
		this.param8 = param8;
	}

	public java.lang.String getParam2() {
		return param2;
	}

	public int getParam3() {
		return param3;
	}

	public Param4Structure getParam4() {
		return param4;
	}

	public Param5Structure getParam5() {
		return param5;
	}

	public Param6Structure getParam6() {
		return param6;
	}

	public Param7Structure getParam7() {
		return param7;
	}

	public com.otk.jesb.resource.builtin.JDBCConnection getParam8() {
		return param8;
	}

	@Override
	public String toString() {
		return "TestOp [param2=" + param2 + ", param3=" + param3 + ", param4=" + param4 + ", param5=" + param5
				+ ", param6=" + param6 + ", param7=" + param7 + ", param8=" + param8 + "]";
	}

	@Override
	public Object execute() throws Throwable {
		return null;
	}

	static public class Builder implements com.otk.jesb.operation.OperationBuilder<TestOp> {
		private java.lang.String param2 = "azerty";
		private com.otk.jesb.Variant<java.lang.Integer> param3Variant = new com.otk.jesb.Variant<java.lang.Integer>(
				java.lang.Integer.class, 123);
		private com.otk.jesb.instantiation.RootInstanceBuilder param4DynamicBuilder = new com.otk.jesb.instantiation.RootInstanceBuilder(
				"param4Input", Param4Structure.class.getName());
		private com.otk.jesb.instantiation.RootInstanceBuilder param5DynamicBuilder = new com.otk.jesb.instantiation.RootInstanceBuilder(
				"param5Input", new Param5InputClassNameAccessor());
		private Param6Structure.GroupBuilder param6GroupBuilder = new Param6Structure.GroupBuilder();
		private com.otk.jesb.Variant<Param7Structure> param7Variant = new com.otk.jesb.Variant<Param7Structure>(
				Param7Structure.class, Param7Structure.ITEM1);
		private com.otk.jesb.Reference<com.otk.jesb.resource.builtin.JDBCConnection> param8Reference = new com.otk.jesb.Reference<com.otk.jesb.resource.builtin.JDBCConnection>(
				com.otk.jesb.resource.builtin.JDBCConnection.class);

		public Builder() {

		}

		public java.lang.String getParam2() {
			return param2;
		}

		public void setParam2(java.lang.String param2) {
			this.param2 = param2;
		}

		public com.otk.jesb.Variant<java.lang.Integer> getParam3Variant() {
			return param3Variant;
		}

		public void setParam3Variant(com.otk.jesb.Variant<java.lang.Integer> param3Variant) {
			this.param3Variant = param3Variant;
		}

		public com.otk.jesb.instantiation.RootInstanceBuilder getParam4DynamicBuilder() {
			return param4DynamicBuilder;
		}

		public void setParam4DynamicBuilder(com.otk.jesb.instantiation.RootInstanceBuilder param4DynamicBuilder) {
			this.param4DynamicBuilder = param4DynamicBuilder;
		}

		public com.otk.jesb.instantiation.RootInstanceBuilder getParam5DynamicBuilder() {
			return param5DynamicBuilder;
		}

		public void setParam5DynamicBuilder(com.otk.jesb.instantiation.RootInstanceBuilder param5DynamicBuilder) {
			this.param5DynamicBuilder = param5DynamicBuilder;
		}

		public Param6Structure.GroupBuilder getParam6GroupBuilder() {
			return param6GroupBuilder;
		}

		public void setParam6GroupBuilder(Param6Structure.GroupBuilder param6GroupBuilder) {
			this.param6GroupBuilder = param6GroupBuilder;
		}

		public com.otk.jesb.Variant<Param7Structure> getParam7Variant() {
			return param7Variant;
		}

		public void setParam7Variant(com.otk.jesb.Variant<Param7Structure> param7Variant) {
			this.param7Variant = param7Variant;
		}

		public com.otk.jesb.Reference<com.otk.jesb.resource.builtin.JDBCConnection> getParam8Reference() {
			return param8Reference;
		}

		public void setParam8Reference(
				com.otk.jesb.Reference<com.otk.jesb.resource.builtin.JDBCConnection> param8Reference) {
			this.param8Reference = param8Reference;
		}

		@Override
		public String toString() {
			return "Builder [param2=" + param2 + ", param3Variant=" + param3Variant + ", param4DynamicBuilder="
					+ param4DynamicBuilder + ", param5DynamicBuilder=" + param5DynamicBuilder + ", param6GroupBuilder="
					+ param6GroupBuilder + ", param7Variant=" + param7Variant + ", param8Reference=" + param8Reference
					+ "]";
		}

		@Override
		public TestOp build(com.otk.jesb.solution.Plan.ExecutionContext context,
				com.otk.jesb.solution.Plan.ExecutionInspector executionInspector) throws Exception {
			java.lang.String param2 = this.param2;
			int param3 = this.param3Variant.getValue();
			Param4Structure param4 = (Param4Structure) this.param4DynamicBuilder
					.build(new com.otk.jesb.instantiation.InstantiationContext(context.getVariables(), context.getPlan()
							.getValidationContext(context.getCurrentStep()).getVariableDeclarations()));
			Param5Structure param5 = (Param5Structure) this.param5DynamicBuilder
					.build(new com.otk.jesb.instantiation.InstantiationContext(context.getVariables(), context.getPlan()
							.getValidationContext(context.getCurrentStep()).getVariableDeclarations()));
			Param6Structure param6 = this.param6GroupBuilder.build(context, executionInspector);
			Param7Structure param7 = this.param7Variant.getValue();
			com.otk.jesb.resource.builtin.JDBCConnection param8 = this.param8Reference.resolve();
			return new TestOp(param2, param3, param4, param5, param6, param7, param8);
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

		public static void customizeUI(xy.reflect.ui.info.custom.InfoCustomizations infoCustomizations) {
			/* com.otk4.TestOp.Builder form customization */
			{
				/* field control positions */
				xy.reflect.ui.info.custom.InfoCustomizations
						.getTypeCustomization(infoCustomizations, com.otk4.TestOp.Builder.class.getName())
						.setCustomFieldsOrder(java.util.Arrays.asList("param2", "param3", "param4", "param5", "param6",
								"param7", "param8"));
				/* param2 control customization */
				{
					xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
							com.otk4.TestOp.Builder.class.getName(), "param2").setCustomFieldCaption("Param 2");
					xy.reflect.ui.info.custom.InfoCustomizations
							.getTypeCustomization(
									xy.reflect.ui.info.custom.InfoCustomizations
											.getFieldCustomization(infoCustomizations,
													com.otk4.TestOp.Builder.class.getName(), "param2")
											.getSpecificTypeCustomizations(),
									java.lang.String.class.getName())
							.setSpecificProperties(new java.util.HashMap<String, Object>() {
								private static final long serialVersionUID = 1L;
								{
									xy.reflect.ui.util.ReflectionUIUtils.setFieldControlPluginIdentifier(this,
											"xy.reflect.ui.control.swing.plugin.StyledTextPlugin");
									xy.reflect.ui.util.ReflectionUIUtils.setFieldControlPluginConfiguration(this,
											"xy.reflect.ui.control.swing.plugin.StyledTextPlugin",
											(java.io.Serializable) com.otk.jesb.PluginBuilder
													.readControlPluginConfiguration(
															"<xy.reflect.ui.control.swing.plugin.StyledTextPlugin_-StyledTextConfiguration><fontName>Serif</fontName><fontBold>true</fontBold><fontItalic>true</fontItalic><fontSize>20</fontSize><horizontalAlignment>LEFT</horizontalAlignment><underlined>false</underlined><struckThrough>false</struckThrough><width><value>400</value><unit>PIXELS</unit></width></xy.reflect.ui.control.swing.plugin.StyledTextPlugin_-StyledTextConfiguration>"));
								}
							});
				}
				/* param3 control customization */
				{
					xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
							com.otk4.TestOp.Builder.class.getName(), "param3").setCustomFieldCaption("Param 3");
					xy.reflect.ui.info.custom.InfoCustomizations
							.getTypeCustomization(xy.reflect.ui.info.custom.InfoCustomizations
									.getFieldCustomization(infoCustomizations,
											com.otk.jesb.ui.JESBReflectionUI.VariantCustomizations.getAdapterTypeName(
													com.otk4.TestOp.Builder.class.getName(), "param3Variant"),
											com.otk.jesb.ui.JESBReflectionUI.VariantCustomizations
													.getConstantValueFieldName("param3Variant"))
									.getSpecificTypeCustomizations(), java.lang.Integer.class.getName())
							.setSpecificProperties(new java.util.HashMap<String, Object>() {
								private static final long serialVersionUID = 1L;
								{
									xy.reflect.ui.util.ReflectionUIUtils.setFieldControlPluginIdentifier(this,
											"xy.reflect.ui.control.swing.plugin.SliderPlugin");
									xy.reflect.ui.util.ReflectionUIUtils.setFieldControlPluginConfiguration(this,
											"xy.reflect.ui.control.swing.plugin.SliderPlugin",
											(java.io.Serializable) com.otk.jesb.PluginBuilder
													.readControlPluginConfiguration(
															"<xy.reflect.ui.control.swing.plugin.SliderPlugin_-SliderConfiguration><maximum>100</maximum><minimum>0</minimum><paintTicks>true</paintTicks><paintLabels>true</paintLabels><minorTickSpacing>1</minorTickSpacing><majorTickSpacing>10</majorTickSpacing></xy.reflect.ui.control.swing.plugin.SliderPlugin_-SliderConfiguration>"));
								}
							});
				}
				/* param4 control customization */
				{
					xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
							com.otk4.TestOp.Builder.class.getName(), "param4").setCustomFieldCaption("Param 4");
					xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
							com.otk4.TestOp.Builder.class.getName(), "param4DynamicBuilder")
							.setFormControlEmbeddingForced(true);
				}
				/* param5 control customization */
				{
					xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
							com.otk4.TestOp.Builder.class.getName(), "param5").setCustomFieldCaption("Param 5");
					xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
							com.otk4.TestOp.Builder.class.getName(), "param5DynamicBuilder")
							.setFormControlEmbeddingForced(true);
				}
				/* param6 control customization */
				{
					xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
							com.otk4.TestOp.Builder.class.getName(), "param6").setCustomFieldCaption("Param 6");
					xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
							com.otk4.TestOp.Builder.class.getName(), "param6GroupBuilder")
							.setNullValueDistinctForced(true);
					xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
							com.otk4.TestOp.Builder.class.getName(), "param6GroupBuilder")
							.setFormControlEmbeddingForced(true);
					Param6Structure.GroupBuilder.customizeUI(infoCustomizations);
				}
				/* param7 control customization */
				{
					xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
							com.otk4.TestOp.Builder.class.getName(), "param7").setCustomFieldCaption("Param 7");
				}
				/* param8 control customization */
				{
					xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
							com.otk4.TestOp.Builder.class.getName(), "param8").setCustomFieldCaption("Param 8");
					xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
							com.otk4.TestOp.Builder.class.getName(), "param8Reference")
							.setFormControlEmbeddingForced(true);
				}
				/* hide UI customization method */
				xy.reflect.ui.info.custom.InfoCustomizations
						.getMethodCustomization(infoCustomizations, com.otk4.TestOp.Builder.class.getName(),
								xy.reflect.ui.util.ReflectionUIUtils.buildMethodSignature("void", "customizeUI",
										java.util.Arrays
												.asList(xy.reflect.ui.info.custom.InfoCustomizations.class.getName())))
						.setHidden(true);
			}
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

	public static class Metadata implements com.otk.jesb.operation.OperationMetadata<TestOp> {
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
		private final java.lang.Byte subParam1;
		private final com.otk.jesb.meta.Date subParam2;
		private final Param6StructureSubParam3Structure subParam3;
		private final Param6StructureSubParam4Structure subParam4;

		public Param6Structure(java.lang.Byte subParam1, com.otk.jesb.meta.Date subParam2,
				Param6StructureSubParam3Structure subParam3, Param6StructureSubParam4Structure subParam4) {
			this.subParam1 = subParam1;
			this.subParam2 = subParam2;
			this.subParam3 = subParam3;
			this.subParam4 = subParam4;
		}

		public java.lang.Byte getSubParam1() {
			return subParam1;
		}

		public com.otk.jesb.meta.Date getSubParam2() {
			return subParam2;
		}

		public Param6StructureSubParam3Structure getSubParam3() {
			return subParam3;
		}

		public Param6StructureSubParam4Structure getSubParam4() {
			return subParam4;
		}

		@Override
		public String toString() {
			return "Param6Structure [subParam1=" + subParam1 + ", subParam2=" + subParam2 + ", subParam3=" + subParam3
					+ ", subParam4=" + subParam4 + "]";
		}

		static public class GroupBuilder implements com.otk.jesb.operation.ParameterBuilder<Param6Structure> {
			private java.lang.Byte subParam1;
			private com.otk.jesb.Variant<com.otk.jesb.meta.Date> subParam2Variant = new com.otk.jesb.Variant<com.otk.jesb.meta.Date>(
					com.otk.jesb.meta.Date.class);
			private com.otk.jesb.instantiation.RootInstanceBuilder subParam3DynamicBuilder = new com.otk.jesb.instantiation.RootInstanceBuilder(
					"subParam3Input", Param6StructureSubParam3Structure.class.getName());
			private Param6StructureSubParam4Structure.GroupBuilder subParam4GroupBuilder = new Param6StructureSubParam4Structure.GroupBuilder();

			public GroupBuilder() {

			}

			public java.lang.Byte getSubParam1() {
				return subParam1;
			}

			public void setSubParam1(java.lang.Byte subParam1) {
				this.subParam1 = subParam1;
			}

			public com.otk.jesb.Variant<com.otk.jesb.meta.Date> getSubParam2Variant() {
				return subParam2Variant;
			}

			public void setSubParam2Variant(com.otk.jesb.Variant<com.otk.jesb.meta.Date> subParam2Variant) {
				this.subParam2Variant = subParam2Variant;
			}

			public com.otk.jesb.instantiation.RootInstanceBuilder getSubParam3DynamicBuilder() {
				return subParam3DynamicBuilder;
			}

			public void setSubParam3DynamicBuilder(
					com.otk.jesb.instantiation.RootInstanceBuilder subParam3DynamicBuilder) {
				this.subParam3DynamicBuilder = subParam3DynamicBuilder;
			}

			public Param6StructureSubParam4Structure.GroupBuilder getSubParam4GroupBuilder() {
				return subParam4GroupBuilder;
			}

			public void setSubParam4GroupBuilder(Param6StructureSubParam4Structure.GroupBuilder subParam4GroupBuilder) {
				this.subParam4GroupBuilder = subParam4GroupBuilder;
			}

			@Override
			public String toString() {
				return "GroupBuilder [subParam1=" + subParam1 + ", subParam2Variant=" + subParam2Variant
						+ ", subParam3DynamicBuilder=" + subParam3DynamicBuilder + ", subParam4GroupBuilder="
						+ subParam4GroupBuilder + "]";
			}

			@Override
			public Param6Structure build(com.otk.jesb.solution.Plan.ExecutionContext context,
					com.otk.jesb.solution.Plan.ExecutionInspector executionInspector) throws Exception {
				java.lang.Byte subParam1 = this.subParam1;
				com.otk.jesb.meta.Date subParam2 = this.subParam2Variant.getValue();
				Param6StructureSubParam3Structure subParam3 = (Param6StructureSubParam3Structure) this.subParam3DynamicBuilder
						.build(new com.otk.jesb.instantiation.InstantiationContext(context.getVariables(), context
								.getPlan().getValidationContext(context.getCurrentStep()).getVariableDeclarations()));
				Param6StructureSubParam4Structure subParam4 = this.subParam4GroupBuilder.build(context,
						executionInspector);
				return new Param6Structure(subParam1, subParam2, subParam3, subParam4);
			}

			@Override
			public void validate(boolean recursively, com.otk.jesb.solution.Plan currentPlan,
					com.otk.jesb.solution.Step currentStep) {

			}

			public static void customizeUI(xy.reflect.ui.info.custom.InfoCustomizations infoCustomizations) {
				/* Param6Structure.GroupBuilder form customization */
				{
					/* field control positions */
					xy.reflect.ui.info.custom.InfoCustomizations
							.getTypeCustomization(infoCustomizations, Param6Structure.GroupBuilder.class.getName())
							.setCustomFieldsOrder(
									java.util.Arrays.asList("subParam1", "subParam2", "subParam3", "subParam4"));
					/* subParam1 control customization */
					{
						xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
								Param6Structure.GroupBuilder.class.getName(), "subParam1")
								.setCustomFieldCaption("Sub Param 1");
					}
					/* subParam2 control customization */
					{
						xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
								Param6Structure.GroupBuilder.class.getName(), "subParam2")
								.setCustomFieldCaption("Sub Param 2");
					}
					/* subParam3 control customization */
					{
						xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
								Param6Structure.GroupBuilder.class.getName(), "subParam3")
								.setCustomFieldCaption("Sub Param 3");
						xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
								Param6Structure.GroupBuilder.class.getName(), "subParam3DynamicBuilder")
								.setFormControlEmbeddingForced(true);
					}
					/* subParam4 control customization */
					{
						xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
								Param6Structure.GroupBuilder.class.getName(), "subParam4")
								.setCustomFieldCaption("Sub Param 4");
						xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
								Param6Structure.GroupBuilder.class.getName(), "subParam4GroupBuilder")
								.setFormControlEmbeddingForced(true);
						Param6StructureSubParam4Structure.GroupBuilder.customizeUI(infoCustomizations);
					}
					/* hide UI customization method */
					xy.reflect.ui.info.custom.InfoCustomizations
							.getMethodCustomization(infoCustomizations, Param6Structure.GroupBuilder.class.getName(),
									xy.reflect.ui.util.ReflectionUIUtils.buildMethodSignature("void", "customizeUI",
											java.util.Arrays.asList(
													xy.reflect.ui.info.custom.InfoCustomizations.class.getName())))
							.setHidden(true);
				}
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
			private final java.lang.String subSubParam1;
			private final java.lang.String subSubParam2;

			public Param6StructureSubParam4Structure(java.lang.String subSubParam1, java.lang.String subSubParam2) {
				this.subSubParam1 = subSubParam1;
				this.subSubParam2 = subSubParam2;
			}

			public java.lang.String getSubSubParam1() {
				return subSubParam1;
			}

			public java.lang.String getSubSubParam2() {
				return subSubParam2;
			}

			@Override
			public String toString() {
				return "Param6StructureSubParam4Structure [subSubParam1=" + subSubParam1 + ", subSubParam2="
						+ subSubParam2 + "]";
			}

			static public class GroupBuilder
					implements com.otk.jesb.operation.ParameterBuilder<Param6StructureSubParam4Structure> {
				private java.lang.String subSubParam1;
				private java.lang.String subSubParam2;

				public GroupBuilder() {

				}

				public java.lang.String getSubSubParam1() {
					return subSubParam1;
				}

				public void setSubSubParam1(java.lang.String subSubParam1) {
					this.subSubParam1 = subSubParam1;
				}

				public java.lang.String getSubSubParam2() {
					return subSubParam2;
				}

				public void setSubSubParam2(java.lang.String subSubParam2) {
					this.subSubParam2 = subSubParam2;
				}

				@Override
				public String toString() {
					return "GroupBuilder [subSubParam1=" + subSubParam1 + ", subSubParam2=" + subSubParam2 + "]";
				}

				@Override
				public Param6StructureSubParam4Structure build(com.otk.jesb.solution.Plan.ExecutionContext context,
						com.otk.jesb.solution.Plan.ExecutionInspector executionInspector) throws Exception {
					java.lang.String subSubParam1 = this.subSubParam1;
					java.lang.String subSubParam2 = this.subSubParam2;
					return new Param6StructureSubParam4Structure(subSubParam1, subSubParam2);
				}

				@Override
				public void validate(boolean recursively, com.otk.jesb.solution.Plan currentPlan,
						com.otk.jesb.solution.Step currentStep) {

				}

				public static void customizeUI(xy.reflect.ui.info.custom.InfoCustomizations infoCustomizations) {
					/* Param6StructureSubParam4Structure.GroupBuilder form customization */
					{
						/* field control positions */
						xy.reflect.ui.info.custom.InfoCustomizations
								.getTypeCustomization(infoCustomizations,
										Param6StructureSubParam4Structure.GroupBuilder.class.getName())
								.setCustomFieldsOrder(java.util.Arrays.asList("subSubParam1", "subSubParam2"));
						/* subSubParam1 control customization */
						{
							xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
									Param6StructureSubParam4Structure.GroupBuilder.class.getName(), "subSubParam1")
									.setCustomFieldCaption("Sub Sub Param 1");
						}
						/* subSubParam2 control customization */
						{
							xy.reflect.ui.info.custom.InfoCustomizations.getFieldCustomization(infoCustomizations,
									Param6StructureSubParam4Structure.GroupBuilder.class.getName(), "subSubParam2")
									.setCustomFieldCaption("Sub Sub Param 2");
						}
						/* hide UI customization method */
						xy.reflect.ui.info.custom.InfoCustomizations
								.getMethodCustomization(infoCustomizations,
										Param6StructureSubParam4Structure.GroupBuilder.class.getName(),
										xy.reflect.ui.util.ReflectionUIUtils.buildMethodSignature("void", "customizeUI",
												java.util.Arrays.asList(
														xy.reflect.ui.info.custom.InfoCustomizations.class.getName())))
								.setHidden(true);
					}
				}

			}

		}
	}

	static public enum Param7Structure {
		ITEM1, ITEM2;
	}
}