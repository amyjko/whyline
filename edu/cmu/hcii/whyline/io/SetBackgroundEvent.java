package edu.cmu.hcii.whyline.io;

import java.awt.Color;
import java.awt.Graphics2D;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class SetBackgroundEvent extends GraphicalOutputEvent {

	private Color color;
	
	public SetBackgroundEvent(Trace trace, int eventID) {

		super(trace, eventID);
		
	}

	public Color getColor() {

		if(color == null) {
			Color c = null;
			try { c = (Color)getArgument(1).getImmutable(); } 
			catch (NoValueException e) {}
			color = c == null ? Color.gray : c;
		}
		return color;
		
	}
	
	public boolean segmentsOutput() { return false; }

	public void paint(Graphics2D g) {

		g.setBackground(getColor());
	
	}

	public String getHumanReadableName() { return "background"; }

	public String getHTMLDescription() { return "set background color"; }

	public String toString() { return super.toString() + getGraphicsID() + "\tsetBackground " + getColor(); }

}
