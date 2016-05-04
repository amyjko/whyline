package edu.cmu.hcii.whyline.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/*

    CONSTANT_Integer_info {
    	u1 tag;
    	u4 bytes;
    }

*/
/**
 * @author Andrew J. Ko
 *
 */ 
public final class IntegerInfo extends ConstantPoolInfo {

	public static final int tag = 3;

	private int value;

    public IntegerInfo(ConstantPool pool, DataInputStream in) throws IOException {
    	super(pool);
        value = in.readInt();
    }

	public IntegerInfo(ConstantPool pool, int info) {
		super(pool);
		value = info;
	}

    public void resolveDependencies() {}

	public void toBytes(DataOutputStream bytes) throws IOException {

		bytes.writeByte(tag);
		bytes.writeInt(value);
		
	}    
    
	public int getValue() { return value; }
	
    public String toString() { return "" + value; }

    
}
