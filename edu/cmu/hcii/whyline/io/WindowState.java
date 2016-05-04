package edu.cmu.hcii.whyline.io;

import java.awt.*;
import java.util.List;
import java.awt.image.BufferedImage;
import java.util.*;

import edu.cmu.hcii.whyline.ui.*;
import edu.cmu.hcii.whyline.ui.io.GraphicsContext;
import gnu.trove.TLongLongHashMap;
import gnu.trove.TLongObjectHashMap;

/**
 * @author Andrew J. Ko
 *
 */
public final class WindowState {

	private final WindowParser parser;

	private final ArrayList<Repaint> repaints = new ArrayList<Repaint>(1000);

	private BufferedImage image;

	// These are all used for determining how to render this window's history. See addRepaint() and addEvent().
	private final TLongObjectHashMap<GraphicsContext> contextsByContextIDs = new TLongObjectHashMap<GraphicsContext>(10000, .75f);
	private final TLongObjectHashMap<Image> imagesByImageID = new TLongObjectHashMap<Image>(20);
	
	private final TLongLongHashMap imageIDByContextID = new TLongLongHashMap(20);
	private final TLongObjectHashMap<List<DrawImageEvent>> drawImageEventByImageID = new TLongObjectHashMap<List<DrawImageEvent>>(20);
	
	private Graphics2D currentRepaintsGraphics;
	private final Set<RenderEvent> visibleEvents = new LinkedHashSet<RenderEvent>(1000);
	private final Vector<ModifyClipEvent> clipEvents = new Vector<ModifyClipEvent>(1000, 1000);
	
	private int maxWidth = 1;
	private int maxHeight = 1;
	private final SortedSet<DrawImageEvent> drawImagesSkipped = new TreeSet<DrawImageEvent>();;
	
	public int getMaxWidth() { return maxWidth; }
	public int getMaxHeight() { return maxHeight; }
	
	public WindowState(WindowParser parser, GetGraphicsOutputEvent getGraphics) {

		this.parser = parser;
		
		addRepaint(getGraphics);
		
	}
	
	private Repaint getFirstRepaint() { return repaints.get(0); }
	private Repaint getLastRepaint() { return repaints.get(repaints.size() - 1); }
	
	public boolean hasRepaints() { return repaints.size() > 0; }
	
	public long getWindowID() { return getFirstRepaint().getGetGraphicsEvent().getObjectID(); }
	
	public synchronized void addRepaint(GetGraphicsOutputEvent event) { 

		assert event.representsWindow() : "A repaint can't be represented by a getGraphics on an Image: " + event;

		maxWidth = Math.max(maxWidth, event.getWidth());
		maxHeight = Math.max(maxHeight, event.getHeight());

		if(image != null && (maxWidth > image.getWidth() || maxHeight > image.getHeight()))
			image = null;
		
		if(image == null)
			image = new BufferedImage(maxWidth, maxHeight, BufferedImage.TYPE_INT_ARGB);

		Repaint repaint = new Repaint(event, image);
				
		repaints.add(repaint);
		
		// Now we prepare the next repaint. Restore the graphics context back to this view's context.
		currentRepaintsGraphics = (Graphics2D)image.getGraphics();
		currentRepaintsGraphics.addRenderingHints(new RenderingHints(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON)); 
		currentRepaintsGraphics.addRenderingHints(new RenderingHints(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)); 

		// Find the id that we're starting with and match it with the current context
		GetGraphicsOutputEvent getGraphics = repaint.getGetGraphicsEvent();
		if(getGraphics != null) {
			
			currentRepaintsGraphics.translate(getGraphics.getTranslateX(), getGraphics.getTranslateY());
			contextsByContextIDs.put(getGraphics.getGraphicsID(), new GraphicsContext(this, currentRepaintsGraphics, getGraphics.getTranslateX(), getGraphics.getTranslateY(), true));
			
		}

	}
	
	public synchronized Image getImage(long imageID) {
		
		return imagesByImageID.get(imageID);
		
	}
	
	public synchronized SortedSet<DrawImageEvent> getDrawImagesSkipped() { return drawImagesSkipped;	}

	public synchronized void addEvent(GraphicalOutputEvent event) {
		
		// If the repaints are empty, add one so that we can add this event to it.
		if(!hasRepaints()) addRepaint(null);
		
		if(event instanceof RenderEvent) getLastRepaint().addEvent((RenderEvent)event);
		else if(event instanceof ModifyClipEvent) clipEvents.add((ModifyClipEvent)event);

		// If we're finding a get graphics output event that's not a repaint marker, that it's an image.
		if(event instanceof GetGraphicsOutputEvent) {
			
			GetGraphicsOutputEvent getImageEvent = ((GetGraphicsOutputEvent)event);
			
			assert !getImageEvent.representsWindow() : "This getGraphics event should have represented a repaint: " + event;
			
			BufferedImage image = new BufferedImage(getImageEvent.getWidth(), getImageEvent.getHeight(), BufferedImage.TYPE_INT_ARGB);
			
			Graphics2D imageContext = (Graphics2D)image.getGraphics();
			imageContext.addRenderingHints(new RenderingHints(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON)); 
			imageContext.addRenderingHints(new RenderingHints(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)); 
			
			long imageID = getImageEvent.getObjectID();
			long graphicsID = getImageEvent.getGraphicsID();
			
			imagesByImageID.put(imageID, image);
			contextsByContextIDs.put(graphicsID, new GraphicsContext(this, imageContext, getImageEvent.getTranslateX(), getImageEvent.getTranslateY(), false));
			imageIDByContextID.put(graphicsID, imageID);
			
		}
		// If we find a create event, replicate it.
		else if(event instanceof CreateGraphicsOutputEvent) {

			long sourceID = ((CreateGraphicsOutputEvent)event).getSourceID();
			GraphicsContext currentContext = contextsByContextIDs.get(sourceID);
			assert currentRepaintsGraphics != null : "Window " + getWindowID() + ": couldn't find graphics context " + sourceID + " to copy";
			long copyID = ((CreateGraphicsOutputEvent)event).getCopyID();
			assert contextsByContextIDs.get(copyID) == null : "Already have graphics for " + copyID;  
			contextsByContextIDs.put(copyID, new GraphicsContext(currentContext));
		
		}
		// Otherwise, find the appropriate context and execute the graphics event
		else {
			
			long id = event.getGraphicsID();

			Graphics2D graphicsToUse = null;
			GraphicsContext context = contextsByContextIDs.get(id); 

			assert context != null : "Couldn't find a window or image context matching context ID " + id;

			// Update the graphics state with the
			if(event instanceof SetPaintEvent) context.setLatestPaintChange((SetPaintEvent)event);
			else if(event instanceof SetCompositeEvent) context.setLatestCompositeChange((SetCompositeEvent)event);
			else if(event instanceof SetFontEvent) context.setLatestFontChange((SetFontEvent)event);
			else if(event instanceof ModifyClipEvent) context.setLatestClipChange((ModifyClipEvent)event);
			else if(event instanceof SetStrokeEvent) context.setLatestStrokeChange((SetStrokeEvent)event);
			else if(event instanceof ModifyTransformEvent) context.setLatestTransformChange((ModifyTransformEvent)event);
			else if(event instanceof SetBackgroundEvent) context.setLatestBackgroundChange((SetBackgroundEvent)event);
			
			// If this draw image is on an offscreen buffer, draw the one we've been drawing to to this window.
			if(event instanceof DrawImageEvent && imagesByImageID.containsKey(((DrawImageEvent)event).getImageID())) {

				DrawImageEvent drawImage = (DrawImageEvent)event;
				
				long imageID = drawImage.getImageID();
				Image image = imagesByImageID.get(imageID);

				// Paint it, then remember the context.
				drawImage.paintWithImage(context.getGraphics(), image);
				drawImage.rememberOffscreenBuffer(image);
				drawImage.rememberContext(context);

				List<DrawImageEvent> drawImages = drawImageEventByImageID.get(imageID);
				if(drawImages == null) {
					drawImages = new ArrayList<DrawImageEvent>();
					drawImageEventByImageID.put(imageID, drawImages);
				}
				drawImages.add(drawImage);
				
			}
			else {
				
				// Paint it (so that we modify the graphics if this is a set color, etc.), then remember it.
				event.paint(context.getGraphics());
				if(event instanceof RenderEvent)
					((RenderEvent)event).rememberContext(context);				
				
			}

			// Keep track of rendered content's visibility.
			if(event instanceof RenderEvent) {
				
				// Does this event occlude any of the visible events? If so,
				// mark their end point and and add this to the visible events.
				if(((RenderEvent)event).canOcclude()) {
				
					Shape occluder = ((RenderEvent)event).getUnclippedShape();
	
					LinkedList<RenderEvent> occludedEvents = new LinkedList<RenderEvent>();
					
					// Find all visible events occluded by the current render event.
					for(RenderEvent renderEvent : visibleEvents)
						if(!renderEvent.hasOccluder() && ((RenderEvent)event).occludes(renderEvent))
							occludedEvents.add(renderEvent);
	
					// Set the occluder of all events occluded by the current event to the current event, 
					// and remove them from the visible events set.
					for(RenderEvent renderEvent : occludedEvents) {
						renderEvent.setOccluder((RenderEvent)event);
						visibleEvents.remove(renderEvent);
					}
					
				}
				
				visibleEvents.add((RenderEvent)event);
					
			}
			
		}		
		
	}

	// Returns the bounds of the window drawn.
	public synchronized Rectangle drawWindowAtEventID(int eventID, BufferedImage targetImage) {
		
		// This block determines the size based on the current time.
		Repaint lastRepaint = null;
		for(Repaint repaint : repaints) {

			if(repaint.getGetGraphicsEvent().representsWindow()) lastRepaint = repaint;
			if(repaint.getGetGraphicsEvent().getEventID() > eventID)
				break;
			
		}
		
		int newWidth = lastRepaint.getGetGraphicsEvent().getWidth();
		int newHeight = lastRepaint.getGetGraphicsEvent().getHeight();

		int newX = 0;
		int newY = 0;
		
		Graphics targetContext = targetImage.createGraphics();
		
		Image lastImageDrawn = null;
		
		// The window parser may still be parsing this, so we need to make sure we lock on this before we loop through the repaints.
		synchronized(this) {
		
			for(Repaint repaint : repaints) {

				if(repaint.getGetGraphicsEvent().getEventID() > eventID)
					break;
				
				int x = (int) repaint.getGetGraphicsEvent().getWindowX(); 
				int y = (int) repaint.getGetGraphicsEvent().getWindowY(); 
				if(!(x == 0 && y == 0)) {
					newX = x;
					newY = y;
				}
				
				// Don't draw past the point we've parsed to.
				if(parser.getLastEventIDParsed() < repaint.getLastEventID())
					break;
				
				// If this is a new one...
				if(lastImageDrawn != repaint.getImage()) {
	
					// if we're done with an image, render it into the target.
					if(lastImageDrawn != null)
						targetContext.drawImage(lastImageDrawn, 0, 0, null);
					
					lastImageDrawn = repaint.getImage();
	
					// Clear the new image	
					Graphics2D imageGraphics = (Graphics2D)lastImageDrawn.getGraphics();
					imageGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
					imageGraphics.setColor(UI.getPanelLightColor());
					imageGraphics.fillRect(0, 0, lastImageDrawn.getWidth(null), lastImageDrawn.getHeight(null));				
					
				}
	
				lastRepaint = repaint;
				
				if(repaint.getGetGraphicsEvent().getEventID() > eventID) break;
				
				for(RenderEvent event : repaint) {
					
					if(event.getEventID() > eventID)
						break;
										
					if(event.isVisibleAfter(eventID))
						event.paintByMemory();

				}
				
			}
			
		}

		targetContext.drawImage(lastImageDrawn, 0, 0, null);
		
		return new Rectangle(newX, newY, newWidth, newHeight);
		
	}

	public synchronized Rectangle getEntityBoundsBefore(long entityID, int eventID) {

		Rectangle bounds = null;

		for(Repaint repaint : repaints) {
			if(repaint.getGetGraphicsEvent().getEventID() > eventID) break;
			for(RenderEvent event : repaint) {
				if(event.isVisibleAfter(eventID)) {
					// Was this drawn by the desired instance?
					// If so, add its bounds to the entity's bounds.
					long entity = event.getInstanceResponsible();
					if(entity == entityID) {
						Rectangle rect = event.getClippedBoundaries();
						if(bounds == null) bounds = rect;
						else bounds = bounds.union(rect);
					}
				}
			}
		}
		
		// If this entity didn't paint anything, find the most recent clip event associated with this entity.
		if(bounds == null) {
			// Aggregate all indirectly related render events.
			for(Repaint repaint : repaints) {
				if(repaint.getGetGraphicsEvent().getEventID() > eventID) break;
				for(RenderEvent event : repaint) {
					if(event.isVisibleAfter(eventID)) {
						if(event.isIndirectlyRenderedBy(entityID)) {
							Rectangle rect = event.getClippedBoundaries();
							if(bounds == null) bounds = rect;
							else bounds = bounds.union(rect);
						}
					}
				}
			}
						
		}
		
		return bounds;

	}
	
	public synchronized SortedSet<GraphicalEventAppearance> getRenderEventsAtLocationAfterEventID(int x, int y, int eventID) {
		
		SortedSet<GraphicalEventAppearance> events = new TreeSet<GraphicalEventAppearance>();

		drawImagesSkipped.clear();

		boolean done = false;
		for(Repaint repaint : repaints) {
			if(done) break;
			for(RenderEvent event : repaint) {

				if(event.getEventID() > eventID) {
					done = true;
					break;
				}

				long graphicsID = event.getGraphicsID();
				GraphicsContext context = contextsByContextIDs.get(graphicsID);
				// If this was an image drawn into a window, we want to wait until we find the primitive paint.
				if(!context.representsWindow()) {

					// Where was it actually drawn on screen? Was the image available at this time?
					// To answer these questions, we need to find the image draw event that drew the image that
					// generated this events graphics context.

					// graphicsID -> imageID -> draw image event
					// Find all of the draws of this image and see if any of them are visible after the given event
					// and contain the given point.
					long imageID = imageIDByContextID.get(graphicsID);
					List<DrawImageEvent> drawImages = drawImageEventByImageID.get(imageID);
					if(drawImages != null) {
						for(DrawImageEvent draw : drawImages) {
							if(draw.isVisibleAfter(eventID) && draw.contains(x, y)) {
								event.setRenderers(drawImages);
								drawImagesSkipped.add(draw);
								// Now we need to see if the mouse is over the specific render event. To do this, we'll translate the x, y coordinates to the image coordinate space.
								if(event.contains(x - draw.getWindowX(), y - draw.getWindowY())) {
									events.add(new GraphicalEventAppearance(event, draw));
								}
							}
						}						
					}
				}
				// If this is visible and contains the point, and is NOT a draw image event that we've skipped above...
				else if(event.isVisibleAfter(eventID) && event.contains(x, y) && !drawImagesSkipped.contains(event))  {
					events.add(new GraphicalEventAppearance(event, event));
				}
				
			}
		}
		
		return events;

	}
	
	/**
	 * Represents a sequence of graphical output events that occurred after a getGraphics() call during the execution
	 * of a Java Swing program.
	 * 
	 * @author Andrew J. Ko
	 *
	 */
	private static class Repaint implements Iterable<RenderEvent> {

		private final GetGraphicsOutputEvent event; 
		private final LinkedList<RenderEvent> eventsByRepaint = new LinkedList<RenderEvent>();
		private final Image image;
		
		public Repaint(GetGraphicsOutputEvent event, Image image) {
			
			this.event = event;
			this.image = image;
			
		}

		public GetGraphicsOutputEvent getGetGraphicsEvent() { return event; }
		public boolean hasEvents() { return eventsByRepaint.size() > 0; }
		public void addEvent(RenderEvent event) { assert event != null : "Event is null!"; eventsByRepaint.add(event); }
		public int getLastEventID() { return eventsByRepaint.isEmpty() ? -1 : eventsByRepaint.getLast().getEventID(); }
		
		public Image getImage() { return image; }
		
		public Iterator<RenderEvent> iterator() { return eventsByRepaint.iterator(); }
		
	}

}
