package com.otk.jesb;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.otk.jesb.activation.ActivatorMetadata;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.resource.ResourceMetadata;
import com.otk.jesb.solution.Asset;
import com.otk.jesb.ui.GUI;

import xy.reflect.ui.ReflectionUI;
import xy.reflect.ui.info.field.GetterFieldInfo;
import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.iterable.IListTypeInfo;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;

public class StaticCodeTests {

	@Test
	public void checkFieldTypes() throws Exception{
		List<Class<?>> classesToCheck = new ArrayList<Class<?>>();
		classesToCheck.addAll(GUI.BUILTIN_OPERATION_METADATAS.stream()
				.map(OperationMetadata::getOperationBuilderClass).collect(Collectors.toList()));
		classesToCheck.addAll(GUI.BUILTIN_COMPOSITE_STEP_OPERATION_METADATAS.stream()
				.map(OperationMetadata::getOperationBuilderClass).collect(Collectors.toList()));
		classesToCheck.addAll(GUI.BUILTIN_RESOURCE_METADATAS.stream().map(ResourceMetadata::getResourceClass)
				.collect(Collectors.toList()));
		classesToCheck.addAll(GUI.BUILTIN_ACTIVATOR__METADATAS.stream().map(ActivatorMetadata::getActivatorClass)
				.collect(Collectors.toList()));
		for (Class<?> classToCheck : classesToCheck) {
			ITypeInfo typeInfo = ReflectionUI.getDefault().getTypeInfo(new JavaTypeInfoSource(classToCheck, null));
			for (IFieldInfo fieldInfo : typeInfo.getFields()) {
				if (fieldInfo.isGetOnly()) {
					continue;
				}
				if (fieldInfo.getType().getName().equals(Reference.class.getName())) {
					continue;
				}
				if (fieldInfo.getType().getName().equals(RootInstanceBuilder.class.getName())) {
					continue;
				}
				if (fieldInfo.getType().getName().equals(Variant.class.getName())) {
					continue;
				}
				if (fieldInfo.getType() instanceof IListTypeInfo) {
					continue;
				}
				if (((GetterFieldInfo) fieldInfo).getJavaGetterMethod()
						.equals(Asset.class.getDeclaredMethod("getName"))) {
					continue;
				}
				if (((GetterFieldInfo) fieldInfo).getJavaGetterMethod()
						.equals(Asset.class.getDeclaredMethod("getNote"))) {
					continue;
				}
				System.out
						.println(typeInfo.getName() + "#" + fieldInfo.getName() + ": " + fieldInfo.getType().getName());
			}
		}
	}

}
