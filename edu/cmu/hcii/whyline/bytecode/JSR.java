package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class JSR extends UnconditionalBranch {

	public JSR(CodeAttribute method, int offset) {
		super(method, offset);
	}

	public final int getOpcode() { return 168; }
	public int byteLength() { return 3; }
	public int getNumberOfOperandsConsumed() { return 0; }
	public int getNumberOfOperandsProduced() { return 1; }
	public int getNumberOfOperandsPeekedAt() { return 0; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		code.writeShort(getOffset());
		
	}

	public EventKind getTypeProduced() { return EventKind.INTEGER_PRODUCED; }

	public String getReadableDescription() { return "jsr"; }

	public String getKeyword() { return "finally"; }

}
