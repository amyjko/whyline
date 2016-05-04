package edu.cmu.hcii.whyline.io;

import java.awt.Graphics2D;
import java.awt.Shape;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class SetClipWithShapeEvent extends ModifyClipEvent {

	private Shape shape;
	
	public SetClipWithShapeEvent(Trace trace, int eventID) {

		super(trace, eventID);
			
	}

	public void paint(Graphics2D g) {
		
		g.setClip(getShape());
	
	}

	public Shape getShape() {
		
		if(shape == null) {
			try { shape = (Shape)getArgument(1).getImmutable(); }
			catch(NoValueException e) {}
		}
		return shape;
		
	}
	
	public Shape getClipShape() { return getShape(); }

	public String getHumanReadableName() { return "clip"; }

	public String toString() { return super.toString() + getGraphicsID() + "\tsetClip " + getShape(); }

}