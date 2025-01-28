package com.otk.jesb;

import com.sun.tools.ws.WsImport;

public class GenerateWSDLStub {

	public static void main(String[] args) throws Throwable {
		String[] input = new String[] { "-s", "unpackaged-src", "-keep", "-Xnocompile", "-verbose",
				"http://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso?WSDL" };
		WsImport.doMain(input);
	}

}
