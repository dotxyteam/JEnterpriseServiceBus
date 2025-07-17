package com.otk.jesb;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;

import java.util.Map;
import java.util.HashMap;

public class JdtReturnTypeAnalyzer {

    public static void main(String[] args) {
        String sourceCode = "public class MyClass {\n" + 
        		"                public Object getSomething() {\n" + 
        		"                    return Integer.valueOf(\"5!\");\n" + 
        		"                }\n" + 
        		"            }";

        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(sourceCode.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(true);

        parser.setUnitName("MyClass.java");

        // Mock environment to enable type resolution
        parser.setEnvironment(null, null, null, true);
        parser.setBindingsRecovery(true);
        parser.setCompilerOptions(new HashMap<>(JavaCore.getOptions()));

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                Block body = node.getBody();
                if (body != null) {
                    body.accept(new ASTVisitor() {
                        @Override
                        public boolean visit(ReturnStatement returnStmt) {
                            Expression expr = returnStmt.getExpression();
                            if (expr != null) {
                                ITypeBinding typeBinding = expr.resolveTypeBinding();
                                if (typeBinding != null) {
                                    System.out.println("Méthode: " + node.getName()
                                        + " retourne : " + typeBinding.getQualifiedName());
                                } else {
                                    System.out.println("Type non résolu.");
                                }
                            }
                            return super.visit(returnStmt);
                        }
                    });
                }
                return super.visit(node);
            }
        });
    }
}
