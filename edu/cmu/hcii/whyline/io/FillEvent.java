package edu.cmu.hcii.whyline.io;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class FillEvent extends RenderEvent {

	private static final String[] ARGUMENT_NAMES = { "graphics", "shape" }; 
	public String getArgumentName(int index) { return ARGUMENT_NAMES[index]; }

	private final Shape shape;
	
	public FillEvent(Trace trace, int eventID) {

		super(trace, eventID);

		Shape s = null;
		try { s = (Shape)trace.getOperandStackValue(eventID, 1).getImmutable(); } 
		catch (NoValueException e) {}

		shape = s;
		
	}

	public void paint(Graphics2D g) {

		g.fill(shape);
	
	}

	protected Shape makeShape() {
		
		Rectangle r = shape.getBounds();
		r.translate(paintState.getOriginX(), paintState.getOriginY());
		return r;
		
	}

	public String getHumanReadableName() { return "filled shape"; }

	public boolean canOcclude() { return true; }

	public String toString() { return super.toString() + getGraphicsID() + "\tfill " + shape; }

}