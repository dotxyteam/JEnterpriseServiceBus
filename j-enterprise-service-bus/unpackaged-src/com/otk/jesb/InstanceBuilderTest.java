package com.otk.jesb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import com.otk.jesb.InstanceBuilderTest.TreeActivity.Builder;
import com.otk.jesb.Plan.ExecutionContext;
import com.otk.jesb.Plan.ValidationContext;
import com.otk.jesb.activity.Activity;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;
import com.otk.jesb.instantiation.EvaluationContext;
import com.otk.jesb.instantiation.Function;
import com.otk.jesb.instantiation.Function.CompilationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;

import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;

public class InstanceBuilderTest {

	public static void main(String[] args) throws Exception {
		Plan plan = new Plan();
		Step step = new Step(new TreeActivity.Metadata());
		plan.getSteps().add(step);
		TreeActivity.Builder builder = (Builder) step.getActivityBuilder();
		GUI.INSTANCE.getReflectionUI()
				.getTypeInfo(new JavaTypeInfoSource(GUI.INSTANCE.getReflectionUI(), Plan.class, null))
				.onFormVisibilityChange(plan, true);
		GUI.INSTANCE.getReflectionUI()
				.getTypeInfo(new JavaTypeInfoSource(GUI.INSTANCE.getReflectionUI(), Step.class, null))
				.onFormVisibilityChange(step, true);
		GUI.INSTANCE.openObjectDialog(null, builder.instanceBuilder);
		GUI.INSTANCE.openObjectDialog(null, builder.instanceBuilder
				.build(new EvaluationContext(new Plan.ExecutionContext(plan, step, Collections.emptyList()), null)));
	}

	public static class TreeActivity implements Activity {

		public TreeActivity() {
		}

		public TreeActivity(int intValue, String stringValue, EnumValue enumValue, ArrayList<String> stringList) {
			super();
			this.intValue = intValue;
			this.stringValue = stringValue;
			this.enumValue = enumValue;
			this.stringList = stringList;
		}

		public TreeActivity(TreeActivity firstChild, TreeActivity[] otherChildrenArray,
				HashMap<String, TreeActivity> childByString, HashMap<Integer, Boolean> booleanByInteger) {
			super();
			this.firstChild = firstChild;
			this.otherChildrenArray = otherChildrenArray;
			this.childByString = childByString;
			this.booleanByInteger = booleanByInteger;
		}

		public enum EnumValue {
			ENUM_ITEM1, ENUM_ITEM2, ENUM_ITEM3
		};

		private int intValue;
		private String stringValue;
		private EnumValue enumValue;
		private ArrayList<String> stringList;
		private TreeActivity firstChild;
		private TreeActivity[] otherChildrenArray;
		private HashMap<String, TreeActivity> childByString;
		private HashMap<Integer, Boolean> booleanByInteger;

		public int getIntValue() {
			return intValue;
		}

		public void setIntValue(int intValue) {
			this.intValue = intValue;
		}

		public String getStringValue() {
			return stringValue;
		}

		public void setStringValue(String stringValue) {
			this.stringValue = stringValue;
		}

		public EnumValue getEnumValue() {
			return enumValue;
		}

		public void setEnumValue(EnumValue enumValue) {
			this.enumValue = enumValue;
		}

		public ArrayList<String> getStringList() {
			return stringList;
		}

		public void setStringList(ArrayList<String> stringList) {
			this.stringList = stringList;
		}

		public TreeActivity getFirstChild() {
			return firstChild;
		}

		public void setFirstChild(TreeActivity firstChild) {
			this.firstChild = firstChild;
		}

		public TreeActivity[] getOtherChildrenArray() {
			return otherChildrenArray;
		}

		public void setOtherChildrenArray(TreeActivity[] otherChildrenArray) {
			this.otherChildrenArray = otherChildrenArray;
		}

		public HashMap<String, TreeActivity> getChildByString() {
			return childByString;
		}

		public void setChildByString(HashMap<String, TreeActivity> childByString) {
			this.childByString = childByString;
		}

		public HashMap<Integer, Boolean> getBooleanByInteger() {
			return booleanByInteger;
		}

		public void setBooleanByInteger(HashMap<Integer, Boolean> booleanByInteger) {
			this.booleanByInteger = booleanByInteger;
		}

		@Override
		public Object execute() throws Exception {
			return "Test !";
		}

		public static class Builder implements ActivityBuilder {
			RootInstanceBuilder instanceBuilder = new RootInstanceBuilder("testInput", TreeActivity.class.getName());

			@Override
			public Activity build(ExecutionContext context) throws Exception {
				return (TreeActivity) instanceBuilder.build(new EvaluationContext(context, null));
			}

			@Override
			public Class<?> getActivityResultClass() {
				return String.class;
			}

			@Override
			public CompilationContext findFunctionCompilationContext(Function function,
					ValidationContext validationContext) {
				return instanceBuilder.getFacade().findFunctionCompilationContext(function, validationContext);
			}

		}

		public static class Metadata implements ActivityMetadata {

			@Override
			public String getCategoryName() {
				return "Test";
			}

			@Override
			public String getActivityTypeName() {
				return "Tree Test";
			}

			@Override
			public ResourcePath getActivityIconImagePath() {
				return null;
			}

			@Override
			public Class<? extends ActivityBuilder> getActivityBuilderClass() {
				return Builder.class;
			}
		}

	}

}
