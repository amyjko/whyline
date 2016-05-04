package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class CASTORE extends SetArrayValue {

	public CASTORE(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 85; }
	public int byteLength() { return 1; }

	public String getReadableDescription() { return "[...] ="; }

	public String getTypeDescriptorOfArray() { return "C"; }

}
