package edu.cmu.hcii.whyline.io;

import java.awt.Graphics2D;
import java.awt.Stroke;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class SetStrokeEvent extends GraphicalOutputEvent {

	private Stroke stroke;
	
	public SetStrokeEvent(Trace trace, int eventID) {

		super(trace, eventID);
			
	}

	public void paint(Graphics2D g) {
	
		if(getStroke() != null)
			g.setStroke(stroke);
	
	}

	public boolean segmentsOutput() { return false; }

	public Value getStrokeProducedEvent() { return getArgument(1); }
	
	public Stroke getStroke() { 
	
		if(stroke == null) {
			try { stroke = (Stroke)getStrokeProducedEvent().getImmutable(); } 
			catch (NoValueException e) {}
		}
		return stroke; 
		
	}
	
	public String getHumanReadableName() { return "stroke"; }

	public String getHTMLDescription() { return "set stroke"; }

	public String toString() { return super.toString() + getGraphicsID() + "\tsetStroke "; }

}