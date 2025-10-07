package com.otk.jesb;

import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.solution.Solution;

/**
 * This class represents the execution period of a {@link Solution}.
 * 
 * @author olitank
 *
 */
public abstract class Session implements AutoCloseable {

	public static Session createDummySession() {
		return new Session() {

			@Override
			public void terminate() {
			}

			@Override
			public void initiate() {
			}

			@Override
			public String toString() {
				return super.toString().replace(getClass().getName(), Session.class.getName() + ".Dummy");
			}
		};
	}

	public abstract void initiate();

	public abstract void terminate();

	private List<AutoCloseable> closables = new ArrayList<AutoCloseable>();

	public List<AutoCloseable> getClosables() {
		return closables;
	}

	@Override
	public void close() throws Exception {
		terminate();
		closables.stream().forEach(closable -> {
			try {
				closable.close();
			} catch (Exception e) {
				throw new PotentialError(e);
			}
		});
	}

}
