package edu.cmu.hcii.whyline.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.cmu.hcii.whyline.analysis.AnalysisException;
import edu.cmu.hcii.whyline.util.Named;
import edu.cmu.hcii.whyline.util.Util;

/*
 * 
 *     field_info {
    	u2 access_flags;
    	u2 name_index;
    	u2 descriptor_index;
    	u2 attributes_count;
    	attribute_info attributes[attributes_count];
    }
 */
/**
 * @author Andrew J. Ko
 *
 */ 
public final class FieldInfo implements Comparable<FieldInfo>, Named {

	private final Classfile classfile;
    private final int access;
    private final UTF8Info nameInfo, descriptorInfo;
    private final String qualifiedName;
    private Attribute[] attributes;
    private ConstantPool pool;
    
    private final int declarationIndex;

    private ArrayList<Definition> definitions = new ArrayList<Definition>(2);
    private ArrayList<Use> uses = new ArrayList<Use>(2);
    private ArrayList<MethodInfo> setters;
    
	public FieldInfo(Classfile classfile, DataInputStream data, ConstantPool pool, int declarationIndex) throws IOException, JavaSpecificationViolation, AnalysisException {

		this.classfile = classfile;
		this.pool = pool;
		this.declarationIndex = declarationIndex;
		
		access = data.readUnsignedShort();
		nameInfo = (UTF8Info)pool.get(data.readUnsignedShort());
		descriptorInfo = (UTF8Info)pool.get(data.readUnsignedShort());
		StringBuilder builder = new StringBuilder(classfile.getInternalName().getText());
		builder.append('.');
		builder.append(nameInfo.toString());
		qualifiedName = builder.toString();
        int attributeCount = data.readUnsignedShort();
        attributes = new Attribute[attributeCount];
        for (int i = 0; i < attributeCount; ++i)
            attributes[i] = Attribute.read(this, pool, data);

	}

	public void toBytes(DataOutputStream stream) throws IOException {

		stream.writeShort(access);
		stream.writeShort(nameInfo.getIndexInConstantPool());
		stream.writeShort(descriptorInfo.getIndexInConstantPool());
		stream.writeShort(attributes.length);
		for(Attribute attr : attributes) attr.toBytes(stream);
		
	}
	
	public int getDeclarationIndex() { return declarationIndex; }

	/**
	 * Returns the set of public methods contain a PUTFIELD that defines this field.
	 * 
	 * @return The collection of methods that are known to set this field.
	 */
	public List<MethodInfo> getSetters() {

		if(setters == null) {

			setters = new ArrayList<MethodInfo>(1);
			for(Definition put : definitions) {
	
				MethodInfo method = put.getMethod();
				if(method.isPublic()) setters.add(method);
							
			}
			setters.trimToSize();

		}

		return Collections.<MethodInfo>unmodifiableList(setters);
		
	}

	public void addDefinition(Definition putfield) { definitions.add(putfield); } 
	public List<Definition> getDefinitions() { return Collections.<Definition>unmodifiableList(definitions); }

	public void addUse(Use inst) { uses.add(inst); }
	public List<Use> getUses() { return Collections.<Use>unmodifiableList(uses); }

	public Classfile getClassfile() { return classfile; }
	
	public int getAccessFlags() { return access; }
	public String getTypeDescriptor() { return descriptorInfo.toString(); }
	public QualifiedClassName getTypeName() { return QualifiedClassName.getFromTypeDescriptor(getTypeDescriptor()); }
	public String getQualifiedName() { return qualifiedName; }
	public String getName() { return nameInfo.toString(); }

	public String getDisplayName(boolean html, int limit) { return Util.elide(getName(), limit); }
	
	public boolean isPublic() { return java.lang.reflect.Modifier.isPublic(access); }
	public boolean isPrivate() { return java.lang.reflect.Modifier.isPrivate(access); }
	public boolean isProtected() { return java.lang.reflect.Modifier.isProtected(access); }
	public boolean isFinal() { return java.lang.reflect.Modifier.isFinal(access); }
	public boolean isStatic() { return java.lang.reflect.Modifier.isStatic(access); }
	public boolean isVolatile() { return java.lang.reflect.Modifier.isVolatile(access); }
	public boolean isTransient() { return java.lang.reflect.Modifier.isTransient(access); }

	public int compareTo(FieldInfo o) { return getQualifiedName().compareTo(o.getQualifiedName()); }
	
	public char getTypeDescriptorCharacter() { return getTypeDescriptor().charAt(0); } 
	
	public Object getDefaultValue() { return getDefaultValueForDescriptor(getTypeDescriptorCharacter()); }
	
	public String toString() { return getClassfile().getInternalName() + " " + java.lang.reflect.Modifier.toString(access) + " " + getTypeDescriptor() + " " + getName(); }

	public static Object getDefaultValueForDescriptor(char descriptor) {
		
		switch(descriptor) {
			case 'B' : return (byte)0;
			case 'C' : return '\u0000';
			case 'D' : return 0.0;
			case 'F' : return 0.0f;
			case 'I' : return 0;
			case 'J' : return 0L;
			case 'L' : return null;
			case 'S' : return (short)0;
			case 'Z' : return false;
			case '[' : return null;
			default : return "unknown type represented by " + descriptor;
		}
		
	}

	public void trimToSize() {
		
		definitions.trimToSize();

	}

	public boolean isReference() { 
		
		char type = getTypeDescriptorCharacter();
		return type == 'L' || type == '[';

	}

	public boolean isBoolean() { return getTypeDescriptorCharacter() == 'Z'; }

}
