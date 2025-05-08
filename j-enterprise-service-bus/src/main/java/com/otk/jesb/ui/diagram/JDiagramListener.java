package com.otk.jesb.ui.diagram;

import java.util.Set;

public interface JDiagramListener {

	void nodesMoved(Set<JNode> nodes);

	void connectionAdded(JConnection conn);

	void selectionChanged();

}
