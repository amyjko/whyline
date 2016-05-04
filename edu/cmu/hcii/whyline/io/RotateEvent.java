package edu.cmu.hcii.whyline.io;

import java.awt.Graphics2D;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class RotateEvent extends ModifyTransformEvent {

	public RotateEvent(Trace trace, int eventID) {
		
		super(trace, eventID);

	}

	public void paint(Graphics2D g) {
	
		g.rotate(getAngle());		

	}

	public double getAngle() { return getDouble(1); }
	
	public String getHumanReadableName() { return "rotate"; }

	public String toString() { return super.toString() + getGraphicsID() + "\trotate " + getAngle(); }

}