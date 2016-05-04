package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class DCONST_1 extends PushConstant<Double> {

	public static final Double one = new Double(1);

	public DCONST_1(CodeAttribute method) {
		super(method);
	}

	public Double getConstant() { return one; }

	public final int getOpcode() { return 15; }
	public int byteLength() { return 1; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		
	}

	public EventKind getTypeProduced() { return EventKind.CONSTANT_DOUBLE_PRODUCED; }

}
