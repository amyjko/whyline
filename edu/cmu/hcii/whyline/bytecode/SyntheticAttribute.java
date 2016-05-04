package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class SyntheticAttribute extends Attribute {

	private UTF8Info attributeName;

	public SyntheticAttribute(UTF8Info attributeName, ConstantPool pool) {

		this.attributeName = attributeName;
	
	}

	public void toBytes(DataOutputStream bytes) throws IOException {

		bytes.writeShort(attributeName.getIndexInConstantPool());
		bytes.writeInt(0);
		
	}

	public int getTotalAttributeLength() { return 6; }

}
