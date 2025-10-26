package com.otk.jesb;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.otk.jesb.activation.ActivatorMetadata;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.resource.ResourceMetadata;
import com.otk.jesb.solution.CompositeStep.CompositeStepMetadata;
import com.otk.jesb.ui.GUI;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.info.custom.InfoCustomizations;
import xy.reflect.ui.info.custom.InfoCustomizations.EnumerationCustomization;
import xy.reflect.ui.info.custom.InfoCustomizations.ListCustomization;
import xy.reflect.ui.info.custom.InfoCustomizations.TypeCustomization;
import xy.reflect.ui.util.Listener;

public class SplitInfoCustomizations {

	private final static File WORKING_DIRECTORY = new File("tmp/SplitInfoCustomizations");

	public static void main(String[] args) throws Exception {
		if(WORKING_DIRECTORY.exists()) {
			MiscUtils.delete(WORKING_DIRECTORY);
		}
		MiscUtils.createDirectory(WORKING_DIRECTORY);
		InfoCustomizations globalCustomizations = GUI.INSTANCE
				.obtainSubCustomizer(GUI.GLOBAL_EXCLUSIVE_CUSTOMIZATIONS).getInfoCustomizations();
		for (OperationMetadata<?> metadata : MiscUtils.BUILTIN_OPERATION_METADATAS) {
			String switchIdentifier = MiscUtils.inferOperationClass(metadata.getOperationBuilderClass()).getName();
			List<String> mainTypeNames = Arrays.asList(
					MiscUtils.inferOperationClass(metadata.getOperationBuilderClass()).getName(),
					metadata.getOperationBuilderClass().getName());
			extractAnsSaveSpecificCustomizations(globalCustomizations, switchIdentifier, mainTypeNames);
		}
		for (ActivatorMetadata metadata : MiscUtils.BUILTIN_ACTIVATOR__METADATAS) {
			String switchIdentifier = metadata.getActivatorClass().getName();
			List<String> mainTypeNames = Arrays.asList(metadata.getActivatorClass().getName());
			extractAnsSaveSpecificCustomizations(globalCustomizations, switchIdentifier, mainTypeNames);
		}
		for (ResourceMetadata metadata : MiscUtils.BUILTIN_RESOURCE_METADATAS) {
			String switchIdentifier = metadata.getResourceClass().getName();
			List<String> mainTypeNames = Arrays.asList(metadata.getResourceClass().getName());
			extractAnsSaveSpecificCustomizations(globalCustomizations, switchIdentifier, mainTypeNames);
		}
		for (CompositeStepMetadata metadata : MiscUtils.BUILTIN_COMPOSITE_STEP_METADATAS) {
			String switchIdentifier = metadata.getCompositeStepClass().getName();
			List<String> mainTypeNames = Arrays.asList(metadata.getCompositeStepClass().getName());
			extractAnsSaveSpecificCustomizations(globalCustomizations, switchIdentifier, mainTypeNames);
		}
		globalCustomizations.saveToFile(new File(WORKING_DIRECTORY, "jesb.icu"), new Listener<String>() {
			@Override
			public void handle(String message) {
				System.out.println(message);
			}
		});

	}

	private static void extractAnsSaveSpecificCustomizations(InfoCustomizations globalCustomizations,
			String switchIdentifier, List<String> mainTypeNames) throws Exception {
		boolean emptyCustomizations = true;
		InfoCustomizations specificCustomizations = new InfoCustomizations();
		for (TypeCustomization tc : new ArrayList<TypeCustomization>(globalCustomizations.getTypeCustomizations())) {
			for (String mainTypeName : mainTypeNames) {
				if (tc.getTypeName().contains(mainTypeName)) {
					globalCustomizations.getTypeCustomizations().remove(tc);
					specificCustomizations.getTypeCustomizations().add(tc);
					emptyCustomizations = false;
					break;
				}
			}
		}
		for (ListCustomization lc : new ArrayList<ListCustomization>(globalCustomizations.getListCustomizations())) {
			for (String mainTypeName : mainTypeNames) {
				if ((lc.getItemTypeName() != null) && lc.getItemTypeName().contains(mainTypeName)) {
					globalCustomizations.getListCustomizations().remove(lc);
					specificCustomizations.getListCustomizations().add(lc);
					emptyCustomizations = false;
					break;
				}
			}
		}
		for (EnumerationCustomization ec : new ArrayList<EnumerationCustomization>(
				globalCustomizations.getEnumerationCustomizations())) {
			for (String mainTypeName : mainTypeNames) {
				if (ec.getEnumerationTypeName().contains(mainTypeName)) {
					globalCustomizations.getEnumerationCustomizations().remove(ec);
					specificCustomizations.getEnumerationCustomizations().add(ec);
					emptyCustomizations = false;
					break;
				}
			}
		}
		if (!emptyCustomizations) {
			specificCustomizations.saveToFile(new File(WORKING_DIRECTORY, switchIdentifier + "-jesb.icu"),
					new Listener<String>() {
						@Override
						public void handle(String message) {
							System.out.println(message);
						}
					});
		}
	}

}
