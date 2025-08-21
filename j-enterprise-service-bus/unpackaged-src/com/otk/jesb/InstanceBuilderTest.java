package com.otk.jesb;

import java.io.PrintStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.SwingUtilities;

import com.otk.jesb.Structure.ClassicStructure;
import com.otk.jesb.Structure.SimpleElement;
import com.otk.jesb.activation.builtin.Operate;
import com.otk.jesb.InstanceBuilderTest.Tree.Builder;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.ui.GUI;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.instantiation.InstanceBuilder;
import com.otk.jesb.instantiation.InstantiationFunction;
import com.otk.jesb.instantiation.ParameterInitializer;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.StepCrossing;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;

import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;

public class InstanceBuilderTest {

	public static void main(String[] args) throws Exception {
		Plan plan = new Plan();
		Operate activator = new Operate();
		plan.setActivator(activator);
		ClassicStructure planInputStructure = new ClassicStructure();
		{
			SimpleElement element = new Structure.SimpleElement();
			element.setName("tree");
			element.setTypeNameOrAlias(Tree.class.getName());
			planInputStructure.getElements().add(element);
			activator.setInputStructure(planInputStructure);
		}
		Step step = new Step(new Tree.Metadata());
		plan.getSteps().add(step);
		ClassicStructure planOutputStructure = new ClassicStructure();
		{
			SimpleElement element = new Structure.SimpleElement();
			element.setName("tree");
			element.setTypeNameOrAlias(Tree.class.getName());
			planOutputStructure.getElements().add(element);
			activator.setOutputStructure(planOutputStructure);
		}
		((InstanceBuilder) ((ParameterInitializer) plan.getOutputBuilder().getRootInstantiationNode())
				.getParameterValue()).getParameterInitializers()
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
				Tree.Builder builder = (Builder) step.getOperationBuilder();
				GUI.INSTANCE.openObjectDialog(null, builder.instanceBuilder);
				Object output;
				try (Session session = Session.createDummySession()) {
					Object input = plan.getActivator().getInputClass().getConstructor(Tree.class)
							.newInstance(inputTree);
					output = plan.execute(input, new ExecutionInspector() {

						@Override
						public void beforeOperation(StepCrossing stepCrossing) {
						}

						@Override
						public void afterOperation(StepCrossing stepCrossing) {
						}

						@Override
						public boolean isExecutionInterrupted() {
							return false;
						}

						@Override
						public void logInformation(String message) {
							log(message, "INFORMATION", System.out);
						}

						@Override
						public void logError(String message) {
							log(message, "ERROR", System.err);
						}

						@Override
						public void logWarning(String message) {
							log(message, "WARNING", System.err);
						}

						private void log(String message, String levelName, PrintStream printStream) {
							String date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
									.format(MiscUtils.now());
							String formattedMessage = String.format("%1$s [%2$s] %3$s - %4$s", date,
									Thread.currentThread().getName(), levelName, message);
							printStream.println(formattedMessage);
						}
					}, new ExecutionContext(session, plan));
				} catch (Throwable t) {
					GUI.INSTANCE.handleException(null, t);
					return;
				}
				GUI.INSTANCE.openObjectDialog(null, output);
			}
		});
	}

	public static class Tree implements Operation {

		public Tree() {
		}

		public Tree(int intValue, boolean booleanValue, String stringValue, EnumValue enumValue, boolean[] booleanArray,
				ArrayList<String> stringList) {
			super();
			this.intValue = intValue;
			this.booleanValue = booleanValue;
			this.stringValue = stringValue;
			this.enumValue = enumValue;
			this.booleanArray = booleanArray;
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
		private boolean booleanValue;
		private String stringValue;
		private EnumValue enumValue;
		private boolean[] booleanArray;
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

		public boolean isBooleanValue() {
			return booleanValue;
		}

		public void setBooleanValue(boolean booleanValue) {
			this.booleanValue = booleanValue;
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

		public boolean[] getBooleanArray() {
			return booleanArray;
		}

		public void setBooleanArray(boolean[] booleanArray) {
			this.booleanArray = booleanArray;
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

		public static class Builder implements OperationBuilder<Tree> {
			RootInstanceBuilder instanceBuilder = new RootInstanceBuilder("testInput", Tree.class.getName());

			@Override
			public Tree build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
				return (Tree) instanceBuilder.build(new InstantiationContext(context.getVariables(),
						context.getPlan().getValidationContext(context.getCurrentStep()).getVariableDeclarations()));
			}

			@Override
			public Class<?> getOperationResultClass(Plan currentPlan, Step currentStep) {
				return Tree.class;
			}

			@Override
			public void validate(boolean recursively, Plan plan, Step step) throws ValidationError {
			}

		}

		public static class Metadata implements OperationMetadata<Tree> {

			@Override
			public String getCategoryName() {
				return "Test";
			}

			@Override
			public String getOperationTypeName() {
				return "Tree Test";
			}

			@Override
			public ResourcePath getOperationIconImagePath() {
				return null;
			}

			@Override
			public Class<? extends OperationBuilder<Tree>> getOperationBuilderClass() {
				return Builder.class;
			}
		}

	}

}
