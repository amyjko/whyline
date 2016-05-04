package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class SIPUSH extends PushConstant<Integer> {

	private int value;
	
	public SIPUSH(CodeAttribute method, short offset) {
		super(method);
		this.value = offset;
	}

	public Integer getConstant() { return value; }

	public final int getOpcode() { return 17; }
	public int byteLength() { return 3; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		code.writeShort(value);
		
	}

	public EventKind getTypeProduced() { return EventKind.CONSTANT_SHORT_PRODUCED; }

}
