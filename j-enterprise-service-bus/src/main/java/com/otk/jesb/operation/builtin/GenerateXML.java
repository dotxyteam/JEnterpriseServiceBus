package com.otk.jesb.operation.builtin;

import java.io.StringWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import com.otk.jesb.ValidationError;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.resource.builtin.XSD.RootElement;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.solution.Step;
import com.otk.jesb.util.Accessor;

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
		return stringWriter.toString();
	}

	public static class Metadata implements OperationMetadata {

		@Override
		public String getOperationTypeName() {
			return "Generate XML";
		}

		@Override
		public String getCategoryName() {
			return "XML";
		}

		@Override
		public Class<? extends OperationBuilder> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(GenerateXML.class.getName().replace(".", "/") + ".png"));
		}
	}

	public static class Builder extends XMLOperation.Builder {

		private RootInstanceBuilder rootObjectBuilder = new RootInstanceBuilder("XMLObject",
				new RootObjectClassNameAccessor());
		private boolean outputFormatted = true;

		public RootInstanceBuilder getRootObjectBuilder() {
			return rootObjectBuilder;
		}

		public void setRootObjectBuilder(RootInstanceBuilder rootObjectBuilder) {
			this.rootObjectBuilder = rootObjectBuilder;
		}

		public boolean isOutputFormatted() {
			return outputFormatted;
		}

		public void setOutputFormatted(boolean outputFormatted) {
			this.outputFormatted = outputFormatted;
		}

		@Override
		public Operation build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			return new GenerateXML(
					rootObjectBuilder.build(new InstantiationContext(context.getVariables(),
							context.getPlan().getValidationContext(context.getCurrentStep())
									.getVariableDeclarations())),
					retrieveRootElement().retrieveClass(), outputFormatted);
		}

		@Override
		public void validate(boolean recursively, Plan plan, Step step) throws ValidationError {
			super.validate(recursively, plan, step);
			if (recursively) {
				rootObjectBuilder.getFacade().validate(recursively,
						plan.getValidationContext(step).getVariableDeclarations());
			}
		}

		@Override
		public Class<?> getOperationResultClass(Plan currentPlan, Step currentStep) {
			return String.class;
		}

		public class RootObjectClassNameAccessor extends Accessor<String> {

			@Override
			public String get() {
				RootElement rootElement = retrieveRootElement();
				if (rootElement == null) {
					return null;
				}
				return rootElement.retrieveClass().getName();
			}

		}

	}

}
