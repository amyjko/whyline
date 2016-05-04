package edu.cmu.hcii.whyline.trace;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

import edu.cmu.hcii.whyline.bytecode.FieldrefContainer;
import edu.cmu.hcii.whyline.util.IntegerVector;
import edu.cmu.hcii.whyline.util.Saveable;
import gnu.trove.TIntLongHashMap;

/**
 * Represents all field assignment events.
 *  
 * @author Andrew J. Ko
 *
 */ 
public class FieldAssignmentHistory implements Saveable {

	private final Trace trace;

	private final HashMap<String,IntegerVector> fieldAssignmentsByName = new HashMap<String,IntegerVector>(100);
	private final TIntLongHashMap objectIDsByAssignmentID = new TIntLongHashMap(100);
	
	public FieldAssignmentHistory(Trace trace) {

		this.trace = trace;
	
	}

	public void addFieldAssignmentID(int eventID) {
		
		String name = ((FieldrefContainer)trace.getInstruction(eventID)).getFieldref().getName();
		IntegerVector vector = fieldAssignmentsByName.get(name);
		if(vector == null) {
			vector = new IntegerVector(10);
			fieldAssignmentsByName.put(name, vector);
		}
		vector.append(eventID);
		
		// Originally, I did this to increase performance, but to find the objectID we often have to look ahead.
		// This doesn't work if we're still loading.
//		getObjectIDAssigned(eventID);
		
	}
	
	private long getObjectIDAssigned(int assignmentID){

		if(objectIDsByAssignmentID.containsKey(assignmentID))
			return objectIDsByAssignmentID.get(assignmentID);
		
		try {
			long objectID = trace.getPutFieldObjectIDAssigned(assignmentID);
			objectIDsByAssignmentID.put(assignmentID, objectID);
			return objectID;
		} catch (NoValueException e) {
			return 0;
		}
		
	}
	
	public int getDefinitionOfFieldBefore(long objectID, String unqualifiedFieldName, int eventIDBefore) {

		IntegerVector objectFieldAssignments = fieldAssignmentsByName.get(unqualifiedFieldName);
		
		// No assignments to fields of this name...
		if(objectFieldAssignments == null) return -1;
		
		// Where do we start looking from?
		int index = objectFieldAssignments.getIndexOfLargestValueLessThanOrEqualTo(eventIDBefore);
		
		// Search backwards until we find the most recent assignment to the objectID of interest.
		while(index >= 0) {
			int eventID = objectFieldAssignments.get(index);
			if(getObjectIDAssigned(eventID) == objectID) return eventID;
			index--;
		}
		
		return -1;
		
	}
	
	public int getDefinitionOfFieldAfter(long objectID, String unqualifiedFieldName, int afterID) {

		IntegerVector assignments = fieldAssignmentsByName.get(unqualifiedFieldName);

		// No assignments to fields of this name...
		if(assignments == null) return -1;
		
		// Where do we start looking from?
		int index = assignments.getIndexOfLargestValueLessThanOrEqualTo(afterID) + 1;
		
		// Search backwards until we find the most recent assignment to the objectID of interest.
		while(index < assignments.size()) {
			int eventID = assignments.get(index);
			if(getObjectIDAssigned(eventID) == objectID) return eventID;
			index++;
		}
		
		return -1;
		
	}
	
	public IntegerVector getDefinitionsOfObjectFieldAfter(long objectID, String unqualifiedFieldName, int eventIDAfter) {

		IntegerVector definitions = new IntegerVector(2);
		
		IntegerVector definitionIDs = fieldAssignmentsByName.get(unqualifiedFieldName);

		if(definitionIDs != null) {
		
			int index = definitionIDs.getIndexOfLargestValueLessThanOrEqualTo(eventIDAfter);
			// If there wasn't one before, start at the beginning.
			if(index == -1) index = 0;
	
			for(int i = index; i < definitionIDs.size(); i++) {

				int defID = definitionIDs.get(i);
				if(defID >= eventIDAfter)
					if(getObjectIDAssigned(defID) == objectID)
						definitions.append(defID);
				
			}
			
		}
		
		return definitions;
		
	}
		
	public void trimToSize() {

		for(IntegerVector vec : fieldAssignmentsByName.values())
			vec.trimToSize();
		
	}

	public void write(DataOutputStream out) throws IOException {

		out.writeInt(fieldAssignmentsByName.size());
		for(String unqualifiedName : fieldAssignmentsByName.keySet()) {
			out.writeUTF(unqualifiedName);
			fieldAssignmentsByName.get(unqualifiedName).write(out);
		}		

	}

	public void read(DataInputStream in) throws IOException {

		int size = in.readInt();
		for(int i = 0; i < size; i++)
			fieldAssignmentsByName.put(in.readUTF(), new IntegerVector(in));		
		
	}
	
}