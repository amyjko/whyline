package edu.cmu.hcii.whyline.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/*

    CONSTANT_Long_info {
    	u1 tag;
    	u4 high_bytes;
    	u4 low_bytes;
    }

*/
/**
 * @author Andrew J. Ko
 *
 */ 
public final class LongInfo extends ConstantPoolInfo {

	public static final int tag = 5;

	private long value;

    public LongInfo(ConstantPool pool, DataInputStream in) throws IOException {
    	super(pool);
        value = in.readLong();
    }

    public LongInfo(ConstantPool pool, long value) {
    	super(pool);
        this.value = value;
    }

    public void resolveDependencies() {}

	public void toBytes(DataOutputStream bytes) throws IOException {

		bytes.writeByte(tag);
		bytes.writeLong(value);
		
	}
	
	public long getValue() { return value; }
    
    public String toString() { return "" + value; }
    
}
