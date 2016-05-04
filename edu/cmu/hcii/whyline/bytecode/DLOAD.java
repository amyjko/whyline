package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class DLOAD extends GetLocal {

	private int index;
	
	public DLOAD(CodeAttribute method, int index) {
		super(method);
		this.index = index;
	}

	public int getLocalID() { return index; }

	public final int getOpcode() { return 24; }
	public int byteLength() { return 2; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		code.writeByte(index);
		
	}

	public EventKind getTypeProduced() { return EventKind.DOUBLE_PRODUCED; }

}
