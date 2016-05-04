package edu.cmu.hcii.whyline.io;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class DrawLineEvent extends RenderEvent {

	private static final String[] ARGUMENT_NAMES = { "graphics", "x1", "y1", "x2", "y2"}; 
	public String getArgumentName(int index) { return ARGUMENT_NAMES[index]; }

	public DrawLineEvent(Trace trace, int eventID) {
	
		super(trace, eventID);

	}

	public void paint(Graphics2D g) {
	
		g.drawLine(getX1(), getY1(), getX2(), getY2());		

	}

	protected Shape makeShape() {
		
		int x1 = getX1() + paintState.getOriginX(), x2 = getX2() + paintState.getOriginX(), 
			y1 = getY1() + paintState.getOriginY(), y2 = getY2() + paintState.getOriginY();
		
		// Determine the angle of the line.
		double angle = Math.atan2(y2 - y1, x2 - x1);

		SetStrokeEvent event = paintState.getLatestStrokeChange(); 
		Stroke stroke = event ==  null ? null : event.getStroke();
		float strokeWidth = Math.max(2, stroke == null ? 1 : ((BasicStroke)stroke).getLineWidth());
		
		double xAlong = Math.cos(angle) * strokeWidth;
		double yAlong = Math.sin(angle) * strokeWidth;
		
		double xClock = Math.cos(angle + Math.PI / 2) * strokeWidth;
		double yClock = Math.sin(angle + Math.PI / 2) * strokeWidth;

		double xCounter = Math.cos(angle - Math.PI / 2) * strokeWidth;
		double yCounter = Math.sin(angle - Math.PI / 2) * strokeWidth;

		Polygon p = new Polygon();
				
		p.addPoint((int)(x1 + xCounter), (int)(y1 + yCounter));
		p.addPoint((int)(x1 - xAlong), (int)(y1 - yAlong));
		p.addPoint((int)(x1 + xClock), (int)(y1 + yClock));

		p.addPoint((int)(x2 + xClock), (int)(y2 + yClock));
		p.addPoint((int)(x2 + xAlong), (int)(y2 + yAlong));
		p.addPoint((int)(x2 + xCounter) ,(int)(y2 + yCounter));
		
		return p;
		
	}
	
	public int getX1() { return getInteger(1); }
	public int getY1() { return getInteger(2); }
	public int getX2() { return getInteger(3); }
	public int getY2() { return getInteger(4); }

	public boolean canOcclude() { return false; }

	public String getHumanReadableName() { return "line"; }

	public String toString() { return super.toString() + getGraphicsID() + "\tdrawLine " + getX1() + " " + getY1() + " " + getX2() + " " + getY2(); }

}
