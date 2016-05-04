package edu.cmu.hcii.whyline.io;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class SetClipEvent extends ModifyClipEvent {

	public SetClipEvent(Trace trace, int eventID) {
		
		super(trace, eventID);

	}

	public void paint(Graphics2D g) {

		g.setClip(getX(), getY(), getWidth(), getHeight());		

	}

	public int getX() { return getInteger(1);  }
	public int getY() { return getInteger(2); }
	public int getWidth() { return getInteger(3); }
	public int getHeight() { return getInteger(4); }
	
	public Shape getClipShape() { return new Rectangle(getX(), getY(), getWidth(), getHeight()); }

	public String getHumanReadableName() { return "clip"; }

	public String toString() { return super.toString() + getGraphicsID() + "\tsetClip " + getX() + " " + getY() + " " + getWidth() + " " + getHeight(); }
	
}
