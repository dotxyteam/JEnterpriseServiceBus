package com.otk.jesb;

import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.SwingUtilities;

import com.otk.jesb.Structure.ClassicStructure;
import com.otk.jesb.Structure.SimpleElement;
import com.otk.jesb.activation.builtin.Operate;
import com.otk.jesb.InstanceBuilderTest.Tree.Builder;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.InstanceBuilder;
import com.otk.jesb.instantiation.InstantiationFunction;
import com.otk.jesb.instantiation.ParameterInitializer;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.solution.Solution;

import xy.reflect.ui.control.RenderingContext;
import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.info.type.ITypeInfo;
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
		((InstanceBuilder) ((ParameterInitializer) plan.getOutputBuilder()
				.getRootInstantiationNode(JESB.UI.INSTANCE.getSolutionInstance())).getParameterValue())
						.getParameterInitializers()
						.add(new ParameterInitializer(0, new InstantiationFunction("return " + step.getName() + ";")));
		JESB.UI.INSTANCE.getReflectionUI().getTypeInfo(new JavaTypeInfoSource(Plan.class, null))
				.onFormVisibilityChange(plan, true);
		JESB.UI.INSTANCE.getReflectionUI().getTypeInfo(new JavaTypeInfoSource(Step.class, null))
				.onFormVisibilityChange(step, true);
		JESB.UI.INSTANCE.getReflectionUI()
				.setRenderingContextThreadLocal(ThreadLocal.withInitial(() -> new RenderingContext(null) {
					@Override
					protected Object findObjectLocally(ITypeInfo type) {
						if (type.getName().equals(Solution.class.getName())) {
							return JESB.UI.INSTANCE.getSolutionInstance();
						}
						if (type.getName().equals(Plan.class.getName())) {
							return plan;
						}
						if (type.getName().equals(Step.class.getName())) {
							return step;
						}
						return null;
					}
				}));
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				Tree inputTree = new Tree();
				JESB.UI.INSTANCE.openObjectDialog(null, inputTree);
				Tree.Builder builder = (Builder) step.getOperationBuilder();
				JESB.UI.INSTANCE.openObjectDialog(null, builder.instanceBuilder);
				Object output;
				try (Session session = Session.openDummySession(JESB.UI.INSTANCE.getSolutionInstance())) {
					Object input = plan.getActivator().getInputClass(JESB.UI.INSTANCE.getSolutionInstance())
							.getConstructor(Tree.class).newInstance(inputTree);
					output = plan.execute(input, Plan.ExecutionInspector.DEFAULT, new ExecutionContext(session, plan));
				} catch (Throwable t) {
					JESB.UI.INSTANCE.handleException(null, t);
					return;
				}
				JESB.UI.INSTANCE.openObjectDialog(null, output);
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
		public Object execute(Solution solutionInstance) throws Exception {
			return this;
		}

		public static class Builder implements OperationBuilder<Tree> {
			RootInstanceBuilder instanceBuilder = new RootInstanceBuilder("testInput", Tree.class.getName());

			@Override
			public Tree build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
				Solution solutionInstance = context.getSession().getSolutionInstance();
				return (Tree) instanceBuilder.build(new InstantiationContext(context.getVariables(), context.getPlan()
						.getValidationContext(context.getCurrentStep(), solutionInstance).getVariableDeclarations(),
						solutionInstance));
			}

			@Override
			public Class<?> getOperationResultClass(Solution solutionInstance, Plan currentPlan, Step currentStep) {
				return Tree.class;
			}

			@Override
			public void validate(boolean recursively, Solution solutionInstance, Plan plan, Step step)
					throws ValidationError {
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
