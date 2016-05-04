package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class ICONST_4 extends PushConstant<Integer> {

	public static final Integer four = new Integer(4);

	public ICONST_4(CodeAttribute method) {
		super(method);
	}

	public static final Integer one = new Integer(four);
	public Integer getConstant() { return four; }

	public final int getOpcode() { return 7; }
	public int byteLength() { return 1; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		
	}

	public EventKind getTypeProduced() { return EventKind.CONSTANT_INTEGER_PRODUCED; }

}
