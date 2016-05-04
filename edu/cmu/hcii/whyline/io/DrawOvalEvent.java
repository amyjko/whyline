package edu.cmu.hcii.whyline.io;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class DrawOvalEvent extends RenderEvent {

	private static final String[] ARGUMENT_NAMES = { "graphics", "x", "y", "width", "height"}; 
	public String getArgumentName(int index) { return ARGUMENT_NAMES[index]; }

	public DrawOvalEvent(Trace trace, int eventID) {
		
		super(trace, eventID);

	}

	public void paint(Graphics2D g) {
	
		g.drawOval(getX(), getY(), getWidth(), getHeight());		

	}

	protected Shape makeShape() {
		
		return new Rectangle(getX() + paintState.getOriginX(), getY() + paintState.getOriginY(), getWidth(), getHeight()); 
		
	}

	public int getX() { return getInteger(1); }
	public int getY() { return getInteger(2); }
	public int getWidth() { return getInteger(3); }
	public int getHeight() { return getInteger(4);  }
	
	public String getHumanReadableName() { return "oval"; }

	public boolean canOcclude() { return false; }

	public String toString() { return super.toString() + getGraphicsID() + "\tdrawOval" + getX() + " " + getY() + " " + getWidth() + " " + getHeight(); }

}
