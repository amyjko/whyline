package edu.cmu.hcii.whyline.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/*

    CONSTANT_InterfaceMethodref_info {
    	u1 tag;
    	u2 class_index;
    	u2 name_and_type_index;
    }

*/
/**
 * @author Andrew J. Ko
 *
 */ 
public final class InterfaceMethodrefInfo extends MethodrefInfo {

	public static final int tag = 11;

    public InterfaceMethodrefInfo(ConstantPool pool, DataInputStream in) throws IOException {
    
    	super(pool, in);
    
    }

	public void toBytes(DataOutputStream bytes) throws IOException {
	
		bytes.writeByte(tag);
		bytes.writeShort(classInfo.getIndexInConstantPool());
		bytes.writeShort(nameAndTypeInfo.getIndexInConstantPool());
		
	}

}
