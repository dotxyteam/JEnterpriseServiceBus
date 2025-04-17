package com.otk.jesb.diagram;

public interface JDiagramListener {

	void nodeMoved(JNode node);

	void connectionAdded(JConnection conn);

	void selectionChanged();

}
