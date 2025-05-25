package com.otk.jesb;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import com.otk.jesb.compiler.InMemoryCompiler;

public class ReturnTypeResolver {
    private final InMemoryCompiler compiler;
    private final Map<String, Class<?>> variableDeclarations = new HashMap<>();
    private final Map<String, ITypeBinding> localVariableTypes = new HashMap<>();
    
    public ReturnTypeResolver(InMemoryCompiler compiler) {
        this.compiler = compiler;
    }
    
    /**
     * Déclare une variable avec son type pour la résolution
     */
    public void setVariableDeclaration(String variableName, Class<?> type) {
        variableDeclarations.put(variableName, type);
    }
    
    /**
     * Résout le type de retour d'un corps de méthode complexe avec JDT
     */
    public Class<?> resolve(String methodBody) throws ResolveException {
        try {
            // Créer le code complet de méthode pour l'analyse JDT
            String fullMethodCode = createFullMethodCode(methodBody);
            
            // Parser avec JDT
            ASTParser parser = ASTParser.newParser(AST.JLS8);
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            parser.setSource(fullMethodCode.toCharArray());
            parser.setResolveBindings(true);
            parser.setBindingsRecovery(true);
            
            // Configuration du classpath pour inclure les classes compilées
            configureClasspath(parser);
            
            ASTNode root = parser.createAST(null);
            
            // Analyser l'AST pour trouver le type de retour
            ReturnTypeVisitor visitor = new ReturnTypeVisitor();
            root.accept(visitor);
            
            return visitor.getReturnType();
            
        } catch (Exception e) {
            throw new ResolveException("Error resolving return type with JDT", e);
        }
    }
    
    /**
     * Crée le code complet de classe/méthode pour l'analyse JDT
     */
    private String createFullMethodCode(String methodBody) {
        StringBuilder code = new StringBuilder();
        
        // Imports nécessaires
        code.append("import java.lang.*;\n\n");
        
        // Classe conteneur
        code.append("public class TempResolverClass {\n");
        
        // Déclarations de champs pour les variables connues
        for (Map.Entry<String, Class<?>> entry : variableDeclarations.entrySet()) {
            code.append("    private ").append(entry.getValue().getCanonicalName())
                .append(" ").append(entry.getKey()).append(";\n");
        }
        
        // Méthode temporaire contenant le corps à analyser
        code.append("    public Object tempMethod() {\n");
        code.append("        ").append(methodBody).append("\n");
        code.append("    }\n");
        code.append("}\n");
        
        return code.toString();
    }
    
    /**
     * Configure le classpath pour JDT incluant les classes compilées en mémoire
     */
    private void configureClasspath(ASTParser parser) {
        try {
            // Classpath de base (JRE)
            String[] classpathEntries = {
                System.getProperty("java.home") + "/lib/rt.jar"
            };
            
            // Sources path (vide pour notre cas)
            String[] sourcepathEntries = new String[0];
            String[] encodings = new String[0];
            
            parser.setEnvironment(classpathEntries, sourcepathEntries, encodings, true);
            
            // Options de compilation
            Map<String, String> options = JavaCore.getOptions();
            options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
            options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
            options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
            parser.setCompilerOptions(options);
            
        } catch (Exception e) {
            // Fallback sans configuration spéciale
        }
    }
    
    /**
     * Visiteur AST pour analyser le type de retour
     */
    private class ReturnTypeVisitor extends ASTVisitor {
        private Class<?> returnType = void.class;
        
        @Override
        public boolean visit(VariableDeclarationStatement node) {
            // Analyser les déclarations de variables locales
            try {
                ITypeBinding typeBinding = node.getType().resolveBinding();
                if (typeBinding != null) {
                    String typeName = typeBinding.getQualifiedName();
                    Class<?> variableType = resolveClassFromBinding(typeBinding);
                    
                    // Stocker les variables locales pour résolution ultérieure
                    node.fragments().forEach(fragment -> {
                        if (fragment instanceof org.eclipse.jdt.core.dom.VariableDeclarationFragment) {
                            org.eclipse.jdt.core.dom.VariableDeclarationFragment varFrag = 
                                (org.eclipse.jdt.core.dom.VariableDeclarationFragment) fragment;
                            String varName = varFrag.getName().getIdentifier();
                            variableDeclarations.put(varName, variableType);
                        }
                    });
                }
            } catch (Exception e) {
                // Ignorer les erreurs de résolution pour les variables locales
            }
            return true;
        }
        
        @Override
        public boolean visit(ReturnStatement node) {
            Expression returnExpression = node.getExpression();
            if (returnExpression != null) {
                try {
                    returnType = resolveExpressionType(returnExpression);
                } catch (Exception e) {
                    // Fallback vers analyse manuelle si JDT échoue
                    returnType = fallbackResolveExpression(returnExpression);
                }
            }
            return false; // Ne pas visiter les enfants
        }
        
        public Class<?> getReturnType() {
            return returnType;
        }
    }
    
    /**
     * Résout le type d'une expression en utilisant JDT
     */
    private Class<?> resolveExpressionType(Expression expression) throws ResolveException {
        ITypeBinding binding = expression.resolveTypeBinding();
        if (binding != null) {
            return resolveClassFromBinding(binding);
        }
        
        // Fallback vers analyse manuelle
        return fallbackResolveExpression(expression);
    }
    
    /**
     * Résolution manuelle quand JDT ne peut pas résoudre
     */
    private Class<?> fallbackResolveExpression(Expression expression) {
        if (expression instanceof InfixExpression) {
            return resolveBinaryOperation((InfixExpression) expression);
        }
        
        if (expression instanceof MethodInvocation) {
            return resolveMethodInvocation((MethodInvocation) expression);
        }
        
        if (expression instanceof SimpleName) {
            SimpleName name = (SimpleName) expression;
            String varName = name.getIdentifier();
            if (variableDeclarations.containsKey(varName)) {
                return variableDeclarations.get(varName);
            }
        }
        
        // Types littéraux par défaut
        String exprString = expression.toString();
        if (exprString.startsWith("\"") && exprString.endsWith("\"")) {
            return String.class;
        }
        if (exprString.matches("\\d+")) {
            return int.class;
        }
        if (exprString.matches("\\d+\\.\\d+")) {
            return double.class;
        }
        if ("true".equals(exprString) || "false".equals(exprString)) {
            return boolean.class;
        }
        
        return Object.class; // Type par défaut
    }
    
    /**
     * Résout le type d'une opération binaire (addition, etc.)
     */
    private Class<?> resolveBinaryOperation(InfixExpression expression) {
        InfixExpression.Operator operator = expression.getOperator();
        
        // Pour les opérations arithmétiques, déterminer le type de promotion
        if (operator == InfixExpression.Operator.PLUS ||
            operator == InfixExpression.Operator.MINUS ||
            operator == InfixExpression.Operator.TIMES ||
            operator == InfixExpression.Operator.DIVIDE) {
            
            Class<?> leftType = fallbackResolveExpression(expression.getLeftOperand());
            Class<?> rightType = fallbackResolveExpression(expression.getRightOperand());
            
            return promoteNumericTypes(leftType, rightType);
        }
        
        return Object.class;
    }
    
    /**
     * Résout le type d'un appel de méthode
     */
    private Class<?> resolveMethodInvocation(MethodInvocation invocation) {
        try {
            IMethodBinding methodBinding = invocation.resolveMethodBinding();
            if (methodBinding != null) {
                ITypeBinding returnTypeBinding = methodBinding.getReturnType();
                return resolveClassFromBinding(returnTypeBinding);
            }
        } catch (Exception e) {
            // Fallback manuel si nécessaire
        }
        
        // Analyse manuelle basique
        String methodName = invocation.getName().getIdentifier();
        if ("length".equals(methodName)) {
            return int.class;
        }
        
        return Object.class;
    }
    
    /**
     * Promotion des types numériques selon les règles Java
     */
    private Class<?> promoteNumericTypes(Class<?> type1, Class<?> type2) {
        // Règles de promotion Java
        if (type1 == double.class || type2 == double.class) return double.class;
        if (type1 == float.class || type2 == float.class) return float.class;
        if (type1 == long.class || type2 == long.class) return long.class;
        return int.class;
    }
    
    /**
     * Convertit un ITypeBinding JDT vers une Class Java
     */
    private Class<?> resolveClassFromBinding(ITypeBinding binding) throws ResolveException {
        if (binding == null) {
            return Object.class;
        }
        
        String qualifiedName = binding.getQualifiedName();
        
        // Types primitifs
        switch (qualifiedName) {
            case "int": return int.class;
            case "long": return long.class;
            case "double": return double.class;
            case "float": return float.class;
            case "boolean": return boolean.class;
            case "char": return char.class;
            case "byte": return byte.class;
            case "short": return short.class;
            case "void": return void.class;
        }
        
        // Types de base
        if ("java.lang.String".equals(qualifiedName)) return String.class;
        if ("java.lang.Object".equals(qualifiedName)) return Object.class;
        if ("java.lang.Integer".equals(qualifiedName)) return Integer.class;
        if ("java.lang.Long".equals(qualifiedName)) return Long.class;
        if ("java.lang.Double".equals(qualifiedName)) return Double.class;
        if ("java.lang.Boolean".equals(qualifiedName)) return Boolean.class;
        
        // Chercher dans les classes compilées
        Class<?> compiledClass = compiler.getCompiledClass(qualifiedName);
        if (compiledClass != null) {
            return compiledClass;
        }
        
        // Essayer de charger la classe normalement
        try {
            return Class.forName(qualifiedName);
        } catch (ClassNotFoundException e) {
            throw new ResolveException("Cannot resolve class: " + qualifiedName, e);
        }
    }
    
    /**
     * Exception pour les erreurs de résolution de type
     */
    public static class ResolveException extends Exception {
        public ResolveException(String message) {
            super(message);
        }
        
        public ResolveException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}