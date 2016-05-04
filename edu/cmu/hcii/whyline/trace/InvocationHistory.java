package edu.cmu.hcii.whyline.trace;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

import edu.cmu.hcii.whyline.Whyline;
import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.util.IntegerVector;
import edu.cmu.hcii.whyline.util.Saveable;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class InvocationHistory implements Saveable {

	private final Trace trace;

	private final gnu.trove.TIntObjectHashMap<Map<MethodInfo, IntegerVector>> startIDsByThreadByMethod;
	private final Map<Invoke, IntegerVector> invocationIDsByInvocation;
	
	public InvocationHistory(Trace trace) {

		this.trace = trace;
		
		startIDsByThreadByMethod = new gnu.trove.TIntObjectHashMap<Map<MethodInfo, IntegerVector>>(trace.getNumberOfThreads());
		invocationIDsByInvocation = new HashMap<Invoke, IntegerVector>(trace.getNumberOfMethods());
		
	}
	
	public void addInvocationID(int invocationID) {

		Invoke invoke = (Invoke)trace.getInstruction(invocationID);
		
		IntegerVector invocations = invocationIDsByInvocation.get(invoke);
		if(invocations == null) {
			invocations = new IntegerVector(10);
			invocationIDsByInvocation.put(invoke, invocations);
		}
		invocations.append(invocationID);
		
	}

	public void addStartID(int startID, int threadID) {

		MethodInfo method = trace.getInstruction(startID).getMethod();
		
		Map<MethodInfo,IntegerVector> startIDsByMethod = startIDsByThreadByMethod.get(threadID);
		if(startIDsByMethod == null) {
			startIDsByMethod = new HashMap<MethodInfo, IntegerVector>(trace.getNumberOfMethods() / 2);
			startIDsByThreadByMethod.put(threadID, startIDsByMethod);
		}

		IntegerVector startIDs = startIDsByMethod.get(method);
		if(startIDs == null) {
			startIDs = new IntegerVector(2);
			startIDsByMethod.put(method, startIDs);
		}
		startIDs.append(startID);
		
	}
	
	public int determineStartMethodIDOf(int eventID) {
		
		if(eventID < 0) return -1;
		
		MethodInfo method = trace.getInstruction(eventID).getMethod();
		
		int threadID = trace.getThreadID(eventID);
		Map<MethodInfo,IntegerVector> byMethod = startIDsByThreadByMethod.get(threadID);
		IntegerVector startIDs = byMethod.get(method);
		int indexOfMostRecentStartID = startIDs.getIndexOfLargestValueLessThanOrEqualTo(eventID);

		// This only happens with old traces 
		if(indexOfMostRecentStartID < 0) {
			Whyline.debug("Must be an old trace, because the given event occurs before the startID representing its methods call. I've since fixed this bug, but this trace is out of date.");
			indexOfMostRecentStartID = 0;
		}
		
		int startID = startIDs.get(indexOfMostRecentStartID);

		int correspondingReturnID = trace.getStartIDsReturnOrCatchID(startID);
		// Keep going to earlier startIDs until we find a corresponding return that happened after the given event.
		//  We also stop if there is no known return yet for the current startID, since this indicates that the method hasn't finished executing (or we haven't loaded it yet.)
		while(correspondingReturnID != -1 && correspondingReturnID < eventID) {
			indexOfMostRecentStartID--;
			if(indexOfMostRecentStartID < 0) break;
			startID = startIDs.get(indexOfMostRecentStartID);
			correspondingReturnID = trace.getStartIDsReturnOrCatchID(startID);
		}

		return startID;

	}

	public IntegerVector findInvocationsOfPublicStateAffectingMethodsWithParametersOnObjectIDBefore(long objectID, int beforeID) {
		
		IntegerVector results = new IntegerVector(10);

		int initID = trace.getInitializationOfObjectID(objectID);		
		
		IntegerVector invocationsToCheck = new IntegerVector(100);
		
		Classfile classfile = trace.getClassfileOfObjectID(objectID);
		if(classfile != null) {
			for(MethodInfo method : classfile.getPublicInstanceMethods()) {
				if(method.isStateAffecting() && method.getNumberOfArguments() > 0) {
					
					for(Invoke invoke : method.getPotentialCallers()) {
						IntegerVector invocations = invocationIDsByInvocation.get(invoke);
						if(invocations != null) {
							int indexJustBeforeEventID = invocations.getIndexOfLargestValueLessThanOrEqualTo(beforeID);
							// Start before the given eventID, stop at the beginning of the trace or after the instantiation of the object.
							for(int i = indexJustBeforeEventID; i >= 0; i--) {
								int invocationID = invocations.get(i);
								// If we're now before this objecti's invocation, quit this loop and try another invocation.
								if(initID > 0 && invocationID < initID)
									break;
								if(invocationID != beforeID)
									invocationsToCheck.append(invocationID);
							}
						}
					}
					
				}
			}
		}
		
		// Now that we have all of the IDs to check, sort them so that we don't thrash the caching mechanisms of the trace.
		invocationsToCheck.sortInAscendingOrder();
		
		// Then check in increasing order
		for(int i = 0; i < invocationsToCheck.size(); i++) {
			
			int invocationID = invocationsToCheck.get(i);
			if(trace.getInvocationInstanceID(invocationID) == objectID)
				results.append(invocationID);
			
		}
		
		return results;
		
	}


	public IntegerVector findInvocationsOnObjectIDAfterEventID(Invoke invoke, long objectID, int eventIDAfter) {

		IntegerVector results = new IntegerVector(10);
		
		IntegerVector invocations = invocationIDsByInvocation.get(invoke);
		
		if(invocations != null) {
			
			int indexJustBeforeEventID = invocations.getIndexOfLargestValueLessThanOrEqualTo(eventIDAfter);
			if(indexJustBeforeEventID < 0) indexJustBeforeEventID = 0;
			for(int i = indexJustBeforeEventID; i < invocations.size(); i++) {

				int invocationID = invocations.get(i);
				if(invocationID >= eventIDAfter) {
					if(objectID == 0 || trace.getInvocationInstanceID(invocationID) == objectID)
						results.append(invocationID);
				}
				
			}
			
		}
	
		return results;
		
	}
	
	public IntegerVector getStartIDsAfterEventID(MethodInfo method, int eventIDAfter) {
		
		return getStartIDsOnObjectIDAfterEventID(method, -1, eventIDAfter);
		
	}
	
	/**
	 * Passing an objectID <= 0 results in all calls of the given method after the given event.
	 */
	public IntegerVector getStartIDsOnObjectIDAfterEventID(MethodInfo method, long objectID, int eventIDAfter) {
		
		IntegerVector results = new IntegerVector(10);
		
		for(int threadID : startIDsByThreadByMethod.keys()) {
		
			Map<MethodInfo,IntegerVector> startIDsByMethod = startIDsByThreadByMethod.get(threadID);
			IntegerVector starts = startIDsByMethod.get(method);
			if(starts != null) {
			
				int indexJustBeforeEventID = starts.getIndexOfLargestValueLessThanOrEqualTo(eventIDAfter);
				if(indexJustBeforeEventID < 0) indexJustBeforeEventID = 0;
				for(int i = indexJustBeforeEventID; i < starts.size(); i++) {
					
					int startID = starts.get(i);
					if(startID >= eventIDAfter) {
						if(objectID <= 0)
							results.append(startID);
						else {
								int invocationID = trace.getStartIDsInvocationID(startID);
								if((invocationID >= 0 && trace.getInvocationInstanceID(invocationID) == objectID))
									results.append(startID);
						}
					}
					
				}
				
			}
		
		}
	
		return results;
		
	}
	
	public void trimToSize() {
				
		for(IntegerVector ids : invocationIDsByInvocation.values())
			ids.trimToSize();

	}

	public void write(DataOutputStream out) throws IOException {

		out.writeInt(startIDsByThreadByMethod.size());
		for(int threadID : startIDsByThreadByMethod.keys()) {
			out.writeInt(threadID);
			Map<MethodInfo, IntegerVector> startIDsByMethod = startIDsByThreadByMethod.get(threadID);
			out.writeInt(startIDsByMethod.size());
			for(MethodInfo method : startIDsByMethod.keySet()) {
				out.writeUTF(method.getClassfile().getInternalName().getText());
				out.writeUTF(method.getMethodNameAndDescriptor());
				startIDsByMethod.get(method).write(out);
			}
		}

		out.writeInt(invocationIDsByInvocation.size());
		for(Invoke invoke : invocationIDsByInvocation.keySet()) {
			out.writeInt(trace.getInstructionIDFor(invoke));
			invocationIDsByInvocation.get(invoke).write(out);
		}

	}

	public void read(DataInputStream in) throws IOException {

		int byThreadSize = in.readInt();
		for(int i = 0; i < byThreadSize; i++) {
			int threadID = in.readInt();
			int byMethodSize = in.readInt();
			Map<MethodInfo, IntegerVector> startIDsByMethod = new HashMap<MethodInfo, IntegerVector>(byMethodSize);
			for(int j = 0; j < byMethodSize; j++) {
				Classfile classfile = trace.getClassfileByName(QualifiedClassName.get(in.readUTF()));
				MethodInfo method = classfile.getDeclaredMethodByNameAndDescriptor(in.readUTF());
				startIDsByMethod.put(method, new IntegerVector(in));
			}
			startIDsByThreadByMethod.put(threadID, startIDsByMethod);
		}

		int byInvocationSize = in.readInt();
		for(int i = 0; i < byInvocationSize; i++)
			invocationIDsByInvocation.put((Invoke) trace.getInstructionWithID(in.readInt()), new IntegerVector(in));
		
	}
	
}