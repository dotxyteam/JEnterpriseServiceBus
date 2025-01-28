package com.otk.jesb;

import com.sun.tools.ws.WsImport;

public class GenerateWSDLStub2 {

	public static void main(String[] args) throws Throwable {
		System.setProperty("javax.xml.accessExternalSchema", "all");
		System.setProperty("javax.xml.accessExternalDTD", "all");
		String[] input = new String[] { "-s", "unpackaged-src", "-keep", "-Xnocompile", "-verbose",
				/*
				 * "-J-Djavax.xml.accessExternalSchema=all",
				 * "-J-Djavax.xml.accessExternalDTD=all",
				 */
				"-b", "http://www.w3.org/2001/XMLSchema.xsd",
				/*"https://www.crcind.com/csp/samples/SOAP.Demo.CLS?WSDL=1"*/
				"unpackaged-src/com/otk/jesb/SOAP.Demo.wsdl" };
		WsImport.doMain(input);
	}

}
