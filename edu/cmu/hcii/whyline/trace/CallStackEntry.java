/**
 * 
 */
package edu.cmu.hcii.whyline.trace;

import edu.cmu.hcii.whyline.bytecode.MethodInfo;
import edu.cmu.hcii.whyline.bytecode.QualifiedClassName;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class CallStackEntry {

	private final Trace trace;
	private final MethodInfo method;
	private final int callerID;
	private final int startEventID;

	public CallStackEntry(Trace trace, MethodInfo method, int invocationID, int startID) {
		
		this.trace = trace;
		this.method = method;
		this.callerID = invocationID;
		this.startEventID = startID;
		
	}

	public int getInvocationID() { return callerID; }

	public int getStartEventID() { return startEventID; }
	
	public MethodInfo getMethod() { return method; }
	
	public QualifiedClassName getClassnameInvokedOn() {

		QualifiedClassName type = method.getClassfile().getInternalName();
		// If we can be more specific about the type that this was invoked on, let's do it.
		if(callerID >= 0) 
			type = trace.getInvocationClassInvokedOn(callerID);

		return type;
		
	}
	
	public String toString() {
		
		QualifiedClassName type = getClassnameInvokedOn();
		return type + "." + method.getMethodNameAndDescriptor(); 
		
	}
	
}