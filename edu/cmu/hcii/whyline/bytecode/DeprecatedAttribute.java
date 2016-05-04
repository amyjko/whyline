package edu.cmu.hcii.whyline.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class DeprecatedAttribute extends Attribute {

	public DeprecatedAttribute(ConstantPool pool, DataInputStream data) {

		throw new UnsupportedOperationException("Haven't implemented DeprecatedAttribute");
		
	}

	public void toBytes(DataOutputStream bytes) throws IOException {

		throw new UnsupportedOperationException("Haven't implemented DeprecatedAttribute");
		
	}

	public int getTotalAttributeLength() { return 0; }

}
