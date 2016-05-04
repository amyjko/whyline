package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class ICONST_M1 extends PushConstant<Integer> {

	public static final Integer minusOne = new Integer(-1);

	public ICONST_M1(CodeAttribute method) {
		super(method);
	}

	public Integer getConstant() { return minusOne; }

	public final int getOpcode() { return 2; }
	public int byteLength() { return 1; }
	
	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		
	}

	public EventKind getTypeProduced() { return EventKind.CONSTANT_INTEGER_PRODUCED; }

}
