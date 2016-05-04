package edu.cmu.hcii.whyline.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Andrew J. Ko
 *
 */
public final class AnonymousAttribute extends Attribute {

	private UTF8Info attributeName;
	private byte[] info;
	private ConstantPool pool;
	
	public AnonymousAttribute(UTF8Info attributeName, ConstantPool pool, DataInputStream data, int length) throws IOException {

		this.pool = pool;
		this.attributeName = attributeName;
		info = new byte[length];
		data.readFully(info);
		
	}

	public void toBytes(DataOutputStream stream) throws IOException {

		stream.writeShort(attributeName.getIndexInConstantPool());
		stream.writeInt(info.length);
		stream.write(info);
		
	}

	public int getTotalAttributeLength() { return 2 + 4 + info.length; }
	
}
