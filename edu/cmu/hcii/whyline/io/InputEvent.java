package edu.cmu.hcii.whyline.io;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public abstract class InputEvent extends IOEvent {

	public InputEvent(Trace trace, int eventID) {

		super(trace, eventID);
	
	}

	public final boolean segmentsOutput() { return true; }

}
