package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class ILOAD extends GetLocal {

	private int index;
	
	public ILOAD(CodeAttribute method, int offset) {
		super(method);
		this.index = offset;
	}

	public int getLocalID() { return index; }

	public final int getOpcode() { return 21; }
	public int byteLength() { return 2; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		code.writeByte(index);
		
	}

	// It's not actually a constant, but it's easy for us to find the assignment to the local. We're actually using this type for the side effect of not recording its value.
	public EventKind getTypeProduced() { return EventKind.CONSTANT_INTEGER_PRODUCED; }

}
