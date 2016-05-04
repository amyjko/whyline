package edu.cmu.hcii.whyline.bytecode;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.*;

import edu.cmu.hcii.whyline.analysis.AnalysisException;
import edu.cmu.hcii.whyline.source.*;
import edu.cmu.hcii.whyline.trace.Trace;

import edu.cmu.hcii.whyline.util.Named;
import edu.cmu.hcii.whyline.util.Util;
import gnu.trove.TIntHashSet;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class Classfile implements FileInterface, Comparable<FileInterface>, Named {
	
	private int magic;
	private int majorVersion, minorVersion;
	private int accessFlag;
	private ConstantPool pool = null;
	private ClassInfo thisClass, superClassInfo;
	private QualifiedClassName classname;
	
	private Classfile superclass;
	private final ArrayList<Classfile> subclasses = new ArrayList<Classfile>(3);
	private final ArrayList<Classfile> implementors = new ArrayList<Classfile>(3);
	private TIntHashSet superclasses;
	
	private ClassInfo[] interfaces;
	private final ArrayList<Classfile> interfacesImplemented = new ArrayList<Classfile>(3);
	private ArrayList<Attribute> attributes = new ArrayList<Attribute>(3);
	private ArrayList<MethodInfo> methods;
	private MethodInfo[] nonAbstractMethods;
	private HashMap<String, MethodInfo> methodsBySignature = new HashMap<String,MethodInfo>();
	private HashMap<String, FieldInfo> fieldsByName = new HashMap<String,FieldInfo>();
	private ArrayList<FieldInfo> fields;
	private SourceFileAttribute sourceFile;
	private JavaSourceFile source = null;
	private MethodInfo main = null;
	private final int originalLength;
	
	private final WeakReference<Trace> trace;
	
	private IOAttribute io;

	private InnerClassesAttribute innerClasses;
	
	private StackDependenciesCache stackDependenciesCache = new StackDependenciesCache() {

		private final HashMap<MethodInfo,StackDependencies> stackDependenciesByMethod = new HashMap<MethodInfo,StackDependencies>(3);

		public StackDependencies getStackDependenciesFor(MethodInfo method) throws AnalysisException {

			StackDependencies dependencies = stackDependenciesByMethod.get(method);
			if(dependencies == null && method.getCode() != null) {
				
				dependencies = new StackDependencies(method.getCode());
				stackDependenciesByMethod.put(method, dependencies);
				dependencies.analyze();
				
			}
			return dependencies;
			
		}

	};
			
	public Classfile(byte[] classfileBuffer) throws IOException, JavaSpecificationViolation, AnalysisException {
		
		DataInputStream stream = new DataInputStream(new ByteArrayInputStream(classfileBuffer));
		originalLength = classfileBuffer.length;
		read(stream);
		
		this.trace = null;
		
	}
	
	public Classfile(DataInputStream stream, Trace trace) throws IOException, JavaSpecificationViolation, AnalysisException {

		this.trace = new WeakReference<Trace>(trace);

		originalLength = 1024;
		read(stream);
		
	}

	public StackDependenciesCache getStackDependenciesCache() { return stackDependenciesCache; }
	
	// By default, Classfile uses an internal class to provide access to a method's stack dependencies, storing them in memory.
	// This interface allows one to obtain dependencies from somewhere else. I added this to support dependencies cached on disk.
	public void setStackDependenciesCache(StackDependenciesCache cache) { this.stackDependenciesCache = cache; }
	
	public boolean isPublic() { return java.lang.reflect.Modifier.isPublic(accessFlag); }
	public boolean isFinal() { return java.lang.reflect.Modifier.isFinal(accessFlag); }
	public boolean isAbstract() { return java.lang.reflect.Modifier.isAbstract(accessFlag); }
	public boolean isInterface() { return java.lang.reflect.Modifier.isInterface(accessFlag); }

	
	/**
	 * The static flag of a class isn't stored in the classfile's access flags; instead, its stored in the InnerClasses attribute.
	 */
	public boolean isStatic() { 

		if(innerClasses == null) {
			return false;
		}
		else {

			int flags = innerClasses.getFlagsFor(getInternalName());
			if(flags >= 0) return java.lang.reflect.Modifier.isStatic(flags);
			else return false;
			
		}
		
	}
	
	public int compareTo(FileInterface c) {
		
		return getShortFileName().compareTo(c.getShortFileName());
		
	}
	
	/* Here's Sun's classfile spec:
	 *     
	 *ClassFile {
    	u4 magic;
    	u2 minor_version;
    	u2 major_version;
    	u2 constant_pool_count;
    	cp_info constant_pool[constant_pool_count-1];
    	u2 access_flags;
    	u2 this_class;
    	u2 super_class;
    	u2 interfaces_count;
    	u2 interfaces[interfaces_count];
    	u2 fields_count;
    	field_info fields[fields_count];
    	u2 methods_count;
    	method_info methods[methods_count];
    	u2 attributes_count;
    	attribute_info attributes[attributes_count];
      }
	 */
	private void read(DataInputStream in) throws IOException, JavaSpecificationViolation, AnalysisException {
		
		magic = in.readInt();	
		minorVersion = in.readUnsignedShort();
		majorVersion = in.readUnsignedShort();
		
		// Read in the constant pool
		pool = new ConstantPool(this, in);
		
		// Access flags; convert to enum
		accessFlag = in.readUnsignedShort();

		thisClass = (ClassInfo)pool.get(in.readUnsignedShort());
		
		int superClassIndex = in.readUnsignedShort();
		superClassInfo = superClassIndex == 0 ? superClassInfo = null : (ClassInfo)pool.get(superClassIndex);			
		
		// Read the interfaces count and the list of constant pool indices
		int interfacesCount= in.readUnsignedShort();
		interfaces = new ClassInfo[interfacesCount];
		for(int i = 0; i < interfacesCount; i++) interfaces[i] = (ClassInfo)pool.get(in.readUnsignedShort());

		// Fields count and fields
		int fieldCount = in.readUnsignedShort();
		fields = new ArrayList<FieldInfo>(fieldCount);
		for(int index = 0; index < fieldCount; index++) {
			fields.add(new FieldInfo(this, in, pool, index));
			fieldsByName.put(fields.get(index).getDisplayName(true, -1), fields.get(index));
		}

		// Methods count and methods
		int totalNumberOfInstructions = 0;
		int methodCount = in.readUnsignedShort();
		methods = new ArrayList<MethodInfo>(methodCount);
		ArrayList<MethodInfo> nonAbstractMethodsTemp = new ArrayList<MethodInfo>(methodCount);
		for(int i = 0; i < methodCount; i++) {
			MethodInfo newMethod = new MethodInfo(in, this, totalNumberOfInstructions, i); 
			totalNumberOfInstructions += newMethod.getCode() == null ? 0 : newMethod.getCode().getNumberOfInstructions();
			methods.add(newMethod);
			if(newMethod.getCode() != null) nonAbstractMethodsTemp.add(newMethod);
			if(newMethod.getInternalName().equals("main") && newMethod.getDescriptor().equals("([Ljava/lang/String;)V"))
				main = newMethod;
			methodsBySignature.put(newMethod.getMethodNameAndDescriptor(), newMethod);
		}
		
		nonAbstractMethods = new MethodInfo[nonAbstractMethodsTemp.size()];
		nonAbstractMethodsTemp.toArray(nonAbstractMethods);
		
		// Attributes count and attributes
		int attributeCount = in.readUnsignedShort();
        for (int i = 0; i < attributeCount; i++) {
        	Attribute attr = Attribute.read(this, pool, in);
        	attributes.add(attr);
        	if(attr instanceof SourceFileAttribute) sourceFile = (SourceFileAttribute)attr;
        	else if(attr instanceof IOAttribute) io = (IOAttribute)attr;
        	else if(attr instanceof InnerClassesAttribute) innerClasses = (InnerClassesAttribute)attr;
        }
        
        if(io == null) {
        	io = new IOAttribute(pool);
        	attributes.add(io);
        }
       
		in.close();
			
	}
	
	public static ClassInfo getSuperclass(byte[] bytes) {
		
		try {
		
			DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));

			int magic = in.readInt();	
			int minorVersion = in.readUnsignedShort();
			int majorVersion = in.readUnsignedShort();
			
			// Read in the constant pool
			ConstantPool pool = new ConstantPool(null, in);
			
			// Access flags; convert to enum
			int accessFlag = in.readUnsignedShort();
	
			ClassInfo thisClass = (ClassInfo)pool.get(in.readUnsignedShort());
	
			int superClassIndex = in.readUnsignedShort();
			ClassInfo superClassInfo = superClassIndex == 0 ? superClassInfo = null : (ClassInfo)pool.get(superClassIndex);			
	
			return superClassInfo;
			
		} catch(Exception e) {
			
			return null;
			
		}
		
	}
	
	/**
	 * If the class has main, returns the name of the class and null otherwise.
	 */
	public static String hasMain(DataInputStream in) {
		
		try {
		
			int magic = in.readInt();	
			int minorVersion = in.readUnsignedShort();
			int majorVersion = in.readUnsignedShort();
		
			// Read in the constant pool
			ConstantPool pool = new ConstantPool(null, in);
		
			// Access flags; convert to enum
			int accessFlag = in.readUnsignedShort();
	
			ClassInfo thisClass = (ClassInfo)pool.get(in.readUnsignedShort());
	
			int superClassIndex = in.readUnsignedShort();
			ClassInfo superClassInfo = superClassIndex == 0 ? superClassInfo = null : (ClassInfo)pool.get(superClassIndex);			
			
			// Read the interfaces count and the list of constant pool indices
			int interfacesCount= in.readUnsignedShort();
			ClassInfo[] interfaces = new ClassInfo[interfacesCount];
			for(int i = 0; i < interfacesCount; i++) interfaces[i] = (ClassInfo)pool.get(in.readUnsignedShort());
	
			// Fields count and fields
			int fieldCount = in.readUnsignedShort();
			for(int i = 0; i < fieldCount; i++) {
				in.readUnsignedShort();
				in.readUnsignedShort();
				in.readUnsignedShort();
				int attributeCount = in.readUnsignedShort();
		        for (int j = 0; j < attributeCount; ++j) {
		        	in.readUnsignedShort();
		    		int length = in.readInt();
		    		in.skipBytes(length);
		        }
			}
	
			// Methods count and methods
			int methodCount = in.readUnsignedShort();
			for(int i = 0; i < methodCount; i++) {

				in.readUnsignedShort();
				UTF8Info nameInfo = (UTF8Info) pool.get(in.readUnsignedShort());
				String name = nameInfo.toString();
				UTF8Info descriptorInfo = (UTF8Info) pool.get(in.readUnsignedShort());
				String descriptor = descriptorInfo.toString();

				if(name.equals("main") && descriptor.equals("([Ljava/lang/String;)V")) return thisClass.getName().getText();
				
				int attributeCount = in.readUnsignedShort();
				for (int j = 0; j < attributeCount; j++) {
					
					in.readUnsignedShort();
					int length = in.readInt();
					in.skipBytes(length);

				}
			
			}
			
		} catch(Exception e) {
			
			e.printStackTrace();
			return null;
			
		}
		
		return null;

	}
	
	public void setSuperclass(Classfile superclass) {

		this.superclass = superclass;
		
	}
	
	protected void forgetSuperclasses() { superclasses = null; }
	
	/**
	 * Creates a cached table of superclass name IDs.
	 */
	private void determineSuperclasses() {

		superclasses = new TIntHashSet(2);

		Classfile clazz = this;
		while(clazz != null) {
			ClassInfo superInfo = clazz.getSuperclassInfo(); 
			if(superInfo  != null) superclasses.add(superInfo.getName().getID());
			clazz = clazz.getSuperclass();
		}
		superclasses.trimToSize();
		
	}
	
	/**
	 * Make sure to call this AFTER setting all of the superclass info. Otherwise, it won't return the correct answer.
	 */
	public boolean isSubclassOf(QualifiedClassName qualifiedClassname) {

		if(qualifiedClassname == null) return false;
		if(superclasses == null) determineSuperclasses();
		
		if(getInternalName() == qualifiedClassname) return true;
		return superclasses.contains(qualifiedClassname.getID());

	}

	public boolean isExtendsOrImplements(QualifiedClassName name) {

		return getInternalName() == name || isSubclassOf(name) || implementsInterface(name);
		
	}
	
	public boolean isSuperclassOf(Classfile classfile) {
		
		Classfile superclass = classfile.getSuperclass();
		while(superclass != null) {
			if(this == superclass) return true;
			else superclass = superclass.getSuperclass();
		}
		return false;
		
	}
	
	/**
	 * Returns the superclass subclassing Object.
	 */
	public Classfile getBaseClass() { return superclass == null ? this : superclass.getSuperclass(); }
	
	public Classfile getSuperclass() { return superclass; }
	
	public void addSubclass(Classfile subclass) {
	
		subclasses.add(subclass);
		
		// We need to check if any of the subclasses methods override this class's methods, so 
		// that methods can be aware of their overriding methods.
		for(MethodInfo method : subclass.getDeclaredMethods()) {
		
			// Does this class have a method by the same name and descriptor? If so,
			// this method overrides it.
			MethodInfo overridenMethod = getDeclaredMethodByNameAndDescriptor(method.getMethodNameAndDescriptor()); 
			if(overridenMethod != null) {
				overridenMethod.addOverrider(method);
			}
			
		}
		
	}
	
	public MethodInfo getMethodNumber(int number) {
		
		return methods.get(number);
		
	}
	
	public int getNumberOfMethod(MethodInfo method) {
		
		assert method.getClassfile() == this : "" + method  + " isn't in " + this;
		
		int number = methods.indexOf(method);
		
		assert number != -1 : "Couldn't find the method in the list of methods.";
		
		return number;
		
	}
	
	public Iterable<Classfile> getDirectSubclasses() { return subclasses; }

	public Iterable<Classfile> getAllSubclasses() { 
		
		ArrayList<Classfile> allSubclasses = new ArrayList<Classfile>(subclasses.size() * 2);
		addSubclasses(allSubclasses);
		return allSubclasses;
		
	}
	
	private void addSubclasses(List<Classfile> allSubclasses) {
		
		for(int i = 0; i < subclasses.size(); i++) {
			Classfile c = subclasses.get(i);
			allSubclasses.add(c);
			c.addSubclasses(allSubclasses);
		}
		
	}

	public void addImplementor(Classfile implementor) {
		
		implementors.add(implementor);
		implementor.interfacesImplemented.add(this);
		
	}

	public boolean implementsInterface(QualifiedClassName qualifiedInterfaceName) {
		
		for(Classfile inter : interfacesImplemented)
			if(inter.getInternalName().equals(qualifiedInterfaceName) || inter.implementsInterface(qualifiedInterfaceName)) return true;
		return false;
		
	}
	
	public List<Classfile> getImplementors() { return Collections.<Classfile>unmodifiableList(implementors); }

	public List<ClassInfo> getInterfacesImplemented() { return Collections.<ClassInfo>unmodifiableList(Arrays.<ClassInfo>asList(interfaces)); }
	
	public ClassInfo getSuperclassInfo() { return superClassInfo; }
	
	public MethodInfo getMain() { return main; }
	
	public Iterable<Attribute> getAttributes() { return attributes; }
	
	public byte[] toByteArray() {
		
		return fillByteArrayStream().toByteArray();
		
	}
	
	public void writeToStream(OutputStream outputStream) {
	
		try {
			fillByteArrayStream().writeTo(outputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
		
	private ByteArrayOutputStream fillByteArrayStream() {
		
		ByteArrayOutputStream bytes = new ByteArrayOutputStream(originalLength * 2);
		DataOutputStream stream = new DataOutputStream(bytes);

		try {

			stream.writeInt(magic);
			stream.writeShort(minorVersion);
			stream.writeShort(majorVersion);
	
			// Write the constant pool
			pool.toBytes(stream);

			stream.writeShort(accessFlag);
	
			stream.writeShort(thisClass.getIndexInConstantPool());
	
			if(superClassInfo == null) stream.writeShort(0);
			else stream.writeShort(superClassInfo.getIndexInConstantPool());
			
			// Read the interfaces count and the list of constant pool indices
			stream.writeShort(interfaces.length);
			for(ClassInfo interfaceInfo : interfaces) stream.writeShort(interfaceInfo.getIndexInConstantPool());
	
			// Fields count and fields
			stream.writeShort(fields.size());
			for(FieldInfo field : fields) field.toBytes(stream);

			// Methods count and methods
			stream.writeShort(methods.size());
			for(MethodInfo method : methods) {
				method.toBytes(stream);
			}

			// Attributes count and attributes
			stream.writeShort(attributes.size());
	        for (Attribute attr : attributes)
	        	attr.toBytes(stream);

	        return bytes;
	        

		} catch (IOException e) {

			e.printStackTrace();
			return bytes;

		}
			
	}
	
	public Instruction getInstructionByID(int id) { 

		// We address instructions by their order of appearance within a class file.
		// Binary search for method with largest first instruction ID less than or equal to given id.
		// We only search methods with code; otherwise, there would be several methods with equivalent first instruction IDs.
		int low = 0;
		int size = nonAbstractMethods.length;
		int high = size - 1;

		MethodInfo method = null;
		while(low <= high) {
			int mid = (low + high) / 2;
			MethodInfo match = nonAbstractMethods[mid];
			int current = match.getFirstInstructionID();
			// Greater than the given id is no good.
			if(current > id)
				high = mid - 1;
			// Before the value is wrong if there are more values after this one and the next value is still less than or equal to the given value.
			else if(current < id && mid < size - 1 && nonAbstractMethods[mid + 1].getFirstInstructionID() <= id)
				low = mid + 1;
			else {
				method = match;
				break;
			}
		}

		assert method != null : 
			"Couldn't find the method of instruction ID " + id;

		return method.getCode().getInstruction(id - method.getFirstInstructionID());
		
	}
	
	public QualifiedClassName getInternalName() { 
	
		if(classname == null) classname = thisClass.getName();
		return classname;
		
	}
	
	public String getJavaDocURL() {
		
		StringBuilder url = new StringBuilder();
		url.append(getInternalName().getText().replace('/', File.separatorChar));
		url.append(".html");
		return url.toString();

	}
	
	public String getDisplayName(boolean html, int lengthLimit) { return Util.elide(getSimpleName(), lengthLimit); }
	
	public String getPackageName() { return getInternalName().getPackageName(); }
	
	public String getSimpleName() {
		
		return getInternalName().getSimpleName();

	}
	
	public String getConstructorName() {
		
		String simple = getSimpleName();
		int indexOfDollarSign = simple.lastIndexOf('$');
		if(indexOfDollarSign >= 0) simple = simple.substring(indexOfDollarSign);
		return simple;
		
	}
	
	public List<FieldInfo> getDeclaredFields() { return Collections.<FieldInfo>unmodifiableList(fields); }

	public List<FieldInfo> getAllFields() {
		
		Vector<FieldInfo> allFields = new Vector<FieldInfo>();
		Classfile parent = this;
		while(parent != null) {
			
			allFields.addAll(parent.fields);
			parent = parent.getSuperclass();
			
		}
		
		return Collections.<FieldInfo>unmodifiableList(allFields);
		
	}
	
	public List<FieldInfo> getAllInstanceFields() {

		Vector<FieldInfo> allFields = new Vector<FieldInfo>();
		Classfile parent = this;
		while(parent != null) {

			for(FieldInfo field : parent.fields) {
				if(!field.isStatic())
					allFields.add(field);
			}

			parent = parent.getSuperclass();
			
		}
		
		return Collections.<FieldInfo>unmodifiableList(allFields);
		
	}
	
	public String getQualifiedSourceFileName() { 

		if(sourceFile == null) return "";
		else return getPackageName() + sourceFile.getSourceFileName();
		
	}
		
	public ConstantPool getConstantPool() { return pool; }
	
	public List<MethodInfo> getDeclaredMethods() { return Collections.<MethodInfo>unmodifiableList(methods); }

	public List<MethodInfo> getAllMethods() {
		
		ArrayList<MethodInfo> allMethods = new ArrayList<MethodInfo>();
		Classfile parent = this;
		while(parent != null) {
			allMethods.addAll(parent.methods);
			parent = parent.getSuperclass();
		}
		return allMethods;

	}

	public List<MethodInfo> getPublicInstanceMethods() {
		
		List<MethodInfo> allMethods = new ArrayList<MethodInfo>();
		Classfile parent = this;
		while(parent != null) {
			for(MethodInfo method : parent.methods)
				if(method.isPublic() && !method.isStatic())
					allMethods.add(method);
			parent = parent.getSuperclass();
		}
		return allMethods;

	}
	
	public boolean isInnerClass() {

		return getInternalName().isInner();
		
	}

	public boolean hasSourceFileAttribute() { return sourceFile != null; }
	
	public boolean hasSource() { return source != null; }

	public Trace getTrace() { return trace == null ? null : trace.get(); }
	
	public JavaSourceFile getSourceFile() { 
	
		Trace t = trace != null ? trace.get() : null;
		if(source == null && t != null) source = t.getSourceFor(this);
		if(source != null) source.linkClassfile(this);
		
		return source; 
	
	}
	
	public MethodInfo getDeclaredMethodByNameAndDescriptor(String methodNameAndDescriptor) {

		return methodsBySignature.get(methodNameAndDescriptor);
	
	}
	
	public MethodInfo getDeclaredOrInheritedMethodByNameAndDescriptor(String methodNameAndDescriptor) {
		
		MethodInfo method = getDeclaredMethodByNameAndDescriptor(methodNameAndDescriptor);
		
		if(method != null) return method;
		else if(superclass != null) return superclass.getDeclaredOrInheritedMethodByNameAndDescriptor(methodNameAndDescriptor);
		else return null;
		
	}
	
	public FieldInfo getFieldByName(String name) {
		
		FieldInfo field = fieldsByName.get(name);
		
		if(field != null) return field;
		else if(superclass != null) return superclass.getFieldByName(name);
		else return null;
		
	}

	public FieldInfo getFieldNumber(int declarationIndex) {
		
		return fields.get(declarationIndex);
		
	}

	public SortedSet<Instruction> getInstructionsOnLineNumber(LineNumber lineNumber) {

		for(MethodInfo method : methods) {

			if(method.getCode() != null) {
				SortedSet<Instruction> instructionsOnLine = method.getCode().getInstructionsOnLineNumber(lineNumber);
				if(!instructionsOnLine.isEmpty()) return instructionsOnLine;
			}

		}
		return new TreeSet<Instruction>();
		
	}

	public IOAttribute getIOAttribute() { return io; }

	public String toString() { return thisClass.getName().getText(); }

	//////////////////////////////////////////////
	// File interface
	//////////////////////////////////////////////
	
	private Line[] lines = null;
	private HashMap<Instruction,LineNumber> lineNumbersByInstruction;
	private HashMap<Token, Instruction> instructionsByToken;
	private HashMap<Instruction, SortedSet<Token>> tokensByInstruction;
	private HashMap<MethodInfo,Token> methodNameTokensByMethod; 
		
	public Line[] getLines() { 

		if(lines != null) return lines;

		lineNumbersByInstruction = new HashMap<Instruction,LineNumber>();
		instructionsByToken = new HashMap<Token, Instruction>();
		tokensByInstruction = new HashMap<Instruction, SortedSet<Token>>();
		methodNameTokensByMethod = new HashMap<MethodInfo,Token>(); 

		List<Line> linesTemp = new ArrayList<Line>();

		Line firstLine = new Line(new LineNumber(this, linesTemp.size() + 1));
		firstLine.addToken(new Token(firstLine, getInternalName().getNameWithDots() + ".class", JavaParserConstants.IDENTIFIER));
		linesTemp.add(firstLine);
		linesTemp.add(Line.createBlankLine(new LineNumber(this, linesTemp.size() + 1)));		
		
		for(FieldInfo field : fields) {
		
			Line newLine = new Line(new LineNumber(this, linesTemp.size() + 1));
			newLine.addToken(new Token(newLine, java.lang.reflect.Modifier.toString(field.getAccessFlags()) + " ", JavaParserConstants.PUBLIC));
			newLine.addToken(new Token(newLine, NameAndTypeInfo.getJavafiedTypeDescriptor(field.getTypeDescriptor()) + " ",  JavaParserConstants.BYTE));
			newLine.addToken(new Token(newLine, field.getDisplayName(true, -1) + " ", JavaParserConstants.IDENTIFIER));
			linesTemp.add(newLine);
			
		}

		linesTemp.add(Line.createBlankLine(new LineNumber(this, linesTemp.size() + 1)));
		
		for(MethodInfo method : methods) {
			
			Line newLine = new Line(new LineNumber(this, linesTemp.size() + 1));

			newLine.addToken(new Token(newLine, java.lang.reflect.Modifier.toString(method.getAccessFlags()) + " ",JavaParserConstants.PUBLIC));
			
			MethodDescriptor descriptor = method.getParsedDescriptor();
			newLine.addToken(new Token(newLine, descriptor.getReturnType().getSimpleName() + " ",JavaParserConstants.BYTE));
			
			Token methodNameToken = new Token(newLine, method.getInternalName() + " ",JavaParserConstants.IDENTIFIER);
			methodNameTokensByMethod.put(method, methodNameToken);
			
			newLine.addToken(methodNameToken);
			newLine.addToken(new Token(newLine, "(",  JavaParserConstants.LPAREN));

			for(int i = 0; i < descriptor.getNumberOfParameters(); i++) {
				newLine.addToken(new Token(newLine, descriptor.getTypeOfArgumentNumber(i).getSimpleName() + " ",JavaParserConstants.BYTE));

				newLine.addToken(new Token(newLine, "arg" + (i + 1),  JavaParserConstants.IDENTIFIER));
				
				if(i < descriptor.getNumberOfParameters() - 1) newLine.addToken(new Token(newLine, ", ",JavaParserConstants.COMMA));


			}

			newLine.addToken(new Token(newLine, ") ",JavaParserConstants.RPAREN));
			newLine.addToken(new Token(newLine, "{",  JavaParserConstants.LBRACE));

			
			if(method.getCode() == null) {

				newLine.addToken(new Token(newLine, "}",  JavaParserConstants.RBRACE));
				linesTemp.add(newLine);

			}
			// Create tokens for each instruction.
			else {

				linesTemp.add(newLine);
				linesTemp.add(Line.createBlankLine(new LineNumber(this, linesTemp.size() + 1)));

				int lineNumber = linesTemp.size() + 1;
				for(Instruction inst : method.getCode().getInstructions()) {

					Line newInstructionLine = new Line(new LineNumber(this, linesTemp.size() + 1));
					
					String line = Util.fillOrTruncateString("" + inst.getIndex(), 5);
					String type = Util.fillOrTruncateString(Opcodes.NAMES[inst.getOpcode()].toLowerCase(), 18);

					newInstructionLine.addToken(new Token(newInstructionLine, line, JavaParserConstants.INTEGER_LITERAL));
					newInstructionLine.addToken(new Token(newInstructionLine, type, JavaParserConstants.RETURN));

					if(inst instanceof PushConstant) {
						
						newInstructionLine.addToken(new Token(newInstructionLine, "" + ((PushConstant<?>)inst).getConstant(),  JavaParserConstants.INTEGER_LITERAL));					
						
					}
					else if(inst instanceof Invoke) {

						newInstructionLine.addToken(new Token(newInstructionLine, "" + ((Invoke)inst).getJavaMethodName(),  JavaParserConstants.IDENTIFIER));											
						
					}
					else if(inst instanceof FieldrefContainer) {

						newInstructionLine.addToken(new Token(newInstructionLine, "" + ((FieldrefContainer)inst).getFieldref().getName(),  JavaParserConstants.IDENTIFIER));											
						
					}
					else if(inst instanceof SetLocal) {

						newInstructionLine.addToken(new Token(newInstructionLine, "" + ((SetLocal)inst).getLocalIDName(), JavaParserConstants.IDENTIFIER));											
						
					}
					else if(inst instanceof GetLocal) {

						newInstructionLine.addToken(new Token(newInstructionLine, "" + ((GetLocal)inst).getLocalIDName(), JavaParserConstants.IDENTIFIER));											
						
					}
					else if(inst instanceof NEW) {

						newInstructionLine.addToken(new Token(newInstructionLine, "" + ((NEW)inst).getClassInstantiated().getSimpleName(), JavaParserConstants.IDENTIFIER));											
						
					}
					else if(inst instanceof INSTANCEOF) {

						newInstructionLine.addToken(new Token(newInstructionLine, "" + ((INSTANCEOF)inst).getClassInfo().getSimpleName(), JavaParserConstants.IDENTIFIER));											
						
					}
					else if(inst instanceof Branch) {

						newInstructionLine.addToken(new Token(newInstructionLine, "" + ((Branch)inst).getTarget().getIndex(), JavaParserConstants.INTEGER_LITERAL));											
						
					}
					
					linesTemp.add(newInstructionLine);
					
					SortedSet<Token> tokenSet = new TreeSet<Token>();
					for(Token t : newInstructionLine.getTokens()) {
						instructionsByToken.put(t, inst);
						tokenSet.add(t);
					}
					tokensByInstruction.put(inst, tokenSet);
					
					lineNumbersByInstruction.put(inst, newInstructionLine.getLineNumber());
					
					// Was this a "terminal" instruction? Add a carriage return to make it easier to read.
					if(inst.getNumberOfOperandsProduced() == 0)
						linesTemp.add(Line.createBlankLine(new LineNumber(this, linesTemp.size() + 1)));
					
				}

				linesTemp.add(Line.createBlankLine(new LineNumber(this, linesTemp.size() + 1)));
				Line rightBrace = new Line(new LineNumber(this, linesTemp.size() + 1));
				rightBrace.addToken(new Token(newLine, "}", JavaParserConstants.RBRACE));
				linesTemp.add(rightBrace);

			}
			
			linesTemp.add(Line.createBlankLine(new LineNumber(this, linesTemp.size() + 1)));
			
		}

		lines = new Line[linesTemp.size()];
		linesTemp.toArray(lines);

		return lines;
		
	}

	public Line getLine(int lineNumber) {
		
		getLines();
		// The vector is 0-indexed, but line numbers start at 1.
		int zeroIndexedLineNumber = lineNumber - 1;
		if(zeroIndexedLineNumber < 0 || zeroIndexedLineNumber >= lines.length) return null;
		return lines[lineNumber - 1];
		
	}

	public Line getLine(Instruction inst) {
		
		assert inst.getClassfile() == this;

		getLines();
		
		return getLine(lineNumbersByInstruction.get(inst).getNumber());
		
	}
	
	public int getNumberOfLines() { getLines(); return lines.length; }
	
	public String getFileName() { return getInternalName().getText().replace('/', '.') + ".class"; }

	public String getShortFileName() { return getSimpleName() + ".class"; }

	public TokenRange getTokenRangeFor(Instruction i) {
		
		getLines();
		SortedSet<Token> tokens = tokensByInstruction.get(i);
		if(tokens != null)
			return new TokenRange(tokens.first(), tokens.last());
		else {
			return null;
		}
		
	}
	
	public TokenRange getTokenRangeForParameter(MethodInfo method, int parameter) {
		
		getLines();
		Token name = methodNameTokensByMethod.get(method);
		return new TokenRange(name, name);		
		
	}
	
	public TokenRange getTokenRangeForMethod(MethodInfo method) {
		
		getLines();
		Token name = methodNameTokensByMethod.get(method);
		return new TokenRange(name, name);
		
	}

	public TokenRange getTokenRangeFor(Classfile c) { 
		
		return getLines()[0].getRange();
		
	}
	
	public Instruction getInstructionFor(Token t) {
		
		return instructionsByToken.get(t);
		
	}

	public QualifiedClassName getClassnameFor(Token token) {
		
		return null;
		
	}

	public Parameter getMethodParameterFor(Token token) {
		
		return null;
		
	}

	public SortedSet<Instruction> getInstructionsOnLine(Line l) { return new TreeSet<Instruction>(); }
	
	private int getIndexOf(Line line) {

		getLines();
		int lineIndex = -1;
		for(int i = 0; i < lines.length; i++)
			if(lines[i] == line) return i;
		return -1;
		
	}

	public Token getCodeTokenAfter(Token t) { 

		Line line = t.getLine();
		int lineIndex = getIndexOf(line);
		if(lineIndex < 0) return null;
		while(line != null) {
			Token next = line.getTokenAfter(t);
			if(next != null) return next;
			else if(lineIndex + 1 >= lines.length) return null;
			else  line = lines[++lineIndex];
		}
		return null; 
		
	}
	
	public Token getCodeTokenBefore(Token t) { 
		
		return null; 
		
	}

	public Token getTokenForMethodName(MethodInfo method) {
		
		getLines();
		return methodNameTokensByMethod.get(method);
		
	}
	
	public boolean isFamiliar() { return false; }
	
	//////////////////////////////////////////////
	
	public void trim() {

		attributes.trimToSize();
		fields.trimToSize();
		methods.trimToSize();
		
		for(MethodInfo method : methods)
			method.trimToSize();
		
	}
	
}