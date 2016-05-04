package edu.cmu.hcii.whyline.io;

import java.awt.Graphics2D;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class CreateGraphicsOutputEvent extends GraphicalOutputEvent {

	public CreateGraphicsOutputEvent(Trace trace, int eventID) {
	
		super(trace, eventID);

	}

	public boolean segmentsOutput() { return false; }

	public long getGraphicsID() { return trace.getCreateGraphicsArguments(eventID).oldID; }

	public long getSourceID() { return trace.getCreateGraphicsArguments(eventID).oldID; }
	public long getCopyID() {  return trace.getCreateGraphicsArguments(eventID).newID; }

	public String getHumanReadableName() { return "create"; }

	public String getHTMLDescription() { return "create graphics"; }

	public void paint(Graphics2D g) {}

	public String toString() { return super.toString() + "create"; }

}