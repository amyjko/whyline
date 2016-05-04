package edu.cmu.hcii.whyline.trace;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.bytecode.Invoke;
import edu.cmu.hcii.whyline.util.IntegerVector;
import edu.cmu.hcii.whyline.util.Saveable;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class ThreadStartHistory implements Saveable {

	private final Trace trace;

	private IntegerVector threadStarts = new IntegerVector(100);
	
	public ThreadStartHistory(Trace trace) {

		this.trace = trace;
	
	}

	public void addThreadStartTime(int timeOfThreadStart) {
		
		threadStarts.append(timeOfThreadStart);
		
	}
	
	public int getMostRecentExecutionOfBefore(Invoke threadStart, int eventID) {

		for(int i = threadStarts.size() - 1; i >= 0; i--) {
			
			int id = threadStarts.get(i);
			if(id < eventID) {
				if(trace.getInstruction(id) == threadStart)
					return id; 
			}
			
		}
		
		return -1;
		
	}

	public void write(DataOutputStream out) throws IOException {

		threadStarts.write(out);
		
	}

	public void read(DataInputStream in) throws IOException {

		threadStarts.read(in);
		
	}
	
}
