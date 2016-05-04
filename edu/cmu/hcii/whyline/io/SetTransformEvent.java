package edu.cmu.hcii.whyline.io;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class SetTransformEvent extends ModifyTransformEvent {

	private final AffineTransform transform;
	
	public SetTransformEvent(Trace trace, int eventID) {
		
		super(trace, eventID);

		AffineTransform t = null;
		try { t = (AffineTransform)trace.getOperandStackValue(eventID, 1).getImmutable();
		} catch (NoValueException e) {}
		
		transform = t;

	}

	public void paint(Graphics2D g) {
	
		if(transform != null)
			g.setTransform(transform);		

	}
	
	public String getHumanReadableName() { return "transform"; }

	public String toString() { return super.toString() + getGraphicsID() + "\tsetTransform " + transform; }
	
}
