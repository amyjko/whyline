package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class ICONST_5 extends PushConstant<Integer> {

	public static final Integer five = new Integer(5);

	public ICONST_5(CodeAttribute method) {
		super(method);
	}

	public Integer getConstant() { return five; }

	public final int getOpcode() { return 8; }
	public int byteLength() { return 1; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		
	}

	public EventKind getTypeProduced() { return EventKind.CONSTANT_INTEGER_PRODUCED; }

}
