package com.otk.jesb.diagram;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.geom.Rectangle2D;

public class JNode {

	private int x = 0;
	private int y = 0;
	private Object object;
	private boolean selected = false;
	private Image image;

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
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

	public void paint(Graphics g) {
		Color selectionColor = new Color(184, 207, 229);
		if (image != null) {
			g.drawImage(image, x - (image.getWidth(null) / 2), y - (image.getHeight(null) / 2),
					selected ? selectionColor : null, null);
			if (object.toString() != null) {
				g.setColor(Color.BLACK);
				Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(object.toString(), g);
				g.drawString(object.toString(), (int) Math.round(x - (stringBounds.getWidth() / 2)),
						(int) Math.round(y + (image.getHeight(null) / 2) + (stringBounds.getHeight() / 2)));
			}
		} else {
			if (selected) {
				g.setColor(selectionColor);
			} else {
				g.setColor(Color.DARK_GRAY);
			}
			g.fillOval(-10 + x, -10 + y, 20, 20);
			if (object != null) {
				if (object.toString() != null) {
					g.setColor(Color.BLACK);
					int stringWidth = g.getFontMetrics().stringWidth(object.toString());
					g.drawString(object.toString(), x - stringWidth / 2, y);
				}
			}
		}
	}

	public boolean containsPoint(int x2, int y2) {
		int width;
		int height;
		if (image != null) {
			width = image.getWidth(null);
			height = image.getHeight(null);
		} else {
			width = 20;
			height = 20;
		}
		if (x2 < (-(width / 2) + x)) {
			return false;
		}
		if (x2 > ((width / 2) + x)) {
			return false;
		}
		if (y2 < (-(height / 2) + y)) {
			return false;
		}
		if (y2 > ((height / 2) + y)) {
			return false;
		}
		return true;
	}

}
