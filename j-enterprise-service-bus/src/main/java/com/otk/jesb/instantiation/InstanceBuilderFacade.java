package com.otk.jesb.instantiation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.meta.TypeInfoProvider;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.util.InstantiationUtils;
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

	public InstanceBuilderFacade(Facade parent, InstanceBuilder underlying, Solution solutionInstance) {
		super(solutionInstance);
		this.parent = parent;
		this.underlying = underlying;
		util = new InitializationCaseFacade(null, null, underlying, solutionInstance) {

			@Override
			public boolean isConcrete() {
				return true;
			}

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
				return new FieldInitializerFacade(InstanceBuilderFacade.this, fieldName, solutionInstance);
			}

			@Override
			protected ListItemInitializerFacade createListItemInitializerFacade(int index) {
				return new ListItemInitializerFacade(InstanceBuilderFacade.this, index, solutionInstance);
			}

			@Override
			protected ParameterInitializerFacade createParameterInitializerFacade(int parameterPosition) {
				return new ParameterInitializerFacade(InstanceBuilderFacade.this, parameterPosition, solutionInstance);
			}

			@Override
			protected InitializationSwitchFacade createInitializationSwitchFacade(
					InitializationSwitch initializationSwitch) {
				return new InitializationSwitchFacade(InstanceBuilderFacade.this, initializationSwitch,
						solutionInstance);
			}

			@Override
			public InstanceBuilderFacade getCurrentInstanceBuilderFacade() {
				return InstanceBuilderFacade.this;
			}

			@Override
			protected InstantiationContext createInstantiationContextForChildren(InstantiationContext context) {
				return new InstantiationContext(context, InstanceBuilderFacade.this);
			}

			@Override
			public void validate(boolean recursively, List<VariableDeclaration> variableDeclarations)
					throws ValidationError {
				if (!isConcrete()) {
					return;
				}
				if (recursively) {
					for (Facade facade : getChildren()) {
						try {
							facade.validate(recursively, variableDeclarations);
						} catch (ValidationError e) {
							throw new ValidationError("Failed to validate '" + facade.toString() + "'", e);
						}
					}
				}
			}

		};
	}

	@Override
	public List<VariableDeclaration> getAdditionalVariableDeclarations(InstantiationFunction function,
			List<VariableDeclaration> baseVariableDeclarations) {
		if (function != null) {
			throw new UnexpectedError();
		}
		if (parent != null) {
			return parent.getAdditionalVariableDeclarations(null, baseVariableDeclarations);
		}
		return Collections.emptyList();
	}

	@Override
	public Class<?> getFunctionReturnType(InstantiationFunction function,
			List<VariableDeclaration> baseVariableDeclarations) {
		throw new UnsupportedOperationException();
	}

	public Facade findInstantiationFunctionParentFacade(InstantiationFunction function) {
		return util.findInstantiationFunctionParentFacade(function);
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
		for (IMethodInfo constructor : InstantiationUtils.listSortedConstructors(typeInfo)) {
			result.add(constructor.getSignature());
		}
		return result;
	}

	@Override
	public InstanceBuilder getUnderlying() {
		return underlying;
	}

	public ITypeInfo getTypeInfo() {
		String actualTypeName = underlying.computeActualTypeName(InstantiationUtils.getAncestorInstanceBuilders(parent),
				solutionInstance);
		ITypeInfo result = TypeInfoProvider.getTypeInfo(actualTypeName, solutionInstance);
		if (result instanceof IListTypeInfo) {
			if (parent instanceof FieldInitializerFacade) {
				FieldInitializerFacade listFieldInitializerFacade = (FieldInitializerFacade) parent;
				IFieldInfo listFieldInfo = listFieldInitializerFacade.getFieldInfo();
				result = TypeInfoProvider.getTypeInfo(actualTypeName, listFieldInfo, solutionInstance);
			}
			if (parent instanceof ParameterInitializerFacade) {
				ParameterInitializerFacade listParameterInitializerFacade = (ParameterInitializerFacade) parent;
				InstanceBuilderFacade parentInstanceBuilderFacade = listParameterInitializerFacade
						.getCurrentInstanceBuilderFacade();
				AbstractConstructorInfo parentInstanceConstructor = InstantiationUtils.getConstructorInfo(
						parentInstanceBuilderFacade.getTypeInfo(),
						parentInstanceBuilderFacade.getSelectedConstructorSignature());
				result = TypeInfoProvider.getTypeInfo(actualTypeName, parentInstanceConstructor,
						listParameterInitializerFacade.getParameterPosition(), solutionInstance);
			}
		}
		if (result instanceof IMapEntryTypeInfo) {
			if (parent instanceof ListItemInitializerFacade) {
				ListItemInitializerFacade listItemInitializerFacade = (ListItemInitializerFacade) parent;
				result = (StandardMapEntryTypeInfo) listItemInitializerFacade.getItemTypeInfo();
			}
		}
		return result;
	}

	@Override
	public List<Facade> getChildren() {
		return util.getChildren();
	}

	public List<Facade> collectLiveInitializerFacades(InstantiationContext context) {
		return util.collectLiveInitializerFacades(context);
	}

	public void copyUnderlying() {
		if (!canCopyUnderlying()) {
			throw new UnexpectedError();
		}
		InstanceBuilderFacade.underlyingClipboard = MiscUtils.copy(underlying,
				solutionInstance.getRuntime().getXstream());
	}

	public void pasteUnderlying() {
		if (!canPasteUnderlying()) {
			throw new UnexpectedError();
		}
		parent.setConcrete(true);
		transferValuesToUnderlying(InstanceBuilderFacade.underlyingClipboard);
		InstanceBuilderFacade.underlyingClipboard = null;
	}

	public boolean canCopyUnderlying() {
		if (getUnderlying().getDynamicTypeNameAccessor() != null) {
			return false;
		}
		return true;
	}

	public boolean canPasteUnderlying() {
		if (InstanceBuilderFacade.underlyingClipboard == null) {
			return false;
		}
		if (getUnderlying().getDynamicTypeNameAccessor() != null) {
			return false;
		}
		return true;
	}

	public boolean canAccessSource() {
		if (canCopyUnderlying()) {
			return true;
		}
		if (canPasteUnderlying()) {
			return true;
		}
		return false;
	}

	public String getSource() {
		if (!canAccessSource()) {
			throw new UnexpectedError();
		}
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			MiscUtils.serialize(underlying, output, solutionInstance.getRuntime().getXstream());
		} catch (IOException e) {
			throw new UnexpectedError(e);
		}
		return output.toString();
	}

	public void setSource(String source) {
		if (!canAccessSource()) {
			throw new UnexpectedError();
		}
		ByteArrayInputStream input = new ByteArrayInputStream(source.getBytes());
		InstanceBuilder deserialized;
		try {
			deserialized = (InstanceBuilder) MiscUtils.deserialize(input, solutionInstance.getRuntime().getXstream());
		} catch (IOException e) {
			throw new UnexpectedError(e);
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
	public void validate(boolean recursively, List<VariableDeclaration> variableDeclarations) throws ValidationError {
		if (!isConcrete()) {
			return;
		}
		ITypeInfo typeInfo;
		try {
			typeInfo = getTypeInfo();
		} catch (Throwable t) {
			throw new ValidationError("Failed to load '" + getTypeName() + "' type", t);
		}
		String selectedConstructorSignature = getSelectedConstructorSignature();
		IMethodInfo constructor = InstantiationUtils.getConstructorInfo(typeInfo, selectedConstructorSignature);
		if (constructor == null) {
			if (selectedConstructorSignature == null) {
				throw new ValidationError(
						"Cannot create '" + typeInfo.getName() + "' instance: No constructor available");
			} else {
				throw new ValidationError("Cannot create '" + typeInfo.getName()
						+ "' instance: Constructor not found: '" + selectedConstructorSignature + "'");
			}
		}
		util.validate(recursively, variableDeclarations);
	}

	@Override
	public String toString() {
		return underlying.toString();
	}

}