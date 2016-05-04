package edu.cmu.hcii.whyline.io;

import java.awt.Graphics2D;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class ScaleEvent extends ModifyTransformEvent {

	public ScaleEvent(Trace trace, int eventID) {
		
		super(trace, eventID);

	}

	public void paint(Graphics2D g) {
	
		g.scale(getSX(), getSY());		

	}

	public double getSX() { return getDouble(1); }
	public double getSY() { return getDouble(2); }
	
	public String getHumanReadableName() { return "scale"; }

	public String toString() { return super.toString() + getGraphicsID() + "\tscale " + getSX() + " " + getSY(); }

}