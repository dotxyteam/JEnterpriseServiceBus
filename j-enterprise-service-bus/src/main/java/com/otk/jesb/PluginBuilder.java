package com.otk.jesb;

import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import com.otk.jesb.Structure.Element;
import com.otk.jesb.Structure.SimpleElement;
import com.otk.jesb.Structure.StructuredElement;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.solution.Step;
import com.otk.jesb.ui.GUI;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.util.ClassUtils;

public class PluginBuilder {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				GUI.INSTANCE.openObjectDialog(null, new PluginBuilder());
			}
		});
	}

	private String packageName;
	private List<OperationDescriptor> operations = new ArrayList<OperationDescriptor>();

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public List<OperationDescriptor> getOperations() {
		return operations;
	}

	public void setOperations(List<OperationDescriptor> operations) {
		this.operations = operations;
	}

	public void save(File file) throws IOException {
		try (FileOutputStream output = new FileOutputStream(file)) {
			MiscUtils.serialize(this, output);
		}
	}

	public static PluginBuilder load(File file) throws IOException {
		try (FileInputStream input = new FileInputStream(file)) {
			return (PluginBuilder) MiscUtils.deserialize(input);
		}
	}

	public class OperationDescriptor {

		private String opertionTypeName;
		private String opertionTypeCaption;
		private String categoryName;
		private byte[] operationIconImageData;
		private List<ParameterDescriptor> parameters = new ArrayList<ParameterDescriptor>();
		private ResultDescriptor result;
		private String executionMethodBody;

		public String getOpertionTypeName() {
			return opertionTypeName;
		}

		public void setOpertionTypeName(String opertionTypeName) {
			this.opertionTypeName = opertionTypeName;
		}

		public String getOpertionTypeCaption() {
			return opertionTypeCaption;
		}

		public void setOpertionTypeCaption(String opertionTypeCaption) {
			this.opertionTypeCaption = opertionTypeCaption;
		}

		public String getCategoryName() {
			return categoryName;
		}

		public void setCategoryName(String categoryName) {
			this.categoryName = categoryName;
		}

		public List<ParameterDescriptor> getParameters() {
			return parameters;
		}

		public void setParameters(List<ParameterDescriptor> parameters) {
			this.parameters = parameters;
		}

		public ResultDescriptor getResult() {
			return result;
		}

		public void setResult(ResultDescriptor result) {
			this.result = result;
		}

		public String getExecutionMethodBody() {
			return executionMethodBody;
		}

		public void setExecutionMethodBody(String executionMethodBody) {
			this.executionMethodBody = executionMethodBody;
		}

		public void loadIconImage(File file) throws IOException {
			operationIconImageData = MiscUtils.readBinary(file);
		}

		public Image getIconImage() {
			if (operationIconImageData == null) {
				return null;
			}
			try {
				return ImageIO.read(new ByteArrayInputStream(operationIconImageData));
			} catch (IOException e) {
				throw new UnexpectedError(e);
			}
		}

		public File generateJavaSourceCode(File sourceDirectroy) {
			String operationClassName = packageName + "." + opertionTypeName;
			String implemented = Operation.class.getName();
			Structure.ClassicStructure operationStructure = new Structure.ClassicStructure();
			Map<Object, Object> codeGenerationOptions = Collections.singletonMap(Structure.ElementAccessMode.class,
					Structure.ElementAccessMode.PUBLIC_FIELD);
			for (ParameterDescriptor parameter : parameters) {
				operationStructure.getElements().add(parameter.createOperationElement(operationClassName));
			}
			StringBuilder additionalDeclarations = new StringBuilder();
			additionalDeclarations
					.append(generateExecutionMethodSourceCode(operationClassName, codeGenerationOptions) + "\n");
			additionalDeclarations
					.append(generateBuilderClassSourceCode(operationClassName, codeGenerationOptions) + "\n");
			additionalDeclarations
					.append(generateMetadataClassSourceCode(operationClassName, codeGenerationOptions) + "\n");
			if (result != null) {
				additionalDeclarations.append(
						result.generateResultClassesSourceCode(operationClassName, codeGenerationOptions) + "\n");
			}
			File javaFile = new File(sourceDirectroy, operationClassName.replace(".", "/") + ".java");
			try {
				if (!javaFile.getParentFile().exists()) {
					MiscUtils.createDirectory(javaFile.getParentFile(), true);
				}
				MiscUtils.write(javaFile, operationStructure.generateJavaTypeSourceCode(operationClassName, implemented,
						null, additionalDeclarations.toString(), codeGenerationOptions), false);
			} catch (IOException e) {
				throw new UnexpectedError(e);
			}
			return javaFile;
		}

		private Object generateExecutionMethodSourceCode(String className, Map<Object, Object> codeGenerationOptions) {
			StringBuilder result = new StringBuilder();
			result.append("@Override\n");
			result.append("public Object execute() throws Throwable {\n");
			result.append(executionMethodBody + "\n");
			result.append("}");
			return result.toString();
		}

		private String generateBuilderClassSourceCode(String operationClassName, Map<Object, Object> options) {
			String operationClassSimpleName = MiscUtils.extractSimpleNameFromClassName(operationClassName);
			String implemented = OperationBuilder.class.getName() + "<" + operationClassSimpleName + ">";
			Structure.ClassicStructure operationBuilderStructure = new Structure.ClassicStructure();
			for (ParameterDescriptor parameter : parameters) {
				operationBuilderStructure.getElements()
						.add(parameter.createOperationBuilderElement(operationClassName));
			}
			StringBuilder additionalDeclarations = new StringBuilder();
			{
				additionalDeclarations.append("@Override\n");
				additionalDeclarations.append("public " + operationClassSimpleName + " build("
						+ MiscUtils.adaptClassNameToSourceCode(ExecutionContext.class.getName()) + " context, "
						+ MiscUtils.adaptClassNameToSourceCode(ExecutionInspector.class.getName())
						+ " executionInspector) throws Exception {\n");
				additionalDeclarations.append(generateBuildMethodBody(operationClassName, options) + "\n");
				additionalDeclarations.append("}\n");
			}
			{
				additionalDeclarations.append("@Override\n");
				additionalDeclarations.append("public Class<?> getOperationResultClass(" + Plan.class.getName()
						+ " currentPlan, " + Step.class.getName() + " currentStep) {\n");
				additionalDeclarations.append(generateResultClassMethodBody(operationClassName, options) + "\n");
				additionalDeclarations.append("}\n");
			}
			{
				additionalDeclarations.append("@Override\n");
				additionalDeclarations.append("public void validate(boolean recursively, " + Plan.class.getName()
						+ " currentPlan, " + Step.class.getName() + " currentStep) {\n");
				additionalDeclarations.append(generateValidationMethodBody(operationClassName, options) + "\n");
				additionalDeclarations.append("}\n");
			}
			return operationBuilderStructure.generateJavaTypeSourceCode("Builder", implemented, null,
					additionalDeclarations.toString(), options);
		}

		private String generateBuildMethodBody(String operationClassName, Map<Object, Object> options) {
			StringBuilder result = new StringBuilder();
			for (ParameterDescriptor parameter : parameters) {
				String buildStatementTarget = parameter.createOperationElement(operationClassName)
						.getFinalTypeNameAdaptedToSourceCode(operationClassName) + " " + parameter.getName();
				result.append(
						parameter.generateBuildStatement(buildStatementTarget, operationClassName, options) + "\n");
			}
			result.append("return new " + opertionTypeName + "("
					+ parameters.stream().map(ParameterDescriptor::getName).collect(Collectors.joining(", ")) + ");");
			return result.toString();
		}

		private String generateMetadataClassSourceCode(String operationClassName, Map<Object, Object> options) {
			String className = "Metadata";
			String operationClassSimpleName = MiscUtils.extractSimpleNameFromClassName(operationClassName);
			String implemented = OperationMetadata.class.getName() + "<" + operationClassSimpleName + ">";
			StringBuilder result = new StringBuilder();
			result.append("public class " + className + " implements " + implemented + "{" + "\n");
			{
				result.append("@Override\n");
				result.append("public String getOperationTypeName() {\n");
				result.append("return \"" + opertionTypeCaption + "\";" + "\n");
				result.append("}\n");
			}
			{
				result.append("@Override\n");
				result.append("public String getCategoryName() {\n");
				result.append("return \"" + categoryName + "\";" + "\n");
				result.append("}\n");
			}
			{
				result.append("@Override\n");
				result.append("public Class<? extends " + OperationBuilder.class.getName() + "<"
						+ operationClassSimpleName + ">> getOperationBuilderClass() {\n");
				result.append("return Builder.class;" + "\n");
				result.append("}\n");
			}
			{
				result.append("@Override\n");
				result.append("public " + ResourcePath.class.getName() + " getOperationIconImagePath() {\n");
				result.append("return new " + ResourcePath.class.getName() + "(" + ResourcePath.class.getName()
						+ ".specifyClassPathResourceLocation(" + operationClassSimpleName
						+ ".class.getName().replace(\".\", \"/\") + \".png\"));" + "\n");
				result.append("}\n");
			}
			result.append("}");
			return result.toString();
		}

		private String generateResultClassMethodBody(String operationClassName, Map<Object, Object> options) {
			if (result == null) {
				return "return null;";
			}
			return result.generateResultClassMethodBody(operationClassName, options);
		}

		private String generateValidationMethodBody(String operationClassName, Map<Object, Object> options) {
			return "";
		}

		public class ParameterDescriptor {

			private String name;
			private String caption;
			private Nature nature = new SimpleNature();

			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}

			public String getCaption() {
				return caption;
			}

			public void setCaption(String caption) {
				this.caption = caption;
			}

			public Nature getNature() {
				return nature;
			}

			public void setNature(Nature nature) {
				this.nature = nature;
			}

			public Element createOperationElement(String operationClassName) {
				Element result = nature.createOperationElement(operationClassName, name);
				return result;
			}

			public Element createOperationBuilderElement(String operationClassName) {
				Element result = nature.createOperationBuilderElement(operationClassName, name);
				result = new Structure.ElementProxy(result) {

					@Override
					protected String generateRequiredInnerJavaTypesSourceCode(String operationBuilderClassName,
							Map<Object, Object> options) {
						return nature.generateBuilderRequiredInnerJavaTypesSourceCode(operationClassName, name,
								options);
					}

				};
				if (result.getOptionality() == null) {
					result = new Structure.ElementProxy(result) {

						String GHOST_DEFAULT_VALUE_EXPRESSION_TO_ENABLE_SETTER = "<"
								+ MiscUtils.getDigitalUniqueIdentifier() + ">";
						{
							Structure.Optionality optionality = new Structure.Optionality();
							optionality.setDefaultValueExpression(GHOST_DEFAULT_VALUE_EXPRESSION_TO_ENABLE_SETTER);
							setOptionality(optionality);
						}

						@Override
						protected String generateJavaFieldDeclaration(String operationBuilderClassName,
								Map<Object, Object> options) {
							return super.generateJavaFieldDeclaration(operationClassName, options)
									.replaceAll("\\s*=\\s*" + GHOST_DEFAULT_VALUE_EXPRESSION_TO_ENABLE_SETTER, "");
						}

					};
				}
				return result;
			}

			public String generateBuildStatement(String targetVariableName, String operationClassName,
					Map<Object, Object> options) {
				return targetVariableName + " = " + nature.generateBuildExpression(operationClassName, name, options)
						+ ";";
			}

		}

	}

	public static class ResultDescriptor {

		private static final String RESULT_AS_PARAMETER_NAME = "result";
		private Structure structure = new Structure.ClassicStructure();
		private List<StructureDerivationAlternative> concreteStructureAlternatives;

		public Structure getStructure() {
			return structure;
		}

		public void setStructure(Structure structure) {
			this.structure = structure;
		}

		public List<StructureDerivationAlternative> getConcreteStructureAlternatives() {
			return concreteStructureAlternatives;
		}

		public void setConcreteStructureAlternatives(
				List<StructureDerivationAlternative> concreteStructureAlternatives) {
			this.concreteStructureAlternatives = concreteStructureAlternatives;
		}

		public String generateResultClassesSourceCode(String operationClassName, Map<Object, Object> options) {
			return getResultElementUtility(operationClassName)
					.generateRequiredInnerJavaTypesSourceCode(operationClassName, options);
		}

		protected String generateResultClassMethodBody(String operationClassName, Map<Object, Object> options) {
			if (concreteStructureAlternatives != null) {
				return getResultNatureUtility(operationClassName)
						.generateConcreteTypeNameGetterBody(operationClassName, RESULT_AS_PARAMETER_NAME, options)
						.replace("class.getName()", "class");
			} else {
				return "return " + getResultElementUtility(operationClassName)
						.getFinalTypeNameAdaptedToSourceCode(operationClassName) + ".class;";
			}
		}

		private Structure.Element getResultElementUtility(String operationClassName) {
			return getResultNatureUtility(operationClassName).createOperationElement(operationClassName,
					RESULT_AS_PARAMETER_NAME);
		}

		private DynamicNature getResultNatureUtility(String operationClassName) {
			return new DynamicNature() {
				{
					setStructure(ResultDescriptor.this.structure);
					setConcreteStructureAlternatives(ResultDescriptor.this.concreteStructureAlternatives);
				}
			};
		}

	}

	public static abstract class Nature {

		protected abstract Element createOperationElement(String operationClassName, String parameterName);

		protected abstract Element createOperationBuilderElement(String operationClassName, String parameterName);

		protected abstract String generateBuilderRequiredInnerJavaTypesSourceCode(String operationClassName,
				String parameterName, Map<Object, Object> options);

		protected abstract String generateBuildExpression(String operationClassName, String parameterName,
				Map<Object, Object> options);

	}

	public static class SimpleNature extends Nature {

		private SimpleElement internalElement = new SimpleElement();
		private String defaultValueExpression;
		private boolean variant = false;

		public String getTypeNameOrAlias() {
			return internalElement.getTypeNameOrAlias();
		}

		public void setTypeNameOrAlias(String typeNameOrAlias) {
			internalElement.setTypeNameOrAlias(typeNameOrAlias);
		}

		public List<String> getTypeNameOrAliasOptions() {
			return internalElement.getTypeNameOrAliasOptions();
		}

		public String getDefaultValueExpression() {
			return defaultValueExpression;
		}

		public void setDefaultValueExpression(String defaultValueExpression) {
			this.defaultValueExpression = defaultValueExpression;
		}

		public boolean isVariant() {
			return variant;
		}

		public void setVariant(boolean variant) {
			this.variant = variant;
		}

		@Override
		public Element createOperationElement(String operationClassName, String parameterName) {
			Structure.SimpleElement result = new Structure.SimpleElement();
			result.setName(parameterName);
			result.setTypeNameOrAlias(getTypeNameOrAlias());
			return result;
		}

		@Override
		protected Element createOperationBuilderElement(String operationClassName, String parameterName) {
			if (variant) {
				return new Structure.SimpleElement() {
					Element base = createOperationElement(operationClassName, parameterName);
					{
						setName(base.getName() + "Variant");
						Class<?> simpleClass = MiscUtils.getJESBClass(base.getTypeName(operationClassName));
						if (simpleClass.isPrimitive()) {
							simpleClass = ClassUtils.primitiveToWrapperClass(simpleClass);
						}
						setTypeNameOrAlias(Variant.class.getName() + "<" + simpleClass.getName() + ">");
						Structure.Optionality optionality = new Structure.Optionality();
						{
							optionality.setDefaultValueExpression("new " + getTypeNameOrAlias() + "("
									+ simpleClass.getName() + ".class"
									+ ((defaultValueExpression != null) ? (", " + defaultValueExpression) : "") + ")");
							setOptionality(optionality);
						}
					}

					@Override
					protected String generateRequiredInnerJavaTypesSourceCode(String operationClassName,
							Map<Object, Object> options) {
						return base.generateRequiredInnerJavaTypesSourceCode(operationClassName, options);
					}
				};
			} else {
				Element result = createOperationElement(operationClassName, parameterName);
				if (defaultValueExpression != null) {
					Structure.Optionality optionality = new Structure.Optionality();
					optionality.setDefaultValueExpression(defaultValueExpression);
					result.setOptionality(optionality);
				}
				return result;
			}
		}

		@Override
		protected String generateBuildExpression(String operationClassName, String parameterName,
				Map<Object, Object> options) {
			Element operationBuilderElement = createOperationBuilderElement(operationClassName, parameterName);
			if (variant) {
				return "this." + operationBuilderElement.getName() + ".getValue()";
			} else {
				return "this." + operationBuilderElement.getName();
			}
		}

		@Override
		protected String generateBuilderRequiredInnerJavaTypesSourceCode(String operationClassName,
				String parameterName, Map<Object, Object> options) {
			return null;
		}

		public void validate(boolean recursively) throws ValidationError {
			internalElement.validate(recursively);
		}
	}

	public static class DynamicNature extends Nature {

		private Structure structure = new Structure.ClassicStructure();
		private List<StructureDerivationAlternative> concreteStructureAlternatives;

		public Structure getStructure() {
			return structure;
		}

		public void setStructure(Structure structure) {
			this.structure = structure;
		}

		public List<StructureDerivationAlternative> getConcreteStructureAlternatives() {
			return concreteStructureAlternatives;
		}

		public void setConcreteStructureAlternatives(
				List<StructureDerivationAlternative> concreteStructureAlternatives) {
			this.concreteStructureAlternatives = concreteStructureAlternatives;
		}

		@Override
		public Structure.Element createOperationElement(String operationClassName, String parameterName) {
			Structure.StructuredElement result = new Structure.StructuredElement();
			result.setName(parameterName);
			result.setStructure(getStructure());
			if (concreteStructureAlternatives != null) {
				return new Structure.ElementProxy(result) {

					@Override
					protected String generateRequiredInnerJavaTypesSourceCode(String operationClassName,
							Map<Object, Object> options) {
						StringBuilder result = new StringBuilder();
						result.append("abstract "
								+ super.generateRequiredInnerJavaTypesSourceCode(operationClassName, options));
						for (StructureDerivationAlternative alternative : concreteStructureAlternatives) {
							result.append("\n" + alternative.generateRequiredInnerJavaTypesSourceCode(
									operationClassName, getBaseStructureTypeName(operationClassName, parameterName),
									structure, options));
						}
						return result.toString();
					}
				};
			} else {
				return result;
			}
		}

		@Override
		protected Element createOperationBuilderElement(String operationClassName, String parameterName) {
			return new Structure.SimpleElement() {
				Element base = createOperationElement(operationClassName, parameterName);
				{
					setName(base.getName() + "Builder");
					setTypeNameOrAlias(RootInstanceBuilder.class.getName());
					Structure.Optionality optionality = new Structure.Optionality();
					{
						String classNameArgumentExpression;
						if (concreteStructureAlternatives != null) {
							classNameArgumentExpression = "new " + getConcreteTypeNameAccessorClassName(parameterName)
									+ "(){}";
						} else {
							classNameArgumentExpression = base.getFinalTypeNameAdaptedToSourceCode(operationClassName)
									+ ".class.getName()";
						}
						optionality.setDefaultValueExpression("new " + RootInstanceBuilder.class.getName() + "(\""
								+ base.getName() + "Input\", " + classNameArgumentExpression + ")");
						setOptionality(optionality);
					}
				}

				@Override
				protected String generateRequiredInnerJavaTypesSourceCode(String operationClassName,
						Map<Object, Object> options) {
					return base.generateRequiredInnerJavaTypesSourceCode(operationClassName, options);
				}

			};
		}

		private String getBaseStructureTypeName(String operationClassName, String parameterName) {
			return createOperationElement(operationClassName, parameterName).getTypeName(operationClassName);
		}

		private String getConcreteTypeNameAccessorClassName(String parameterName) {
			return parameterName.substring(0, 1).toUpperCase() + parameterName.substring(1) + "InputClassNameAccessor";
		}

		private String generateConcreteTypeNameAccessorClassSourceCode(String operationClassName, String parameterName,
				Map<Object, Object> options) {
			StringBuilder result = new StringBuilder();
			result.append("private class " + getConcreteTypeNameAccessorClassName(parameterName) + " extends "
					+ Accessor.class.getName() + "<String> {\n");
			result.append("	@Override\n");
			result.append("	public String get() {\n");
			result.append(generateConcreteTypeNameGetterBody(operationClassName, parameterName, options) + "\n");
			result.append("	}\n");
			result.append("}");
			return result.toString();
		}

		protected String generateConcreteTypeNameGetterBody(String operationClassName, String parameterName,
				Map<Object, Object> options) {
			StringBuilder result = new StringBuilder();
			for (StructureDerivationAlternative alternative : concreteStructureAlternatives) {
				result.append("		if (" + alternative.getCondition() + ") {\n");
				result.append("			return "
						+ alternative.getConcreteStructureTypeName(operationClassName,
								getBaseStructureTypeName(operationClassName, parameterName), structure)
						+ ".class.getName();\n");
				result.append("		}\n");
			}
			result.append("		return null;");
			return result.toString();
		}

		@Override
		protected String generateBuildExpression(String operationClassName, String parameterName,
				Map<Object, Object> options) {
			Element operationElement = createOperationElement(operationClassName, parameterName);
			Element operationBuilderElement = createOperationBuilderElement(operationClassName, parameterName);
			return "(" + operationElement.getFinalTypeNameAdaptedToSourceCode(operationClassName) + ") this."
					+ operationBuilderElement.getName() + ".build(new " + InstantiationContext.class.getName()
					+ "(context.getVariables(), context.getPlan().getValidationContext(context.getCurrentStep()).getVariableDeclarations()))";
		}

		@Override
		protected String generateBuilderRequiredInnerJavaTypesSourceCode(String operationClassName,
				String parameterName, Map<Object, Object> options) {
			if (concreteStructureAlternatives != null) {
				return generateConcreteTypeNameAccessorClassSourceCode(operationClassName, parameterName, options);
			}
			return null;
		}

		public void validate(boolean recursively) throws ValidationError {
			structure.validate(recursively);
		}
	}

	public static class StructureDerivationAlternative {

		private String alternativeName;
		private String condition;
		private Structure.ClassicStructure structureDerivation = new Structure.ClassicStructure();

		public String getAlternativeName() {
			return alternativeName;
		}

		public void setAlternativeName(String alternativeName) {
			this.alternativeName = alternativeName;
		}

		public Structure.ClassicStructure getStructureDerivation() {
			return structureDerivation;
		}

		public void setStructureDerivation(Structure.ClassicStructure structureDerivation) {
			this.structureDerivation = structureDerivation;
		}

		public String getCondition() {
			return condition;
		}

		public void setCondition(String condition) {
			this.condition = condition;
		}

		public String generateRequiredInnerJavaTypesSourceCode(String operationClassName, String baseStructureTypeName,
				Structure baseStructure, Map<Object, Object> options) {
			Structure.StructuredElement utilityElement = getConcreteStructureUtilityElement(baseStructureTypeName,
					baseStructure);
			return utilityElement.generateRequiredInnerJavaTypesSourceCode(operationClassName, options);
		}

		public String getConcreteStructureTypeName(String operationClassName, String baseStructureTypeName,
				Structure baseStructure) {
			Structure.StructuredElement utilityElement = getConcreteStructureUtilityElement(baseStructureTypeName,
					baseStructure);
			return utilityElement.getFinalTypeNameAdaptedToSourceCode(operationClassName);
		}

		private StructuredElement getConcreteStructureUtilityElement(String baseStructureTypeName,
				Structure baseStructure) {
			return new Structure.StructuredElement() {
				{
					setName(alternativeName + baseStructureTypeName.replaceAll("Structure$", ""));
					setStructure(new Structure.DerivedClassicStructure(baseStructureTypeName, baseStructure) {
						{
							getElements().addAll(structureDerivation.getElements());
						}
					});
				}

			};
		}
	}

}
