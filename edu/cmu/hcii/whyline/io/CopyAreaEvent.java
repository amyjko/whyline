package edu.cmu.hcii.whyline.io;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class CopyAreaEvent extends RenderEvent {

	private static final String[] ARGUMENT_NAMES = { "graphics", "x", "y", "width", "height", "dx", "dy" }; 
	public String getArgumentName(int index) { return ARGUMENT_NAMES[index]; }

	public CopyAreaEvent(Trace trace, int eventID) {
		
		super(trace, eventID);

	}

	public void paint(Graphics2D g) {
	
		g.copyArea(getX(), getY(), getWidth(), getHeight(), getDX(), getDY());		

	}

	protected Shape makeShape() {
		
		return new Rectangle(getX() + paintState.getOriginX() + getDX(), getY() + paintState.getOriginY() + getDY(), getWidth(), getHeight()); 
		
	}

	public int getX() { return getInteger(1); }
	public int getY() { return getInteger(2); }
	public int getWidth() { return getInteger(3); }
	public int getHeight() { return getInteger(4);  }
	public int getDX() { return getInteger(5); }
	public int getDY() { return getInteger(6); }
	
	public boolean canOcclude() { return true; }

	public String getHumanReadableName() { return "copied area"; }

	public String toString() { return super.toString() + getGraphicsID() + "\tcopyArea " + getX() + " " + getY() + " " + getWidth() + " " + getHeight() + " " + getDX() + " " + getDY(); }

}
