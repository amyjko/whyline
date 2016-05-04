package edu.cmu.hcii.whyline.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
    SourceFile_attribute {
    	u2 attribute_name_index;
    	u4 attribute_length;
    	u2 sourcefile_index;
    }
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class SourceFileAttribute extends Attribute {

	private UTF8Info attributeName;
	private UTF8Info sourceFileNameInfo;
	private String sourceFileName;
	private ConstantPool pool;
	
	public SourceFileAttribute(UTF8Info name, ConstantPool pool, DataInputStream data, int length) throws IOException {

		this.attributeName = name;
		this.pool = pool;
		sourceFileNameInfo = (UTF8Info)pool.get(data.readUnsignedShort()); 
		sourceFileName = sourceFileNameInfo.toString();
		
	}

	public void toBytes(DataOutputStream bytes) throws IOException {

		bytes.writeShort(attributeName.getIndexInConstantPool());
		bytes.writeInt(2);
		bytes.writeShort(sourceFileNameInfo.getIndexInConstantPool());
		
	}

	public String getSourceFileName() { return sourceFileName; }

	public int getTotalAttributeLength() { return 2 + 4 + 2; }

}