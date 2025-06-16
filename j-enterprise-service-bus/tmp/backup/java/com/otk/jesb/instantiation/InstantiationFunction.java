package com.otk.jesb.instantiation;

import static com.github.javaparser.Providers.provider;

import java.util.List;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParseStart;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.otk.jesb.Function;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.compiler.CompiledFunction;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.UpToDate;
import com.otk.jesb.util.UpToDate.VersionAccessException;

import xy.reflect.ui.info.type.ITypeInfo;

public class InstantiationFunction extends Function {

	private Function returnTypeUtil = new Function();
	private UpToDate<ITypeInfo> upToDateGuessedReturnTypeInfo = new UpToDate<ITypeInfo>() {

		@Override
		protected Object retrieveLastVersionIdentifier() {
			CompiledFunction compiledFunction = (CompiledFunction) getCustomValue();
			return compiledFunction.getFunctionClass();
		}

		@Override
		protected ITypeInfo obtainLatest(Object versionIdentifier) throws VersionAccessException {
			CompiledFunction compiledFunction = (CompiledFunction) getCustomValue();
			TypeSolver typeSolver = new CombinedTypeSolver(
					new ClassLoaderTypeSolver(ClassLoader.getSystemClassLoader()),
					new ClassLoaderTypeSolver(MiscUtils.IN_MEMORY_COMPILER.getClassLoader()));
			ParserConfiguration configuration = new ParserConfiguration()
					.setSymbolResolver(new JavaSymbolSolver(typeSolver));
			JavaParser javaParser = new JavaParser(configuration);
			ParseResult<CompilationUnit> result = javaParser.parse(ParseStart.COMPILATION_UNIT,
					provider(compiledFunction.getFunctionClassSource()));
			if (!result.isSuccessful()) {
				throw new UnexpectedError();
			}
			if (!result.getResult().isPresent()) {
				throw new UnexpectedError();
			}
			CompilationUnit compilationUnit = result.getResult().get();
			List<ReturnStmt> returnStatements = compilationUnit.findAll(ReturnStmt.class);
			ResolvedType resolvedType = returnStatements.get(returnStatements.size() - 1).getExpression().get()
					.calculateResolvedType();
			return MiscUtils.getInfoFromResolvedType(resolvedType);
		}

	};

	public InstantiationFunction() {
	}

	public InstantiationFunction(String functionBody) {
		super(functionBody);
		returnTypeUtil.setFunctionBody(functionBody);
	}

	@Override
	public void setFunctionBody(String functionBody) {
		super.setFunctionBody(functionBody);
		returnTypeUtil.setFunctionBody(functionBody);
	}

	public ITypeInfo guessReturnTypeInfo(java.util.function.Function<String, String> precompiler,
			List<VariableDeclaration> variableDeclarations) throws CompilationError {
		CompiledFunction compiledFunction = returnTypeUtil.getCompiledVersion(precompiler, variableDeclarations,
				Object.class);
		upToDateGuessedReturnTypeInfo.setCustomValue(compiledFunction);
		try {
			return upToDateGuessedReturnTypeInfo.get();
		} catch (VersionAccessException e) {
			return null;
		}
	}

}