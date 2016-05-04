package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class FCONST_1 extends PushConstant<Float> {

	public static final Float one = new Float(1);

	public FCONST_1(CodeAttribute method) {
		super(method);
	}

	public Float getConstant() { return one; }

	public final int getOpcode() { return 12; }
	public int byteLength() { return 1; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		
	}

	public EventKind getTypeProduced() { return EventKind.CONSTANT_FLOAT_PRODUCED; }

}
