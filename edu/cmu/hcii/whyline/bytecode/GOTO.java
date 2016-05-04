package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class GOTO extends UnconditionalBranch {

	public GOTO(CodeAttribute method, short offset) {
		super(method, offset);
	}

	public final int getOpcode() { return 167; }
	public int byteLength() { return 3; }
	public int getNumberOfOperandsConsumed() { return 0; }
	public int getNumberOfOperandsProduced() { return 0; }
	public int getNumberOfOperandsPeekedAt() { return 0; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		code.writeShort(getOffset());
		
	}

	public String getReadableDescription() { return "goto"; }

	public String getKeyword() { return "goto"; }

}
