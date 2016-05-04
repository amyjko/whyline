package edu.cmu.hcii.whyline.io;

import java.awt.Color;
import java.awt.Graphics2D;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class SetXORModeEvent extends SetCompositeEvent {

	private final Color color;
	
	public SetXORModeEvent(Trace trace, int eventID) {

		super(trace, eventID);

		Color c = null;
		try {
			c = (Color)trace.getOperandStackValue(eventID, 1).getImmutable();
		} catch (NoValueException e) {}
		
		color = c;		
	
	}

	public void paint(Graphics2D g) {

		if(color != null)
			g.setXORMode(color);
	
	}

	public String toString() { return super.toString() + getGraphicsID() + "\tsetXORMode" + color; }

}
