package edu.cmu.hcii.whyline.io;

import java.awt.Graphics2D;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class TranslateEvent extends ModifyTransformEvent {

	public TranslateEvent(Trace trace, int eventID) {
		
		super(trace, eventID);
		
	}

	public void paint(Graphics2D g) {

		g.translate(getTranslateX(), getTranslateY());
	
	}
	
	public int getTranslateX() { return getInteger(1); }
	public int getTranslateY() { return getInteger(2); }

	public String getHumanReadableName() { return "translate"; }

	public String toString() { return super.toString() + getGraphicsID() + "\ttranslate " + getTranslateX() + " " + getTranslateY(); }
	
}