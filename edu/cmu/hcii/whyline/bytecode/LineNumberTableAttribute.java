package edu.cmu.hcii.whyline.bytecode;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.SortedSet;

import edu.cmu.hcii.whyline.source.JavaSourceFile;
import edu.cmu.hcii.whyline.source.LineNumber;

/*

    LineNumberTable_attribute {
    	u2 attribute_name_index;
    	u4 attribute_length;
    	u2 line_number_table_length;
    	{  u2 start_pc;	     
    	   u2 line_number;	     
    	} line_number_table[line_number_table_length];
    }


*/
/**
 * @author Andrew J. Ko
 *
 *	One assumption we can make is that the byte indices increase in the table.
 *	The line numbers don't necessarily increase.
 */ 
public final class LineNumberTableAttribute extends Attribute {

	private final UTF8Info attributeName;
	private final int attributeLength;
	private final ConstantPool pool;
	private CodeAttribute code;
	private LineNumberTableEntry[] lines;
	private int lineNumberTableLength;
	private byte[] bytes;
		
	public LineNumberTableAttribute(UTF8Info attributeName, CodeAttribute owner, ConstantPool pool, DataInputStream data, int length) throws IOException {

		this.pool = pool;
		this.attributeName = attributeName;
		this.attributeLength = length;
		this.code = owner;
	
		// Save making the table for later.
		bytes = new byte[attributeLength];
		data.readFully(bytes);
		
	}

	private LineNumberTableEntry[] parseLines() {

		if(lines == null) {
			try {
				DataInputStream data = new DataInputStream(new ByteArrayInputStream(bytes));
				lineNumberTableLength = data.readUnsignedShort();
				lines = new LineNumberTableEntry[lineNumberTableLength];
				for(int i = 0; i < lineNumberTableLength; i++) {
					int startPC = data.readUnsignedShort();
					int line = data.readUnsignedShort();
					Instruction inst = code.getInstructionAtByteIndex(startPC);
					lines[i] = new LineNumberTableEntry(pool.getClassfile(), inst, line);			
				}
				bytes = null;
			} catch(IOException e) {
				e.printStackTrace();
			}

		}
		return lines;
		
	}
	
	public void toBytes(DataOutputStream stream) throws IOException {

		stream.writeShort(attributeName.getIndexInConstantPool());
		stream.writeInt(getAttributeLengthWithoutNameAndLength());
		
		if(lines == null)
			stream.write(bytes);
		else {
			stream.writeShort(lineNumberTableLength);
			for(LineNumberTableEntry pair : lines) {
				
				stream.writeShort(pair.startPC.getByteIndex());
				stream.writeShort(pair.getLineNumber().getNumber());
				
			}
		}
		
	}

	/**
	 * 
	 * 
	 * @param byteIndex
	 * @return -1 if there's no line number for this byte index.
	 */
	public LineNumber getLineNumberOf(Instruction inst) {

		LineNumberTableEntry[] entries = parseLines();
		
		if(entries.length == 1) return entries[0].getLineNumber();
		
		Instruction instructionOfPreviousRow = entries[0].startPC;
		for(int currentRow = 1; currentRow < entries.length; currentRow++) {
			
			if(instructionOfPreviousRow.getByteIndex() <= inst.getByteIndex() && inst.getByteIndex() < entries[currentRow].startPC.getByteIndex())
				return entries[currentRow - 1].getLineNumber();
			else instructionOfPreviousRow = entries[currentRow].startPC;
			
		}
		
		if(inst.getByteIndex() >= entries[entries.length - 1].startPC.getByteIndex())
			return entries[entries.length - 1].getLineNumber();
		
		return null;
		
	}

	public LineNumber getFirstLineNumber() {
		
		LineNumberTableEntry[] entries = parseLines();

		LineNumber smallest = null;
		for(int i = 0; i < entries.length; i++) {
			
			if(smallest == null || entries[i].getLineNumber().isBefore(smallest))
				smallest = entries[i].getLineNumber();
			
		}
			
		return smallest;
		
	}
	
	public LineNumber getLastLineNumber() {

		LineNumberTableEntry[] entries = parseLines();

		LineNumber largest = null;
		for(int i = 0; i < entries.length; i++) {
			
			if(largest == null || entries[i].getLineNumber().isAfter(largest))
				largest = entries[i].getLineNumber();
			
		}
			
		return largest;
	
	}

	public int getTotalAttributeLength() { 

//		return 2 + 4 + 2 + lines.length * 4; 
		return 2 + 4 + attributeLength;
		
	}

	public void getInstructionsOnLineNumber(SortedSet<Instruction> instructions, LineNumber lineNumber) {

		LineNumberTableEntry[] entries = parseLines();
		
		// Need to find all of the byte index ranges that correspond to this line number
		int tableRowThatMatchesLineNumber = 0;
		for(tableRowThatMatchesLineNumber = 0; tableRowThatMatchesLineNumber < entries.length; tableRowThatMatchesLineNumber++) {
		
			// Find all of the instructions that match this range of byte indices.
			if(entries[tableRowThatMatchesLineNumber].getLineNumber().is(lineNumber)) {

				Instruction currentInstruction = entries[tableRowThatMatchesLineNumber].startPC;
				while(currentInstruction != null && currentInstruction.getLineNumber().is(lineNumber)) {
					instructions.add(currentInstruction);
					currentInstruction = currentInstruction.getNext();
				}
				// If the instruction is null, it's either the end of the code, or the code was generated by the compiler, and has no line number.
				
			}

		}
	
	}

	private String getStringFormOfTable() {
		
		LineNumberTableEntry[] entries = parseLines();

		String string = "***\n";
		
		for(int i = 0; i < entries.length; i++) {
			string = string + entries[i].line + " starts at byte index " + entries[i].startPC + "\n";
		}

		return string + "***\n";
		
	}

	private static class LineNumberTableEntry {
		
		public final Instruction startPC;
		private final int line;
		private LineNumber lineNumber;
		private final Classfile classfile;
		
		public LineNumberTableEntry(Classfile classfile, Instruction startPC, int line) {

			this.classfile = classfile;
			this.startPC = startPC;
			this.line = line;
			
		}
		
		public LineNumber getLineNumber() {
			
			if(lineNumber == null) {
				JavaSourceFile source = classfile.getSourceFile();
				lineNumber = new LineNumber(source == null ? classfile : source, line);
			}
			return lineNumber;
			
		}
		
	}

}