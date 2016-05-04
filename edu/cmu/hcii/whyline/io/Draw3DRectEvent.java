package edu.cmu.hcii.whyline.io;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class Draw3DRectEvent extends RenderEvent {

	private static final String[] ARGUMENT_NAMES = { "graphics", "x", "y", "width", "height", "raised"}; 
	public String getArgumentName(int index) { return ARGUMENT_NAMES[index]; }

	public Draw3DRectEvent(Trace trace, int eventID) {
		
		super(trace, eventID);

	}

	public void paint(Graphics2D g) {
	
		g.draw3DRect(getX(), getY(), getWidth(), getHeight(), getRaised());		

	}

	protected Shape makeShape() {
		
		return new Rectangle(getX() + paintState.getOriginX(), getY() + paintState.getOriginY(), getWidth(), getHeight()); 
		
	}

	public int getX() { return getInteger(1); }
	public int getY() { return getInteger(2); }
	public int getWidth() { return getInteger(3); }
	public int getHeight() { return getInteger(4);  }
	public boolean getRaised() { return getBoolean(5); }
	
	public boolean canOcclude() { return false; }

	public String getHumanReadableName() { return "3D rectangle"; }

	public String toString() { return super.toString() + getGraphicsID() + "\tdraw3DRect " + getX() + " " + getY() + " " + getWidth() + " " + getHeight(); }

}
