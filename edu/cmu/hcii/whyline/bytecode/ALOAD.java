package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;


/**
 * @author Andrew J. Ko
 *
 */
import edu.cmu.hcii.whyline.trace.EventKind;

public final class ALOAD extends GetLocal {

	private int index;
	
	public ALOAD(CodeAttribute method, int index) {
		super(method);
		this.index = index;
	}

	public int getLocalID() { return index; }

	public final int getOpcode() { return 25; }
	public int byteLength() { return 2; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		code.writeByte(index);
		
	}

	public EventKind getTypeProduced() { return EventKind.OBJECT_PRODUCED; }

}
