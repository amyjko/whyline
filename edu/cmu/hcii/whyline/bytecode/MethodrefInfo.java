package edu.cmu.hcii.whyline.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.bytecode.QualifiedClassName;

/**

    CONSTANT_Methodref_info {
    	u1 tag;
    	u2 class_index;
    	u2 name_and_type_index;
    }
 * 
 * @author Andrew J. Ko
 *
 */ 
public class MethodrefInfo extends ConstantPoolInfo {

	public static final int tag = 10;

    private int classInfoIndex;
    private int nameAndTypeIndex;
    protected ClassInfo classInfo;
    protected NameAndTypeInfo nameAndTypeInfo;
    private MethodDescriptor descriptor;
    private boolean isStatic = false;
    
    // Cached, so we don't have to look them up
    private String fullName;
    private String methodName;

    public MethodrefInfo(ConstantPool pool, DataInputStream in) throws IOException {

    	super(pool);
        classInfoIndex = in.readUnsignedShort();
        nameAndTypeIndex = in.readUnsignedShort();
    
    }

	public MethodrefInfo(ConstantPool pool, ClassInfo classInfo, NameAndTypeInfo nameAndType) {
	
		super(pool);

		this.classInfo = classInfo;
		this.nameAndTypeInfo = nameAndType;
	
	}
	
	public void setStatic() { isStatic = true; }
	public boolean isStatic() { return isStatic; }
	
	public void resolveDependencies() {

		classInfo = ((ClassInfo)pool.get(classInfoIndex));
		nameAndTypeInfo = ((NameAndTypeInfo)pool.get(nameAndTypeIndex));		
		
	}
	
	public void toBytes(DataOutputStream bytes) throws IOException {

		bytes.writeByte(tag);
		bytes.writeShort(classInfo.getIndexInConstantPool());
		bytes.writeShort(nameAndTypeInfo.getIndexInConstantPool());
		
	}

    public QualifiedClassName getClassName() { return classInfo.getName(); }
    public String getMethodName() { 
    	
    	if(methodName == null) methodName = nameAndTypeInfo.getName(); 
    	return methodName;
    	
    }
       
    public boolean returnsVoid() { 
    	
    	String descriptor = nameAndTypeInfo.getTypeDescriptor();
    	return descriptor.charAt(descriptor.length() - 1) == 'V';
    	
    }
    
    public String getMethodDescriptor() { return nameAndTypeInfo.getTypeDescriptor(); }
    public String getMethodNameAndDescriptor() { return nameAndTypeInfo.toString(); }
	public String getQualfiedNameAndDescriptor() { 
		
		if(fullName == null) {
			StringBuilder builder = new StringBuilder(getClassName().getText());
			builder.append('.');
			builder.append(getMethodName());
			builder.append(getMethodDescriptor());
			fullName = builder.toString().intern();
		}
		return fullName; 
		
	}

	public boolean callsInstanceInitializer() { return getMethodName().equals("<init>"); }
	
	public String getShortQualifiedNameAndDescriptor() {
		
		return getClassName().getSimpleName() + "." + getMethodName() + getMethodDescriptor();		
		
	}
	
	public boolean matchesNameAndDescriptor(String name, String descriptor) { 
		
		return nameAndTypeInfo.getName().equals(name) && nameAndTypeInfo.getTypeDescriptor().equals(descriptor); 
		
	}

	public boolean matchesClassAndName(QualifiedClassName classname, String methodname) {
		
		return classname.equals(classInfo.getName()) && methodname.equals(nameAndTypeInfo.getName());
		
	}

	public boolean matchesClassNameAndDescriptor(QualifiedClassName classname, String name, String descriptor) { 
		
		return getClassName().equals(classname) && nameAndTypeInfo.getName().equals(name) && nameAndTypeInfo.getTypeDescriptor().equals(descriptor); 
		
	}

	public int getNumberOfParameters() {
		
		return getParsedDescriptor().getNumberOfParameters();
		
	}

	public QualifiedClassName getReturnType() { 
		
		return getParsedDescriptor().getReturnType(); 
		
	}
	
	public MethodDescriptor getParsedDescriptor() {

		if(descriptor == null) descriptor = MethodDescriptor.get(isStatic, getMethodDescriptor());
		return descriptor;
		
	}

	public boolean explicitlyReferences(MethodInfo method) {
		
		return 
			getMethodName().equals(method.getInternalName()) &&
			getClassName().equals(method.getClassfile().getInternalName()) &&
			getMethodDescriptor().equals(method.getDescriptor());
		
	}
		
    public String toString() { return getClassName() + "." + getMethodName() + getMethodDescriptor(); }
    
}
