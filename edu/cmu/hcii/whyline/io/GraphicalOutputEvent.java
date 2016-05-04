package edu.cmu.hcii.whyline.io;

import java.awt.Graphics2D;
import java.util.Arrays;

import edu.cmu.hcii.whyline.bytecode.QualifiedClassName;
import edu.cmu.hcii.whyline.trace.*;
import edu.cmu.hcii.whyline.util.Named;
import edu.cmu.hcii.whyline.util.Util;

/**
 * @author Andrew J. Ko
 *
 */
public abstract class GraphicalOutputEvent extends OutputEvent implements Named {
	
	private long instanceResponsible = -1;

	private long graphicsIDCache = 0;
	private int[] intCache;
	
	public GraphicalOutputEvent(Trace trace, int eventID) {
	
		super(trace, eventID);
			
	}
	
	public abstract String getHumanReadableName();

	public String getDisplayName(boolean html, int lengthLimit) { return getHumanReadableName(); }

	public long getInstanceResponsible() { 
		
		if(instanceResponsible >= 0) return instanceResponsible;
		
		CallStack callStack = trace.getCallStack(eventID);
		instanceResponsible = 0;
		for(int i = callStack.getDepth() - 1; i >= 0; i--) {

			CallStackEntry entry = callStack.getEntryAt(i);
			int invocationID = entry.getInvocationID();
			if(invocationID >= 0) {
				
				QualifiedClassName classOfInstanceCalled = trace.getInvocationClassInvokedOn(invocationID);
				
				boolean classIsReferenced = trace.classIsReferencedInFamiliarSourceFile(classOfInstanceCalled);
				long id = trace.getInvocationInstanceID(invocationID);
				if(id > 0 && classIsReferenced) {
					instanceResponsible = id;
					break;
				}

			}

		}
		
		return instanceResponsible;
		
	}

	public boolean isIndirectlyRenderedBy(long entityID) {

		CallStack callStack = trace.getCallStack(eventID);
		for(int i = callStack.getDepth() - 1; i >= 0; i--) {
			CallStackEntry entry = callStack.getEntryAt(i);
			int invocationID = entry.getInvocationID();
			if(invocationID >= 0) {
				long id = trace.getInvocationInstanceID(invocationID);
				if(id == entityID) return true;
			}
		}
		return false;
	
	}

	protected int getInteger(int arg) { 

		if(intCache == null) {
			// This is five because most of the commonly used rendering methods have five arguments.
			intCache = new int[5];
			Arrays.fill(intCache, Integer.MAX_VALUE);
		}
		else if(arg < intCache.length && intCache[arg] != Integer.MAX_VALUE) return intCache[arg];

		try { 
			
			int value = trace.getOperandStackValue(eventID, arg).getInteger();
			if(arg < intCache.length) intCache[arg] = value;
			return value;
		}
		catch (NoValueException e) { return 0; } 
		
	}
	
	protected long getLong(int arg) { return trace.getOperandStackValue(eventID, arg).getLong(); }
	
	protected boolean getBoolean(int arg) { 
		
		try { return trace.getOperandStackValue(eventID, arg).getBoolean(); } 
		catch (NoValueException e) { return false; } 
		
	}

	protected float getFloat(int arg) { 
		
		try { return trace.getOperandStackValue(eventID, arg).getFloat(); } 
		catch (NoValueException e) { return 0.0f; } 
		
	}

	protected double getDouble(int arg) { 
		
		try { return trace.getOperandStackValue(eventID, arg).getDouble(); } 
		catch (NoValueException e) { return 0.0f; } 
		
	}

	public long getGraphicsID() { 
		
		if(graphicsIDCache == 0) graphicsIDCache = getLong(0);
		return graphicsIDCache;
		
	}
	
	public abstract void paint(Graphics2D g);	

	public String toString() { return Util.fillOrTruncateString(trace.getInstruction(eventID).getMethod().getQualifiedNameAndDescriptor(), 40) + " eventID=" + getEventID() + " "; }
	
}