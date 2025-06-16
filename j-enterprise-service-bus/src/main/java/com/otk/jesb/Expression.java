package com.otk.jesb;

import java.util.Collections;
import java.util.List;
import com.otk.jesb.Function.Precompiler;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.compiler.CompiledFunction;
import com.otk.jesb.compiler.CompiledFunction.FunctionCallError;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.util.ClassUtils;
import xy.reflect.ui.util.ReflectionUIUtils;

public class Expression<T> {

	private static final Precompiler PRECOMPILER = new Precompiler() {
		@Override
		public String apply(String s) {
			return "return " + s + ";";
		}

		@Override
		public int unprecompileFunctionBodyPosition(int position, String precompiledFunctionBody) {
			return (position != -1) ? (position - "return ".length()) : position;
		}
	};

	private Class<T> valueClass;
	private Function internalFunction;
	private boolean dynamic = false;

	private List<VariableDeclaration> variableDeclarations = Collections.emptyList();
	private List<Variable> variables = Collections.emptyList();

	public Expression(Class<T> valueClass) {
		this.valueClass = valueClass;
	}

	public String get() {
		return (internalFunction != null) ? internalFunction.getFunctionBody() : null;
	}

	public void set(String string) {
		internalFunction = (string != null) ? new Function(string) : null;
		dynamic = true;
	}

	public List<VariableDeclaration> getVariableDeclarations() {
		return variableDeclarations;
	}

	public void setVariableDeclarations(List<VariableDeclaration> variableDeclarations) {
		this.variableDeclarations = variableDeclarations;
	}

	public List<Variable> getVariables() {
		return variables;
	}

	public void setVariables(List<Variable> variables) {
		this.variables = variables;
	}

	public boolean isDynamic() {
		return dynamic;
	}

	public void setDynamic(boolean dynamic) {
		if (!dynamic) {
			represent(evaluate());
		}
		this.dynamic = dynamic;
	}

	public T evaluate() {
		if (internalFunction == null) {
			return null;
		}
		try {
			CompiledFunction compiledFunction = compile();
			return valueClass.cast(compiledFunction.call(getVariables()));
		} catch (CompilationError | FunctionCallError e) {
			throw new UnexpectedError(e);
		}
	}

	public void represent(T t) {
		if (t == null) {
			internalFunction = null;
			return;
		}
		if (ClassUtils.isPrimitiveClassOrWrapper(valueClass)) {
			internalFunction = new Function(ReflectionUIUtils.primitiveToString(t));
		} else if (valueClass == String.class) {
			internalFunction = new Function('"' + MiscUtils.escapeJavaString((String) t) + '"');
		} else if (valueClass.isEnum()) {
			internalFunction = new Function(valueClass.getName() + "." + t.toString());
		} else {
			throw new UnexpectedError();
		}
		dynamic = false;
	}

	public CompiledFunction compile() throws CompilationError {
		return internalFunction.getCompiledVersion(PRECOMPILER, getVariableDeclarations(), valueClass);
	}

}
