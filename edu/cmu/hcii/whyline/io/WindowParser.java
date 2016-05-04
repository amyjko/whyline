package edu.cmu.hcii.whyline.io;

import java.util.*;

import edu.cmu.hcii.whyline.Whyline;
import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.util.Util;
import edu.cmu.hcii.whyline.util.Util.ProgressListener;
import gnu.trove.TLongLongHashMap;
import gnu.trove.TLongObjectHashMap;

/**
 * @author Andrew J. Ko
 *
 */
public class WindowParser {
	
	private final Trace trace;

	private boolean parsed = false;
	
	private Thread parser = null;

	private TLongObjectHashMap<WindowState> windowsByWindowID = new TLongObjectHashMap<WindowState>(3);
	private TLongObjectHashMap<WindowState> windowsByGraphicsID = new TLongObjectHashMap<WindowState>(10000);
	
	private TLongLongHashMap imageIDsByImageContextIDs = new TLongLongHashMap(1000);
	private TLongObjectHashMap<List<GraphicalOutputEvent>> unclaimedImageOutputEventsByImageID = new TLongObjectHashMap<List<GraphicalOutputEvent>>(50); 
	
	private int lastEventIDParsed = -1;

	public WindowParser(Trace trace) {

		this.trace = trace;
		
	}

	public int getLastEventIDParsed() { return lastEventIDParsed; }

	private void addUnclaimedImageEvent(long imageID, GraphicalOutputEvent event) {
		
		List<GraphicalOutputEvent> imageEvents = unclaimedImageOutputEventsByImageID.get(imageID);
		if(imageEvents == null) {
			imageEvents = new ArrayList<GraphicalOutputEvent>(20);
			unclaimedImageOutputEventsByImageID.put(imageID, imageEvents);
		}
		imageEvents.add(event);

	}	
	
	public void parse(ProgressListener progressListener) {
		
		int repaint = 0;
		
		// Start parsing from event after the last event parsed.
		IOHistory<GraphicalOutputEvent> output = trace.getGraphicsHistory();
		int count = output.getNumberOfEvents();
		for(int i = 0; i < count; i++) {
						
			GraphicalOutputEvent event = output.getEventAtIndex(i);
			
			// This is how we attach graphics contexts to windows.
			if(event instanceof GetGraphicsOutputEvent) {

				GetGraphicsOutputEvent getGraphics = ((GetGraphicsOutputEvent)event); 
				long objectID = getGraphics.getObjectID();
				
				if(getGraphics.representsWindow()) {
											
					long windowID = getGraphics.getObjectID();
					
					// Is there a window for this window id? If not, make one.
					WindowState window = windowsByWindowID.get(windowID);
					
					if(window == null) {

						window = new WindowState( WindowParser.this, (GetGraphicsOutputEvent)event);
						windowsByWindowID.put(windowID, window);
						trace.addWindow(window);
						
					}
					// Otherwise, mark this as a repaint in the existing window.
					else {
					
						// In case we're painting while we do this.
						synchronized(window) {
							window.addRepaint((GetGraphicsOutputEvent)event);
						}

						repaint++;
						if(repaint == 24) {
							repaint = 0;
							progressListener.notice("Parsing I/O events (" + Util.commas(count - i) + " remaining)");
							progressListener.progress(((double)i) / count);
						}
						
					}

					// Remember the graphics context for this window, so we can chain together contexts that are cloned from this one.
					long graphicsID = ((GetGraphicsOutputEvent)event).getGraphicsID();
					windowsByGraphicsID.put(graphicsID, window);

				}
				// Otherwise, this represents an Image that's being draw to. Remember the mapping between images and their contexts
				// so we know which output events 
				else {
					
					long imageID = getGraphics.getObjectID();
					imageIDsByImageContextIDs.put(getGraphics.getGraphicsID(), imageID);
					addUnclaimedImageEvent(imageID, getGraphics);
					
				}

			}
			else if(event instanceof CreateGraphicsOutputEvent) {
				
				long sourceID = ((CreateGraphicsOutputEvent)event).getSourceID();
				long copyID = ((CreateGraphicsOutputEvent)event).getCopyID();

				WindowState windowForCopy = windowsByGraphicsID.get(sourceID);
				
				// If we found a window, map the window to the copied context and add the event.
				if(windowForCopy != null) {

					windowsByGraphicsID.put(copyID, windowForCopy);
					windowForCopy.addEvent(event);

				}
				else if(imageIDsByImageContextIDs.containsKey(sourceID)) {

					long imageID = imageIDsByImageContextIDs.get(sourceID);
					imageIDsByImageContextIDs.put(copyID, imageID);
					addUnclaimedImageEvent(imageID, event);
					
				}
				// This must be because we didn't capture a getGraphics() on the image.
				else Whyline.debug("Couldn't find a window or image for graphics ID " + sourceID + " which was used to create graphics ID " + copyID + " in " + event.getInstruction().getMethod().getQualifiedNameAndDescriptor());
				
			}
			// Find the window that this event belongs to and add the event to the window's list of events.
			else {

				long graphicsID = event.getGraphicsID();
				WindowState window = windowsByGraphicsID.get(graphicsID);
				if(window != null) window.addEvent(event);
				// If we couldn't find it, it's probably because it was not from a window, but from an image which was used to double buffer a window.
				// Collect these events in order, and then once we find a drawImage, we can add them to the appropriate window.
				else if(imageIDsByImageContextIDs.containsKey(graphicsID)) {
					
					long imageID = imageIDsByImageContextIDs.get(graphicsID);
					addUnclaimedImageEvent(imageID, event);
					
				}
				else Whyline.debug("Couldn't find a window or image to claim " + event + " with graphicsID " + graphicsID);
			
				if(event instanceof DrawImageEvent) {
					
					long imageID = ((DrawImageEvent)event).getImageID();
					
					List<GraphicalOutputEvent> unclaimedEvents = unclaimedImageOutputEventsByImageID.get(imageID);

					// If there are unclaimed events and a window to add them to, add them!
					// Then clear the list of events, because we only want to add them once.
					if(unclaimedEvents != null) {

						// If we have a window that matches this, then add them there.
						if(window != null) {
							
							for(GraphicalOutputEvent goe : unclaimedEvents) window.addEvent(goe);
							unclaimedImageOutputEventsByImageID.remove(imageID);
						
						}
						// If we have an event that matches this, then add them to this image's unclaimed list.
						else if(imageIDsByImageContextIDs.containsKey(graphicsID)) {

							long subImageID = imageIDsByImageContextIDs.get(graphicsID);
							
							for(GraphicalOutputEvent goe : unclaimedEvents) addUnclaimedImageEvent(subImageID, goe);
							unclaimedImageOutputEventsByImageID.remove(imageID);
							
						}
						else {

							Whyline.debug("Couldn't find a window to draw these unclaimed image output events onto: " + unclaimedEvents);
						
						}

					}
					
				}
				
			}
			
			lastEventIDParsed = event.getEventID();
			
		}

		parser = null;
		parsed = true;
		
	}
	
}