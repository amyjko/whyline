package edu.cmu.hcii.whyline.trace.nodes;

import edu.cmu.hcii.whyline.trace.Trace;
import gnu.trove.TIntObjectHashMap;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class ThreadState extends DynamicNode<FrameState> implements Comparable<ThreadState> {

	private final TIntObjectHashMap<FrameState> frames = new TIntObjectHashMap<FrameState>();

	private final Trace trace;
	private final int  threadID;
	private int currentEventID = -1;
	
	public ThreadState(Trace trace, int threadID) {

		this.trace = trace;
		this.threadID = threadID;
		
	}
	
	public String toString() { return getName(); }

	public String getName() { return  trace.getThreadName(threadID); }
	
	public int compareTo(ThreadState object) { 

		int names = getName().compareTo(object.getName());
		
		if(currentEventID < 0) return names;
		else if(trace.getThreadID(currentEventID) == threadID) return Integer.MIN_VALUE;
		else return names;
		
	}

	public boolean isLeaf() { return false; }

	private FrameState createFrame(int startID, int invocationID) {
		
		FrameState frame = frames.get(startID);
		if(frame == null) {
			frame = new FrameState(trace, startID, invocationID);
			frames.put(startID, frame);
		}
		return frame;
		
	}
	
	protected void determineChildren() {

		int eventID = currentEventID;
		
		eventID = trace.getThreadEventIDNearest(threadID, eventID);
		
		while(eventID >= 0) {
			// Find the startID of the current event.
			int startID = trace.getStartID(eventID);
			if(startID >= 0) {
				FrameState frame = createFrame(startID, eventID); 
				frame.showEventID(currentEventID);
				addChild(frame);
				// Find the invocation ID of the startID
				int invocationID = trace.getStartIDsInvocationID(startID); 
				eventID = invocationID;
			}
			else break;
		}

	}

	public int getThreadID() { return threadID; }

	public void showEventID(int eventID) {
		
		if(currentEventID != eventID) {
			
			currentEventID = eventID;
			
			resetChildren();
			for(FrameState frame : getChildren())
				frame.showEventID(eventID);
			
		}
		
	}

	public FrameState getFrameFor(int eventID) {
		
		return frames.get(trace.getStartID(eventID));
		
	}

	public int getAssociatedEventID() { return trace.getThreadFirstEventID(threadID); }

	public String getAssociatedEventIDDescription() { return "thread start"; }

	protected boolean performUpdate() { return false; }

}