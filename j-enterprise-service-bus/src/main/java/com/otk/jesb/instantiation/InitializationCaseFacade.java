package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.parameter.IParameterInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.iterable.IListTypeInfo;

public class InitializationCaseFacade implements Facade {

	private InitializationSwitchFacade parent;
	private Function condition;
	private InitializationCase underlying;

	public InitializationCaseFacade(InitializationSwitchFacade parent, Function condition,
			InitializationCase underlying) {
		this.parent = parent;
		this.condition = condition;
		this.underlying = underlying;
	}

	public Function getCondition() {
		return condition;
	}

	@Override
	public List<Facade> getChildren() {
		List<Facade> result = new ArrayList<Facade>();
		InstanceBuilderFacade instanceBuilderFacade = getInstanceBuilderFacade();
		ITypeInfo typeInfo = instanceBuilderFacade.getTypeInfo();
		IMethodInfo constructor = MiscUtils.getConstructorInfo(typeInfo,
				instanceBuilderFacade.getSelectedConstructorSignature());
		if (constructor != null) {
			for (IParameterInfo parameterInfo : constructor.getParameters()) {
				if (isParameterInitializedInSwitch(parameterInfo)) {
					continue;
				}
				if (!mustHaveParameterFacade(parameterInfo)) {
					continue;
				}
				result.add(createParameterInitializerFacade(parameterInfo.getPosition()));
			}
		}
		if (typeInfo instanceof IListTypeInfo) {
			if (mustHaveListItemFacades()) {
				int i = 0;
				for (; i < underlying.getListItemInitializers().size();) {
					result.add(createListItemInitializerFacade(i));
					i++;
				}
				result.add(createListItemInitializerFacade(i));
			}
		} else {
			for (IFieldInfo fieldInfo : typeInfo.getFields()) {
				if (fieldInfo.isGetOnly()) {
					continue;
				}
				if (isFieldInitializedInSwitch(fieldInfo)) {
					continue;
				}
				if (!mustHaveFieldFacade(fieldInfo)) {
					continue;
				}
				result.add(createFieldInitializerFacade(fieldInfo.getName()));
			}
		}
		for (InitializationSwitch initializationSwitch : underlying.getInitializationSwitches()) {
			result.add(createInitializationSwitchFacade(initializationSwitch));
		}
		Collections.sort(result, new Comparator<Facade>() {
			List<Class<?>> CLASSES_ORDER = Arrays.asList(ParameterInitializerFacade.class, FieldInitializerFacade.class,
					ListItemInitializerFacade.class, InitializationSwitchFacade.class);

			@Override
			public int compare(Facade o1, Facade o2) {
				if (!o1.getClass().equals(o2.getClass())) {
					return Integer.valueOf(CLASSES_ORDER.indexOf(o1.getClass()))
							.compareTo(Integer.valueOf(CLASSES_ORDER.indexOf(o2.getClass())));
				}
				if ((o1 instanceof ParameterInitializerFacade) && (o2 instanceof ParameterInitializerFacade)) {
					ParameterInitializerFacade pif1 = (ParameterInitializerFacade) o1;
					ParameterInitializerFacade pif2 = (ParameterInitializerFacade) o2;
					return Integer.valueOf(pif1.getParameterPosition())
							.compareTo(Integer.valueOf(pif2.getParameterPosition()));
				} else if ((o1 instanceof FieldInitializerFacade) && (o2 instanceof FieldInitializerFacade)) {
					FieldInitializerFacade fif1 = (FieldInitializerFacade) o1;
					FieldInitializerFacade fif2 = (FieldInitializerFacade) o2;
					return fif1.getFieldInfo().getName().compareTo(fif2.getFieldInfo().getName());
				} else if ((o1 instanceof ListItemInitializerFacade) && (o2 instanceof ListItemInitializerFacade)) {
					ListItemInitializerFacade liif1 = (ListItemInitializerFacade) o1;
					ListItemInitializerFacade liif2 = (ListItemInitializerFacade) o2;
					return Integer.valueOf(liif1.getIndex()).compareTo(Integer.valueOf(liif2.getIndex()));
				} else if ((o1 instanceof InitializationSwitchFacade) && (o2 instanceof InitializationSwitchFacade)) {
					// InitializationSwitchFacade isf1 = (InitializationSwitchFacade) o1;
					// InitializationSwitchFacade isf2 = (InitializationSwitchFacade) o2;
					return 0;
				} else {
					throw new AssertionError();
				}

			}
		});
		return result;
	}

	protected InitializationSwitchFacade createInitializationSwitchFacade(InitializationSwitch initializationSwitch) {
		return new InitializationSwitchFacade(this, initializationSwitch);
	}

	protected FieldInitializerFacade createFieldInitializerFacade(String fieldName) {
		return new FieldInitializerFacade(this, fieldName);
	}

	protected ListItemInitializerFacade createListItemInitializerFacade(int index) {
		return new ListItemInitializerFacade(this, index);
	}

	protected ParameterInitializerFacade createParameterInitializerFacade(int parameterPosition) {
		return new ParameterInitializerFacade(this, parameterPosition);
	}

	protected boolean isFieldInitializedInSwitch(IFieldInfo fieldInfo) {
		for (InitializationSwitch initializationSwitch : underlying.getInitializationSwitches()) {
			InitializationSwitchFacade switchFacade = new InitializationSwitchFacade(this, initializationSwitch);
			InitializationCaseFacade defaultCaseFacade = new InitializationCaseFacade(switchFacade, null,
					switchFacade.getUnderlying().getDefaultInitializationCase());
			if (defaultCaseFacade.getUnderlying().getFieldInitializer(fieldInfo.getName()) != null) {
				return true;
			}
			if (defaultCaseFacade.isFieldInitializedInSwitch(fieldInfo)) {
				return true;
			}
		}
		return false;
	}

	protected boolean isParameterInitializedInSwitch(IParameterInfo parameterInfo) {
		for (InitializationSwitch initializationSwitch : underlying.getInitializationSwitches()) {
			InitializationSwitchFacade switchFacade = new InitializationSwitchFacade(this, initializationSwitch);
			InitializationCaseFacade defaultCaseFacade = new InitializationCaseFacade(switchFacade, null,
					switchFacade.getUnderlying().getDefaultInitializationCase());
			if (defaultCaseFacade.getUnderlying().getParameterInitializer(parameterInfo.getPosition(),
					parameterInfo.getType().getName()) != null) {
				return true;
			}
			if (defaultCaseFacade.isParameterInitializedInSwitch(parameterInfo)) {
				return true;
			}
		}
		return false;
	}

	protected boolean areListItemsInitializedInSwitch() {
		for (InitializationSwitch initializationSwitch : underlying.getInitializationSwitches()) {
			InitializationSwitchFacade switchFacade = new InitializationSwitchFacade(this, initializationSwitch);
			InitializationCaseFacade defaultCaseFacade = new InitializationCaseFacade(switchFacade, null,
					switchFacade.getUnderlying().getDefaultInitializationCase());
			if (defaultCaseFacade.getUnderlying().getListItemInitializers().size() > 0) {
				return true;
			}
			if (defaultCaseFacade.areListItemsInitializedInSwitch()) {
				return true;
			}
		}
		return false;
	}

	protected boolean mustHaveFieldFacade(IFieldInfo fieldInfo) {
		if (isFieldInitializedInSwitch(fieldInfo)) {
			return false;
		}
		if (isDefaultCaseFacade()) {
			if (underlying.getFieldInitializer(fieldInfo.getName()) != null) {
				return true;
			}
		} else {
			InitializationCaseFacade defaultCaseFacade = new InitializationCaseFacade(parent, null,
					parent.getUnderlying().getDefaultInitializationCase());
			if (defaultCaseFacade.mustHaveFieldFacade(fieldInfo)) {
				return true;
			}
			if (defaultCaseFacade.isFieldInitializedInSwitch(fieldInfo)) {
				return true;
			}
		}
		return false;
	}

	protected boolean mustHaveListItemFacades() {
		if (isDefaultCaseFacade()) {
			if (underlying.getListItemInitializers().size() > 0) {
				return true;
			}
			if (areListItemsInitializedInSwitch()) {
				return true;
			}
		} else {
			InitializationCaseFacade defaultCaseFacade = new InitializationCaseFacade(parent, null,
					parent.getUnderlying().getDefaultInitializationCase());
			if (defaultCaseFacade.mustHaveListItemFacades()) {
				return true;
			}
			if (defaultCaseFacade.areListItemsInitializedInSwitch()) {
				return true;
			}
		}
		return false;
	}

	protected boolean mustHaveParameterFacade(IParameterInfo parameterInfo) {
		if (isParameterInitializedInSwitch(parameterInfo)) {
			return false;
		}
		if (isDefaultCaseFacade()) {
			if (underlying.getParameterInitializer(parameterInfo.getPosition(),
					parameterInfo.getType().getName()) != null) {
				return true;
			}
		} else {
			InitializationCaseFacade defaultCaseFacade = new InitializationCaseFacade(parent, null,
					parent.getUnderlying().getDefaultInitializationCase());
			if (defaultCaseFacade.mustHaveParameterFacade(parameterInfo)) {
				return true;
			}
			if (defaultCaseFacade.isParameterInitializedInSwitch(parameterInfo)) {
				return true;
			}
		}
		return false;
	}

	protected InstanceBuilderFacade getInstanceBuilderFacade() {
		return (InstanceBuilderFacade) Facade.getAncestors(this).stream()
				.filter(f -> (f instanceof InstanceBuilderFacade)).findFirst().get();
	}

	protected boolean isDefaultCaseFacade() {
		return condition == null;
	}

	@Override
	public boolean isConcrete() {
		if (!parent.isConcrete()) {
			return false;
		}
		return true;
	}

	@Override
	public void setConcrete(boolean b) {
		if (b == isConcrete()) {
			return;
		}
		if (b) {
			if (!parent.isConcrete()) {
				parent.setConcrete(true);
			}
		}
	}

	@Override
	public InitializationCase getUnderlying() {
		return underlying;
	}

	public InitializationSwitchFacade getParent() {
		return parent;
	}

	@Override
	public String toString() {
		if (isDefaultCaseFacade()) {
			return "<Default>";
		} else {
			return "<Case "
					+ new ArrayList<InitializationCase>(
							parent.getUnderlying().getInitializationCaseByCondition().values()).indexOf(underlying)
					+ ">";
		}
	}
}