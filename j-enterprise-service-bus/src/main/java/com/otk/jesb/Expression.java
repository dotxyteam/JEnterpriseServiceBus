package com.otk.jesb;

import java.util.List;
import com.otk.jesb.Function.Precompiler;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.compiler.CompiledFunction;
import com.otk.jesb.compiler.CompiledFunction.FunctionCallError;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.util.ClassUtils;
import xy.reflect.ui.util.ReflectionUIUtils;

/**
 * This class allows to specify a dynamic value in the form of a simple
 * expression.
 * 
 * @author olitank
 *
 */
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

	public boolean isDynamic() {
		return dynamic;
	}

	public void setDynamic(boolean dynamic, List<VariableDeclaration> variableDeclarations, List<Variable> variables) {
		if (!dynamic) {
			represent(evaluate(variableDeclarations, variables));
		}
		this.dynamic = dynamic;
	}

	public T evaluate(List<VariableDeclaration> variableDeclarations, List<Variable> variables) {
		if (internalFunction == null) {
			return null;
		}
		try {
			CompiledFunction compiledFunction = compile(variableDeclarations);
			return valueClass.cast(compiledFunction.call(variables));
		} catch (CompilationError | FunctionCallError e) {
			throw new PotentialError(e);
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

	public CompiledFunction compile(List<VariableDeclaration> variableDeclarations) throws CompilationError {
		return internalFunction.getCompiledVersion(PRECOMPILER, variableDeclarations, valueClass);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (dynamic ? 1231 : 1237);
		result = prime * result + ((internalFunction == null) ? 0 : internalFunction.hashCode());
		result = prime * result + ((valueClass == null) ? 0 : valueClass.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		@SuppressWarnings("rawtypes")
		Expression other = (Expression) obj;
		if (dynamic != other.dynamic)
			return false;
		if (internalFunction == null) {
			if (other.internalFunction != null)
				return false;
		} else if (!internalFunction.equals(other.internalFunction))
			return false;
		if (valueClass == null) {
			if (other.valueClass != null)
				return false;
		} else if (!valueClass.equals(other.valueClass))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return get();
	}

}
