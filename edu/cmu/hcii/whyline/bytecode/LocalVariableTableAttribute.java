package edu.cmu.hcii.whyline.bytecode;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/*

    LocalVariableTable_attribute {
    	u2 attribute_name_index;
    	u4 attribute_length;
    	u2 local_variable_table_length;
    	{  u2 start_pc;
    	    u2 length;
    	    u2 name_index;
    	    u2 descriptor_index;
    	    u2 index;
    	} local_variable_table[local_variable_table_length];
    }	

*/
/**
 * @author Andrew J. Ko
 *
 */ 
public final class LocalVariableTableAttribute extends Attribute {

	private final UTF8Info attributeName;
	private final CodeAttribute code;
	private final ConstantPool pool;
	private int length;
	private LocalVariable[] table;
	private byte[] bytes;
	
	private class LocalVariable {
		
		public int startPC, length;
		public UTF8Info name;
		public UTF8Info descriptor;
		public int index;
		
		public LocalVariable(int start, int length, UTF8Info name, UTF8Info descriptor, int index) {
			
			this.startPC = start;
			this.length = length;
			this.name = name;
			this.descriptor = descriptor;
			this.index = index;
			
		}

		public boolean localIDIsDefinedAt(int localID, int byteIndex) {

			return localID == index && byteIndex >= startPC && byteIndex <= startPC + length;
			
		}
		
		public boolean nameIsDefinedAt(String name, int byteIndex) {
			
			return this.name.toString().equals(name) && byteIndex >= startPC && byteIndex <= startPC + length;
			
		}
		
		public String toString() { return "" + name + " of type " + descriptor + " is defined at [" + startPC + ", " + (startPC + length) + "] inclusive at index " + index; }
		
	}
	
	public LocalVariableTableAttribute(CodeAttribute code, UTF8Info attributeName, int length, ConstantPool pool, DataInputStream data) throws IOException {

		this.code = code;
		this.pool = pool;
		this.attributeName = attributeName;
		this.length = length;

		bytes = new byte[length];
		data.readFully(bytes);
	
	}

	private void parseTable() {
		
		if(table == null) {
			try {
				DataInputStream data = new DataInputStream(new ByteArrayInputStream(bytes));
				int tableLength = data.readUnsignedShort();
				table = new LocalVariable[tableLength];
				for(int i = 0; i < tableLength; i++) {
					table[i] = 
						new LocalVariable(
							data.readUnsignedShort(), 
							data.readUnsignedShort(), 
							(UTF8Info)pool.get(data.readUnsignedShort()), 
							(UTF8Info)pool.get(data.readUnsignedShort()), 
							data.readUnsignedShort());
				}
			} catch(IOException e) {
				e.printStackTrace();
			}
			bytes = null;
		}
		
	}
	
	public void toBytes(DataOutputStream stream) throws IOException {

		parseTable();
		stream.writeShort(attributeName.getIndexInConstantPool());
		stream.writeInt(getAttributeLengthWithoutNameAndLength());
		stream.writeShort(table.length);
		for(LocalVariable var : table) {

			stream.writeShort(var.startPC);
			stream.writeShort(var.length);
			stream.writeShort(var.name.getIndexInConstantPool());
			stream.writeShort(var.descriptor.getIndexInConstantPool());
			stream.writeShort(var.index);
			
		}
	
	}

	public int getTotalAttributeLength() { 
		
		parseTable();
		return 2 + 4 + 2 + table.length * 10; 
		
	}
	
	public boolean isLocalIDDefinedRelativeToInstruction(int localID, Instruction instruction) {

		return getNameOfLocalIDRelativeToInstruction(localID, instruction) != null;
		
	}
	
	public String getNameOfLocalIDRelativeToInstruction(int localID, Instruction instruction) { 
		
		parseTable();
		for(LocalVariable var : table)
			if(var.localIDIsDefinedAt(localID, instruction.getByteIndex()))
				return var.name.toString(); 
			
		return null;
		
	}
	
	public String getDescriptorOfLocalIDRelativeToInstruction(int localID, Instruction instruction) { 
		
		parseTable();
		for(LocalVariable var : table)
			if(var.localIDIsDefinedAt(localID, instruction.getByteIndex()))
				return var.descriptor.toString(); 
			
		return null;
		
	}	

	public int getLocalIDOfNameRelativeToInstruction(String name, Instruction instruction) {
		
		parseTable();
		for(LocalVariable var : table)
			if(var.nameIsDefinedAt(name, instruction.getByteIndex()))
				return var.index;
		
		return -1;
		
	}

	public int getNumberOfLocals() { 
		
		parseTable();
		return table.length; 
		
	}
	
	public String getDescriptorOf(int uniqueID) { 
		
		parseTable();
		return table[uniqueID].descriptor.toString(); 
		
	}

	public boolean localIDIsDefinedAt(int localID, Instruction inst) {
		
		parseTable();
		assert inst.getCode() == code : "But the method of the instruction, " + inst.getMethod() + ", isn't in the same method as this local table's, " + code.getMethod(); 
		
		int byteIndex = inst.getByteIndex();
		for(LocalVariable var : table)
			if(var.localIDIsDefinedAt(localID, byteIndex)) return true;
		
		return false;
		
	}
	
	public String toString() {
		
		parseTable();
		String result = "";
		for(int id = 0; id < table.length; id++) {
			
			result = result + table[id].toString() + "\n";
			
		}
		return result;
		
	}


	public Set<String> getLocalNames() {
		
		parseTable();

		Set<String> names = new HashSet<String>();
		for(LocalVariable var : table)
			names.add(var.name.toString());
		
		return names;
		
	}

	
}