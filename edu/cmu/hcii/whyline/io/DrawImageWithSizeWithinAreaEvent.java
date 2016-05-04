package edu.cmu.hcii.whyline.io;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class DrawImageWithSizeWithinAreaEvent extends DrawImageEvent {

	private static final String[] ARGUMENT_NAMES = { "graphics", "image", "destination x1", "destination y1", "destination x2", "destination y2", "source x1", "source y1", "source x2", "source y2", "observer" }; 
	public String getArgumentName(int index) { return ARGUMENT_NAMES[index]; }
	
	public DrawImageWithSizeWithinAreaEvent(Trace trace, int eventID) {
		
		super(trace, eventID);

	}

	public void paintWithImage(Graphics2D g, Image image) {
	
		if(image == null) {
			double destX = Math.min(getDX1(), getDX2());
			double destY = Math.min(getDY1(), getDY2());
			double destWidth = Math.abs(getDX1() - getDX2());
			double destHeight = Math.abs(getDY1() - getDY2());
			drawPlaceholder(g, "image " + getImageID(), (int)destX, (int)destY, (int)destWidth, (int)destHeight, null);
		}
		else
			g.drawImage(image, getDX1(), getDY1(), getDX2(), getDY2(), getSX1(), getSY1(), getSX2(), getSY2(), null);		
		
	}
	
	protected Shape makeShape() {
		
		return new Rectangle(
				getWindowX(), getWindowY(),
				Math.abs(getDX1() - getDX2()), Math.abs(getDY1() - getDY2()));
		
	}

	public int getWindowX() { return paintState.getOriginX() + Math.min(getDX1(), getDX2()); }
	public int getWindowY() { return paintState.getOriginY() + Math.min(getDY1(), getDY2()); }

	public void transformContextToDrawPrimitive(Graphics2D g) {

		g.translate(getWindowX(), getWindowY());
		
	}

	public int getDX1() { return getInteger(2); }
	public int getDY1() { return getInteger(3); }
	public int getDX2() { return getInteger(4); }
	public int getDY2() { return getInteger(5); }
	public int getSX1() { return getInteger(6); }
	public int getSY1() { return getInteger(7); }
	public int getSX2() { return getInteger(8); }
	public int getSY2() { return getInteger(9); }
	public long getObserver() { return getLong(10); }
	
	public String getHumanReadableName() { return "scaled image"; }

	public String toString() { return super.toString() + "\tdrawImageWithSizeWithinArea " + getImage() + " " + getDX1() + " " + getDY1() + " " + getDX2() + " " + getDY2() + " " + getSX1() + " " + getSY1() + " " + getSX2() + " " + getSY2(); }

}