/**
 * 
 */
package edu.cmu.hcii.whyline.util;

import java.awt.*;

import edu.cmu.hcii.whyline.ui.UI;

/**
 * A little helper class to draw a close icon.
 * 
 * @author Andrew J. Ko
 *
 */ 
public abstract class CloseIcon implements javax.swing.Icon {

	public CloseIcon() {}
	
	public static final int SIZE = 12;
	private static final int TOPLEFT = (int) (SIZE * .25);
	private static final int BOTTOMRIGHT = (int) (SIZE * .75) - 1;
	
	public int getIconHeight() {
		return SIZE;
	}

	public int getIconWidth() {
		return SIZE;
	}
	
	public abstract boolean isSelected(Component c);

	public void paintIcon(Component c, Graphics g, int x, int y) {

		boolean selected = isSelected(c);

		Color dark = UI.getControlFrontColor();
		Color light = UI.getControlCenterColor();
		
		Color back = selected ? dark : light;
		Color front = selected ? light : light.brighter().brighter();
		
		((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(back);
		g.fillOval(x, y, SIZE, SIZE);
		g.setColor(front);
		g.drawLine(x + TOPLEFT, y + TOPLEFT, x + BOTTOMRIGHT , y + BOTTOMRIGHT);
		g.drawLine(x + TOPLEFT, y + BOTTOMRIGHT, x + BOTTOMRIGHT, y + TOPLEFT);
		
	}
	
	
}