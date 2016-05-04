package edu.cmu.hcii.whyline.io;

import java.awt.Graphics2D;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class SetPaintModeEvent extends SetCompositeEvent {

	public SetPaintModeEvent(Trace trace, int eventID) {

		super(trace, eventID);
	
	}

	public void paint(Graphics2D g) {

		g.setPaintMode();
	
	}

	public String toString() { return super.toString() + getGraphicsID() + "\tsetPaintMode"; }

}
