package edu.cmu.hcii.whyline.trace;

import java.util.ArrayList;
import java.util.Iterator;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.util.*;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class CallStack implements Iterable<CallStackEntry> {

	private int eventIDLastExecuted = -1;
	private final Trace trace;
	private final int threadID;
	private final Listener listener;

	private final ArrayList<CallStackEntry> callStack = new ArrayList<CallStackEntry>(3);
	private final ArrayList<InvocationInfo> invocationsWaitingForStartMethods = new ArrayList<InvocationInfo>(2);
	private final IntegerVector startsWaitingForReturns;

	public CallStack(Trace trace, int threadID, int firstEventID, Listener listener) {

		this.eventIDLastExecuted = firstEventID;
		this.trace = trace;
		this.threadID = threadID;
		this.startsWaitingForReturns = new IntegerVector(4);
		this.listener = listener;
		
		handleNextEventID(firstEventID);
		
	}

	public CallStack(Trace trace, int eventID) {

		this.trace = trace;
		this.eventIDLastExecuted = eventID;
		this.threadID = trace.getThreadID(eventID);
		this.startsWaitingForReturns = null;
		this.listener = null;
		
		int event = eventID;
		while(event >= 0) {
			// Find the startID of the current event.
			int startID = trace.getStartID(event);
			int invocationID = startID >= 0 ? trace.getStartIDsInvocationID(startID) : -1; 
			this.callStack.add(0, new CallStackEntry(trace, startID >= 0 ? trace.getInstruction(startID).getMethod() : null, invocationID, startID));
			event = invocationID; 
		}
		
	}
	
	public CallStack(Trace trace, int threadID, CallStack cs) {

		this.trace = trace;
		this.threadID = threadID;
		this.listener = null;
		
		if(cs != null) {
		
			this.eventIDLastExecuted = cs.eventIDLastExecuted;
			
			for(CallStackEntry m : cs.callStack)
				this.callStack.add(m);

			this.startsWaitingForReturns = new IntegerVector(cs.startsWaitingForReturns);
			
			for(InvocationInfo i : cs.invocationsWaitingForStartMethods)
				this.invocationsWaitingForStartMethods.add(i);
		
		}
		else
			startsWaitingForReturns = new IntegerVector(3);
		
	}
	
	public int getThreadID() { return threadID; }
	
	/**
	 * As a site effect, associates invocations, method starts, returns, and exception.
	 * @param event
	 */
	public void handleNextEventID(int eventID) {
				
		EventKind kind = trace.getKind(eventID);
		
		switch(kind) {
		
			// We only start a new frame if we reach a start method event. That's because some invocations weren't traced.
			case START_METHOD :
	
				MethodInfo methodStarting = trace.getInstruction(eventID).getMethod();

				startsWaitingForReturns.push(eventID);
				
				// If the method this started is implicitly invoked, then we shouldn't have an invocation waiting.
				// If the method this started doesn't match the signature of the invocation on the stack, then we don't use it either.
				int invocationWaiting = invocationsWaitingForStartMethods.isEmpty() ? -1 : (invocationsWaitingForStartMethods.get(invocationsWaitingForStartMethods.size() - 1)).invocationEventID;
				int correspondingInvocationID = 
					methodStarting.isImplicitlyInvoked() ? -1 : 
						invocationWaiting < 0 ? -1 : 
							invocationsWaitingForStartMethods.remove(invocationsWaitingForStartMethods.size() - 1).invocationEventID;
	
				// This skips invocations that we didn't trace.
				if(	correspondingInvocationID >= 0 && 
					!((Invoke)trace.getInstruction(correspondingInvocationID)).getMethodInvoked().getMethodNameAndDescriptor().equals(methodStarting.getMethodNameAndDescriptor())) {
					correspondingInvocationID = -1;
				}
	
				// If we found a corresponding invocation, remember it.
				if(correspondingInvocationID >= 0) {
					if(listener != null) {
						listener.foundInvocationStartPair(correspondingInvocationID, eventID);
					}
				}
				
				callStack.add(new CallStackEntry(trace, methodStarting, correspondingInvocationID, eventID));
	
				break;
				
			case RETURN :
				
				MethodInfo methodReturning = trace.getInstruction(eventID).getMethod();
				if(startsWaitingForReturns.size() > 0) {
					int correspondingStartEventID = startsWaitingForReturns.pop();
					if(listener != null)
						listener.foundStartReturnOrCatchPair(correspondingStartEventID, eventID);
					callStack.remove(callStack.size() - 1);
				}
								
				popInvocationsThatWereNotTraced();
	
				break;
				
			// Pop the call stack until we reach the method this exception was caught in
			case EXCEPTION_CAUGHT :
	
				MethodInfo methodWithException = trace.getInstruction(eventID).getMethod();

				while(!callStack.isEmpty() && callStack.get(callStack.size() - 1).getMethod() != methodWithException) {
					callStack.remove(callStack.size() - 1);
					if(listener != null)
						listener.foundStartReturnOrCatchPair(startsWaitingForReturns.pop(), eventID);
				}
	
				popInvocationsThatWereNotTraced();
	
				assert !callStack.isEmpty() : "An exception can't pop ALL the frames off the stack. It must have been caught somewhere.";
	
				break;
	
			// If we see an invocation, the next StartMethodEvent could be of the method this called.
			// Or this could be an invocation of something we didn't trace, in which case we should ignore it.
			case INVOKE_INTERFACE :
			case INVOKE_VIRTUAL :
			case INVOKE_STATIC :
			case INVOKE_SPECIAL :
				
				invocationsWaitingForStartMethods.add(new InvocationInfo(eventID, callStack.size()));
				break;

		}
		
		eventIDLastExecuted = eventID;
		
	}
	
	private void popInvocationsThatWereNotTraced() {

		int currentDepth = callStack.size();
		while(invocationsWaitingForStartMethods.size() > 0 && invocationsWaitingForStartMethods.get(invocationsWaitingForStartMethods.size() - 1).depth > currentDepth) {
			invocationsWaitingForStartMethods.remove(invocationsWaitingForStartMethods.size() - 1);
		}

	}
	
	public Iterator<CallStackEntry> iterator() { return callStack.iterator(); }

	public CallStackEntry getEntryAt(int index) { return callStack.get(index); }
	
	public CallStackEntry getEntryOnTop() { return callStack.isEmpty() ? null : callStack.get(callStack.size() - 1); }
	
	public int getEventIDLastExecuted() { return eventIDLastExecuted; }
	
	public int getDepth() { return callStack.size(); }
	
	public String toString() {
		
		String s = "Call stack for thread " + trace.getThreadName(threadID) + "\n\nafter executing " + eventIDLastExecuted;
		
		for(int i = callStack.size() - 1; i >= 0; i--)
			s = s + "\n" + callStack.get(i).getMethod().getQualifiedNameAndDescriptor();
		
		return s;
		
	}
	
	private static class InvocationInfo {
		
		public final int invocationEventID;
		public final int depth;
		
		public InvocationInfo(int invocationEventID, int depth) {
			
			this.invocationEventID = invocationEventID; 
			this.depth = depth;
			
		}
		
	}
	
	public static interface Listener {
		
		public void foundInvocationStartPair(int invocationID, int startID);
		public void foundStartReturnOrCatchPair(int startID, int returnOrCatchID);
		
	}
	
}