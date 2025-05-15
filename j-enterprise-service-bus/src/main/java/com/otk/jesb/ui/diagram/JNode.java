package com.otk.jesb.ui.diagram;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import com.otk.jesb.util.MiscUtils;

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

	@Override
	public void paint(Graphics g, JDiagram diagram) {
		MiscUtils.improveRenderingQuality((Graphics2D) g);
		Rectangle imageBounds = getImageBounds();
		if (image != null) {
			g.drawImage(image, imageBounds.x, imageBounds.y, selected ? diagram.getSelectionColor() : null, null);
		} else {
			if (selected) {
				g.setColor(diagram.getSelectionColor());
			} else {
				g.setColor(diagram.getNodeColor());
			}
			g.fillOval(imageBounds.x, imageBounds.y, imageBounds.width, imageBounds.height);
		}
		if (value != null) {
			g.setColor(diagram.getTextColor());
			Rectangle labelBounds = getLabelBounds(g);
			if (labelBounds != null) {
				g.drawString(value.toString(), labelBounds.x, labelBounds.y + labelBounds.height);
			}
		}
	}

	public Rectangle getImageBounds() {
		if (image != null) {
			return new Rectangle(centerX - (image.getWidth(null) / 2), centerY - (image.getHeight(null) / 2),
					getImageWidth(), getImageHeight());
		} else {
			return new Rectangle(-10 + centerX, -10 + centerY, 20, 20);
		}
	}

	public Rectangle getLabelBounds(Graphics g) {
		if ((value == null) || (value.toString().length() == 0)) {
			return null;
		}
		Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(value.toString(), g);
		return new Rectangle((int) Math.round(centerX - (stringBounds.getWidth() / 2)),
				(int) Math.round(centerY + (getImageHeight() / 2)), (int) Math.round(stringBounds.getWidth()),
				(int) Math.round(stringBounds.getHeight()));
	}

	public int getImageWidth() {
		if (image != null) {
			return image.getWidth(null);
		} else {
			return 20;
		}
	}

	public int getImageHeight() {
		if (image != null) {
			return image.getHeight(null);
		} else {
			return 20;
		}
	}

	@Override
	public boolean containsPoint(int x, int y, JDiagram diagram) {
		Graphics g = diagram.getGraphics();
		if (g != null) {
			Rectangle labelBounds = getLabelBounds(g);
			if (labelBounds != null) {
				if (labelBounds.contains(x, y)) {
					return true;
				}
			}
		}
		int width = getImageWidth();
		int height = getImageHeight();
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

	@Override
	public Rectangle getBounds(JDiagram diagram) {
		Rectangle result = getImageBounds();
		Graphics g = diagram.getGraphics();
		if (g != null) {
			result.add(getLabelBounds(g));
		}
		return result;
	}

}
