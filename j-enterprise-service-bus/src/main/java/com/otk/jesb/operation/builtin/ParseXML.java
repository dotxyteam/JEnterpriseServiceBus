package com.otk.jesb.operation.builtin;

import java.io.StringReader;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import com.otk.jesb.Structure.ClassicStructure;
import com.otk.jesb.Structure.SimpleElement;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.resource.builtin.XSD.RootElementDescriptor;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.solution.Step;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.UpToDate;
import com.otk.jesb.util.UpToDate.VersionAccessException;

import xy.reflect.ui.info.ResourcePath;

public class ParseXML extends XMLOperation {

	private String xmlText;

	public ParseXML(String xmlText, Class<?> rootElementClass) {
		super(rootElementClass);
		this.xmlText = xmlText;
	}

	public String getXmlText() {
		return xmlText;
	}

	@Override
	public Object execute() throws Exception {
		JAXBContext context = JAXBContext.newInstance(getRootElementClass());
		Unmarshaller unmarshaller = context.createUnmarshaller();
		return unmarshaller.unmarshal(new StringReader(xmlText));
	}

	public static class Metadata implements OperationMetadata<ParseXML> {

		@Override
		public String getOperationTypeName() {
			return "Parse XML";
		}

		@Override
		public String getCategoryName() {
			return "XML";
		}

		@Override
		public Class<? extends OperationBuilder<ParseXML>> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(
					ResourcePath.specifyClassPathResourceLocation(ParseXML.class.getName().replace(".", "/") + ".png"));
		}
	}

	public static class Builder extends XMLOperation.Builder<ParseXML> {

		private RootInstanceBuilder xmlTextBuilder = new RootInstanceBuilder("XMLText", String.class.getName());
		private UpToDateResultClass upToDateResultClass = new UpToDateResultClass();

		public RootInstanceBuilder getXmlTextBuilder() {
			return xmlTextBuilder;
		}

		public void setXmlTextBuilder(RootInstanceBuilder xmlTextBuilder) {
			this.xmlTextBuilder = xmlTextBuilder;
		}

		@Override
		public ParseXML build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			return new ParseXML(
					(String) xmlTextBuilder.build(new InstantiationContext(context.getVariables(), context.getPlan()
							.getValidationContext(context.getCurrentStep()).getVariableDeclarations())),
					retrieveRootElement().retrieveClass());
		}

		@Override
		public void validate(boolean recursively, Plan plan, Step step) throws ValidationError {
			if (recursively) {
				xmlTextBuilder.getFacade().validate(recursively,
						plan.getValidationContext(step).getVariableDeclarations());
			}
		}

		@Override
		public Class<?> getOperationResultClass(Plan currentPlan, Step currentStep) {
			try {
				return upToDateResultClass.get();
			} catch (VersionAccessException e) {
				throw new UnexpectedError(e);
			}
		}

		private class UpToDateResultClass extends UpToDate<Class<?>> {

			@Override
			protected Object retrieveLastVersionIdentifier() {
				RootElementDescriptor rootElementDescriptor = retrieveRootElement();
				if (rootElementDescriptor == null) {
					return null;
				}
				return rootElementDescriptor.retrieveClass();
			}

			@Override
			protected Class<?> obtainLatest(Object versionIdentifier) throws VersionAccessException {
				RootElementDescriptor rootElementDescriptor = retrieveRootElement();
				if (rootElementDescriptor == null) {
					return null;
				}
				ClassicStructure resultStructure = new ClassicStructure();
				{
					SimpleElement rootElement = new SimpleElement();
					rootElement.setName(getRootElementName());
					rootElement.setTypeName(rootElementDescriptor.retrieveClass().getName());
					resultStructure.getElements().add(rootElement);
				}
				String className = ParseXML.class.getName() + "Result" + MiscUtils.toDigitalUniqueIdentifier(this);
				try {
					return MiscUtils.IN_MEMORY_COMPILER.compile(className,
							resultStructure.generateJavaTypeSourceCode(className));
				} catch (CompilationError e) {
					throw new UnexpectedError(e);
				}
			}

		}

	}

}
