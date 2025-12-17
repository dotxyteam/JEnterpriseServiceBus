package com.otk.jesb.util;

import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;

import com.otk.jesb.Session;
import com.otk.jesb.activation.ActivationHandler;
import com.otk.jesb.instantiation.InstanceBuilder;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.solution.Asset;
import com.otk.jesb.solution.AssetVisitor;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.solution.Plan.ExecutionError;

public class SolutionUtils {

	public static <T extends Asset> T findAsset(Solution solution, Class<T> assetClass, String assetName) {
		return assetClass.cast(
				findAsset(solution, asset -> assetClass.isInstance(asset) && asset.getName().contentEquals(assetName)));
	}

	public static Asset findAsset(Solution solution, Function<Asset, Boolean> assetPredicate) {
		Asset[] result = new Asset[1];
		solution.visitContents(new AssetVisitor() {
			@Override
			public boolean visitAsset(Asset asset) {
				if (assetPredicate.apply(asset)) {
					result[0] = asset;
					return false;
				}
				return true;
			}
		});
		return result[0];
	}

	public static void activatePlan(Plan plan, Session session) throws Exception {
		plan.getActivator().initializeAutomaticTrigger(new ActivationHandler() {
			@Override
			public Object trigger(Object planInput) throws ExecutionError {
				return plan.execute(planInput, Plan.ExecutionInspector.DEFAULT,
						new Plan.ExecutionContext(session, plan));
			}
		}, session.getSolutionInstance());
	}

	public static void deactivatePlan(Plan plan, Session session) throws Exception {
		plan.getActivator().finalizeAutomaticTrigger(session.getSolutionInstance());
	}

	public static Object executePlan(Plan plan, Session session, Consumer<InstanceBuilder> inputSetup)
			throws Exception {
		InstanceBuilder inputBuilder = new InstanceBuilder(
				Accessor.returning(plan.getActivator().getInputClass(session.getSolutionInstance()).getName()));
		if (inputSetup != null) {
			inputSetup.accept(inputBuilder);
		}
		Object planInput = inputBuilder.build(new InstantiationContext(Collections.emptyList(), Collections.emptyList(),
				session.getSolutionInstance()));
		Object planOutput = plan.execute(planInput, Plan.ExecutionInspector.DEFAULT,
				new Plan.ExecutionContext(session, plan));
		return planOutput;
	}

}
