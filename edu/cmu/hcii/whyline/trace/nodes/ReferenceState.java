package edu.cmu.hcii.whyline.trace.nodes;

import edu.cmu.hcii.whyline.bytecode.Classfile;
import edu.cmu.hcii.whyline.bytecode.FieldInfo;
import edu.cmu.hcii.whyline.bytecode.QualifiedClassName;
import edu.cmu.hcii.whyline.trace.Trace;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public abstract class ReferenceState extends DynamicNode<ReferenceState> {

	protected final Trace trace;
	protected long objectID;
	protected int currentEventID;
	private int initID;

	public ReferenceState(Trace trace, long objectID) {
		
		this.trace = trace;
		this.objectID = objectID;
		
	}

	public int getCurrentEventID() { return currentEventID; }

	public Trace getTrace() { return trace; }
	public long getObjectID() { return objectID; }

	public boolean isUpdated() { return isUpdated; }
		
	public void propagateCurrentEventID(int eventID) {
		
		currentEventID = eventID;
		isUpdated = false;

		initID = trace.getInitializationOfObjectID(objectID);

		if(hasDeterminedChildren())
			for(ReferenceState node : getChildren())
				node.propagateCurrentEventID(currentEventID);
		
	}
	
	public abstract long getObjectIDForChildren();

	public boolean isInstantiated() { return initID < currentEventID; }
	
	protected void determineChildren() {
		
		long objectIDForChildren = getObjectIDForChildren();
		
		if(objectIDForChildren <= 0) return;

		QualifiedClassName name = trace.getClassnameOfObjectID(objectIDForChildren);

		if(name.isArray()) {
			
			int length = trace.getArrayLength(objectIDForChildren);
			
			if(length < 0) {
				addChild(new Message(trace, "didn't record array length"));
			}
			else {
				int range = 25;
				for(int i = 0; i < length; i+= range)
					addChild(new ArrayElementSeriesState(trace, name, objectIDForChildren, i, Math.min(length - 1, i + range - 1)));
			}
			
		}
		else {
		
			// Does this reference state point to an object?
			if(objectIDForChildren > 0) {
				Classfile classfile = trace.getClassfileOfObjectID(objectIDForChildren);
				if(classfile != null) {
					for(FieldInfo field : classfile.getAllFields()) {
						if(!field.isStatic()) {
							FieldState node = trace.getFieldNode(objectIDForChildren, field);
							addChild(node);
						}
					}
				}
			}
			
		}

	}

	private static class Message extends ReferenceState implements Comparable<ReferenceState> {
		private final String message;
		public Message(Trace trace, String message) { super(trace, -1); this.message = message; }
		public long getObjectIDForChildren() { return -1; }
		public int getAssociatedEventID() { return -1; }
		public String getAssociatedEventIDDescription() { return ""; }
		public boolean isLeaf() { return true; }
		protected boolean performUpdate() { return false; }
		public String toString() { return "<html><i>" + message + "</i>"; }
		public int compareTo(ReferenceState o) { return 0; }
	}
	
}