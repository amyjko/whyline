package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class ICONST_0 extends PushConstant<Integer> {

	public static final Integer zero = new Integer(0);
	
	public ICONST_0(CodeAttribute method) {
		super(method);
	}
	
	public Integer getConstant() { return zero; }
	
	public final int getOpcode() { return 3; }
	public final int byteLength() { return 1; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		
	}

	public EventKind getTypeProduced() { return EventKind.CONSTANT_INTEGER_PRODUCED; }

}
