package edu.cmu.hcii.whyline.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/*

    ConstantValue_attribute {
    	u2 attribute_name_index;
    	u4 attribute_length;
    	u2 constantvalue_index;
    }

*/
/**
 * @author Andrew J. Ko
 *
 */ 
public final class ConstantValueAttribute extends Attribute {

	private UTF8Info attributeName;
	private ConstantPoolInfo constant;
	private ConstantPool pool;
	
	public ConstantValueAttribute(UTF8Info name, ConstantPool pool, DataInputStream data, int length) throws IOException {

		this.pool = pool;
		attributeName = name;
		constant = pool.get(data.readUnsignedShort());
		
	}

	public void toBytes(DataOutputStream stream) throws IOException {

		stream.writeShort(attributeName.getIndexInConstantPool());
		stream.writeInt(2);
		stream.writeShort(constant.getIndexInConstantPool());
		
	}

	public int getTotalAttributeLength() { return 2 + 4 + 2; }

}
