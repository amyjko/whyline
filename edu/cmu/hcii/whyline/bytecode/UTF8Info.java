package edu.cmu.hcii.whyline.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class UTF8Info extends ConstantPoolInfo {

	public static final int tag = 1;

    private final String string;

    public UTF8Info(ConstantPool pool, DataInputStream in) throws IOException {
    	super(pool);
    	// These are highly duplicated.
        string = in.readUTF().intern();
    }
  
	public UTF8Info(ConstantPool pool, String string) {
		
		super(pool);
		this.string = string.intern();

	}

    public void resolveDependencies() {}

	public void toBytes(DataOutputStream bytes) throws IOException {

		bytes.writeByte(tag);
		bytes.writeUTF(string);
		
	}

    public String toString() { return string; }
    
	public boolean equals(Object o) {
		
		return (o instanceof UTF8Info) && ((UTF8Info)o).string.equals(string);
		
	}

}