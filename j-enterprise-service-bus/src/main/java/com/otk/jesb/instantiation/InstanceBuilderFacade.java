package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.Plan.ExecutionContext;
import com.otk.jesb.Plan.ValidationContext;
import com.otk.jesb.instantiation.Function.CompilationContext;
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

public class InstanceBuilderFacade implements Facade {

	private Facade parent;
	private InstanceBuilder underlying;

	private InitializationCaseFacade util;

	public InstanceBuilderFacade(Facade parent, InstanceBuilder underlying) {
		this.parent = parent;
		this.underlying = underlying;
		util = new InitializationCaseFacade(null, null, underlying) {

			@Override
			protected boolean mustHaveParameterFacadeLocally(IParameterInfo parameterInfo) {
				if(isParameterInitializedInChildSwitch(parameterInfo)) {
					return false;
				}
				return true;
			}

			@Override
			protected boolean mustHaveFieldFacadeLocally(IFieldInfo fieldInfo) {
				if(isFieldInitializedInChildSwitch(fieldInfo)) {
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

	public List<String> getConstructorSignatureChoices() {
		List<String> result = new ArrayList<String>();
		ITypeInfo typeInfo = getTypeInfo();
		for (IMethodInfo constructor : typeInfo.getConstructors()) {
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

	public List<Facade> collectInitializerFacades(EvaluationContext context) {
		return util.collectInitializerFacades(context);
	}

	public CompilationContext findFunctionCompilationContext(Function function, ValidationContext validationContext) {
		return util.findFunctionCompilationContext(function, validationContext);
	}

	@Override
	public String toString() {
		return underlying.toString();
	}

}