package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class IASTORE extends SetArrayValue {

	public IASTORE(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 79; }
	public int byteLength() { return 1; }

	public String getTypeDescriptorOfArray() { return "I"; }

}
