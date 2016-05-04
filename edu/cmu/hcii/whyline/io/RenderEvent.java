package edu.cmu.hcii.whyline.io;

import java.awt.*;

import edu.cmu.hcii.whyline.trace.*;
import edu.cmu.hcii.whyline.ui.io.GraphicsContext;

/**
 * @author Andrew J. Ko
 *
 */
public abstract class RenderEvent extends GraphicalOutputEvent implements Comparable<RenderEvent> {

	protected GraphicsContext paintState;
	private Shape unclippedShape = null;
	private Rectangle clippedBoundaries = null;
	private RenderEvent occluder;
	private java.util.List<DrawImageEvent> renderers = null;
	
	public RenderEvent(Trace trace, int eventID) {
		
		super(trace, eventID);

		// We call this right away, because at the time of this comment, we load these as we load the trace,
		// meaning we can get the current call stack without having to construct it. Saves time! Avoids lag later!
		getInstanceResponsible();

	}
	
	public String getHTMLDescription() { return getHumanReadableName(); }

	public void setRenderers(java.util.List<DrawImageEvent> renderers) {
		
		this.renderers = renderers;
		
	}
	
	public java.util.List<DrawImageEvent> getRenderers() { return  renderers; }
	
	public void paintByMemory() {
		
		Graphics2D g = paintState.getGraphics();
		
		paint(g);

	}
	
	public abstract void paint(Graphics2D g);
	
	public void rememberContext(GraphicsContext g) {

		paintState = new GraphicsContext(g);
		
	}

	public GraphicsContext getGraphicsState() { return paintState; }
		
	// Global window coordinates. For a point to be contained, it MUST
	// be within the clipped boundaries and within the unclipped shape.
	public boolean contains(int x, int y) { 
		
		Rectangle bounds = getClippedBoundaries(); 
		if(bounds != null && !bounds.contains(x, y)) return false;
		
		Shape shape = getUnclippedShape(); 
		if(shape == null) return false;
		// Make the test boundaries a pixel wider to ease selection.
		return getUnclippedShape().intersects(x -1, y - 1, 2, 2); 
		
	}
	
	public Shape getClip() { return paintState.getGraphics().getClip(); }
	
	public boolean occludes(RenderEvent renderEvent) {

		Rectangle occludersClippedBoundaries = getClippedBoundaries();
		Rectangle eventsClippedBoundaries = renderEvent.getClippedBoundaries();
		
		if(occludersClippedBoundaries == null || eventsClippedBoundaries == null)
			return false;		

		// If this event's shape doesn't entirely contain the subject's boundaries, then return false.
		return occludersClippedBoundaries.contains(eventsClippedBoundaries);
		
	}

	public void setOccluder(RenderEvent re) {

		assert occluder == null : "We've already set an occluder on " + this;
		occluder = re;
		
	}

	public boolean isVisibleAfter(int eventID) {

		if(eventID < getEventID()) return false;
		else if(occluder == null) return true;
		else return occluder.getEventID() > eventID;
		
	}
	
	public boolean hasOccluder() { return occluder != null; }
	
	public RenderEvent getOccluder() { return occluder; }
	
	public abstract boolean canOcclude();
	
	protected abstract Shape makeShape();
	
	public Shape getUnclippedShape() { 
		
		if(unclippedShape == null) unclippedShape = makeShape();
		return unclippedShape;
		
	}
	
	public Rectangle getClippedBoundaries() {
		
		if(clippedBoundaries != null) return clippedBoundaries;
		
		Shape clip = paintState.getGraphics().getClip(); 
		Shape unclippedShape = getUnclippedShape(); 
		if(clip == null) return unclippedShape == null ? null : unclippedShape.getBounds();
		if(unclippedShape == null) return null;

		Rectangle shapeRect = unclippedShape.getBounds();

		Rectangle clipRect = clip.getBounds();
		clipRect.translate(paintState.getOriginX(), paintState.getOriginY());

		clippedBoundaries = clipRect.intersection(shapeRect);		
		
		return clippedBoundaries;
		
	}

	public abstract String getArgumentName(int index);
	
	public boolean segmentsOutput() { return false; }
	
	public int compareTo(RenderEvent o) {
		 
		 return eventID - o.eventID;
		 
	 }
	
}
