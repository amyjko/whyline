package edu.cmu.hcii.whyline.io;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public abstract class ModifyTransformEvent extends GraphicalOutputEvent {

	public ModifyTransformEvent(Trace trace, int eventID) {
		
		super(trace, eventID);

	}

	public boolean segmentsOutput() { return false; }
	
	public String getHTMLDescription() { return "transform"; }

}
