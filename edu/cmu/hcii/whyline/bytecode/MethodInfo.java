package edu.cmu.hcii.whyline.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

import edu.cmu.hcii.whyline.analysis.AnalysisException;
import edu.cmu.hcii.whyline.analysis.Cancelable;
import edu.cmu.hcii.whyline.trace.Trace;

import edu.cmu.hcii.whyline.util.Named;
import edu.cmu.hcii.whyline.util.Util;

/**
 *	 method_info {
 *	 	u2 access_flags;
 *		u2 name_index;
 *		u2 descriptor_index;
 *		u2 attributes_count;
 *		attribute_info attributes[attributes_count];
 *	}
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class MethodInfo implements Comparable<MethodInfo>, Named {
	
	private final Classfile classfile;
	private final int access;
	private final UTF8Info nameInfo;
	private final String name;
	private final UTF8Info descriptorInfo;
	private final String descriptor;
	private final String nameAndDescriptor;
	private MethodDescriptor parsedDescriptor;
	private CodeAttribute code;
	private ExceptionsAttribute exceptions;
	private Attribute[] attributes;
	
	private ArrayList<Invoke> potentialCallers;
	private Set<Invoke> actualCallers;

	private final ArrayList<MethodInfo> overriders = new ArrayList<MethodInfo>(1);
	private MethodInfo methodOverriden = null;

	private int flags = 0;
	private static final int IS_MAIN = 2;
	private static final int IS_RUN = 3;
	private static final int IS_CLINIT = 4;
	private static final int IS_INIT = 5;
	private static final int IS_SYNTHETIC = 7;
	
	private boolean getFlag(int flag) { return (flags & (1 << flag)) != 0; }
	private void setFlag(int flag) { flags = flags | (1 << flag); }

	private final int firstInstructionID;
	private final int declarationIndex;
	
	
	public MethodInfo(DataInputStream data, Classfile classfile, int instructionID, int declarationIndex) throws IOException, JavaSpecificationViolation, AnalysisException {

		ConstantPool pool = classfile.getConstantPool();
		this.classfile = classfile;
		this.firstInstructionID = instructionID;
		this.declarationIndex = declarationIndex;
		
		access = data.readUnsignedShort();

		nameInfo = (UTF8Info) pool.get(data.readUnsignedShort());
		name = nameInfo.toString();
		
		descriptorInfo = (UTF8Info) pool.get(data.readUnsignedShort());
		descriptor = descriptorInfo.toString();

		nameAndDescriptor = (name + descriptor).intern();

		boolean isPublic = isPublic();
		
		if(name.startsWith("<cl"))
			setFlag(IS_CLINIT);
	
		else if(name.equals("<init>"))
			setFlag(IS_INIT);

		else if(isPublic && name.equals("main") && isStatic() && getDescriptor().equals("([Ljava/lang/String;)V"))
			setFlag(IS_MAIN);

		else if(name.startsWith("access$"))
			setFlag(IS_SYNTHETIC);
		
		else if(isPublic && 
			name.equals("run") && 
			getDescriptor().equals("()V") && 
			(getClassfile().isSubclassOf(QualifiedClassName.JAVA_LANG_THREAD) || getClassfile().implementsInterface(QualifiedClassName.JAVA_LANG_RUNNABLE)))
			setFlag(IS_RUN);
		
		getClassfile().forgetSuperclasses();
		
		int attributeCount = data.readUnsignedShort();
		
		attributes = new Attribute[attributeCount];
		for (int i = 0; i < attributeCount; i++) {
			Attribute attr = Attribute.read(this, pool, data);
			attributes[i] = attr;
			if (attr instanceof CodeAttribute)
				code = (CodeAttribute) attr;
			else if (attr instanceof ExceptionsAttribute)
				exceptions = (ExceptionsAttribute) attr;
			else if (attr instanceof SyntheticAttribute)
				setFlag(IS_SYNTHETIC);
		}

	}

	public void toBytes(DataOutputStream bytes) throws IOException {

		bytes.writeShort(access);
		bytes.writeShort(nameInfo.getIndexInConstantPool());
		bytes.writeShort(descriptorInfo.getIndexInConstantPool());
		bytes.writeShort(attributes.length);
		for (Attribute attr : attributes)
			attr.toBytes(bytes);

	}
	
	public int getDeclarationIndex() { return declarationIndex; }
	
	/**
	 * Returns true if this method doesn't appear in the classes corresponding source file 
	 * (due to the appearance of a SyntheticAttribute in the method's attribute list)
	 */
	public boolean isSynthetic() { return getFlag(IS_SYNTHETIC); }
	
	public Iterable<AbstractReturn> getReturns() {
		
		if(code == null) return new ArrayList<AbstractReturn>(1);
		else return code.getReturns();
		
	}
	
	/**
	 * Called "internal" because names like <init> don't look like constructors.
	 */
	public String getInternalName() { return name; }
	
	/**
	 * Called "java" name because they look like the declared name in source.
	 */
	public String getJavaName() { 
	
		if(isInstanceInitializer())
			return getClassfile().getSimpleName();
		
		if(isClassInitializer())
			return "static";

		return name;
		
	}
	
	public String getDisplayName(boolean html, int lengthLimit) { return Util.elide(getJavaName(), lengthLimit) + "()"; }

	public String getDescriptor() { return descriptor; }
	public String getMethodNameAndDescriptor() { return nameAndDescriptor; }
	public String getQualifiedNameAndDescriptor() { 

		StringBuilder builder = new StringBuilder();
		builder.append(classfile.getInternalName().getText());
		builder.append(".");
		builder.append(nameAndDescriptor);
		return builder.toString();
		
	}
	
	/**
	 * Like a regular descriptor, but stripped of its qualifications and return type.
	 */
	public String  getSimpleDescriptor() { return getParsedDescriptor().getSimpleDescriptor(); }

	public String getJavaDocURL() {
		
		StringBuilder url = new StringBuilder(getClassfile().getJavaDocURL());
		url.append("#");
		url.append(getJavaName());
		url.append(getParsedDescriptor().getJavaDocURL());
		return url.toString();
		
	}

	/**
	 * This may have arguments representing enclosing instances, if this is a constructor call on an inner class.
	 * If so, we may return 2 or 3, depending on the type of inner class. Otherwise, we return 1.
	 */
	public int getFirstArgumentAppearingInSource() {
		
		if(isInstanceInitializer() && getClassfile().getInternalName().isInner() && !getClassfile().isStatic()) {

			if(getClassfile().isInnerClass())
				return 3;
			else
				return 2;
			
		}
		else 
			return 1;

	}

	public int getArgumentNumberOfLocalID(int localID) { return getParsedDescriptor().getArgumentNumberFromLocalID(localID); }
	
	public int getLocalIDFromArgumentNumber(int argumentNumber) { return getParsedDescriptor().getLocalIDFromArgumentNumber(argumentNumber); }
	
	public Classfile getClassfile() { return classfile; }

	public boolean matchesNameAndDescriptor(String name, String descriptor) {

		return getInternalName().equals(name) && getDescriptor().equals(descriptor);

	}

	public boolean matchesClassAndName(QualifiedClassName classname, String methodname) {

		return classname.equals(classfile.getInternalName()) && methodname.equals(name);

	}

	public boolean matchesClassNameAndDescriptor(QualifiedClassName classname, String name, String descriptor) {

		return classfile.getInternalName().equals(classname) && this.name.equals(name) && this.descriptor.equals(descriptor);

	}

	public MethodDescriptor getParsedDescriptor() { 

		if(parsedDescriptor == null) parsedDescriptor = MethodDescriptor.get(isStatic(), descriptor);
		return parsedDescriptor; 
		
	}

	public int getNumberOfArguments() { return getParsedDescriptor().getNumberOfParameters(); }

	private int localIDOfFirstNonArgument = -1;
	
	public int getLocalIDOfFirstNonArgument() {

		if(localIDOfFirstNonArgument == -1) {
		
			int index = isStatic() ? 0 : 1;
			for (String type : getParsedDescriptor())
				index += (type.equals(MethodDescriptor.LONG) || type.equals(MethodDescriptor.DOUBLE)) ? 2 : 1;
			localIDOfFirstNonArgument = index;
			
		}
		return localIDOfFirstNonArgument;

	}

	public void addOverrider(MethodInfo overrider) {
		
		overriders.add(overrider);
		overrider.setMethodOverriden(this);
		
	}
	
	public List<MethodInfo> getOverriders() { return Collections.<MethodInfo>unmodifiableList(overriders); }
	
	private void setMethodOverriden(MethodInfo methodOverriden) {
		
		this.methodOverriden = methodOverriden;
		
	}
	
	public MethodInfo getMethodOverriden() { return methodOverriden; }
	
	public void addPotentialCaller(Invoke invocation) { 
	
		if(potentialCallers == null) potentialCallers = new ArrayList<Invoke>(2);
		potentialCallers.add(invocation);  
		
	}
	
	public Collection<Invoke> getPotentialCallers() { return potentialCallers == null ? Collections.<Invoke>emptySet() : potentialCallers; }
	
	public Set<Invoke> getPreciseCallers(Trace trace, Cancelable cancelable) { 

		if(actualCallers == null) {
		
			actualCallers = new HashSet<Invoke>(getPotentialCallers().size() / 2);

			// For each potential caller, ask the caller about its precise method resolution. If this isn't part of it, we don't include it. 
			for(Invoke caller : getPotentialCallers()) {
	
				MethodInfo[] methods = caller.getPreciseMethodsCalled(trace, cancelable);
				if(cancelable != null && cancelable.wasCanceled()) return null;

				for(MethodInfo m : methods)
					if(m == this) {
						actualCallers.add(caller);
						break;
					}
				
			}
				
		}
			
		return actualCallers; 		
	
	}

	public int getAccessFlags() { return access; } 
	
	public boolean isSynchronized() { return java.lang.reflect.Modifier.isSynchronized(access); }
	public boolean isNative() { return java.lang.reflect.Modifier.isNative(access); }
	public boolean isVirtual() { return !isStatic(); }
	public boolean isStatic() { return java.lang.reflect.Modifier.isStatic(access); }
	public boolean isFinal() { return java.lang.reflect.Modifier.isFinal(access); }
	public boolean isPublic() { return java.lang.reflect.Modifier.isPublic(access); }
	public boolean isPrivate() { return java.lang.reflect.Modifier.isPrivate(access); }
	public boolean isProtected() { return java.lang.reflect.Modifier.isProtected(access); }
	public boolean isClassInitializer() { return getFlag(IS_CLINIT); }
	public boolean isInstanceInitializer() { return getFlag(IS_INIT); }
	public boolean isAbstract() { return java.lang.reflect.Modifier.isAbstract(access); }
	public boolean isStrict() { return java.lang.reflect.Modifier.isStrict(access); }
	public boolean isMain() { return getFlag(IS_MAIN); }
	public boolean isRun() { return getFlag(IS_RUN); }
	public boolean isImplicitlyInvoked() { return isMain() || isClassInitializer() || isRun(); }
	
	public boolean isAccessibleFrom(Classfile type) {
		
		if(classfile == type) return true;
		
		if(type.isSubclassOf(classfile.getInternalName()))
			return isPublic() || isProtected();
		else 
			return false;

	}
	
	public boolean isStateAffecting() { return code == null ? false : code.isStateAffecting(); }

	public int getFirstInstructionID() { return firstInstructionID; }

	public int compareTo(MethodInfo o) {
		
		return getQualifiedNameAndDescriptor().compareTo(o.getQualifiedNameAndDescriptor());

	}

	public CodeAttribute getCode() {
		return code;
	}

	public void trimToSize() {

		if(potentialCallers != null) potentialCallers.trimToSize();
		overriders.trimToSize();
		
	}

	public boolean callsSuper() { return code != null && code.getCallToSuper() != null; }
	
	public String toString() { return getQualifiedNameAndDescriptor();	}
	
}