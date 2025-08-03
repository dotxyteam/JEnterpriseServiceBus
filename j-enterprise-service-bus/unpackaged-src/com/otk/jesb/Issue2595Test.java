package com.otk.jesb;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParseStart;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.compiler.CompiledFunction;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.util.IOUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static com.github.javaparser.Providers.provider;

public class Issue2595Test {

	public static void main(String[] args) {

		String testClassSrc = "package test; public class Test{ public long i = 0; }";
		Class<?> testClass;
		try {
			testClass = MiscUtils.IN_MEMORY_COMPILER.compile("test.Test", testClassSrc);
		} catch (CompilationError e1) {
			e1.printStackTrace();
			return;
		}
		CompiledFunction cf;
		try {
			cf = CompiledFunction.get("int a = 2; int b = a; String s = \"azerty\"; return new java.util.ArrayList<Long>(java.util.Arrays.asList(s.length() + a + b + t.i));",
					Arrays.asList(new VariableDeclaration() {

						@Override
						public Class<?> getVariableType() {
							return testClass;
						}

						@Override
						public String getVariableName() {
							return "t";
						}
					}), Object.class);
		} catch (CompilationError e) {
			System.out.println(e.getStartPosition());
			System.out.println(e.getEndPosition());
			System.out.println(e.getSourceCode());
			System.out.println(e.getMessage());
			return;
		}
		try {
			IOUtils.write(new File("tmp/src/" + testClass.getName().replace(".", "/") + ".java"), testClassSrc, false);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		// TypeSolver typeSolver = new CombinedTypeSolver(new ReflectionTypeSolver(),
		// new JavaParserTypeSolver(new File("tmp/src/")));
		TypeSolver typeSolver = new CombinedTypeSolver(new ClassLoaderTypeSolver(ClassLoader.getSystemClassLoader()),
				new ClassLoaderTypeSolver(MiscUtils.IN_MEMORY_COMPILER.getClassLoader()));
		ParserConfiguration configuration = new ParserConfiguration()
				.setSymbolResolver(new JavaSymbolSolver(typeSolver));
		JavaParser javaParser = new JavaParser(configuration);
		ParseResult<CompilationUnit> result = javaParser.parse(ParseStart.COMPILATION_UNIT,
				provider(cf.getFunctionClassSource()));
		assumeTrue(result.isSuccessful());
		assumeTrue(result.getResult().isPresent());

		CompilationUnit cu = result.getResult().get();
//        System.out.println(cu);

		List<ReturnStmt> returnStatements = cu.findAll(ReturnStmt.class);
		assumeFalse(returnStatements.isEmpty());
		for (int i = returnStatements.size() - 1; i >= 0; i--) {
			ReturnStmt returnStatement = returnStatements.get(i);
			System.out.println();
			System.out.println("returnStatement = " + returnStatement);
			System.out.println("returnStatement.calculateResolvedType() = "
					+ returnStatement.getExpression().get().calculateResolvedType());
		}

	}

	private static void assumeFalse(boolean b) {
		if (b) {
			throw new AssertionError();
		}
	}

	private static void assumeTrue(boolean b) {
		if (!b) {
			throw new AssertionError();
		}
	}

}