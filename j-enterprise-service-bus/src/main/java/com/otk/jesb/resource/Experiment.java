package com.otk.jesb.resource;

import javax.swing.SwingUtilities;

import com.otk.jesb.AbstractExperiment;
import com.otk.jesb.JESB;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.builtin.JDBCConnection;
import com.otk.jesb.solution.Solution;

public class Experiment extends AbstractExperiment implements AutoCloseable {

	public static void main(String... args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try (Experiment experiment = new Experiment(new JDBCConnection(), new Solution())) {
					JESB.UI.INSTANCE.openObjectFrame(experiment);
				} catch (Exception e) {
					throw new UnexpectedError(e);
				}
			}
		});
	}

	private Resource mainResource;

	public Experiment(Resource resource, Solution solutionInstance) {
		super(solutionInstance);
		this.mainResource = resource;
	}

	public Resource getMainResource() {
		return mainResource;
	}

	public void setMainResource(Resource mainResource) {
		this.mainResource = mainResource;
	}

}
