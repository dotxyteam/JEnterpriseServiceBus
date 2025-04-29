package com.otk.jesb.diagram;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

public class JNode extends JDiagramObject {

	private int centerX = 0;
	private int centerY = 0;
	private boolean selected = false;
	private Image image;

	public int getCenterX() {
		return centerX;
	}

	public void setCenterX(int centerX) {
		this.centerX = centerX;
	}

	public int getCenterY() {
		return centerY;
	}

	public void setCenterY(int centerY) {
		this.centerY = centerY;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public Image getImage() {
		return image;
	}

	public void setImage(Image image) {
		this.image = image;
	}

	public void paint(Graphics g, JDiagram diagram) {
		Color selectionColor = diagram.getSelectionColor();
		if (image != null) {
			g.drawImage(image, centerX - (image.getWidth(null) / 2), centerY - (image.getHeight(null) / 2),
					selected ? selectionColor : null, null);
		} else {
			if (selected) {
				g.setColor(selectionColor);
			} else {
				g.setColor(Color.DARK_GRAY);
			}
			g.fillOval(-10 + centerX, -10 + centerY, 20, 20);
		}
		if (value != null) {
			g.setColor(Color.BLACK);
			Rectangle labelBounds = getLabelBounds(g);
			if (labelBounds != null) {
				g.drawString(value.toString(), labelBounds.x, labelBounds.y + labelBounds.height);
			}
		}
	}

	public Rectangle getLabelBounds(Graphics g) {
		if ((value == null) || (value.toString().length() == 0)) {
			return null;
		}
		Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(value.toString(), g);
		return new Rectangle((int) Math.round(centerX - (stringBounds.getWidth() / 2)),
				(int) Math.round(centerY + (getHeight() / 2)), (int) Math.round(stringBounds.getWidth()),
				(int) Math.round(stringBounds.getHeight()));
	}

	public int getWidth() {
		if (image != null) {
			return image.getWidth(null);
		} else {
			return 20;
		}
	}

	public int getHeight() {
		if (image != null) {
			return image.getHeight(null);
		} else {
			return 20;
		}
	}

	public boolean containsPoint(int x, int y, JDiagram diagram) {
		Rectangle labelBounds = getLabelBounds(diagram.getGraphics());
		if (labelBounds != null) {
			if (labelBounds.contains(x, y)) {
				return true;
			}
		}
		int width = getWidth();
		int height = getHeight();
		if (x < (-(width / 2) + centerX)) {
			return false;
		}
		if (x > ((width / 2) + centerX)) {
			return false;
		}
		if (y < (-(height / 2) + centerY)) {
			return false;
		}
		if (y > ((height / 2) + centerY)) {
			return false;
		}
		return true;
	}

}
