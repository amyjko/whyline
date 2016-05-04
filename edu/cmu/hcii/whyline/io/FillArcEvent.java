package edu.cmu.hcii.whyline.io;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class FillArcEvent extends RenderEvent {

	private static final String[] ARGUMENT_NAMES = { "graphics", "x", "y", "width", "height", "start angle", "arc angle" }; 
	public String getArgumentName(int index) { return ARGUMENT_NAMES[index]; }

	public FillArcEvent(Trace trace, int eventID) {
		
		super(trace, eventID);

	}

	public void paint(Graphics2D g) {
	
		g.fillArc(getX(), getY(), getWidth(), getHeight(), getStartAngle(), getArcAngle());		

	}

	protected Shape makeShape() {
		
		return new Rectangle(getX() + paintState.getOriginX(), getY() + paintState.getOriginY(), getWidth(), getHeight()); 
		
	}

	public int getX() { return getInteger(1); }
	public int getY() { return getInteger(2); }
	public int getWidth() { return getInteger(3); }
	public int getHeight() { return getInteger(4);  }
	public int getStartAngle() { return getInteger(5); }
	public int getArcAngle() { return getInteger(6); }
	
	public String getHumanReadableName() { return "filled arc"; }

	public boolean canOcclude() { return true; }

	public String toString() { return super.toString() + getGraphicsID() + "\tfillArc" + getX() + " " + getY() + " " + getWidth() + " " + getHeight(); }

}
