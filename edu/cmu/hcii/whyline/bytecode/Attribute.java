package edu.cmu.hcii.whyline.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.analysis.AnalysisException;

/**
 * @author Andrew J. Ko
 *
 *	attribute_info {
 *		u2 attribute_name_index;
 *		u4 attribute_length;
 *		u1 info[attribute_length];
 *	}
 */
public abstract class Attribute {
	
	public static Attribute read(Object owner, ConstantPool pool, DataInputStream data) throws IOException, JavaSpecificationViolation, AnalysisException {

		int attributeNameIndex = data.readUnsignedShort();
		int length = data.readInt();

		ConstantPoolInfo info = pool.get(attributeNameIndex);
		UTF8Info attributeName = (UTF8Info)info;
		String name = attributeName.toString();
		
		///////////////////////////////////////////////////////////////////////////////
		// Predefined in the classfile specification and required for execution.
		///////////////////////////////////////////////////////////////////////////////
		if(name.equals("Code")) return new CodeAttribute(attributeName, (MethodInfo)owner, pool, data, length);
		//else if(name.equals("ConstantValue")) return new ConstantValueAttribute(attributeName, pool, data, length);
		else if(name.equals("Exceptions")) return new ExceptionsAttribute(attributeName, pool, data, length);

		///////////////////////////////////////////////////////////////////////////////
		// Predefined for the Java and Java 2 class libraries
		///////////////////////////////////////////////////////////////////////////////

		// Need this to detect if a class is declared static.
		else if(name.equals("InnerClasses")) return new InnerClassesAttribute(attributeName, pool, data, length);
//		else if(name.equals("Synthetic")) return new SyntheticAttribute(attributeName, pool);
		
		///////////////////////////////////////////////////////////////////////////////
		// Predefined and optional
		///////////////////////////////////////////////////////////////////////////////
		else if(name.equals("SourceFile")) return new SourceFileAttribute(attributeName, pool, data, length);
		else if(name.equals("LineNumberTable")) return new LineNumberTableAttribute(attributeName, (CodeAttribute)owner, pool, data, length);
		else if(name.equals("LocalVariableTable")) return new LocalVariableTableAttribute((CodeAttribute)owner, attributeName, length, pool, data);
//		else if(name.equals("Deprecated")) return new DeprecatedAttribute(pool, data, length);

//		else if(name.equals("Signature")) return new Signature(pool, data, length);
//		else if(name.equals("EnclosingMethod")) return new EnclosingMethod(pool, data, length);
//		else if(name.equals("RuntimeVisibleAnnotations")) return new Deprecated(pool, data, length);

		else if(name.equals(IOAttribute.NAME)) return new IOAttribute(attributeName, pool, data, length);
		
		return new AnonymousAttribute(attributeName, pool, data, length);
		
	}

	public abstract int getTotalAttributeLength();
	public int getAttributeLengthWithoutNameAndLength() { return getTotalAttributeLength() - 2 - 4; }
	
	public abstract void toBytes(DataOutputStream stream) throws IOException;
	
}