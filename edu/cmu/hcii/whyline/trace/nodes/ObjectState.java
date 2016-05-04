package edu.cmu.hcii.whyline.trace.nodes;

import edu.cmu.hcii.whyline.bytecode.Classfile;
import edu.cmu.hcii.whyline.bytecode.QualifiedClassName;
import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.util.Named;
import edu.cmu.hcii.whyline.util.Util;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class ObjectState extends ReferenceState implements Comparable<ObjectState>, Named {

	public ObjectState(Trace trace, long objectID) {

		super(trace, objectID);
		
	}

	public String getDisplayName(boolean html, int limit) {

		String entityDescription = trace.getDescriptionOfObjectID(objectID);
		QualifiedClassName classname = trace.getClassnameOfObjectID(objectID);
		Classfile classfile = trace.getClassfileByName(classname);
		String simpleName = classname == null ? "(unknown)" : classname.isAnonymous() ? classfile.getSuperclass().getInternalName().getSimpleName() : classname.getSimpleName();
		String associatedName = trace.getAssociatedNameOfObjectID(objectID);
	
		// Truncate names if necessary.
		if(limit > 0) {
			simpleName = Util.elide(simpleName, limit); 
			if(associatedName != null) associatedName = Util.elide(associatedName, limit);
		}
		
		if(html) return "<b>" + simpleName + "</b>" + (associatedName != null ? " <em>\"" + associatedName + "\"</em>" : "");
		else return simpleName + " " + (associatedName == null ? "" : associatedName);
		
	}
	
	public int compareTo(ObjectState object) { return (int) (getObjectID() - object.getObjectID()); }

	public boolean isLeaf() { return false; }

	public long getObjectIDForChildren() { return objectID; }

	protected boolean performUpdate() { return false; }

	public int getAssociatedEventID() { return trace.getInstantiationOf(getObjectID()); }

	public String getAssociatedEventIDDescription() { return "instantiation"; }

	public String toString() { 
	
		String name = trace.getDescriptionOfObjectID(getObjectID());
		return isInstantiated() ? name : "<html><strike>" + name + "</strike>";
		
	}

}