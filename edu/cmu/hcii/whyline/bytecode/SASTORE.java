package edu.cmu.hcii.whyline.bytecode;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class SASTORE extends SetArrayValue {

	public SASTORE(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 86; }
	public int byteLength() { return 1; }

	public String getTypeDescriptorOfArray() { return "S"; }

}
