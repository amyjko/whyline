package edu.cmu.hcii.whyline.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
CONSTANT_String_info {
	u1 tag;
	u2 string_index;
}
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class StringInfo extends ConstantPoolInfo {

	public static final int tag = 8;

	private int stringIndex;
	private UTF8Info string;

    public StringInfo(ConstantPool pool, DataInputStream in) throws IOException {

    	super(pool);
        stringIndex = in.readUnsignedShort();
    
    }

    public StringInfo(ConstantPool pool, UTF8Info string) {

    	super(pool);
    	stringIndex = -1;
    	this.string = string;
    
    }

	public void toBytes(DataOutputStream bytes) throws IOException {

		bytes.writeByte(tag);
		bytes.writeShort(string.getIndexInConstantPool());
		
	}

    public void resolveDependencies() {
    	
    	if(stringIndex >= 0)
    		string = (UTF8Info)pool.get(stringIndex);
    	
    }

    public String getString() { return string.toString(); }

    public String toString() { return "\"" + getString() + "\""; }

}