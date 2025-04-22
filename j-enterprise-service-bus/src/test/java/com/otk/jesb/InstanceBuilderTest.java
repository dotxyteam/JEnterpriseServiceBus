package com.otk.jesb;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.SwingUtilities;

import com.otk.jesb.Plan;
import com.otk.jesb.Plan.ExecutionContext;
import com.otk.jesb.Plan.ExecutionInspector;
import com.otk.jesb.Step;
import com.otk.jesb.Structure.ClassicStructure;
import com.otk.jesb.Structure.SimpleElement;
import com.otk.jesb.InstanceBuilderTest.Tree.Builder;
import com.otk.jesb.activity.Activity;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;
import com.otk.jesb.instantiation.InstantiationFunctionCompilationContext;
import com.otk.jesb.instantiation.EvaluationContext;
import com.otk.jesb.ui.GUI;
import com.otk.jesb.instantiation.InstanceBuilder;
import com.otk.jesb.instantiation.InstantiationFunction;
import com.otk.jesb.instantiation.ParameterInitializer;
import com.otk.jesb.instantiation.RootInstanceBuilder;

import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;

public class InstanceBuilderTest {

	public static void main(String[] args) throws Exception {
		Plan plan = new Plan();
		ClassicStructure planInputStructure = new ClassicStructure();
		{
			SimpleElement element = new Structure.SimpleElement();
			element.setName("tree");
			element.setTypeName(Tree.class.getName());
			planInputStructure.getElements().add(element);
			plan.setInputStructure(planInputStructure);
		}
		Step step = new Step(new Tree.Metadata());
		plan.getSteps().add(step);
		ClassicStructure planOutputStructure = new ClassicStructure();
		{
			SimpleElement element = new Structure.SimpleElement();
			element.setName("tree");
			element.setTypeName(Tree.class.getName());
			planOutputStructure.getElements().add(element);
			plan.setOutputStructure(planOutputStructure);
		}
		((InstanceBuilder) ((ParameterInitializer) plan.getOutputBuilder().getRootInitializer()).getParameterValue())
				.getParameterInitializers()
				.add(new ParameterInitializer(0, new InstantiationFunction("return " + step.getName() + ";")));
		GUI.INSTANCE.getReflectionUI().getTypeInfo(new JavaTypeInfoSource(Plan.class, null))
				.onFormVisibilityChange(plan, true);
		GUI.INSTANCE.getReflectionUI().getTypeInfo(new JavaTypeInfoSource(Step.class, null))
				.onFormVisibilityChange(step, true);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				Tree inputTree = new Tree();
				GUI.INSTANCE.openObjectDialog(null, inputTree);
				Tree.Builder builder = (Builder) step.getActivityBuilder();
				GUI.INSTANCE.openObjectDialog(null, builder.instanceBuilder);
				Object output;
				try {
					Object input = plan.getInputClass().getConstructor(Tree.class).newInstance(inputTree);
					output = plan.execute(input);
				} catch (Throwable t) {
					GUI.INSTANCE.handleObjectException(null, t);
					return;
				}
				GUI.INSTANCE.openObjectDialog(null, output);
			}
		});
	}

	public static class Tree implements Activity {

		public Tree() {
		}

		public Tree(int intValue, String stringValue, EnumValue enumValue, ArrayList<String> stringList) {
			super();
			this.intValue = intValue;
			this.stringValue = stringValue;
			this.enumValue = enumValue;
			this.stringList = stringList;
		}

		public Tree(Tree firstChild, Tree[] otherChildrenArray, HashMap<String, Tree> childByString,
				HashMap<Integer, Boolean> booleanByInteger) {
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
		private Tree firstChild;
		private Tree[] otherChildrenArray;
		private HashMap<String, Tree> childByString;
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

		public Tree getFirstChild() {
			return firstChild;
		}

		public void setFirstChild(Tree firstChild) {
			this.firstChild = firstChild;
		}

		public Tree[] getOtherChildrenArray() {
			return otherChildrenArray;
		}

		public void setOtherChildrenArray(Tree[] otherChildrenArray) {
			this.otherChildrenArray = otherChildrenArray;
		}

		public HashMap<String, Tree> getChildByString() {
			return childByString;
		}

		public void setChildByString(HashMap<String, Tree> childByString) {
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
			return this;
		}

		public static class Builder implements ActivityBuilder {
			RootInstanceBuilder instanceBuilder = new RootInstanceBuilder("testInput", Tree.class.getName());

			@Override
			public Activity build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
				return (Tree) instanceBuilder.build(
						new EvaluationContext(context.getVariables(), null, context.getComilationContextProvider()));
			}

			@Override
			public Class<?> getActivityResultClass() {
				return Tree.class;
			}

			@Override
			public InstantiationFunctionCompilationContext findFunctionCompilationContext(
					InstantiationFunction function, Step currentStep, Plan currentPlan) {
				return instanceBuilder.getFacade().findFunctionCompilationContext(function,
						currentPlan.getValidationContext(currentStep).getVariableDeclarations());
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
