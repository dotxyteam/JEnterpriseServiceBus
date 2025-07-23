package com.example.generated;

import java.io.File;
import java.io.FileReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.otk.jesb.resource.builtin.XMLBasedDocumentResource;
import xy.reflect.ui.util.IOUtils;

public class Test {

	public static void main(String[] args) throws Exception {
		IOUtils.copy(new File("tmp/java-from-xsd2"), new File("tmp/java-from-xsd"));
		new XMLBasedDocumentResource("") {
			
			{
				JAXBPostProcessor.process(new File("tmp/java-from-xsd"));
			}
			@Override
			protected void runClassesGenerationTool(File mainFile, File metaSchemaFile, File outputDirectory) throws Exception {
			}
		};
		
		
		JAXBContext context = JAXBContext.newInstance(Postals.class);
		
		Unmarshaller unmarshaller = context.createUnmarshaller();
		Object object = unmarshaller.unmarshal(new FileReader("src/test/java/com/example/generated/postals.xml"));
		System.out.println(object);
		
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		marshaller.marshal(object, System.out);
	}

}
