package edu.cmu.hcii.whyline.io;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;

import edu.cmu.hcii.whyline.trace.*;
import edu.cmu.hcii.whyline.ui.UI;

/**
 * @author Andrew J. Ko
 *
 */
public abstract class DrawImageEvent extends RenderEvent {

	private Image buffer;
	
	public DrawImageEvent(Trace trace, int eventID) {
		
		super(trace, eventID);

	}

	public void paint(Graphics2D g) {
		
		paintWithImage(g, getImage());
		
	}

	public abstract void paintWithImage(Graphics2D g, Image image);

	protected void drawPlaceholder(Graphics2D g, String label, int x, int y, int width, int height, Color color) {
		
		g = (Graphics2D)g.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		
		Color lineBorderColor = UI.getControlFrontColor();
		
		g.clipRect(x, y, width + 1, height + 1);
		g.setColor(color == null ? UI.getControlCenterColor() : color);
		g.fillRect(x, y, width, height);
		g.setColor(Color.gray);
		int bottom = y + height;
		int max = x + width;
		int inc = UI.getCrosshatchSpacing();
		for(int left = x - width; left < max; left+= inc)
			g.drawLine(left, bottom, left + height, y);
		g.setColor(lineBorderColor);
		g.drawRect(x, y, width, height);
		
	}
	
	public void paintByMemory() {

		Image image = buffer == null ? getImage() : buffer;
		paintWithImage(paintState.getGraphics(), image);
		
	}
	
	public void rememberOffscreenBuffer(Image image) {

		buffer = image;
		
	}
	
	public abstract int getWindowX();
	public abstract int getWindowY();
	
	public abstract void transformContextToDrawPrimitive(Graphics2D g);
	
	public final boolean canOcclude() { return true; }

	public long getImageID() { return getLong(1); }
	public Image getImage() { 
		
		try {
			return (Image)trace.getOperandStackValue(eventID, 1).getImmutable();
		} catch (NoValueException e) {
			return null;
		} 
		
	}
	
	public String toString() { return super.toString() + "gID=" + getGraphicsID() + " imageID=" + getImageID(); } 
	
}
