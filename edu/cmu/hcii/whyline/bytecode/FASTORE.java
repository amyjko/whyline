package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class FASTORE extends SetArrayValue {

	public FASTORE(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 81; }
	public int byteLength() { return 1; }

	public String getTypeDescriptorOfArray() { return "F"; }

}
