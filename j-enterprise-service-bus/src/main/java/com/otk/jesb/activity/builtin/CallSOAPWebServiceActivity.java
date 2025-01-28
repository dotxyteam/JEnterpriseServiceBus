package com.otk.jesb.activity.builtin;

import java.lang.reflect.Method;

import com.otk.jesb.InstanceBuilder;
import com.otk.jesb.InstanceBuilder.Function;
import com.otk.jesb.InstanceBuilder.VerificationContext;
import com.otk.jesb.Plan.ExecutionContext;
import com.otk.jesb.Plan.ValidationContext;
import com.otk.jesb.activity.Activity;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;
import com.otk.jesb.resource.builtin.WSDL;

import xy.reflect.ui.info.ResourcePath;

public class CallSOAPWebServiceActivity implements Activity {

	private Class<?> serviceClass;
	private Class<?> portClass;
	private Method operationMethod;
	private OperationInput operationInput;

	public Class<?> getServiceClass() {
		return serviceClass;
	}

	public void setServiceClass(Class<?> serviceClass) {
		this.serviceClass = serviceClass;
	}

	public Class<?> getPortClass() {
		return portClass;
	}

	public void setPortClass(Class<?> portClass) {
		this.portClass = portClass;
	}

	public Method getOperationMethod() {
		return operationMethod;
	}

	public void setOperationMethod(Method operationMethod) {
		this.operationMethod = operationMethod;
	}

	public OperationInput getOperationInput() {
		return operationInput;
	}

	public void setOperationInput(OperationInput operationInput) {
		this.operationInput = operationInput;
	}

	@Override
	public Object execute() throws Exception {
		Object service = serviceClass.newInstance();
		Object port = serviceClass.getMethod("getPort", Class.class).invoke(service, portClass);
		return operationMethod.invoke(port, operationInput.listParameterValues());
	}

	public interface OperationInput {

		Object[] listParameterValues();

	}

	public static class Metadata implements ActivityMetadata {

		@Override
		public String getActivityTypeName() {
			return "Call SOAP Web Service";
		}

		@Override
		public String getCategoryName() {
			return "SOAP";
		}

		@Override
		public Class<? extends ActivityBuilder> getActivityBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getActivityIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(ExecutePlanActivity.class.getName().replace(".", "/") + ".png"));
		}
	}

	public static class Builder implements ActivityBuilder {

		private WSDL wsdl;
		private InstanceBuilder operationInputBuilder;

		@Override
		public Activity build(ExecutionContext context) throws Exception {
			WSDL.Service service = wsdl.getServices().stream().filter(s -> s.getName().equals(selectedServiceTypeName))
					.findFirst().get();
			WSDL.PortType portType = service.getPortTypes().stream()
					.filter(pt -> pt.getName().equals(selectedPortTypeName)).findFirst().get();
			WSDL.Operation operation = portType.getOperations().stream()
					.filter(o -> o.getName().equals(selectedOperationName)).findFirst().get();
			CallSOAPWebServiceActivity result = new CallSOAPWebServiceActivity();
			result.setServiceClass(service.toClass());
			result.setPortClass(portType.toClass());
			result.setOperationMethod(operation.toMethod());
			result.setOperationInput(operationInputBuilder.build(new InstanceBuilder.EvaluationContext(context, null)));
			return result;
		}

		@Override
		public Class<?> getActivityResultClass() {
			if (wsdl == null) {
				return null;
			}
			WSDL.Service service = wsdl.getServices().stream().filter(s -> s.getName().equals(selectedServiceTypeName))
					.findFirst().get();
			if (service == null) {
				return null;
			}
			WSDL.PortType portType = service.getPortTypes().stream()
					.filter(pt -> pt.getName().equals(selectedPortTypeName)).findFirst().get();
			if (portType == null) {
				return null;
			}
			WSDL.Operation operation = portType.getOperations().stream()
					.filter(o -> o.getName().equals(selectedOperationName)).findFirst().get();
			if (operation == null) {
				return null;
			}
			return operation.toMethod().getReturnType();
		}

		@Override
		public VerificationContext findFunctionVerificationContext(Function function,
				ValidationContext validationContext) {
			// TODO Auto-generated method stub
			return null;
		}

	}

}
