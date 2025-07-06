package com.otk.jesb;

import java.io.File;

import org.xml.sax.SAXParseException;

import com.otk.jesb.util.MiscUtils;
import com.sun.tools.xjc.Driver;
import com.sun.tools.xjc.XJCListener;

public class XsdToJavaGenerator {
	public static void main(String[] args) throws Exception {
		File outputDirectory = new File("tmp/java-from-xsd");
		if (outputDirectory.exists()) {
			MiscUtils.delete(outputDirectory);
		}
		MiscUtils.createDirectory(outputDirectory);
		String[] xjcArgs = { "-d", outputDirectory.getPath(), // dossier de sortie
				"-p", "com.example.generated", // package cible
				"src/test/java/com/otk/jesb/postals.xsd" // chemin vers le fichier XSD
		};
		Driver.run(xjcArgs, new XJCListener() {

			@Override
			public void warning(SAXParseException exception) {
				System.out.println("warning: " + exception);
			}

			@Override
			public void info(SAXParseException exception) {
				System.out.println("info: " + exception);
			}

			@Override
			public void fatalError(SAXParseException exception) {
				System.out.println("fatalError: " + exception);
			}

			@Override
			public void error(SAXParseException exception) {
				System.out.println("error: " + exception);
			}
		});
	}
}