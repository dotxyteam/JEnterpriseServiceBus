package com.otk.jesb.instantiation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.Plan.ExecutionContext;
import com.otk.jesb.Plan.ValidationContext;
import com.otk.jesb.meta.TypeInfoProvider;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.method.AbstractConstructorInfo;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.parameter.IParameterInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.iterable.IListTypeInfo;
import xy.reflect.ui.info.type.iterable.map.IMapEntryTypeInfo;
import xy.reflect.ui.info.type.iterable.map.StandardMapEntryTypeInfo;

public class InstanceBuilderFacade extends Facade {

	private static InstanceBuilder underlyingClipboard;

	private Facade parent;
	private InstanceBuilder underlying;

	private InitializationCaseFacade util;

	public InstanceBuilderFacade(Facade parent, InstanceBuilder underlying) {
		this.parent = parent;
		this.underlying = underlying;
		util = new InitializationCaseFacade(null, null, underlying) {

			@Override
			protected boolean mustHaveParameterFacadeLocally(IParameterInfo parameterInfo) {
				if (isParameterInitializedInChildSwitch(parameterInfo)) {
					return false;
				}
				return true;
			}

			@Override
			protected boolean mustHaveFieldFacadeLocally(IFieldInfo fieldInfo) {
				if (isFieldInitializedInChildSwitch(fieldInfo)) {
					return false;
				}
				return true;
			}

			@Override
			protected boolean mustHaveListItemFacadesLocally() {
				return true;
			}

			@Override
			protected FieldInitializerFacade createFieldInitializerFacade(String fieldName) {
				return new FieldInitializerFacade(InstanceBuilderFacade.this, fieldName);
			}

			@Override
			protected ListItemInitializerFacade createListItemInitializerFacade(int index) {
				return new ListItemInitializerFacade(InstanceBuilderFacade.this, index);
			}

			@Override
			protected ParameterInitializerFacade createParameterInitializerFacade(int parameterPosition) {
				return new ParameterInitializerFacade(InstanceBuilderFacade.this, parameterPosition);
			}

			@Override
			protected InitializationSwitchFacade createInitializationSwitchFacade(
					InitializationSwitch initializationSwitch) {
				return new InitializationSwitchFacade(InstanceBuilderFacade.this, initializationSwitch);
			}

			@Override
			public InstanceBuilderFacade getCurrentInstanceBuilderFacade() {
				return InstanceBuilderFacade.this;
			}

			@Override
			protected EvaluationContext createEvaluationContextForChildren(ExecutionContext executionContext) {
				return new EvaluationContext(executionContext, InstanceBuilderFacade.this);
			}

		};
	}

	@Override
	public String express() {
		return null;
	}

	@Override
	public Facade getParent() {
		return parent;
	}

	@Override
	public boolean isConcrete() {
		if (parent != null) {
			return parent.isConcrete();
		}
		return true;
	}

	@Override
	public void setConcrete(boolean b) {
		if (parent != null) {
			parent.setConcrete(b);
		}
	}

	public String getTypeName() {
		return underlying.getTypeName();
	}

	public void setTypeName(String typeName) {
		underlying.setTypeName(typeName);
	}

	public String getSelectedConstructorSignature() {
		return underlying.getSelectedConstructorSignature();
	}

	public void setSelectedConstructorSignature(String selectedConstructorSignature) {
		underlying.setSelectedConstructorSignature(selectedConstructorSignature);
		setConcrete(true);
	}

	public List<String> getConstructorSignatureOptions() {
		List<String> result = new ArrayList<String>();
		ITypeInfo typeInfo = getTypeInfo();
		for (IMethodInfo constructor : MiscUtils.listSortedConstructors(typeInfo)) {
			result.add(constructor.getSignature());
		}
		return result;
	}

	@Override
	public InstanceBuilder getUnderlying() {
		return underlying;
	}

	public ITypeInfo getTypeInfo() {
		String actualTypeName = underlying
				.computeActualTypeName(MiscUtils.getAncestorStructureInstanceBuilders(parent));
		ITypeInfo result = TypeInfoProvider.getTypeInfo(actualTypeName);
		if (result instanceof IListTypeInfo) {
			if (parent instanceof FieldInitializerFacade) {
				FieldInitializerFacade listFieldInitializerFacade = (FieldInitializerFacade) parent;
				IFieldInfo listFieldInfo = listFieldInitializerFacade.getFieldInfo();
				result = TypeInfoProvider.getTypeInfo(actualTypeName, listFieldInfo);
			}
			if (parent instanceof ParameterInitializerFacade) {
				ParameterInitializerFacade listParameterInitializerFacade = (ParameterInitializerFacade) parent;
				InstanceBuilderFacade parentInstanceBuilderFacade = listParameterInitializerFacade
						.getCurrentInstanceBuilderFacade();
				AbstractConstructorInfo parentInstanceConstructor = MiscUtils.getConstructorInfo(
						parentInstanceBuilderFacade.getTypeInfo(),
						parentInstanceBuilderFacade.getSelectedConstructorSignature());
				result = TypeInfoProvider.getTypeInfo(actualTypeName, parentInstanceConstructor,
						listParameterInitializerFacade.getParameterPosition());
			}
		}
		if (result instanceof IMapEntryTypeInfo) {
			if (parent instanceof ListItemInitializerFacade) {
				ListItemInitializerFacade listItemInitializerFacade = (ListItemInitializerFacade) parent;
				result = (StandardMapEntryTypeInfo) listItemInitializerFacade.getItemType();
			}
		}
		return result;
	}

	@Override
	public List<Facade> getChildren() {
		return util.getChildren();
	}

	public List<Facade> collectLiveInitializerFacades(EvaluationContext context) {
		return util.collectLiveInitializerFacades(context);
	}

	public CompilationContext findFunctionCompilationContext(Function function, ValidationContext validationContext) {
		return util.findFunctionCompilationContext(function, validationContext);
	}

	public void copyUnderlying() {
		InstanceBuilderFacade.underlyingClipboard = MiscUtils.copy(underlying);
	}

	public void pasteUnderlying() {
		parent.setConcrete(true);
		transferValuesToUnderlying(InstanceBuilderFacade.underlyingClipboard);
		InstanceBuilderFacade.underlyingClipboard = null;
	}

	public boolean canPasteUnderlying() {
		if (InstanceBuilderFacade.underlyingClipboard == null) {
			return false;
		}
		if (parent == null) {
			return false;
		}
		return true;
	}

	public String getSource() {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			MiscUtils.serialize(underlying, output);
		} catch (IOException e) {
			throw new AssertionError(e);
		}
		return output.toString();
	}

	public void setSource(String source) {
		ByteArrayInputStream input = new ByteArrayInputStream(source.getBytes());
		InstanceBuilder deserialized;
		try {
			deserialized = (InstanceBuilder) MiscUtils.deserialize(input);
		} catch (IOException e) {
			throw new AssertionError(e);
		}
		transferValuesToUnderlying(deserialized);
	}

	private void transferValuesToUnderlying(InstanceBuilder source) {
		underlying.setDynamicTypeNameAccessor(source.getDynamicTypeNameAccessor());
		underlying.setTypeName(source.getTypeName());
		underlying.setSelectedConstructorSignature(source.getSelectedConstructorSignature());
		underlying.setParameterInitializers(source.getParameterInitializers());
		underlying.setFieldInitializers(source.getFieldInitializers());
		underlying.setListItemInitializers(source.getListItemInitializers());
		underlying.setInitializationSwitches(source.getInitializationSwitches());
	}

	@Override
	public String toString() {
		return underlying.toString();
	}

}