package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Andrew J. Ko
 *
 */
public abstract class BinaryComputation extends Computation {

	public BinaryComputation(CodeAttribute method) {
		super(method);
	}
	
	public int getNumberOfOperandsConsumed() { return 2; }
	public int getNumberOfOperandsProduced() { return 1; }
	public int getNumberOfOperandsPeekedAt() { return 0; }

	public abstract String getPastTenseVerb();
	
	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		
	}
	
}
