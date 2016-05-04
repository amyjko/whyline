package edu.cmu.hcii.whyline.io;

import java.awt.Shape;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public abstract class ModifyClipEvent extends GraphicalOutputEvent {

	public ModifyClipEvent(Trace trace, int eventID) {
		
		super(trace, eventID);

		// We call this right away, because at the time of this comment, we load these as we load the trace,
		// meaning we can get the current call stack without having to construct it. Saves time! Avoids lag later!
		getInstanceResponsible();

	}
	
	public boolean segmentsOutput() { return false; }

	public abstract Shape getClipShape();
	
	public String getHTMLDescription() { return "rendering clip"; }
	
}
