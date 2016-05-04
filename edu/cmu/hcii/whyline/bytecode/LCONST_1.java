package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class LCONST_1 extends PushConstant<Long> {

	public static final Long one = new Long(1);
	
	public LCONST_1(CodeAttribute method) {
		super(method);
	}

	public Long getConstant() { return one; }

	public final int getOpcode() { return 10; }
	public int byteLength() { return 1; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		
	}

	public EventKind getTypeProduced() { return EventKind.CONSTANT_LONG_PRODUCED; }

}
