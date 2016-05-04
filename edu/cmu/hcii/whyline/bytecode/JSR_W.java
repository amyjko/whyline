package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class JSR_W extends UnconditionalBranch {

	public JSR_W(CodeAttribute method, int offset) {
		super(method, offset);
	}

	public final int getOpcode() { return 201; }
	public int byteLength() { return 5; }
	public int getNumberOfOperandsConsumed() { return 0; }
	public int getNumberOfOperandsProduced() { return 1; }
	public int getNumberOfOperandsPeekedAt() { return 0; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		code.writeInt(getOffset());
		
	}

	public EventKind getTypeProduced() { return EventKind.INTEGER_PRODUCED; }

	public String getReadableDescription() { return "jsr_w"; }

	public String getKeyword() { return "finally"; }

}