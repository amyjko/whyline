package edu.cmu.hcii.whyline.trace;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.util.IntegerVector;
import edu.cmu.hcii.whyline.util.Saveable;

/**
 * Represents all invocations in a Whyline recording.
 * 
 * @author Andrew J. Ko
 *
 */ 
public class InstantiationHistory implements Saveable {

	private final Trace trace;

	private final IntegerVector instantiationIDs;
	private final gnu.trove.TLongIntHashMap instantiationsByObjectID = new gnu.trove.TLongIntHashMap();

	public InstantiationHistory(Trace trace, long numberOfObjectsCreated) {

		this.trace = trace;
		
		instantiationIDs = new IntegerVector((int)(numberOfObjectsCreated * .80));
	
	}

	public void addArrayInstantiationID(int eventID, long arrayID) {
		
		instantiationsByObjectID.put(arrayID, eventID);
		
	}
	
	public void addObjectInstantiationID(int eventID, long objectID) {

		instantiationIDs.append(eventID);
		instantiationsByObjectID.put(objectID, eventID);
		
	}
	
	public IntegerVector getInstantiationsOf(Classfile classfile) {

		IntegerVector instantiations = new IntegerVector(10);
		
		for(int i = 0; i < instantiationIDs.size(); i++) {

			int eventID = instantiationIDs.get(i);
			Instruction allocation = trace.getInstruction(eventID);
			if(allocation instanceof NEW) {

				QualifiedClassName classname = ((NEW)allocation).getClassInstantiated().getName();
				Classfile c = trace.getClassfileByName(classname);
				
				if(c != null && (c == classfile || c.isSubclassOf(classfile.getInternalName())))
					instantiations.append(eventID);
				
			}
			
		}

		return instantiations;
	
	}
	
	public int getInstantiationIDOf(long objectID) {
		
		int eventID = instantiationsByObjectID.get(objectID);
		return eventID == 0 ? -1 : eventID;
		
	}

	public void trimToSize() {

		instantiationsByObjectID.trimToSize();
		instantiationIDs.trimToSize();
		
	}

	public void write(DataOutputStream out) throws IOException {

		instantiationIDs.write(out);
		out.writeInt(instantiationsByObjectID.size());
		for(long objectID : instantiationsByObjectID.keys()) {
			out.writeLong(objectID);
			out.writeInt(instantiationsByObjectID.get(objectID));
		}
		
	}

	public void read(DataInputStream in) throws IOException {

		instantiationIDs.read(in);
		int size = in.readInt();
		for(int i = 0; i < size; i++)
			instantiationsByObjectID.put(in.readLong(), in.readInt());
		instantiationsByObjectID.trimToSize();
		
	}
	
}