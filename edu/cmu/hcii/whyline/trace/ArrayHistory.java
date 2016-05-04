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
public final class ArrayHistory implements Saveable {

	private final Trace trace;
	
	private final gnu.trove.TLongObjectHashMap<IntegerVector> eventsByArrayID = new gnu.trove.TLongObjectHashMap<IntegerVector>();
	
	public ArrayHistory(Trace trace) {

		this.trace = trace;
	
	}
	
	private void storeArrayEventID(long arrayID, int eventID) {
		
		IntegerVector assignmentIDs = eventsByArrayID.get(arrayID);
		if(assignmentIDs == null) {
			assignmentIDs = new IntegerVector(10);
			eventsByArrayID.put(arrayID, assignmentIDs);
		}
		assignmentIDs.append(eventID);
		
	}
	
	public void addArrayAssignmentID(int eventID) { 
		
		long arrayID = trace.getSetArrayArraySet(eventID).getLong();
		if(arrayID > 0)
			storeArrayEventID(arrayID, eventID);

	}
	
	public void addToCharArrayID(int eventID) { 

			long arrayID = trace.getObjectIDProduced(eventID);
			if(arrayID > 0)
				storeArrayEventID(arrayID, eventID);
	
	}
	
	public void addArrayCopyID(int eventID) { 
		
			long arrayID = trace.getOperandStackValue(eventID, 2).getLong();
			if(arrayID > 0)
				storeArrayEventID(arrayID, eventID);
		
	}
	
	public Object getValueOfIndexAtTime(long arrayID, int index, int time) {
		
		return getSomethingAtIndexAtTime(arrayID, index, time, true);
		
	}
	
	/**
	 * 
	 * @param arrayID
	 * @param index
	 * @param time
	 * @return Returns either a SetArrayValueEvent or an InvocationEvent representing a call to System.arraycopy()
	 */
	public int getIndexAssignmentBefore(long arrayID, int index, int time) {
		
		return (Integer)getSomethingAtIndexAtTime(arrayID, index, time, false);
		
	}
	
	private Object getSomethingAtIndexAtTime(long arrayID, int index, int time, boolean returnValue) {
		
		IntegerVector arrayEvents = eventsByArrayID.get(arrayID);
		
		if(arrayEvents != null) {
			
			for(int i = arrayEvents.size() - 1; i >= 0; i--) {

				int arrayEventID = arrayEvents.get(i);
				EventKind kind = trace.getKind(arrayEventID);

				try {

					if(kind == EventKind.SETARRAY) {
					
						if(trace.getSetArrayIndexSet(arrayEventID).getInteger() == index) {
							if(returnValue)
								return trace.getSetArrayValueSet(arrayEventID).getValue(); 
							else 
								return arrayEventID;
						}
						
					} 
					// Was this a System.arraycopy() that affected the desired index?
					else if(kind.isInvocation) {
						
						String method = ((Invoke)trace.getInstruction(arrayEventID)).getMethodInvoked().getMethodName();

						if(method.equals("arraycopy")) {

							int destPos = trace.getOperandStackValue(arrayEventID, 3).getInteger();
							int length = trace.getOperandStackValue(arrayEventID, 4).getInteger();
	
							if(destPos <= index && destPos + length - 1 >= index) {
							
								long sourceID = trace.getOperandStackValue(arrayEventID, 0).getLong();
								if(sourceID < 0) return -1;
								int sourcePos = trace.getOperandStackValue(arrayEventID, 1).getInteger();
								return getSomethingAtIndexAtTime(sourceID, sourcePos + (index - destPos), time, returnValue);
								
							}
							
						}
						else if(method.equals("toCharArray")) {
							if(returnValue)
								return getCharFromToCharArrayEvent(arrayEventID, index);
							else
								return arrayEventID;
						}
						
					}
					else
						assert false : 
							"We looked up " + i + " in " + arrayEvents + ", which pointed to event " + arrayEvents.get(i) + " and got " + arrayEventID + 
							" instead of a SetArrayValueEvent. Here's the rest of the history:\n\n" + trace.getContextAroundEventAtIndex(arrayEventID, 30);
				
				} catch (NoValueException e) {

					// If we don't know the value, ignore it.

				}
				
			}
		
		}

		return returnValue ? null : -1;
		
	}
	
	private char getCharFromToCharArrayEvent(int arrayEventID, int index) {
		
		long stringID = trace.getOperandStackValue(arrayEventID, 0).getLong();
		if(stringID > 0) {
			String text = (String)trace.getImmutableObject(stringID);
			char c = text.charAt(index);
			return c;
		}
		else return '?';

	}
	
	public void trimToSize() {

	}

	public void write(DataOutputStream out) throws IOException {

		out.writeInt(eventsByArrayID.size());
		for(long objectID : eventsByArrayID.keys()) {
			out.writeLong(objectID);
			eventsByArrayID.get(objectID).write(out);
		}
		
	}

	public void read(DataInputStream in) throws IOException {

		int size = in.readInt();
		eventsByArrayID.ensureCapacity(size);
		for(int i = 0; i < size; i++)
			eventsByArrayID.put(in.readLong(), new IntegerVector(in));
		eventsByArrayID.trimToSize();
		
	}
	
}
