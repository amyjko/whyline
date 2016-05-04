package edu.cmu.hcii.whyline.io;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class MouseStateInputEvent extends InputEvent {

	public MouseStateInputEvent(Trace trace, int eventID) {
	
		super(trace, eventID);

	}

	public int getType() { return trace.getMouseArguments(eventID).type; }
	public int getX() { return trace.getMouseArguments(eventID).x; }
	public int getY() { return trace.getMouseArguments(eventID).y; }
	public long getSource() { return trace.getMouseArguments(eventID).source; }
	
	public String getHTMLDescription() { return trace.getDescription(eventID); }

}