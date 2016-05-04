package edu.cmu.hcii.whyline.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/*
 * 
    InnerClasses_attribute {
    	u2 attribute_name_index;
    	u4 attribute_length;
    	u2 number_of_classes;
    	{  u2 inner_class_info_index;	     
    	   u2 outer_class_info_index;	     
    	   u2 inner_name_index;	     
    	   u2 inner_class_access_flags;	     
    	} classes[number_of_classes];
    }
    
 */
/**
 * @author Andrew J. Ko
 *
 */ 
public final class InnerClassesAttribute extends Attribute {
	
	public class InnerClass {
		
		final ClassInfo definition, outerClass;
		final UTF8Info name;
		final int flags;
		
		public InnerClass(ClassInfo classInfo, ClassInfo outerClass, UTF8Info name, int flags) {

			definition = classInfo;
			this.outerClass = outerClass;
			this.name = name;
			this.flags = flags;
			
		}
		
	}
	
	private final ConstantPool pool;
	private final UTF8Info attributeName;
	private final List<InnerClass> classes = new ArrayList<InnerClass>();
	
	public InnerClassesAttribute(UTF8Info attributeName, ConstantPool pool, DataInputStream data, int length) throws IOException {
		
		this.pool = pool;
		this.attributeName = attributeName;

		int numberOfClasses = data.readUnsignedShort();
		
		for(int i = 0; i < numberOfClasses; i++) {
			
			int innerClassInfoIndex = data.readUnsignedShort();
			int outerClassInfoIndex = data.readUnsignedShort();
			int innerNameIndex = data.readUnsignedShort();
			
			int innerClassAccessFlags = data.readUnsignedShort();

			classes.add(new InnerClass(
				(ClassInfo)pool.get(innerClassInfoIndex), 
				(ClassInfo)(outerClassInfoIndex == 0 ? null : pool.get(outerClassInfoIndex)),
				(UTF8Info)(innerNameIndex == 0 ? null : pool.get(innerNameIndex)),
				innerClassAccessFlags));			
			
		}
	
	}

	public int getFlagsFor(QualifiedClassName name) {

		for(InnerClass c : classes) {

			if(c.definition != null && c.definition.getName() == name)
				return c.flags;
			
		}
		return -1;
		
	}
	
	public void toBytes(DataOutputStream stream) throws IOException {

		stream.writeShort(attributeName.getIndexInConstantPool());
		stream.writeInt(getAttributeLengthWithoutNameAndLength());
		stream.writeShort(classes.size());
		for(InnerClass c : classes) {

			stream.writeShort(c.definition.getIndexInConstantPool());
			stream.writeShort(c.outerClass == null ? 0 : c.outerClass.getIndexInConstantPool());
			stream.writeShort(c.name == null ? 0 : c.name.getIndexInConstantPool());
			stream.writeShort(c.flags);
			
		}
		
	}

	public int getTotalAttributeLength() { return 2 + 4 + 2 + classes.size() * 8; }

}
