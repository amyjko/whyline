package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class MONITORENTER extends Instruction {

	public MONITORENTER(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 194; }
	public int byteLength() { return 1; }
	public int getNumberOfOperandsConsumed() { return 1; }
	public int getNumberOfOperandsProduced() { return 0; }
	public int getNumberOfOperandsPeekedAt() { return 0; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		
	}

	public String getTypeDescriptorOfArgument(int argIndex) { return "Ljava/lang/Object;"; }

	public EventKind getTypeProduced() { return null; }

	public String getReadableDescription() { return "synchronized"; }

	public String getAssociatedName() { return null; }

}
