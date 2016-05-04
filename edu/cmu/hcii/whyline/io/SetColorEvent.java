package edu.cmu.hcii.whyline.io;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class SetColorEvent extends SetPaintEvent {

	public SetColorEvent(Trace trace, int eventID) {

		super(trace, eventID);
	
	}

	public String toString() { return super.toString() + getGraphicsID() + " (actually a setColor)"; }

}