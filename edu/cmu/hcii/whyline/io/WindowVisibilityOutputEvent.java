package edu.cmu.hcii.whyline.io;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class WindowVisibilityOutputEvent extends OutputEvent {

	public WindowVisibilityOutputEvent(Trace trace, int eventID) {
	
		super(trace, eventID);

	}

	public String getHTMLDescription() { return trace.getDescription(eventID); }

	public boolean segmentsOutput() { return true; }

}