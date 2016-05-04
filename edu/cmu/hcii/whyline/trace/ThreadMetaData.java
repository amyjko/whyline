package edu.cmu.hcii.whyline.trace;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class ThreadMetaData {

	public final String name;
	public final int threadID; 
	public final long objectID;
	public final int numberOfEventsInThread;
	/**
	 * This CANNOT be trusted. I haven't recorded this in a thread safe manner, so other threads can proceed while the thread
	 * tracer is initialized at record time.
	 */
	public final int firstEventID;
	public final int lastEventID;

	public ThreadMetaData(DataInputStream data) throws IOException {
		
		this.name = data.readUTF();
		this.threadID = data.readInt();
		this.objectID = data.readLong();
		this.numberOfEventsInThread = data.readInt();
		this.firstEventID = data.readInt();
		this.lastEventID = data.readInt();
		
	}
	
	public String toString() { return "thread " + name + " (events " + firstEventID + " - " + lastEventID + ", " + numberOfEventsInThread + " total)"; }
	
}
