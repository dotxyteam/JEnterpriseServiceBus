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
			String className = packageName + "." + opertionTypeName;
			String implemented = Operation.class.getName();
			Structure.ClassicStructure operationStructure = new Structure.ClassicStructure();
			Map<Object, Object> codeGenerationOptions = Collections.singletonMap(Structure.ElementAccessMode.class,
					Structure.ElementAccessMode.PUBLIC_FIELD);
			for (ParameterDescriptor parameter : parameters) {
				operationStructure.getElements().add(parameter.createOperationElement(className));
			}
			StringBuilder additionalDeclarations = new StringBuilder();
			{
				additionalDeclarations.append("@Override\n");
				additionalDeclarations.append("public Object execute() throws Throwable {\n");
				additionalDeclarations.append(executionMethodBody + "\n");
				additionalDeclarations.append("}\n");
			}
			additionalDeclarations.append(generateBuilderClassSourceCode(className, codeGenerationOptions));
			additionalDeclarations.append(generateMetadataClassSourceCode(className, codeGenerationOptions));
			File javaFile = new File(sourceDirectroy, className.replace(".", "/") + ".java");
			try {
				if (!javaFile.getParentFile().exists()) {
					MiscUtils.createDirectory(javaFile.getParentFile(), true);
				}
				MiscUtils.write(javaFile, operationStructure.generateJavaTypeSourceCode(className, implemented, null,
						additionalDeclarations.toString(), codeGenerationOptions), false);
			} catch (IOException e) {
				throw new UnexpectedError(e);
			}
			return javaFile;
		}

		private String generateBuilderClassSourceCode(String parentClassName, Map<Object, Object> options) {
			String className = "Builder";
			String operationClassSimpleName = MiscUtils.extractSimpleNameFromClassName(parentClassName);
			String implemented = OperationBuilder.class.getName() + "<" + operationClassSimpleName + ">";
			Structure.ClassicStructure operationBuilderStructure = new Structure.ClassicStructure();
			for (ParameterDescriptor parameter : parameters) {
				operationBuilderStructure.getElements().add(parameter.createOperationBuilderElement(className));
			}
			StringBuilder additionalDeclarations = new StringBuilder();
			{
				additionalDeclarations.append("@Override\n");
				additionalDeclarations.append("public " + operationClassSimpleName + " build("
						+ MiscUtils.adaptClassNameToSourceCode(ExecutionContext.class.getName()) + " context, "
						+ MiscUtils.adaptClassNameToSourceCode(ExecutionInspector.class.getName())
						+ " executionInspector) throws Exception {\n");
				additionalDeclarations.append(generateBuildMethodBody(className, options) + "\n");
				additionalDeclarations.append("}\n");
			}
			{
				additionalDeclarations.append("@Override\n");
				additionalDeclarations.append("public Class<?> getOperationResultClass(" + Plan.class.getName()
						+ " currentPlan, " + Step.class.getName() + " currentStep) {\n");
				additionalDeclarations.append(generateResultClassMethodBody(className, options) + "\n");
				additionalDeclarations.append("}\n");
			}
			{
				additionalDeclarations.append("@Override\n");
				additionalDeclarations.append("public void validate(boolean recursively, " + Plan.class.getName()
						+ " currentPlan, " + Step.class.getName() + " currentStep) {\n");
				additionalDeclarations.append(generateValidationMethodBody(className, options) + "\n");
				additionalDeclarations.append("}\n");
			}
			return operationBuilderStructure.generateJavaTypeSourceCode(className, implemented, null,
					additionalDeclarations.toString(), options);
		}

		private String generateBuildMethodBody(String parentClassName, Map<Object, Object> options) {
			StringBuilder result = new StringBuilder();
			for (ParameterDescriptor parameter : parameters) {
				result.append(
						parameter.generateBuildStatement(
								parameter.createOperationElement(parentClassName)
										.getFinalTypeNameAdaptedToSourceCode("") + " " + parameter.getName(),
								parentClassName, options) + "\n");
			}
			result.append("return new " + opertionTypeName + "("
					+ parameters.stream().map(ParameterDescriptor::getName).collect(Collectors.joining(", ")) + ");");
			return result.toString();
		}

		private String generateMetadataClassSourceCode(String parentClassName, Map<Object, Object> options) {
			String className = "Metadata";
			String operationClassSimpleName = MiscUtils.extractSimpleNameFromClassName(parentClassName);
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

		private String generateResultClassMethodBody(String parentClassName, Map<Object, Object> options) {
			return "return null;";
		}

		private String generateValidationMethodBody(String parentClassName, Map<Object, Object> options) {
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

			public Element createOperationElement(String parentClassName) {
				Element result = nature.createOperationElement(parentClassName, name);
				return result;
			}

			public Element createOperationBuilderElement(String parentClassName) {
				Element result = nature.createOperationBuilderElement(parentClassName, name);
				result = new Structure.ElementProxy(result) {

					@Override
					protected String generateRequiredInnerJavaTypesSourceCode(String parentClassName,
							Map<Object, Object> options) {
						return null;
					}

					@Override
					protected String generateJavaFieldDeclarationSourceCode(String parentClassName,
							Map<Object, Object> options) {
						return super.generateJavaFieldDeclarationSourceCode("", options);
					}

					@Override
					protected String generateJavaMethodsDeclarationSourceCode(String parentClassName,
							Map<Object, Object> options) {
						return super.generateJavaMethodsDeclarationSourceCode("", options);
					}

					@Override
					protected String generateJavaConstructorParameterDeclarationSourceCode(String parentClassName,
							Map<Object, Object> options) {
						return super.generateJavaConstructorParameterDeclarationSourceCode("", options);
					}

					@Override
					protected String generateJavaFieldInitializationInConstructorSourceCode(String parentClassName,
							Map<Object, Object> options) {
						return super.generateJavaFieldInitializationInConstructorSourceCode("", options);
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
						protected String generateJavaFieldDeclarationSourceCode(String parentClassName,
								Map<Object, Object> options) {
							return super.generateJavaFieldDeclarationSourceCode(parentClassName, options)
									.replaceAll("\\s*=\\s*" + GHOST_DEFAULT_VALUE_EXPRESSION_TO_ENABLE_SETTER, "");
						}

					};
				}
				return result;
			}

			public String generateBuildStatement(String targetVariableName, String parentClassName,
					Map<Object, Object> options) {
				return targetVariableName + " = " + nature.generateBuildExpression(name, parentClassName, options)
						+ ";";
			}

		}

	}

	public static class ResultDescriptor {
		private Nature nature = new SimpleNature();

		public Nature getNature() {
			return nature;
		}

		public void setNature(Nature nature) {
			this.nature = nature;
		}

	}

	public static abstract class Nature {

		public abstract Element createOperationElement(String parentClassName, String name);

		protected abstract String generateBuildExpression(String elementName, String parentClassName,
				Map<Object, Object> options);

		protected abstract Element createOperationBuilderElement(String parentClassName, String name);

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
		public Element createOperationElement(String parentClassName, String name) {
			Structure.SimpleElement result = new Structure.SimpleElement();
			result.setName(name);
			result.setTypeNameOrAlias(getTypeNameOrAlias());
			return result;
		}

		@Override
		protected Element createOperationBuilderElement(String parentClassName, String name) {
			if (variant) {
				return new Structure.SimpleElement() {
					Element base = createOperationElement(parentClassName, name);
					{
						setName(base.getName() + "Variant");
						Class<?> simpleClass = MiscUtils.getJESBClass(base.getTypeName(""));
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
					protected String generateRequiredInnerJavaTypesSourceCode(String parentClassName,
							Map<Object, Object> options) {
						return base.generateRequiredInnerJavaTypesSourceCode(parentClassName, options);
					}
				};
			} else {
				Element result = createOperationElement(parentClassName, name);
				if (defaultValueExpression != null) {
					Structure.Optionality optionality = new Structure.Optionality();
					optionality.setDefaultValueExpression(defaultValueExpression);
					result.setOptionality(optionality);
				}
				return result;
			}
		}

		@Override
		protected String generateBuildExpression(String elementName, String parentClassName,
				Map<Object, Object> options) {
			Element operationBuilderElement = createOperationBuilderElement(parentClassName, elementName);
			if (variant) {
				return "this." + operationBuilderElement.getName() + ".getValue()";
			} else {
				return "this." + operationBuilderElement.getName();
			}
		}

		public void validate(boolean recursively) throws ValidationError {
			internalElement.validate(recursively);
		}
	}

	public static class StructuredNature extends Nature {

		private Structure structure = new Structure.ClassicStructure();
		private boolean dynamic = false;
		private List<StructureDerivationAlternative> concreteStructureAlternatives;

		public Structure getStructure() {
			return structure;
		}

		public void setStructure(Structure structure) {
			this.structure = structure;
		}

		public boolean isDynamic() {
			return dynamic;
		}

		public void setDynamic(boolean dynamic) {
			this.dynamic = dynamic;
		}

		public List<StructureDerivationAlternative> getConcreteStructureAlternatives() {
			return concreteStructureAlternatives;
		}

		public void setConcreteStructureAlternatives(
				List<StructureDerivationAlternative> concreteStructureAlternatives) {
			this.concreteStructureAlternatives = concreteStructureAlternatives;
		}

		@Override
		public Structure.Element createOperationElement(String parentClassName, String name) {
			Structure.StructuredElement result = new Structure.StructuredElement();
			result.setName(name);
			result.setStructure(getStructure());
			if (concreteStructureAlternatives != null) {
				return new Structure.ElementProxy(result) {

					@Override
					protected String generateRequiredInnerJavaTypesSourceCode(String parentClassName,
							Map<Object, Object> options) {
						StringBuilder result = new StringBuilder();
						result.append(
								"abstract " + super.generateRequiredInnerJavaTypesSourceCode(parentClassName, options));
						for (StructureDerivationAlternative alternative : concreteStructureAlternatives) {
							result.append("\n" + alternative.generateRequiredInnerJavaTypesSourceCode(parentClassName,
									name, super.getTypeName(parentClassName), options));
						}
						return result.toString();
					}
				};
			} else {
				return result;
			}
		}

		@Override
		protected Element createOperationBuilderElement(String parentClassName, String name) {
			Element result;
			if (dynamic) {
				result = new Structure.SimpleElement() {
					Element base = createOperationElement(parentClassName, name);
					{
						setName(base.getName() + "Builder");
						setTypeNameOrAlias(RootInstanceBuilder.class.getName());
						Structure.Optionality optionality = new Structure.Optionality();
						{
							optionality.setDefaultValueExpression(
									"new " + RootInstanceBuilder.class.getName() + "(\"" + base.getName() + "Input\", "
											+ base.getFinalTypeNameAdaptedToSourceCode("") + ".class.getName())");
							setOptionality(optionality);
						}
					}

					@Override
					protected String generateRequiredInnerJavaTypesSourceCode(String parentClassName,
							Map<Object, Object> options) {
						return base.generateRequiredInnerJavaTypesSourceCode(parentClassName, options);
					}
				};
			} else {
				result = createOperationElement(parentClassName, name);
			}
			return result;
		}

		@Override
		protected String generateBuildExpression(String elementName, String parentClassName,
				Map<Object, Object> options) {
			Element operationElement = createOperationElement(parentClassName, elementName);
			Element operationBuilderElement = createOperationBuilderElement(parentClassName, elementName);
			if (dynamic) {
				return "(" + operationElement.getFinalTypeNameAdaptedToSourceCode("") + ") this."
						+ operationBuilderElement.getName() + ".build(new " + InstantiationContext.class.getName()
						+ "(context.getVariables(), context.getPlan().getValidationContext(context.getCurrentStep()).getVariableDeclarations()))";
			} else {
				return "this." + operationBuilderElement.getName();
			}
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

		public String generateRequiredInnerJavaTypesSourceCode(String parentClassName, String elementName,
				String baseStructureTypeName, Map<Object, Object> options) {
			Structure.StructuredElement utilityElement = new Structure.StructuredElement();
			utilityElement.setName(alternativeName + baseStructureTypeName.replaceAll("Structure$", ""));
			utilityElement.setStructure(new Structure.ClassicStructure());
			return utilityElement.generateRequiredInnerJavaTypesSourceCode(parentClassName, options).replace(
					"class " + utilityElement.getFinalTypeNameAdaptedToSourceCode(""),
					"class " + utilityElement.getFinalTypeNameAdaptedToSourceCode("") + " extends "
							+ baseStructureTypeName);
		}
	}

}
