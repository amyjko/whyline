package edu.cmu.hcii.whyline.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/*

    CONSTANT_Float_info {
    	u1 tag;
    	u4 bytes;
    }

*/
/**
 * @author Andrew J. Ko
 *
 */ 
public final class FloatInfo extends ConstantPoolInfo {

	public static final int tag = 4;

	private float value;

    public FloatInfo(ConstantPool pool, DataInputStream in) throws IOException {

    	super(pool);
        value = in.readFloat();
    
    }

    public void resolveDependencies() {}

	public void toBytes(DataOutputStream bytes) throws IOException {

		bytes.writeByte(tag);
		bytes.writeFloat(value);
		
	}

	public float getValue() { return value; }
	
    public String toString() { return "" + value; }

}
