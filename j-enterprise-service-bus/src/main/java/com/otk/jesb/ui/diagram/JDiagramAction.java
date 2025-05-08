package com.otk.jesb.ui.diagram;

import javax.swing.Icon;

public interface JDiagramAction {

	void perform(int x, int y);

	String getLabel();

	Icon getIcon();

}
