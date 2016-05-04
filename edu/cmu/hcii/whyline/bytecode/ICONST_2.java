package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class ICONST_2 extends PushConstant<Integer> {

	public static final Integer two = new Integer(2);

	public ICONST_2(CodeAttribute method) {
		super(method);
	}

	public Integer getConstant() { return two; }

	public final int getOpcode() { return 5; }
	public int byteLength() { return 1; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		
	}

	public EventKind getTypeProduced() { return EventKind.CONSTANT_INTEGER_PRODUCED; }

}
