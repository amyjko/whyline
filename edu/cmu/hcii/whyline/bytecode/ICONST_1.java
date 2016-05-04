package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class ICONST_1 extends PushConstant<Integer> {

	public static final Integer one = new Integer(1);

	public ICONST_1(CodeAttribute method) {
		super(method);
	}

	public Integer getConstant() { return one; }

	public final int getOpcode() { return 4; }
	public int byteLength() { return 1; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		
	}

	public EventKind getTypeProduced() { return EventKind.CONSTANT_INTEGER_PRODUCED; }

}
