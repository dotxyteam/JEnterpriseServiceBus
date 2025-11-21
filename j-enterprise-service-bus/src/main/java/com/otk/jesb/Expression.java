package com.otk.jesb;

import java.util.Arrays;
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

	private Class<T> resultClass;
	private Function internalFunction;
	private boolean dynamic = false;

	public Expression(Class<T> resultClass) {
		this.resultClass = resultClass;
	}

	public Expression(String string, Class<T> resultClass) {
		this(resultClass);
		set(string);
	}

	public String get() {
		return (internalFunction != null) ? internalFunction.getFunctionBody() : null;
	}

	public void set(String string) {
		internalFunction = (string != null) ? new Function(string) : null;
		dynamic = true;
	}

	public Class<T> getResultClass() {
		return resultClass;
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
			CompiledFunction<T> compiledFunction = compile(variableDeclarations);
			return resultClass.cast(compiledFunction.call(variables));
		} catch (CompilationError | FunctionCallError e) {
			throw new PotentialError(e);
		}
	}

	public void represent(T t) {
		if (t == null) {
			internalFunction = null;
			return;
		}
		if (ClassUtils.isPrimitiveClassOrWrapper(resultClass)) {
			internalFunction = new Function(ReflectionUIUtils.primitiveToString(t));
		} else if (resultClass == String.class) {
			internalFunction = new Function('"' + MiscUtils.escapeJavaString((String) t) + '"');
		} else if (resultClass.isEnum()) {
			internalFunction = new Function(resultClass.getName() + "." + t.toString());
		} else {
			throw new UnexpectedError();
		}
		dynamic = false;
	}

	public CompiledFunction<T> compile(VariableDeclaration... variableDeclarations) throws CompilationError {
		return compile(Arrays.asList(variableDeclarations));
	}

	public CompiledFunction<T> compile(List<VariableDeclaration> variableDeclarations) throws CompilationError {
		return internalFunction.getCompiledVersion(PRECOMPILER, variableDeclarations, resultClass);
	}

	public static <T> T evaluateObjectMemberSelection(Object object, String selection, Class<T> resultClass)
			throws CompilationError, FunctionCallError {
		String variableName = "var" + MiscUtils.getDigitalUniqueIdentifier();
		return new Expression<T>(variableName + "." + selection, resultClass)
				.compile(new VariableDeclaration.BasicVariableDeclaration(variableName, object.getClass()))
				.call(new Variable.BasicVariable(variableName, object));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (dynamic ? 1231 : 1237);
		result = prime * result + ((internalFunction == null) ? 0 : internalFunction.hashCode());
		result = prime * result + ((resultClass == null) ? 0 : resultClass.hashCode());
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
		if (resultClass == null) {
			if (other.resultClass != null)
				return false;
		} else if (!resultClass.equals(other.resultClass))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return get();
	}

}
