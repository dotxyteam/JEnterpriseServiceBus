package com.otk.jesb.operation.builtin;

import java.io.StringWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

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
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.UpToDate.VersionAccessException;

import xy.reflect.ui.info.ResourcePath;

public class GenerateXML extends XMLOperation {

	private Object rootObject;
	private boolean outputFormatted;

	public GenerateXML(Object rootObject, Class<?> rootElementClass, boolean outputFormatted) {
		super(rootElementClass);
		this.rootObject = rootObject;
		this.outputFormatted = outputFormatted;
	}

	public Object getRootObject() {
		return rootObject;
	}

	public boolean isOutputFormatted() {
		return outputFormatted;
	}

	@Override
	public Object execute() throws Exception {
		JAXBContext context = JAXBContext.newInstance(getRootElementClass());
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, outputFormatted);
		StringWriter stringWriter = new StringWriter();
		marshaller.marshal(rootObject, stringWriter);
		return new SourceDocument(stringWriter.toString());
	}

	public static class Metadata implements OperationMetadata<GenerateXML> {

		@Override
		public String getOperationTypeName() {
			return "Generate XML";
		}

		@Override
		public String getCategoryName() {
			return "XML";
		}

		@Override
		public Class<? extends OperationBuilder<GenerateXML>> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(GenerateXML.class.getName().replace(".", "/") + ".png"));
		}
	}

	public static class Builder extends XMLOperation.Builder<GenerateXML> {

		private boolean outputFormatted = true;
		private RootInstanceBuilder documentBuilder = new RootInstanceBuilder("XMLDocumentObject",
				new DocumentClassNameAccessor());

		public RootInstanceBuilder getDocumentBuilder() {
			return documentBuilder;
		}

		public void setDocumentBuilder(RootInstanceBuilder documentBuilder) {
			this.documentBuilder = documentBuilder;
		}

		public boolean isOutputFormatted() {
			return outputFormatted;
		}

		public void setOutputFormatted(boolean outputFormatted) {
			this.outputFormatted = outputFormatted;
		}

		@Override
		public GenerateXML build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			return new GenerateXML(
					documentBuilder.build(new InstantiationContext(context.getVariables(),
							context.getPlan().getValidationContext(context.getCurrentStep())
									.getVariableDeclarations())),
					retrieveRootElement().retrieveClass(), outputFormatted);
		}

		@Override
		public void validate(boolean recursively, Plan plan, Step step) throws ValidationError {
			super.validate(recursively, plan, step);
			if (recursively) {
				documentBuilder.getFacade().validate(recursively,
						plan.getValidationContext(step).getVariableDeclarations());
			}
		}

		@Override
		public Class<?> getOperationResultClass(Plan currentPlan, Step currentStep) {
			return SourceDocument.class;
		}

		public class DocumentClassNameAccessor extends Accessor<String> {

			@Override
			public String get() {
				Class<?> documentClass;
				try {
					documentClass = upToDateDocumentObjectClass.get();
				} catch (VersionAccessException e) {
					throw new UnexpectedError(e);
				}
				if (documentClass == null) {
					return null;
				}
				return documentClass.getName();
			}

		}

	}

}
