package com.otk.jesb.diagram;

import java.util.Set;

public interface JDiagramListener {

	void nodesMoved(Set<JNode> nodes);

	void connectionAdded(JConnection conn);

	void selectionChanged();

}
