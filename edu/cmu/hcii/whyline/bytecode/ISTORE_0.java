package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class ISTORE_0 extends SetLocal {

	public ISTORE_0(CodeAttribute method) {
		super(method);
	}

	public int getLocalID() { return 0; }

	public final int getOpcode() { return 59; }
	public int byteLength() { return 1; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "I"; }

}

