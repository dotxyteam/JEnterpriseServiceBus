package com.otk.jesb;

import java.util.AbstractList;
import java.util.List;

import com.otk.jesb.resource.Resource;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Solution;

public abstract class AbstractExperiment extends Plan implements AutoCloseable {

	private List<Resource> temporaryTestResources = new AbstractList<Resource>() {

		int size = 0;

		@Override
		public Resource get(int index) {
			return (Resource) Solution.INSTANCE.getContents().get(index);
		}

		@Override
		public Resource set(int index, Resource element) {
			return (Resource) Solution.INSTANCE.getContents().set(index, element);
		}

		@Override
		public void add(int index, Resource element) {
			Solution.INSTANCE.getContents().add(index, element);
			size++;
		}

		@Override
		public Resource remove(int index) {
			Resource result = (Resource) Solution.INSTANCE.getContents().remove(index);
			size--;
			return result;
		}

		@Override
		public int size() {
			return size;
		}
	};

	public List<Resource> getTemporaryTestResources() {
		return temporaryTestResources;
	}

	public void setTemporaryTestResources(List<Resource> temporaryTestResources) {
		this.temporaryTestResources = temporaryTestResources;
	}

	@Override
	public void close() throws Exception {
		temporaryTestResources.clear();
	}

}
