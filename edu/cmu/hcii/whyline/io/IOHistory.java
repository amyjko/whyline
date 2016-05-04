package edu.cmu.hcii.whyline.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import edu.cmu.hcii.whyline.trace.*;
import edu.cmu.hcii.whyline.util.Saveable;

/**
 * The output history consists of a sequence of "frames", which encompass a sequence of
 * output events from the history. The first thing we do is take the trace and extract
 * the output history from it.
 * 
 * @author Andrew J. Ko
 *
 */
public final class IOHistory<T extends IOEvent> implements Iterable<T>, Saveable {

	private final ArrayList<T> events;
	private final Trace trace;
	private final IOHistory<? super T> parent;
	
	public IOHistory(IOHistory<? super T> parent, Trace trace) {

		this.parent = parent;
		this.trace = trace;

		events = new ArrayList<T>(20000);

	}
	
	public void trimToSize() {
		
		events.trimToSize();
		
	}
	
	public void add(T event) { 
		
		assert events.isEmpty() || event.getEventID() > getLastEvent().getEventID() : "IO events must be added in order of occurrence.";
		
		events.add(event);
		if(parent != null) parent.add(event);
		
	}

	public int getNumberOfEvents() { return events.size(); }
	
	public T getEventAtIndex(int index) { return events.get(index); }
		
	public T getMostRecentBeforeTime(int time) {
		
		int index = events.size() - 1;
		
		while(index >= 0) {
			
			T e = events.get(index);
			if(e.getEventID() < time) {
				return e;
			}
			index--;
			
		}
		return null;
		
	}
	
	public T getEventAtTime(int time) {

		int index = events.size() - 1;
		
		while(index >= 0) {
			
			T e = events.get(index);
			if(e.getEventID() == time) return e;
			index--;
			
		}
		return null;
		
	}
	
	public T getNextAfterTime(int time) {
		
		int index = 0;
		
		while(index < events.size()) {
			
			T e = events.get(index);
			if(e.getEventID() > time) return e;
			index++;
			
		}
		return null;
		
		
	}

	public T getLastEvent() { return events.size() > 0 ? events.get(events.size() - 1) : null; }
	public T getFirstEvent() { return events.size() > 0 ? events.get(0) : null; }
	
	public Iterator<T> iterator() { return events.iterator(); }
	public Iterable<T> getIteratorForEventsAfter(T event) { return new EventsBeforeOrAfterIterator(true, event); }
	public Iterable<T> getIteratorForEventsBefore(T event) { return new EventsBeforeOrAfterIterator(false, event); }
	
	private class EventsBeforeOrAfterIterator implements Iterator<T>, Iterable<T> {
		
		private final boolean forward;
		private int historyIndex;
		
		public EventsBeforeOrAfterIterator(boolean forward, T startEvent) {
			
			this.forward = forward;
			historyIndex = startEvent == null ? 0 : events.indexOf(startEvent);
			if(historyIndex < 0) historyIndex = -1;			
			
			// Go the one after the given event.
			next();
			
		}
		
		public boolean hasNext() {
			
			if(historyIndex < 0) return false;
			return forward ? historyIndex <= events.size() - 1 : historyIndex >= 0; 
			
		}

		public T next() {

			if(!hasNext()) return null;

			T nextEvent = events.get(historyIndex);
			historyIndex += forward ? 1 : -1;
			return nextEvent;
			
		}

		public void remove() { throw new UnsupportedOperationException("Can't remove from IOHistory."); }
		
		public Iterator<T> iterator() { return this; }		
		
	}

	public void write(DataOutputStream out) throws IOException {
		
		out.writeInt(getNumberOfEvents());
		for(IOEvent io : events)
			out.writeInt(io.getEventID());

	}

	public void read(DataInputStream in) throws IOException {

		throw new UnsupportedOperationException("We write an IOHistory in IOHistory, but we read it in Trace.Loader");
		
	}
	
}
