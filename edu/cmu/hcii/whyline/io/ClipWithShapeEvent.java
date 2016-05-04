package edu.cmu.hcii.whyline.io;

import java.awt.Graphics2D;
import java.awt.Shape;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class ClipWithShapeEvent extends ModifyClipEvent {

	private Shape shape;
	
	public ClipWithShapeEvent(Trace trace, int eventID) {

		super(trace, eventID);
	
	}

	public void paint(Graphics2D g) {

		g.clip(getShape());
	
	}

	public Shape getShape() {
		
		if(shape == null) {
			try { shape = (Shape)trace.getOperandStackValue(eventID, 1).getImmutable(); } 
			catch (NoValueException e) {}
		}
		return shape;
		
	}
	
	public Shape getClipShape() { return getShape(); }

	public String getHumanReadableName() { return "clip"; }

	public String toString() { return super.toString() + getGraphicsID() + "\tclip " + shape; }

}
