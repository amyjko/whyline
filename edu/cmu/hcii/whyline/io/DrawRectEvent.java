package edu.cmu.hcii.whyline.io;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class DrawRectEvent extends RenderEvent {

	private static final String[] ARGUMENT_NAMES = { "graphics", "x", "y", "width", "height"}; 
	public String getArgumentName(int index) { return ARGUMENT_NAMES[index]; }

	public DrawRectEvent(Trace trace, int eventID) {
		
		super(trace, eventID);

	}

	public void paint(Graphics2D g) {
	
		g.drawRect(getX(), getY(), getWidth(), getHeight());		

	}

	protected Shape makeShape() {
		
		int x1 = getX() + paintState.getOriginX(), x2 = getX() + paintState.getOriginX() + getWidth(), 
			y1 = getY() + paintState.getOriginY(), y2 = getY() + paintState.getOriginY() + getHeight();
	
		int thickness = 2;

		Polygon p = new Polygon();
		p.addPoint(x1 - thickness, y1 - thickness);
		p.addPoint(x2 + thickness, y1 - thickness);
		p.addPoint(x2 + thickness, y2 + thickness);
		p.addPoint(x1 + thickness, y2 + thickness);
		p.addPoint(x1 + thickness, y2 - thickness);
		p.addPoint(x2 - thickness, y2 - thickness);
		p.addPoint(x2 - thickness, y1 + thickness);
		p.addPoint(x1 + thickness, y1 + thickness);
		p.addPoint(x1 + thickness, y2 + thickness);
		p.addPoint(x1 - thickness, y2 + thickness);
		p.addPoint(x1 - thickness, y1 - thickness);
		
		return p;
		
	}

	public int getX() { return getInteger(1); }
	public int getY() { return getInteger(2); }
	public int getWidth() { return getInteger(3); }
	public int getHeight() { return getInteger(4);  }
	
	public String getHumanReadableName() { return "rectangle"; }

	public boolean canOcclude() { return false; }

	public String toString() { return super.toString() + getGraphicsID() + "\tdrawRect " + getX() + " " + getY() + " " + getWidth() + " " + getHeight(); }

}
