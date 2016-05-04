package edu.cmu.hcii.whyline.io;

import java.awt.Graphics2D;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class GetGraphicsOutputEvent extends GraphicalOutputEvent {

	public GetGraphicsOutputEvent(Trace trace, int eventID) {
	
		super(trace, eventID);

	}

	public long getGraphicsID() { return trace.getRepaintArguments(eventID).graphicsID; }

	public boolean representsWindow() { return trace.getRepaintArguments(eventID).representsWindow; }
	public int getWidth() { return trace.getRepaintArguments(eventID).width; }
	public int getHeight() { return trace.getRepaintArguments(eventID).height; }
	public int getTranslateX() { return trace.getRepaintArguments(eventID).translateX; }
	public int getTranslateY() { return trace.getRepaintArguments(eventID).translateY; }
	public long getObjectID() { return trace.getRepaintArguments(eventID).objectID; }
	public long getWindowID() { return trace.getRepaintArguments(eventID).windowID; }
	public long getWindowX() { return trace.getRepaintArguments(eventID).windowX; }
	public long getWindowY() { return trace.getRepaintArguments(eventID).windowY; }
	
	public void paint(Graphics2D g) {
		
	}

	public boolean segmentsOutput() { return true; }
	
	public String getHumanReadableName() { return "repaint"; }

	public String getHTMLDescription() { return "window repainted"; }

	public String toString() { return super.toString() + "getGraphics"; }

}