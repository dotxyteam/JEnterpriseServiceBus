package com.otk.jesb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.compiler.CompiledFunction;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.util.Pair;
import com.otk.jesb.util.UpToDate;
import com.otk.jesb.util.UpToDate.VersionAccessException;

/**
 * This class allows to specify a dynamic value in source code form.
 * 
 * @author olitank
 *
 */
public class Function {
	private static final String SOLUTION_INSTANCE_KEY = Function.class.getName() + ".SOLUTION_INSTANCE_KEY";
	private static final String PRECOMPILED_FUNCTION_BODY_KEY = Function.class.getName()
			+ ".PRECOMPILED_FUNCTION_BODY_KEY";
	private static final String VARIABLE_DECLARATIONS_KEY = Function.class.getName() + ".VARIABLE_DECLARATIONS_KEY";
	private static final String RETURN_TYPE_KEY = Function.class.getName() + ".RETURN_TYPE_KEY";

	private String functionBody;
	@SuppressWarnings("rawtypes")
	private UpToDate<Map<String, Object>, CompiledFunction> upToDateCompiledVersion = new UpToDate<Map<String, Object>, CompiledFunction>() {

		@SuppressWarnings("unchecked")
		@Override
		protected Object retrieveLastVersionIdentifier(Map<String, Object> compilationData) {
			List<Object> result = new ArrayList<Object>();
			Solution solutionInstance = (Solution) compilationData.get(SOLUTION_INSTANCE_KEY);
			String precompiledFunctionBody = (String) compilationData.get(PRECOMPILED_FUNCTION_BODY_KEY);
			List<VariableDeclaration> variableDeclarations = (List<VariableDeclaration>) compilationData
					.get(VARIABLE_DECLARATIONS_KEY);
			Class<?> returnType = (Class<?>) compilationData.get(RETURN_TYPE_KEY);
			result.add(solutionInstance);
			result.add(precompiledFunctionBody);
			result.addAll(variableDeclarations.stream()
					.map(varDecl -> new Pair<String, Class<?>>(varDecl.getVariableName(), varDecl.getVariableType()))
					.collect(Collectors.toList()));
			result.add(returnType);
			return result;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected CompiledFunction<?> obtainLatest(Map<String, Object> compilationData, Object versionIdentifier)
				throws VersionAccessException {
			Solution solutionInstance = (Solution) compilationData.get(SOLUTION_INSTANCE_KEY);
			String precompiledFunctionBody = (String) compilationData.get(PRECOMPILED_FUNCTION_BODY_KEY);
			List<VariableDeclaration> variableDeclarations = (List<VariableDeclaration>) compilationData
					.get(VARIABLE_DECLARATIONS_KEY);
			Class<?> returnType = (Class<?>) compilationData.get(RETURN_TYPE_KEY);
			try {
				return CompiledFunction.get(precompiledFunctionBody, variableDeclarations, returnType,
						solutionInstance);
			} catch (CompilationError e) {
				throw new VersionAccessException(e);
			}
		}
	};

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

	@SuppressWarnings("unchecked")
	public <T> CompiledFunction<T> getCompiledVersion(Precompiler precompiler,
			List<VariableDeclaration> variableDeclarations, Class<T> functionReturnType) throws CompilationError {
		Map<String, Object> compilationData = new HashMap<String, Object>();
		compilationData.put(PRECOMPILED_FUNCTION_BODY_KEY,
				(precompiler != null) ? precompiler.apply(functionBody) : functionBody);
		compilationData.put(VARIABLE_DECLARATIONS_KEY, variableDeclarations);
		compilationData.put(RETURN_TYPE_KEY, functionReturnType);
		try {
			return upToDateCompiledVersion.get(compilationData);
		} catch (VersionAccessException e) {
			if (e.getCause() instanceof CompilationError) {
				String sourceCode = functionBody;
				int startPosition = (precompiler != null)
						? precompiler.unprecompileFunctionBodyPosition(
								((CompilationError) e.getCause()).getStartPosition(),
								((CompilationError) e.getCause()).getSourceCode())
						: ((CompilationError) e.getCause()).getStartPosition();
				int endPosition = (precompiler != null)
						? precompiler.unprecompileFunctionBodyPosition(
								((CompilationError) e.getCause()).getEndPosition(),
								((CompilationError) e.getCause()).getSourceCode())
						: ((CompilationError) e.getCause()).getEndPosition();
				throw new CompilationError(startPosition, endPosition, e.getMessage(), null, sourceCode, e);
			} else {
				throw new UnexpectedError(e);
			}
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((functionBody == null) ? 0 : functionBody.hashCode());
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
		Function other = (Function) obj;
		if (functionBody == null) {
			if (other.functionBody != null)
				return false;
		} else if (!functionBody.equals(other.functionBody))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Function [functionBody=" + functionBody + "]";
	}

	public static interface Precompiler {
		String apply(String functionBody);

		int unprecompileFunctionBodyPosition(int position, String precompiledFunctionBody);
	}

}