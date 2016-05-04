package edu.cmu.hcii.whyline.io;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class DrawEvent extends RenderEvent {

	private static final String[] ARGUMENT_NAMES = { "graphics", "shape" }; 
	public String getArgumentName(int index) { return ARGUMENT_NAMES[index]; }

	private Shape shape;
	
	public DrawEvent(Trace trace, int eventID) {

		super(trace, eventID);
			
	}

	public void paint(Graphics2D g) {

		g.draw(getShape());
	
	}

	protected Shape makeShape() {
		
		Rectangle r = getShape().getBounds();
		r.translate(paintState.getOriginX(), paintState.getOriginY());
		return r;
		
	}

	public Shape getShape() { 
		
		if(shape == null) {
			try { shape = (Shape)trace.getOperandStackValue(eventID, 1).getImmutable(); }
			catch(NoValueException e) {}
		}
		return shape;
		
	}
	
	public boolean canOcclude() { return true; }

	public String getHumanReadableName() { return "shape"; }

	public String toString() { return super.toString() + getGraphicsID() + "\tdraw " + getShape(); }

}