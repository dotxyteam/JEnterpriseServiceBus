package com.examples.wsdl.helloservice;

import javax.jws.WebService;

@WebService(endpointInterface = "com.examples.wsdl.helloservice.HelloPortType")
public class HelloImpl implements HelloPortType{

	@Override
	public String sayHello(String firstName) {
		return "Hello " + firstName + "!!!";
	}

}
