package org.tempuri;

import javax.jws.WebService;

@WebService(endpointInterface = "org.tempuri.SOAPDemoSoap")
public class SOAPDemoImpl implements SOAPDemoSoap { 

	@Override
	public long addInteger(Long arg1, Long arg2) {
		return arg1 + arg2;
	}

	@Override
	public long divideInteger(Long arg1, Long arg2) {
		return arg1/arg2;
	}

	@Override
	public Person findPerson(String id) {
		return null;
	}

	@Override
	public DataSet getByName(String name) {
		return null;
	}

	@Override
	public ByNameDataSet getDataSetByName(String name) {
		return null;
	}

	@Override
	public ArrayOfPersonIdentificationPersonIdentification getListByName(String name) {
		return null;
	}

	@Override
	public Address lookupCity(String zip) {
		return null;
	}

	@Override
	public String mission() {
		return null;
	}

	@Override
	public QueryByNameDataSet queryByName(String name) {
		return null;
	}

	
}
