package edu.cmu.hcii.whyline.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

import edu.cmu.hcii.whyline.analysis.AnalysisException;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class ConstantPool {

	public static final int MAXIMUM_CONTANT_POOL_ENTRIES = 65535;

	private final Classfile classfile;
	
	// We ONLY ADD to this; that way, every item has a constant index and we don't have to search.
	private ConstantPoolInfo[] items;
	private int size;
	
	private Set<QualifiedClassName> classesReferenced;
	
	public ConstantPool(Classfile classfile, DataInputStream data) throws IOException, JavaSpecificationViolation {

		this.classfile = classfile;
		
		short constantPoolCount = data.readShort();
		
        items = new ConstantPoolInfo[constantPoolCount];

        addItem(null);          // Constant pool items are index from 1 to count - 1

        while (--constantPoolCount > 0) {       // index 0 is reserved by JVM

    		final ConstantPoolInfo info;
            int tag = data.readUnsignedByte();
            boolean addPadding = false;
            switch (tag) {
    	        case UTF8Info.tag :                     // 1
    	            info = new UTF8Info(this, data);
    	            break;
    	        case IntegerInfo.tag:                  // 3
    	            info = new IntegerInfo(this, data);
    	            break;
    	        case FloatInfo.tag:                    // 4
    	            info = new FloatInfo(this, data);
    	            break;
    	        case LongInfo.tag:                     // 5
    	            info = new LongInfo(this, data);
    	            addPadding = true;
    	            break;
    	        case DoubleInfo.tag :                   // 6
    	            info = new DoubleInfo(this, data);
    	            addPadding = true;
    	            break;
    	        case ClassInfo.tag:                    // 7
    	        	// Sends the current size as the index of the class info
    	            info = new ClassInfo(this, data);
    	            break;
    	        case StringInfo.tag:                   // 8
    	            info = new StringInfo(this, data);
    	            break;
    	        case FieldrefInfo.tag:                 // 9
    	            info = new FieldrefInfo(this, data);
    	            break;
    	        case MethodrefInfo.tag:                // 10
    	            info = new MethodrefInfo(this, data);
    	            break;
    	        case InterfaceMethodrefInfo.tag:       // 11
    	            info = new InterfaceMethodrefInfo(this, data);
    	            break;
    	        case NameAndTypeInfo.tag:              // 12
    	            info = new NameAndTypeInfo(this, data);
    	            break;
    	        default :
    	            throw new IOException("Invalid constant pool tag: " + tag);
            }

            items[size++] = info;

            // If the item was a long or a double info, then we add padding. The designers say,
            // "In retrospect, making 8-byte constants take two constant pool entries was a poor choice."
            if(addPadding) {
            	items[size++] = new ConstantPoolPadding(this);
                --constantPoolCount;
            }
            
        }

        // Now go through all of the constant pool entries and resolve the pointers.
        for(ConstantPoolInfo info : items) if(info != null) info.resolveDependencies();	        
		
	}
	
	public void toBytes(DataOutputStream bytes) throws IOException {
		
		bytes.writeShort(size);
		for(int i = 0; i < size; i++)
			if(items[i] != null)
				items[i].toBytes(bytes); 
		
	}
	
	private void addItem(ConstantPoolInfo info) throws JavaSpecificationViolation {

		if(size >= items.length) {
			ConstantPoolInfo[] newItems = new ConstantPoolInfo[size * 2];
			System.arraycopy(items, 0, newItems, 0, size);
			items = newItems;
		}
		items[size++] = info;
		
		if(size > MAXIMUM_CONTANT_POOL_ENTRIES) 
			throw new JavaSpecificationViolation("The constant pool of " + classfile + " exceeds " + MAXIMUM_CONTANT_POOL_ENTRIES + " entries, which is a violation of the liimtations of the bytecode spec.");
		
	}
	
	public ConstantPoolInfo get(int index) throws ArrayIndexOutOfBoundsException { return items[index]; }
	
	public ConstantPoolInfo[] getItems() {
		
		if(size < items.length) {
			ConstantPoolInfo[] newItems = new ConstantPoolInfo[size];
			System.arraycopy(items, 0, newItems, 0, size);
			items = newItems;
		}
		return items;
		
	}
	
	public Set<QualifiedClassName> getClassNamesReferenced() { 

		if(classesReferenced == null) {
			classesReferenced = new HashSet<QualifiedClassName>();
			for(ConstantPoolInfo info : items)
				if(info instanceof ClassInfo)
					classesReferenced.add(((ClassInfo)info).getName());
		}

		return classesReferenced; 
		
	}
	
	public Classfile getClassfile() { return classfile; }

	public int getSize() { return size; }
	
	// Returns the index of this specific entry (not one that's simply equivalent to it)
	public int indexOf(ConstantPoolInfo entry) throws AnalysisException { 

		// We DON'T use the indexOf() method, because we want strict object equality here
		for(int i = 0; i < size; i++)
			if(items[i] == entry) return i;
		throw new AnalysisException("Fatal error: couldn't find constant pool info " + entry);
		
	}
	
	
	public IntegerInfo addIntegerInfo(int info) throws JavaSpecificationViolation {

		IntegerInfo intinfo = new IntegerInfo(this, info);
		addItem(intinfo);
		return intinfo;
		
	}

	/**
	 * Adds a blank entry afterwards since 8 byte contants take two constant pool entries.
	 * 
	 * @param value
	 * @return The long info object that was added.
	 * @throws JavaSpecificationViolation Thrown if the number of entries in the pool exceeds 2^16
	 */
	public LongInfo addLongInfo(long value) throws JavaSpecificationViolation {

		LongInfo longinfo = new LongInfo(this, value);
		addItem(longinfo);
		addItem(new ConstantPoolPadding(this));
		return longinfo;
	
	}

	/**
	 * Adds a blank entry afterwards since 8 byte contants take two constant pool entries.
	 * 
	 * @param value
	 * @return The double info object that was added.
	 * @throws JavaSpecificationViolation Thrown if the number of entries in the pool exceeds 2^16
	 */
	public DoubleInfo addDoubleInfo(double value) throws JavaSpecificationViolation {

		DoubleInfo doubleinfo = new DoubleInfo(this, value);
		addItem(doubleinfo);
		addItem(new ConstantPoolPadding(this));
		return doubleinfo;
	
	}

	// Remember, the fully qualified class name must be an internal JVM name with / instead of .
	public ClassInfo addClassInfo(Class<?> classToAdd) throws JavaSpecificationViolation {

		return addClassInfo(classToAdd.getName().replace('.', '/'));
		
	}
	
	public ClassInfo addClassInfo(String fullyQualifiedJVMClassName) throws JavaSpecificationViolation {
		
		ClassInfo classinfo = new ClassInfo(this, addUTF8Info(fullyQualifiedJVMClassName));
		addItem(classinfo);
		return classinfo;
		
	}

	public UTF8Info addUTF8Info(String string) throws JavaSpecificationViolation {

		UTF8Info utf8Info = new UTF8Info(this, string);
		addItem(utf8Info);
		return utf8Info;
	
	}

	public StringInfo addStringInfo(String string) throws JavaSpecificationViolation {
		
		StringInfo stringInfo = new StringInfo(this, addUTF8Info(string));
		addItem(stringInfo);
		return stringInfo;
		
	}
	
	public MethodrefInfo addMethodrefInfo(ClassInfo classinfo, String methodName, String signature) throws JavaSpecificationViolation {

		MethodrefInfo methodInfo = new MethodrefInfo(this, classinfo, addNameAndTypeInfo(addUTF8Info(methodName), addUTF8Info(signature)));
		addItem(methodInfo);
		return methodInfo;

	}

	private NameAndTypeInfo addNameAndTypeInfo(UTF8Info info, UTF8Info info2) throws JavaSpecificationViolation {
		
		NameAndTypeInfo nameAndType = new NameAndTypeInfo(this, info, info2);
		addItem(nameAndType);
		return nameAndType;

	}
	
	public String toString() {
	
		String result = "";
		int number = 0;
		for(ConstantPoolInfo info : getItems()) {

			result = result + "\n" + number + "\t" + info;
			number++;
			
		}
		return result;
		
	}
	
}