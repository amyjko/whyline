package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;


import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class DNEG extends UnaryComputation {

	public DNEG(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 119; }
	public int byteLength() { return 1; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		
	}

	public String getPastTenseVerb() { return "negated"; }

	public EventKind getTypeProduced() { return EventKind.DOUBLE_PRODUCED; }

	public String getOperator() { return "-"; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "D"; }

}
