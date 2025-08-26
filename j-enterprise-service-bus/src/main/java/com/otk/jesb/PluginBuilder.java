package com.otk.jesb;

import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import com.otk.jesb.Structure.Element;
import com.otk.jesb.Structure.SimpleElement;
import com.otk.jesb.Structure.StructuredElement;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.ui.GUI;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.info.ResourcePath;

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
		private String buildMethodBody;

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

		public String getBuildMethodBody() {
			return buildMethodBody;
		}

		public void setBuildMethodBody(String buildMethodBody) {
			this.buildMethodBody = buildMethodBody;
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
			additionalDeclarations.append(generateBuilderClassSourceCode(className));
			additionalDeclarations.append(generateMetadataClassSourceCode(className));
			File javaFile = new File(sourceDirectroy, className.replace(".", "/") + ".java");
			try {
				if (!javaFile.getParentFile().exists()) {
					MiscUtils.createDirectory(javaFile.getParentFile(), true);
				}
				MiscUtils.write(javaFile, operationStructure.generateJavaTypeSourceCode(className, implemented, null,
						additionalDeclarations.toString()), false);
			} catch (IOException e) {
				throw new UnexpectedError(e);
			}
			return javaFile;
		}

		private String generateBuilderClassSourceCode(String parentClassName) {
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
				additionalDeclarations.append("public " + operationClassSimpleName
						+ " build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {\n");
				additionalDeclarations.append(buildMethodBody + "\n");
				additionalDeclarations.append("}\n");
			}
			{
				additionalDeclarations.append("@Override\n");
				additionalDeclarations
						.append("public Class<?> getOperationResultClass(Plan currentPlan, Step currentStep) {\n");
				additionalDeclarations.append(generateResultClassMethodBody() + "\n");
				additionalDeclarations.append("}\n");
			}
			return operationBuilderStructure.generateJavaTypeSourceCode(className, implemented, null,
					additionalDeclarations.toString());
		}

		private String generateMetadataClassSourceCode(String parentClassName) {
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
				result.append("public Class<? extends OperationBuilder<" + operationClassSimpleName
						+ ">> getOperationBuilderClass() {\n");
				result.append("return Builder.class;" + "\n");
				result.append("}\n");
			}
			{
				result.append("@Override\n");
				result.append("public ResourcePath getOperationIconImagePath() {\n");
				result.append("return new " + ResourcePath.class.getName() + "(" + ResourcePath.class.getName()
						+ ".specifyClassPathResourceLocation(" + operationClassSimpleName
						+ ".class.getName().replace(\".\", \"/\") + \".png\"));" + "\n");
				result.append("}\n");
			}
			result.append("}");
			return result.toString();
		}

		private String generateResultClassMethodBody() {
			return "return null;";
		}

		public class ParameterDescriptor {

			private String name;
			private String caption;
			private Variability variability = new StaticVariability();
			private Nature nature = new SimpleNature();
			private String defaultValueExpression;

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

			public Variability getVariability() {
				return variability;
			}

			public void setVariability(Variability variability) {
				this.variability = variability;
			}

			public Nature getNature() {
				return nature;
			}

			public void setNature(Nature nature) {
				this.nature = nature;
			}

			public String getDefaultValueExpression() {
				return defaultValueExpression;
			}

			public void setDefaultValueExpression(String defaultValueExpression) {
				this.defaultValueExpression = defaultValueExpression;
			}

			public Element createOperationElement(String parentClassName) {
				Element result = nature.createOperationElement(parentClassName, name, defaultValueExpression);
				result = new Structure.AccessorBasedElementProxy(result);
				return result;
			}

			public Element createOperationBuilderElement(String parentClassName) {
				Element result = nature.createOperationElement(parentClassName, name, defaultValueExpression);
				result.setOptionality(null);
				result = new Structure.AccessorBasedElementProxy(result);
				result = new Structure.ElementProxy(result) {

					@Override
					protected String generateRequiredInnerJavaTypesSourceCode(String parentClassName) {
						return null;
					}

				};
				result = variability.adaptOperationElement(parentClassName, result);
				return result;
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

		public abstract Element createOperationElement(String parentClassName, String name,
				String defaultValueExpression);

		protected void configureOptionality(Structure.Element element, String defaultValueExpression) {
			if (defaultValueExpression != null) {
				Structure.Optionality optionality = new Structure.Optionality();
				optionality.setDefaultValueExpression(defaultValueExpression);
				element.setOptionality(optionality);
			}

		}
	}

	public static class SimpleNature extends Nature {

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
		public Element createOperationElement(String parentClassName, String name, String defaultValueExpression) {
			Structure.SimpleElement result = new Structure.SimpleElement();
			result.setName(name);
			result.setTypeNameOrAlias(getTypeNameOrAlias());
			configureOptionality(result, defaultValueExpression);
			return result;
		}

		public void validate(boolean recursively) throws ValidationError {
			internalElement.validate(recursively);
		}
	}

	public static class StructuredNature extends Nature {

		private StructuredElement internalElement = new StructuredElement();

		public Structure getStructure() {
			return internalElement.getStructure();
		}

		public void setStructure(Structure structure) {
			internalElement.setStructure(structure);
		}

		@Override
		public Structure.StructuredElement createOperationElement(String parentClassName, String name,
				String defaultValueExpression) {
			Structure.StructuredElement result = new Structure.StructuredElement();
			result.setName(name);
			result.setStructure(getStructure());
			configureOptionality(result, defaultValueExpression);
			return result;
		}

		public void validate(boolean recursively) throws ValidationError {
			internalElement.validate(recursively);
		}
	}

	public static class PolymorphicNature extends Nature {

		private StructuredNature baseNature = new StructuredNature();
		private List<NatureAlternative> alternatives = new ArrayList<NatureAlternative>();

		public StructuredNature getBaseNature() {
			return baseNature;
		}

		public void setBaseNature(StructuredNature baseNature) {
			this.baseNature = baseNature;
		}

		public List<NatureAlternative> getAlternatives() {
			return alternatives;
		}

		public void setAlternatives(List<NatureAlternative> alternatives) {
			this.alternatives = alternatives;
		}

		@Override
		public Element createOperationElement(String parentClassName, String name, String defaultValueExpression) {
			return new Structure.ElementProxy(
					baseNature.createOperationElement(parentClassName, name, defaultValueExpression)) {

				@Override
				protected String generateRequiredInnerJavaTypesSourceCode(String parentClassName) {
					StringBuilder result = new StringBuilder();
					result.append("abstract " + super.generateRequiredInnerJavaTypesSourceCode(parentClassName));
					for (NatureAlternative alternative : alternatives) {
						result.append("\n" + alternative.generateRequiredInnerJavaTypesSourceCode(parentClassName, name,
								super.getTypeName(parentClassName)));
					}
					return result.toString();
				}
			};
		}

		public static class NatureAlternative {
			private String alternativeName;
			private String condition;
			private StructuredNature nature = new StructuredNature();

			public String getAlternativeName() {
				return alternativeName;
			}

			public void setAlternativeName(String alternativeName) {
				this.alternativeName = alternativeName;
			}

			public String getCondition() {
				return condition;
			}

			public void setCondition(String condition) {
				this.condition = condition;
			}

			public StructuredNature getNature() {
				return nature;
			}

			public void setNature(StructuredNature nature) {
				this.nature = nature;
			}

			public String generateRequiredInnerJavaTypesSourceCode(String parentClassName, String elementName,
					String baseStructureTypeName) {
				return nature.createOperationElement(parentClassName,
						alternativeName + elementName.substring(0, 1).toUpperCase() + elementName.substring(1), null)
						.generateRequiredInnerJavaTypesSourceCode(parentClassName);
			}
		}
	}

	public static abstract class Variability {

		public abstract Element adaptOperationElement(String parentClassName, Element element);

	}

	public static class StaticVariability extends Variability {

		@Override
		public Element adaptOperationElement(String parentClassName, Element element) {
			return element;
		}

	}

	public static class EnvironmentVariability extends Variability {
		@Override
		public Element adaptOperationElement(String parentClassName, Element element) {
			return new Structure.SimpleElement() {
				{
					setName(element.getName() + "Variant");
					setTypeNameOrAlias(Variant.class.getName() + "<" + element.getTypeName(parentClassName) + ">");
				}

				@Override
				protected String generateRequiredInnerJavaTypesSourceCode(String parentClassName) {
					return element.generateRequiredInnerJavaTypesSourceCode(parentClassName);
				}
			};
		}
	}

	public static class DynamicVariability extends Variability {
		@Override
		public Element adaptOperationElement(String parentClassName, Element element) {
			return new Structure.SimpleElement() {
				{
					setName(element.getName() + "Builder");
					setTypeNameOrAlias(RootInstanceBuilder.class.getName());
					Structure.Optionality optionality = new Structure.Optionality();
					{
						optionality.setDefaultValueExpression(
								"new " + RootInstanceBuilder.class.getName() + "(\"" + element.getName() + "Input\", "
										+ element.getFinalTypeNameAdaptedToSourceCode(parentClassName) + ".class)");
						setOptionality(optionality);
					}
				}

				@Override
				protected String generateRequiredInnerJavaTypesSourceCode(String parentClassName) {
					return element.generateRequiredInnerJavaTypesSourceCode(parentClassName);
				}
			};
		}
	}

}
