package edu.cmu.hcii.whyline.io;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class FillRectEvent extends RenderEvent {

	private static final String[] ARGUMENT_NAMES = { "graphics", "x", "y", "width", "height"}; 
	public String getArgumentName(int index) { return ARGUMENT_NAMES[index]; }

	public FillRectEvent(Trace trace, int eventID) {
		
		super(trace, eventID);

	}

	public void paint(Graphics2D g) {

		g.fillRect(getX(), getY(), getWidth(), getHeight());		

	}

	protected Shape makeShape() {
		
		return new Rectangle(getX() + paintState.getOriginX(), getY() + paintState.getOriginY(), getWidth(), getHeight()); 
		
	}

	public int getX() { return getInteger(1); }
	public int getY() { return getInteger(2); }
	public int getWidth() { return getInteger(3); }
	public int getHeight() { return getInteger(4);  }
	
	public String getHumanReadableName() { return "filled rectangle"; }

	public boolean canOcclude() { return true; }

	public String toString() { return super.toString() + getGraphicsID() + "\tfillRect " + getX() + " " + getY() + " " + getWidth() + " " + getHeight(); }

}