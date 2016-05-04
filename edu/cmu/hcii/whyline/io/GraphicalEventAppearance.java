package edu.cmu.hcii.whyline.io;

import edu.cmu.hcii.whyline.util.Named;

/**
 * Why do we have this strange class? To distinguish between the time some render event occurred
 * and the time that it was rendered. We have to make this distinction because paint can be drawn into
 * buffers, and then the buffers can be drawn later. By keep these events separate, we can reason about
 * the events separately. 
 * 
 * @author Andrew J. Ko
 *
 */
public class GraphicalEventAppearance implements Comparable<GraphicalEventAppearance>, Named {
	
	public final RenderEvent event, renderer;
	
	public GraphicalEventAppearance(RenderEvent event, RenderEvent renderer) {
		this.event = event;
		this.renderer = renderer;
	}

	public String getDisplayName(boolean html, int lengthLimit) { return event.getDisplayName(html, lengthLimit); }
	
	public int compareTo(GraphicalEventAppearance o) { 
		// If they were rendered by the same renderer, sorted by the time of the original render event.
		if(renderer.eventID == o.renderer.eventID)
			return event.getEventID() - o.event.getEventID();
		// If they were rendered at different times, sort by their rendering times.
		else 
			return renderer.eventID - o.renderer.eventID;
	}
	
}