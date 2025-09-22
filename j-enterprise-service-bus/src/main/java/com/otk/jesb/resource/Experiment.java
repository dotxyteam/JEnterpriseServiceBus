package com.otk.jesb.resource;

import javax.swing.SwingUtilities;

import com.otk.jesb.AbstractExperiment;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.builtin.JDBCConnection;
import com.otk.jesb.ui.GUI;

public class Experiment extends AbstractExperiment implements AutoCloseable {

	public static void main(String... args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try (Experiment experiment = new Experiment(new JDBCConnection())) {
					GUI.INSTANCE.openObjectFrame(experiment);
				} catch (Exception e) {
					throw new UnexpectedError(e);
				}
			}
		});
	}

	private Resource mainResource;

	public Experiment(Resource resource) {
		this.mainResource = resource;
	}

	public Resource getMainResource() {
		return mainResource;
	}

	public void setMainResource(Resource mainResource) {
		this.mainResource = mainResource;
	}

}
