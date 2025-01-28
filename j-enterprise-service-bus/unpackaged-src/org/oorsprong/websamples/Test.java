package org.oorsprong.websamples;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Service;

@SuppressWarnings("unused")
public class Test {

	public static void main(String[] args) {
		System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
		System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
		System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
		System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");
		System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dumpTreshold", "999999");

		try {
			//URL url = new URL("http://localhost:9090/SOAPDemo");
			/*SOAPDemoImpl implementor = new SOAPDemoImpl();
			Endpoint.publish(url.toString(), implementor);*/

			
			CountryInfoServiceSoapType port = new CountryInfoService().getPort(CountryInfoServiceSoapType.class); 
			//BindingProvider bindingProvider = (BindingProvider) port;
			//bindingProvider.getRequestContext().put(BindingProvider.
			//ENDPOINT_ADDRESS_PROPERTY, url.toString());
			System.out.println(port.listOfContinentsByName());
			 

			/*QName qname = new QName("http://www.examples.com/wsdl/HelloService.wsdl", "Hello_Service");
			Service service = Service.create(url, qname);
			SOAPDemoSoap port = service.getPort(SOAPDemoSoap.class);
			System.out.println("1+2=" + port.addInteger(1l, 2l));*/
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.exit(0);
		}
	}

}
