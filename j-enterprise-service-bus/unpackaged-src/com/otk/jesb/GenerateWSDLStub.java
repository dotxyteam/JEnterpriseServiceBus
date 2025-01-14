package com.otk.jesb;

import com.sun.tools.ws.WsImport;

public class GenerateWSDLStub {

	public static void main(String[] args) throws Throwable {
		String[] input = new String[] { "-d", "unpackaged-src", "-keep", "-verbose", "-Xnocompile",
				"unpackaged-src/com/otk/jesb/HelloService.wsdl" };
		WsImport.doMain(input);
	}

}
