package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;


import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */
public final class ARRAYLENGTH extends Computation {

	public ARRAYLENGTH(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 190; }
	public int byteLength() { return 1; }
	public int getNumberOfOperandsConsumed() { return 1; }
	public int getNumberOfOperandsProduced() { return 1; }
	public int getNumberOfOperandsPeekedAt() { return 0; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		
	}

	public EventKind getTypeProduced() { return EventKind.INTEGER_PRODUCED; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "Ljava/lang/Object;"; }

	public String getOperator() { return "length"; }

}
