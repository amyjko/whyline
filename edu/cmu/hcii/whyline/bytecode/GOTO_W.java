package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class GOTO_W extends UnconditionalBranch {
	
	public GOTO_W(CodeAttribute method, int offset) {
		super(method, offset);
	}

	public final int getOpcode() { return 200; }
	public int byteLength() { return 5; }
	public int getNumberOfOperandsConsumed() { return 0; }
	public int getNumberOfOperandsProduced() { return 0; }
	public int getNumberOfOperandsPeekedAt() { return 0; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		code.writeInt(getOffset());
		
	}

	public String getReadableDescription() { return "goto_w"; }

	public String getKeyword() { return "goto"; }

}
