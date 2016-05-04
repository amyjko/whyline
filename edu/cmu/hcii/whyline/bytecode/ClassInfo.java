package edu.cmu.hcii.whyline.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Andrew J. Ko
 *
 *  CONSTANT_Class_info {
 *     	u1 tag;
 *     	u2 name_index;
 *     }
 */ 
public final class ClassInfo extends ConstantPoolInfo {

	public static final int tag = 7;

	private int indexOfName;
	private UTF8Info name;
	private QualifiedClassName qualifiedName;

    public ClassInfo(ConstantPool pool, DataInputStream in) throws IOException {

    	super(pool);
        indexOfName = in.readUnsignedShort();
    
    }

	public ClassInfo(ConstantPool pool, UTF8Info name) {

		super(pool);
		
		this.name = name;
		
	}

	public void resolveDependencies() {

		name = ((UTF8Info)pool.get(indexOfName));

	}

	public void toBytes(DataOutputStream bytes) throws IOException {

		bytes.writeByte(tag);
		bytes.writeShort(name.getIndexInConstantPool());
		
	}
    
	public QualifiedClassName getName() { 
		
		if(qualifiedName == null) qualifiedName = QualifiedClassName.get(name.toString()); 
		return qualifiedName;
		
	}

	public String getSimpleName() { return getName().getSimpleName(); }
	
	public String toString() { return getName().toString() + ".class"; }

	public boolean equals(Object o) {
		
		return (o instanceof ClassInfo) && ((ClassInfo)o).name.equals(name);
		
	}
	
}