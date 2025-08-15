package com.otk.jesb;

import java.util.ArrayList;
import java.util.List;

public abstract class Session implements AutoCloseable {

	public static final Session NO_SESSION = new Session() {

		@Override
		public void terminate() {
		}

		@Override
		public void initiate() {
		}
	};

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
