package edu.cmu.hcii.whyline.bytecode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * 
 * On average, there is 1 unique descriptor per 10 descriptors, so we pool these to save space.
 * 
 * @author Andrew J. Ko
 * 
 */ 
public final class MethodDescriptor implements Iterable<String> {
	
	public static final String BYTE = "B";
	public static final String CHAR = "C";
	public static final String DOUBLE = "D";
	public static final String FLOAT = "F";
	public static final String INT = "I";
	public static final String LONG = "J";
	public static final String SHORT = "S";
	public static final String BOOLEAN = "Z";
	public static final String VOID = "V";
	
	private final String[] parameterTypes;
	private final String returnType;
	private final boolean isStatic;
	
	private static HashMap<String,MethodDescriptor> staticDescriptors = new HashMap<String,MethodDescriptor>(10);
	private static HashMap<String,MethodDescriptor> nonStaticDescriptors = new HashMap<String,MethodDescriptor>(10);

	public static MethodDescriptor get(boolean isStatic, String descriptor) {
		
		MethodDescriptor desc = isStatic ? staticDescriptors.get(descriptor) : nonStaticDescriptors.get(descriptor);
		if(desc == null) {
				desc = new MethodDescriptor(isStatic, descriptor);
				if(isStatic) staticDescriptors.put(descriptor, desc);
				else nonStaticDescriptors.put(descriptor, desc);
		}
		return desc;
		
	}	
	
	private MethodDescriptor(boolean isStatic, String descriptor) {
		
		this.isStatic = isStatic;
		
		ArrayList<String> parameterTypes = new ArrayList<String>(5);

		int closingParenIndex = descriptor.lastIndexOf(')');
		
		int index = 1; // Skip the first left paren

		// Read until we read the end of the parameter list
		while(index != closingParenIndex) {

			// Read any array descriptors
			int indexOfTypeStart = index;
			while(descriptor.charAt(index) == '[') { index++; }

			// If it's an object, read to the semi colon
			if(descriptor.charAt(index) == 'L')
				while(descriptor.charAt(index) != ';') index++;

			String type = descriptor.substring(indexOfTypeStart, index + 1);
			// These should be quite redundant (based on profiler data), so we pool them.
			type = type.intern();
			parameterTypes.add(type);
			
			// Go to the next character
			index++;

		}

		// Skip over the right paren
		index++;
		
		// The return type is the rest
		String rest = descriptor.substring(index);
		returnType = rest.intern();

		this.parameterTypes = new String[parameterTypes.size()];
		parameterTypes.toArray(this.parameterTypes);
		
	}
	
	public Iterator<String> iterator() { 
		return new Iterator<String>() {
			int i = 0;
			public boolean hasNext() { return i < parameterTypes.length; }
			public String next() { return parameterTypes[i++]; }
			public void remove() { throw new UnsupportedOperationException("Can't remove method descriptor types."); }
		};
	}

	/**
	 * Returns this descriptor from ( to ) (without the return type) and with unqualified types.
	 * Used to match MethodInfos to ClassBodyElements representing methods. Also removes outer class qualifications separated by $'s.
	 */
	public String getSimpleDescriptor() {

		StringBuilder builder = new StringBuilder("(");
		for(String param : parameterTypes) {
			builder.append(dequalifyClassname(param));
		}
		builder.append(")");
		return builder.toString();
		
	}
	
	private static String dequalifyClassname(String param) {
		
		// Find where the 'L' is, if there is one.
		int elIndex = param.indexOf('L');
		// Find the last / or $
		int lastIndex = Math.max(param.lastIndexOf('/'), param.lastIndexOf('$'));
		// If there's an L and a / or $, get everything in between.
		if(elIndex >= 0 && lastIndex >= 0) {
			StringBuilder builder = new StringBuilder();
			builder.append(param.substring(0, elIndex + 1));
			builder.append(param.substring(lastIndex + 1));
			return builder.toString();
		}
		else return param;

	}

	public int getNumberOfParameters() { return parameterTypes.length; }
	
	public QualifiedClassName getReturnType() { return QualifiedClassName.getFromTypeDescriptor(returnType); }
	
	// 0 is the 1st argument of the method, 1 the 2nd, 2 the third, and so on.
	// If its a reference type, it returns a qualified class name without the preceding L and trailing ;
	// Otherwise, the type descriptor is left as is. Like Class.getName(), except without replacing / with .
	public QualifiedClassName getTypeOfArgumentNumber(int argumentNumber) { 
		
		assert argumentNumber >= 0 && argumentNumber < parameterTypes.length : "Illegal parameter type index: " + argumentNumber + " must be between 0 and " + (parameterTypes.length - 1) + " inclusive.";
		return QualifiedClassName.getFromTypeDescriptor(parameterTypes[argumentNumber]);
		
	}
		
	// If the local index is 0 and this is not static, returns 0, referring to local "this". 
	public int getArgumentNumberFromLocalID(int localID) {
		
		// Is this local index a legal argument index?
		if(!isStatic && localID == 0) return 0;
		
		// Skip over the instance, if there is one.
		int currentIndex = isStatic ? 0 : 1;
		int argumentNumber = currentIndex;
		
		for(String type : parameterTypes) {

			if(currentIndex == localID)
				return argumentNumber;
			
			currentIndex = currentIndex + ((type.equals(MethodDescriptor.DOUBLE) || type.equals(MethodDescriptor.LONG)) ? 2 : 1); 
			argumentNumber++;
			
		}
		
		return -1;
		
	}

	// For static methods, the arguments are numbered 0 through N - 1, where N is the number of arguments.
	// For virtual methods, the arguments are numbered 0 through N, where N is the number of arguments, and argument 0 is the instance.
	public int getLocalIDFromArgumentNumber(int argumentNumber) {
		
		if(argumentNumber < 0) return -1;
		if(argumentNumber == 0) return 0;
		
		// Skip over the instance, if there is one.
		int currentArgument = isStatic ? 0 : 1;
		int currentIndex = currentArgument;
		
		for(String type : parameterTypes) {

			if(currentArgument == argumentNumber)
				return currentIndex;
			
			currentIndex = currentIndex + ((type.equals(MethodDescriptor.DOUBLE) || type.equals(MethodDescriptor.LONG)) ? 2 : 1); 
			currentArgument++;
			
		}

		return -1;
		
	}

	public String toString() {
		
		StringBuilder builder = new StringBuilder();
		builder.append("(");
		for(String s : parameterTypes)
			builder.append(s);
		builder.append(")");
		builder.append(returnType);
		
		return builder.toString();
		
	}

	public String  getJavaDocURL() {

		StringBuilder url = new StringBuilder();
		url.append("(");
		for(int i = 0; i < parameterTypes.length; i++) {
			url.append(getTypeOfArgumentNumber(i).getNameWithDots());
			if(i < parameterTypes.length - 1)
				url.append(",%20");
		}
		url.append(")");
		return url.toString();
	
	}
	
}
