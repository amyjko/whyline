package edu.cmu.hcii.whyline.io;

import java.awt.Font;
import java.awt.Graphics2D;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class SetFontEvent extends GraphicalOutputEvent {

	private Font font;
	
	public SetFontEvent(Trace trace, int eventID) {

		super(trace, eventID);
			
	}

	public boolean segmentsOutput() { return false; }

	public void paint(Graphics2D g) {

		g.setFont(getFont());
	
	}

	public Value getFontProducedEvent() { return getArgument(1); }
	
	public Font getFont() { 
	
		if(font == null) {
			try { font = (Font)(getFontProducedEvent()).getImmutable(); } 
			catch (NoValueException e) {}
		}
		return font; 
		
	}
	
	public String getHumanReadableName() { return "font"; }

	public String getHTMLDescription() { return "set font"; }

	public String toString() { return super.toString() + getGraphicsID() + "\tsetFont " +getFont(); }

}
