package edu.cmu.hcii.whyline.ui.components;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import edu.cmu.hcii.whyline.ui.UI;

/**
 * @author Andrew J. Ko
 *
 */
public class WhylineProgressBar extends WhylineLabel {

	private double value = 1.0;
	
	public WhylineProgressBar() {
		
		super("");
		
	}
	
	public void setValue(double value) {
		
		this.value = value;
		repaint();
		
	}
	
	/**
	 * Automatically prefixes with <html>
	 */
	public void setNote(String note) {
		
		this.setText("<html>" + note);
		
	}
		
	public void removeNote() {
		
		setText("");
		
	}
	
	public void paintComponent(Graphics g) {
		
		Graphics2D g2 = (Graphics2D)g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		boolean showingProgress = value != 0.0;
		
		int round = UI.getRoundedness();
		
		Insets insets = getInsets();
		
		int left = 0;//insets.left / 2;
		int top = 0;//insets.top;
		int width = getWidth() - 1;// - insets.right - insets.left - 1;
		int height = getHeight() - 1;// - insets.bottom - insets.top - 1;
		
		g2.setColor(UI.getControlCenterColor());
		g2.fillRoundRect(left, top, width, height, round, round);

		g2.setColor(UI.getHighlightColor());
		int filledWidth =  (int)(width * value);
		g2.fillRoundRect(left, top + 1, filledWidth, height, round, round);
		int widthOfRect = round / 3 + 1;
		int leftOfRect = Math.max(0, filledWidth - widthOfRect);
		if(leftOfRect > widthOfRect) g2.fillRect(leftOfRect, top + 1, widthOfRect, height - 2);

		g2.setColor(UI.getControlBorderColor());
		g2.drawRoundRect(left, top, width, height, round, round);
		
		super.paintComponent(g);

	}
	
}
