package com.examples.wsdl.helloservice;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Service;

public class Test {

	public static void main(String[] args) {
		System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
		System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
		System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
		System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");
		System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dumpTreshold", "999999");

		try {
			HelloPortType implementor = new HelloImpl();
			Endpoint.publish("http://localhost:9090/HelloServerPort", implementor);
			
			/*
			HelloPortType port = new HelloService().getHelloPort();
			BindingProvider bindingProvider = (BindingProvider) port;
			bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
					"http://localhost:9090/HelloServerPort");
					System.out.println(port.sayHello("Oli"));*/
			
			URL url = new URL("http://localhost:9090/HelloServerPort");
		    QName qname = new QName("http://helloservice.wsdl.examples.com/", "HelloImplService");
		    Service service = Service.create(url, qname);
		    HelloPortType port = service.getPort(HelloPortType.class);
		    System.out.println(port.sayHello("Oli"));
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.exit(0);
		}
	}

}
