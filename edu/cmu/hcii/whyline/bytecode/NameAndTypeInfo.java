package edu.cmu.hcii.whyline.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
    CONSTANT_NameAndType_info {
    	u1 tag;
    	u2 name_index;
    	u2 descriptor_index;
    }
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class NameAndTypeInfo extends ConstantPoolInfo {

	public static final int tag = 12;

	private int indexOfName;
	private int indexOfTypeDescriptor;
	private UTF8Info name;
	private UTF8Info descriptor;

	private String toStringCache;
	
    public NameAndTypeInfo(ConstantPool pool, DataInputStream in) throws IOException {

    	super(pool);
        indexOfName = in.readUnsignedShort();
        indexOfTypeDescriptor = in.readUnsignedShort();
    
    }

	public NameAndTypeInfo(ConstantPool pool, UTF8Info methodName, UTF8Info signature) {
		
		super(pool);
		
		this.name = methodName;
		this.descriptor = signature;
		
	}

    public void resolveDependencies() {
    	
    	name = ((UTF8Info)pool.get(indexOfName));
    	descriptor = ((UTF8Info)pool.get(indexOfTypeDescriptor));
    	
    }

	public void toBytes(DataOutputStream bytes) throws IOException {

		bytes.writeByte(tag);
		bytes.writeShort(name.getIndexInConstantPool());
		bytes.writeShort(descriptor.getIndexInConstantPool());
		
	}
        
    public String getName() { return name.toString(); }
    
    public String getTypeDescriptor() { return descriptor.toString(); }
    
    public static EventKind typeCharacterToClass(char c) {
    	
		switch(c) {
			case 'B' : return EventKind.BYTE_PRODUCED;
			case 'C' : return EventKind.CHARACTER_PRODUCED;
			case 'D' : return EventKind.DOUBLE_PRODUCED;
			case 'F' : return EventKind.FLOAT_PRODUCED;
			case 'I' : return EventKind.INTEGER_PRODUCED;
			case 'J' : return EventKind.LONG_PRODUCED;
			case 'S' : return EventKind.SHORT_PRODUCED;
			case 'Z' : return EventKind.BOOLEAN_PRODUCED;
			case 'L' : return EventKind.OBJECT_PRODUCED;
			case '[' : return EventKind.OBJECT_PRODUCED;
			default: return null;
		}
    
    }

    public static String sourceTypeToDescriptorType(String type) {
    	
		StringBuilder builder = new StringBuilder();
		int lastBracketIndex = type.lastIndexOf('[');
		if(lastBracketIndex  >= 0) {
			builder.append(type.substring(0, lastBracketIndex + 1));
			type = type.substring(lastBracketIndex + 1);
		}
    	    	
    	if(type.equals("boolean")) builder.append("Z");
    	else if(type.equals("char")) builder.append("C");
    	else if(type.equals("double")) builder.append("D");
    	else if(type.equals("float")) builder.append("F");
    	else if(type.equals("int")) builder.append("I");
    	else if(type.equals("long")) builder.append("J");
    	else if(type.equals("short")) builder.append("S");
    	else if(type.equals("byte")) builder.append("B");
    	else {
    		builder.append("L");
    		builder.append(type);
    		builder.append(";");
		}
    	
    	return builder.toString();    		
    
    }

    public static String getJavafiedTypeDescriptor(String type) {
    	
    	if(type.startsWith("L")) return getJavafiedObjectTypeDescriptor(type);
    	else if(type.startsWith("[")) {
    		
    		int lastBracket = type.lastIndexOf('[');
    		String brackets = type.substring(0, lastBracket + 1);
    		String objectType = getJavafiedTypeDescriptor(type.substring(lastBracket + 1));
    		for(int i = 0; i < brackets.length(); i++)
    			objectType = objectType + "[]";
    		return objectType;
    	
    	}
    	else return getJavafiedPrimitiveTypeDescriptor(type.charAt(0));
    	
    }

    public static String getJavafiedObjectTypeDescriptor(String objectType) {
    	
    	// Erase the L and ; and replace / with .
    	assert objectType.startsWith("L") && objectType.endsWith(";") : "" + objectType + " isn't an object type, it doesn't start with L and end with ;";
    	return objectType.substring(1, objectType.length() - 1).replace('/', '.');
    	
    }
    
    public static String getJavafiedPrimitiveTypeDescriptor(char c) {
    	
		switch(c) {
			case 'B' : return "byte";
			case 'C' : return "char";
			case 'D' : return "double";
			case 'F' : return "float";
			case 'I' : return "int";
			case 'J' : return "long";
			case 'S' : return "short";
			case 'Z' : return "boolean";
			case 'V' : return "void";
			default: return null;
		}
    	
    }
    
	public boolean equals(Object o) {
		
		return (o instanceof NameAndTypeInfo) &&
			((NameAndTypeInfo)o).name.equals(name) &&
			((NameAndTypeInfo)o).descriptor.equals(descriptor);
		
	}
	
    public String toString() { 
    	
    	if(toStringCache == null) {
    		StringBuilder builder = new StringBuilder(getName());
    		builder.append(getTypeDescriptor());
    		toStringCache = builder.toString(); 
    	}
    	return toStringCache;
    	
    }

}