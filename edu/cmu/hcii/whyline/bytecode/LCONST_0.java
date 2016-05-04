package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class LCONST_0 extends PushConstant<Long> {

	public static final Long zero = new Long(0);

	public LCONST_0(CodeAttribute method) {
		super(method);
	}

	public Long getConstant() { return zero; }

	public final int getOpcode() { return 9; }
	public int byteLength() { return 1; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		
	}

	public EventKind getTypeProduced() { return EventKind.CONSTANT_LONG_PRODUCED; }

}
