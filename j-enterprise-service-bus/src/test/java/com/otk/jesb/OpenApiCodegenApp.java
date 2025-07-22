package com.otk.jesb;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.openapitools.codegen.ClientOptInput;

public class OpenApiCodegenApp {
    public static void main(String[] args) {
        CodegenConfigurator configurator = new CodegenConfigurator();

        configurator.setInputSpec("https://api.apis.guru/v2/specs/amazonaws.com/budgets/2016-10-20/openapi.json"); // chemin vers ton fichier OpenAPI
        configurator.setGeneratorName("java"); // "jaxrs-cxf" pour un serveur
        configurator.setOutputDir("tmp/openapi-gen-test"); // dossier de sortie

        // options supplémentaires possibles
        configurator.addAdditionalProperty("library", "resttemplate"); // ou okhttp, feign, etc.
        configurator.addAdditionalProperty("dateLibrary", "java8");
        configurator.addAdditionalProperty("useSpringBoot3", "true");

        ClientOptInput clientOptInput = configurator.toClientOptInput();
        DefaultGenerator generator = new DefaultGenerator();
        generator.opts(clientOptInput).generate();
    }
}
