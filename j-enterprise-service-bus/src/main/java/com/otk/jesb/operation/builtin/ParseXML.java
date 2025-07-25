package com.otk.jesb.operation.builtin;

import java.io.StringReader;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.solution.Step;
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

		private RootInstanceBuilder sourceDocumentBuilder = new RootInstanceBuilder("XMLSourceDocument",
				SourceDocument.class.getName());

		public RootInstanceBuilder getSourceDocumentBuilder() {
			return sourceDocumentBuilder;
		}

		public void setSourceDocumentBuilder(RootInstanceBuilder sourceDocumentBuilder) {
			this.sourceDocumentBuilder = sourceDocumentBuilder;
		}

		@Override
		public ParseXML build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			return new ParseXML(
					(String) sourceDocumentBuilder.build(new InstantiationContext(context.getVariables(), context
							.getPlan().getValidationContext(context.getCurrentStep()).getVariableDeclarations())),
					retrieveRootElement().retrieveClass());
		}

		@Override
		public void validate(boolean recursively, Plan plan, Step step) throws ValidationError {
			if (recursively) {
				sourceDocumentBuilder.getFacade().validate(recursively,
						plan.getValidationContext(step).getVariableDeclarations());
			}
		}

		@Override
		public Class<?> getOperationResultClass(Plan currentPlan, Step currentStep) {
			try {
				return upToDateDocumentObjectClass.get();
			} catch (VersionAccessException e) {
				throw new UnexpectedError(e);
			}
		}

	}

}
