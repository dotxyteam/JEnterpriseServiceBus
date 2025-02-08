package com.otk.jesb.instantiation;

public class Function {
	private String functionBody;

	public Function() {
	}

	public Function(String functionBody) {
		this.functionBody = functionBody;
	}

	public String getFunctionBody() {
		return functionBody;
	}

	public void setFunctionBody(String functionBody) {
		this.functionBody = functionBody;
	}

	public static class CompilationContext {
		private VerificationContext verificationContext;
		private Class<?> functionReturnType;

		public CompilationContext(VerificationContext verificationContext, Class<?> functionReturnType) {
			this.verificationContext = verificationContext;
			this.functionReturnType = functionReturnType;
		}

		public VerificationContext getVerificationContext() {
			return verificationContext;
		}

		public Class<?> getFunctionReturnType() {
			return functionReturnType;
		}

	}

}