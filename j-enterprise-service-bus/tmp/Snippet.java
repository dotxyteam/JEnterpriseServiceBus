package com.otk.jesb;

import java.lang.reflect.Array;
import java.util.Objects;
import java.util.function.Predicate;

import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.compiler.InMemoryCompiler;

public class Snippet {

	public static void main(String[] args) throws CompilationError {
		InMemoryCompiler compiler = new InMemoryCompiler();
		compiler.compile("Test", "public class Test{ public int i = 0; }");
		ReturnTypeResolver r = compiler.getReturnTypeResolver(compiler);
		r.setVariableDeclaration("t", Test.class);
		assert r.resolve("return t.i;") == int.class;
	}

	public class Test{
		public int i = 0;
	}
	
	
	public Object test(Test t) {
		return t.i;
	}

}
