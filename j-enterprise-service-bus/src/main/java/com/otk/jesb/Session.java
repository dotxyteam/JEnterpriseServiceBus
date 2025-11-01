package com.otk.jesb;

import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.solution.Solution;
import com.otk.jesb.util.MiscUtils;

/**
 * This class represents the execution period of a {@link Solution}.
 * 
 * @author olitank
 *
 */
public abstract class Session implements AutoCloseable {

	public static Session openDummySession() {
		return new Session() {
			{
				open();
			}

			@Override
			protected void initiate() {
			}

			@Override
			protected void terminate() {
			}

			@Override
			public String toString() {
				return super.toString().replace(getClass().getName(), Session.class.getName() + ".Dummy");
			}
		};
	}

	protected abstract void initiate();

	protected abstract void terminate();

	private List<AutoCloseable> closables;
	private boolean active = false;

	public List<AutoCloseable> getClosables() {
		return closables;
	}

	public boolean isActive() {
		return active;
	}

	public void open() {
		if (active) {
			throw new UnexpectedError();
		}
		active = true;
		closables = new ArrayList<AutoCloseable>();
		initiate();
	}

	@Override
	public void close() throws Exception {
		if (!active) {
			throw new UnexpectedError();
		}
		MiscUtils.willRethrowCommonly((compositeException) -> {
			compositeException.tryCactch(() -> {
				terminate();
			});
			closables.stream().forEach(closable -> {
				compositeException.tryCactch(() -> {
					closable.close();
				});
			});
			closables = null;
			active = false;
		});
	}

}
