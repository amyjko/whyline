package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Andrew J. Ko
 *
 */ 
public abstract class Conversion extends UnaryComputation {

	public Conversion(CodeAttribute method) {
		super(method);
	}

	public abstract int getOpcode();

	public String getPastTenseVerb() { return "converted"; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		
	}

}
