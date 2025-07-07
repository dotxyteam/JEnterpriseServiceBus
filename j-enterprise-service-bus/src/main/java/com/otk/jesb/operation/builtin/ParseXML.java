package com.otk.jesb.operation.builtin;

import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import com.otk.jesb.Reference;
import com.otk.jesb.ValidationError;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.resource.builtin.XMLBasedDocumentResource;
import com.otk.jesb.resource.builtin.XSD;
import com.otk.jesb.resource.builtin.XSD.RootElement;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.solution.Step;

import xy.reflect.ui.info.ResourcePath;

public class ParseXML implements Operation {

	private String xmlText;
	private XMLBasedDocumentResource xsd;
	private Class<?> rootElementClass;

	public ParseXML(String xmlText, XMLBasedDocumentResource xsd, Class<?> rootElementClass) {
		this.xmlText = xmlText;
		this.xsd = xsd;
		this.rootElementClass = rootElementClass;
	}

	public String getXmlText() {
		return xmlText;
	}

	public XMLBasedDocumentResource getXsd() {
		return xsd;
	}

	public Class<?> getRootElementClass() {
		return rootElementClass;
	}

	@Override
	public Object execute() throws Exception {
		JAXBContext context = JAXBContext.newInstance(rootElementClass);
		Unmarshaller unmarshaller = context.createUnmarshaller();
		return unmarshaller.unmarshal(new StringReader(xmlText));
	}

	public static class Metadata implements OperationMetadata {

		@Override
		public String getOperationTypeName() {
			return "Parse XML";
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
			return new ResourcePath(
					ResourcePath.specifyClassPathResourceLocation(ParseXML.class.getName().replace(".", "/") + ".png"));
		}
	}

	public static class Builder implements OperationBuilder {

		private Reference<XSD> xsdReference = new Reference<XSD>(XSD.class);
		private String rootElementName;
		private RootInstanceBuilder xmlTextBuilder = new RootInstanceBuilder("XMLText", String.class.getName());

		private XSD getXSD() {
			return xsdReference.resolve();
		}

		public Reference<XSD> getXsdReference() {
			return xsdReference;
		}

		public void setXsdReference(Reference<XSD> xsdReference) {
			this.xsdReference = xsdReference;
			tryToSelectValuesAutomatically();
		}

		public String getRootElementName() {
			return rootElementName;
		}

		public void setRootElementName(String rootElementName) {
			this.rootElementName = rootElementName;
		}

		public RootInstanceBuilder getXmlTextBuilder() {
			return xmlTextBuilder;
		}

		public void setXmlTextBuilder(RootInstanceBuilder xmlTextBuilder) {
			this.xmlTextBuilder = xmlTextBuilder;
		}

		private void tryToSelectValuesAutomatically() {
			try {
				if (rootElementName == null) {
					XSD xsd = getXSD();
					if (xsd != null) {
						List<XSD.RootElement> rootElements = xsd.getRootElements();
						if (rootElements.size() > 0) {
							rootElementName = rootElements.get(0).getName();
						}
					}
				}
			} catch (Throwable t) {
			}
		}

		public List<String> getRootElementNameOptions() {
			XSD xsd = getXSD();
			if (xsd == null) {
				return Collections.emptyList();
			}
			return xsd.getRootElements().stream().map(e -> e.getName()).collect(Collectors.toList());
		}

		@Override
		public Operation build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			return new ParseXML(
					(String) xmlTextBuilder
							.build(new InstantiationContext(context.getVariables(),
									context.getPlan().getValidationContext(context.getCurrentStep())
											.getVariableDeclarations())),
					getXSD(), retrieveRootElement().retrieveClass());
		}

		private XSD.RootElement retrieveRootElement() {
			XSD xsd = getXSD();
			if (xsd == null) {
				return null;
			}
			return xsd.getRootElements().stream().filter(e -> e.getName().equals(rootElementName)).findFirst()
					.orElse(null);
		}

		@Override
		public void validate(boolean recursively, Plan plan, Step step) throws ValidationError {
			if (getXSD() == null) {
				throw new ValidationError("Failed to resolve the XSD reference");
			}
			if (retrieveRootElement() == null) {
				throw new ValidationError("Invalid root element name '" + rootElementName + "'");
			}
			if (recursively) {
				xmlTextBuilder.getFacade().validate(recursively,
						plan.getValidationContext(step).getVariableDeclarations());
			}
		}

		@Override
		public Class<?> getOperationResultClass(Plan currentPlan, Step currentStep) {
			RootElement rootElement = retrieveRootElement();
			if (rootElement == null) {
				return null;
			}
			return rootElement.retrieveClass();
		}

	}

}
