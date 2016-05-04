package edu.cmu.hcii.whyline.io;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class KeyStateInputEvent extends InputEvent {

	public KeyStateInputEvent(Trace trace, int eventID) {
	
		super(trace, eventID);

	}

	public int getType() { return trace.getKeyArguments(eventID).type; }
	public long getSource() { return trace.getKeyArguments(eventID).source; }
	
	public String getHTMLDescription() { return trace.getDescription(eventID); }

}