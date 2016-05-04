package edu.cmu.hcii.whyline.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/*

   CONSTANT_Double_info {
    	u1 tag;
    	u4 high_bytes;
    	u4 low_bytes;
    }

 */
/**
 * @author Andrew J. Ko
 *
 */ 
public final class DoubleInfo extends ConstantPoolInfo {

	public static final int tag = 6;

	public final double value;

    public DoubleInfo(ConstantPool pool, DataInputStream in) throws IOException {

    	super(pool);
    	value = in.readDouble();

    }

    public DoubleInfo(ConstantPool pool, double value) {

    	super(pool);
    	this.value = value;

    }

    public void resolveDependencies() {}

	public void toBytes(DataOutputStream bytes) throws IOException {

		bytes.writeByte(tag);
		bytes.writeDouble(value);
		
	}
	
	public double getValue() { return value; }

	public String toString() { return "" + value; }

}