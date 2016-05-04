package edu.cmu.hcii.whyline.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


/*

    CONSTANT_Fieldref_info {
    	u1 tag;
    	u2 class_index;
    	u2 name_and_type_index;
    }

*/
/**
 * @author Andrew J. Ko
 *
 */ 
public final class FieldrefInfo extends ConstantPoolInfo {

	public static final int tag = 9;

	private int classInfoIndex;
	private int nameAndTypeIndex;
	private ClassInfo classInfo;
	private NameAndTypeInfo nameAndTypeInfo;
	
    public FieldrefInfo(ConstantPool pool, DataInputStream in) throws IOException {

    	super(pool);
        classInfoIndex = in.readUnsignedShort();
        nameAndTypeIndex = in.readUnsignedShort();
    
    }

    public void resolveDependencies() {

		classInfo = ((ClassInfo)pool.get(classInfoIndex));
		nameAndTypeInfo = ((NameAndTypeInfo)pool.get(nameAndTypeIndex));		
	}

	public void toBytes(DataOutputStream bytes) throws IOException {

		bytes.writeByte(tag);
		bytes.writeShort(classInfo.getIndexInConstantPool());
		bytes.writeShort(nameAndTypeInfo.getIndexInConstantPool());
		
	}

	public String getQualifiedName() { return getClassname() + "." + getName(); }
    public QualifiedClassName getClassname() { return classInfo.getName(); }
    public String getName() { return nameAndTypeInfo.getName(); }
    public String getTypeDescriptor() { return nameAndTypeInfo.getTypeDescriptor(); }
    
    public boolean matches(FieldInfo field) {
    	
    	return field.getQualifiedName().equals(getQualifiedName());
    	
    }
    
    public boolean matchesClassNameAndDescriptor(QualifiedClassName classname, String name, String descriptor) {
    	
    	return getClassname().equals(classname) && getName().equals(name) && getTypeDescriptor().equals(descriptor);
    	
    }

    public String toString() { return getClassname() + "." + getName(); }

    public boolean equals(Object o) {
    	
    	return (o instanceof FieldrefInfo) &&
    		((FieldrefInfo)o).classInfo.equals(classInfo) &&
    		((FieldrefInfo)o).nameAndTypeInfo.equals(nameAndTypeInfo);
    	
    }
    
}
