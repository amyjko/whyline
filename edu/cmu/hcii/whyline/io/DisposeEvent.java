package edu.cmu.hcii.whyline.io;

import java.awt.Graphics2D;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class DisposeEvent extends GraphicalOutputEvent {

	public DisposeEvent(Trace trace, int eventID) {
	
		super(trace, eventID);

	}

	public void paint(Graphics2D g) {
	
//		g.dispose();		

	}
	
	public boolean segmentsOutput() { return false; }

	public String getHumanReadableName() { return "dispose"; }

	public String getHTMLDescription() { return "dispose"; }

	public String toString() { return super.toString() + getGraphicsID() + "\tdispose"; }

}
