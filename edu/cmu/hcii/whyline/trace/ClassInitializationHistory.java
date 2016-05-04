package edu.cmu.hcii.whyline.trace;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.bytecode.QualifiedClassName;
import edu.cmu.hcii.whyline.util.Saveable;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class ClassInitializationHistory implements Saveable {

	private final Trace trace;

	private final gnu.trove.TObjectIntHashMap<QualifiedClassName> initializationIDsByClassname = new gnu.trove.TObjectIntHashMap<QualifiedClassName>();
	
	public ClassInitializationHistory(Trace trace) {

		this.trace = trace;
	
	}
	
	public void addClassInitializationEvent(QualifiedClassName classname, int eventID) {

		initializationIDsByClassname.put(classname, eventID);
		
	}
	
	public int getClassInitializationEventFor(QualifiedClassName classname) {
	
		return initializationIDsByClassname.get(classname);
		
	}

	public void write(DataOutputStream out) throws IOException {

		out.writeInt(initializationIDsByClassname.size());
		for(Object name : initializationIDsByClassname.keys()) {
			out.writeUTF(((QualifiedClassName)name).getText());
			out.writeInt(initializationIDsByClassname.get((QualifiedClassName) name));
		}
		
	}

	public void read(DataInputStream in) throws IOException {

		int size = in.readInt();
		initializationIDsByClassname.ensureCapacity(size);
		for(int i = 0; i < size; i++)
			initializationIDsByClassname.put(QualifiedClassName.get(in.readUTF()), in.readInt());
		initializationIDsByClassname.trimToSize();

	}
	
}
