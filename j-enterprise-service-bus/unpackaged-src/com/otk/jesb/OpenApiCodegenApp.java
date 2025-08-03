package com.otk.jesb;

import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.openapitools.codegen.ClientOptInput;

public class OpenApiCodegenApp {
	public static void main(String[] args) {
		{
			CodegenConfigurator configurator = new CodegenConfigurator();

			configurator.setInputSpec("tmp/petstore.openapi.json");
			configurator.setGeneratorName("java");
			configurator.setOutputDir("tmp/openapi-gen-test");
			configurator.addAdditionalProperty("apiPackage", "my.client");
			configurator.addAdditionalProperty("modelPackage", "my.client");
			configurator.addAdditionalProperty("dateLibrary", "java8");
			configurator.addAdditionalProperty("library", "resttemplate");
			ClientOptInput clientOptInput = configurator.toClientOptInput();
			DefaultGenerator generator = new DefaultGenerator();
			generator.opts(clientOptInput).generate();
		}
		{
			CodegenConfigurator configurator = new CodegenConfigurator();

			configurator.setInputSpec("tmp/petstore.openapi.json");
			configurator.setGeneratorName("jaxrs-cxf");
			configurator.setOutputDir("tmp/openapi-gen-test");
			configurator.addAdditionalProperty("apiPackage", "my.service");
			configurator.addAdditionalProperty("modelPackage", "my.service");
			configurator.addAdditionalProperty("dateLibrary", "java8");
			ClientOptInput clientOptInput = configurator.toClientOptInput();
			DefaultGenerator generator = new DefaultGenerator();
			generator.opts(clientOptInput).generate();
		} 
	}
}
