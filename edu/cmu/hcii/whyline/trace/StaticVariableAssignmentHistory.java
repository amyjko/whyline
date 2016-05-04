package edu.cmu.hcii.whyline.trace;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

import edu.cmu.hcii.whyline.bytecode.FieldrefContainer;
import edu.cmu.hcii.whyline.bytecode.FieldrefInfo;
import edu.cmu.hcii.whyline.util.IntegerVector;
import edu.cmu.hcii.whyline.util.Saveable;

/**
 * Represents all assignments to class variables.
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class StaticVariableAssignmentHistory implements Saveable {

	private final Trace trace;

	private final HashMap<String,IntegerVector> assignmentIDsByField = new HashMap<String,IntegerVector>(100);
	
	public StaticVariableAssignmentHistory(Trace trace) {

		this.trace = trace;
	
	}

	public void addStaticAssignmentID(int assignmentID) {
		
		FieldrefInfo ref = ((FieldrefContainer)trace.getInstruction(assignmentID)).getFieldref();
		IntegerVector vector = assignmentIDsByField.get(ref.getQualifiedName());
		if(vector == null) {
			vector = new IntegerVector(10);
			assignmentIDsByField.put(ref.getQualifiedName(), vector);
		}
		vector.append(assignmentID);
		
	}

	public int getLastDefinitionOfBefore(String qualifiedFieldName, int eventIDBefore) {
	
		IntegerVector assignmentIDs = assignmentIDsByField.get(qualifiedFieldName);

		// No assignments to fields of this name...
		if(assignmentIDs == null) return -1;
		
		// Where do we start looking from?
		int index = assignmentIDs.getIndexOfLargestValueLessThanOrEqualTo(eventIDBefore);

		if(index >= 0)
			return assignmentIDs.get(index);
		else
			return -1;
		
	}

	public void write(DataOutputStream out) throws IOException {

		out.writeInt(assignmentIDsByField.size());
		for(String field : assignmentIDsByField.keySet()) {
			out.writeUTF(field);
			assignmentIDsByField.get(field).write(out);
		}
		
	}

	public void read(DataInputStream in) throws IOException {

		int size = in.readInt();
		for(int i = 0; i < size; i++)
			assignmentIDsByField.put(in.readUTF(), new IntegerVector(in));
		
	}
		
}
