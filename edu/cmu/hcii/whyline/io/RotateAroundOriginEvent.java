package edu.cmu.hcii.whyline.io;

import java.awt.Graphics2D;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class RotateAroundOriginEvent extends ModifyTransformEvent {

	public RotateAroundOriginEvent(Trace trace, int eventID) {
		
		super(trace, eventID);

	}

	public void paint(Graphics2D g) {
	
		g.rotate(getAngle(), getX(), getY());

	}

	public double getAngle() { return getDouble(1); }
	public double getX() { return getDouble(2); }
	public double getY() { return getDouble(3); }
	
	public String getHumanReadableName() { return "rotate"; }

	public String toString() { return super.toString() + getGraphicsID() + "\trotate " + getAngle() + " " + getX() + " " + getY(); }

}