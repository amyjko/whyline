package edu.cmu.hcii.whyline.io;

import java.awt.Graphics2D;
import java.awt.Paint;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public class SetPaintEvent extends GraphicalOutputEvent {

	private Paint paint;
	
	public SetPaintEvent(Trace trace, int eventID) {

		super(trace, eventID);
			
	}

	public Paint getPaint() { 
		
		if(paint == null) {
			try { paint = (Paint)getPaintProducedEvent().getImmutable(); } 
			catch (NoValueException e) {}
		}
		return paint; 
		
	}
	
	public Value getPaintProducedEvent() { return getArgument(1); }
	
	public boolean segmentsOutput() { return false; }

	public void paint(Graphics2D g) {

		g.setPaint(getPaint());
	
	}

	public String getHumanReadableName() { return "color"; }
	
	public String getHTMLDescription() { return "set paint"; }

	public String toString() { return super.toString() + getGraphicsID() + "\tsetPaint " + getPaint(); }

}