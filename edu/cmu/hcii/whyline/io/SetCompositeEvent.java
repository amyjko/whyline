package edu.cmu.hcii.whyline.io;

import java.awt.Composite;
import java.awt.Graphics2D;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public class SetCompositeEvent extends GraphicalOutputEvent {

	private Composite composite;
	
	public SetCompositeEvent(Trace trace, int eventID) {

		super(trace, eventID);

	}

	public boolean segmentsOutput() { return false; }

	public void paint(Graphics2D g) {

//		g.setComposite();
	
	}

	public Value getCompositeProducedEvent() { return getArgument(1); }
	
	public Composite getComposite() { 
	
		if(composite == null) {
			try { composite = (Composite)getCompositeProducedEvent().getImmutable(); } 
			catch (NoValueException e) {}
		}
		return composite; 
	
	}
	
	public String getHumanReadableName() { return "composite"; }

	public String getHTMLDescription() { return "set composite"; }

	public String toString() { return super.toString() + getGraphicsID() + "\tsetComposite "; }

}
