package edu.cmu.hcii.whyline.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/*

    Exceptions_attribute {
    	u2 attribute_name_index;
    	u4 attribute_length;
    	u2 number_of_exceptions;
    	u2 exception_index_table[number_of_exceptions];
    }

*/
/**
 * @author Andrew J. Ko
 *
 */ 
public final class ExceptionsAttribute extends Attribute {

	private UTF8Info attributeName;
	private ConstantPool pool;
	private ClassInfo[] exceptionsThrown;
	private int length;
	
	public ExceptionsAttribute(UTF8Info attributeName, ConstantPool pool, DataInputStream data, int length) throws IOException {

		this.attributeName = attributeName;
		this.pool = pool;
		this.length = length;
		
		exceptionsThrown = new ClassInfo[data.readUnsignedShort()];
		
		for(int i = 0; i < exceptionsThrown.length; i++) {
			exceptionsThrown[i] = (ClassInfo)pool.get(data.readUnsignedShort());
		}
	
	}

	public void toBytes(DataOutputStream stream) throws IOException {

		stream.writeShort(attributeName.getIndexInConstantPool());
		stream.writeInt(getAttributeLengthWithoutNameAndLength());
		stream.writeShort(exceptionsThrown.length);
		for(ClassInfo ex : exceptionsThrown) stream.writeShort(ex.getIndexInConstantPool());		
		
	}

	public int getTotalAttributeLength() { return 2 + 4 + 2 + exceptionsThrown.length * 2; }

}
