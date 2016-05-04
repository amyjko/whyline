package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;


/**
 * @author Andrew J. Ko
 *
 */ 
import edu.cmu.hcii.whyline.trace.EventKind;

public final class FNEG extends UnaryComputation {

	public FNEG(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 118; }
	public int byteLength() { return 1; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		
	}

	public String getPastTenseVerb() { return "negated"; }

	public EventKind getTypeProduced() { return EventKind.FLOAT_PRODUCED; }

	public String getOperator() { return "-"; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "F"; }

}
