package edu.cmu.hcii.whyline.io;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public abstract class OutputEvent extends IOEvent {

	public OutputEvent(Trace trace, int eventID) {

		super(trace, eventID);
		
	}

	public abstract boolean segmentsOutput();
	
}
