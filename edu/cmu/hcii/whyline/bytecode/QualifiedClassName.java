package edu.cmu.hcii.whyline.bytecode;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Rather than just using strings to represent class names, I'm trying to make it more type safe by using a class to represent the name.
 * 
 * @author Andrew J. Ko
 *
 */
public final class QualifiedClassName implements Comparable<QualifiedClassName> {

	private final String qualifiedName;
	private String simpleName;
	private boolean familiar = false;
	private final int id;
	
	private static final Map<String, QualifiedClassName> names = new HashMap<String, QualifiedClassName>(1000);
	private static int nextID = 1;
	
	public static final QualifiedClassName JAVA_LANG_CLASS = get("java/lang/Class");
	public static final QualifiedClassName JAVA_LANG_OBJECT = get("java/lang/Object");
	public static final QualifiedClassName JAVA_LANG_STRING = get("java/lang/String");
	public static final QualifiedClassName JAVA_LANG_THREAD = get("java/lang/Thread");
	public static final QualifiedClassName JAVA_LANG_RUNNABLE = get("java/lang/Runnable");
	public static final QualifiedClassName NULL = get("null");
	public static final QualifiedClassName JAVA_LANG_THROWABLE = get("java/lang/Throwable");
	public static final QualifiedClassName JAVA_LANG_EXCEPTION = get("java/lang/Exception");

	public static final QualifiedClassName BYTE = get("B");
	public static final QualifiedClassName CHAR = get("C");
	public static final QualifiedClassName DOUBLE = get("D");
	public static final QualifiedClassName FLOAT = get("F");
	public static final QualifiedClassName INT = get("I");
	public static final QualifiedClassName LONG = get("J");
	public static final QualifiedClassName SHORT = get("S");
	public static final QualifiedClassName BOOLEAN = get("Z");
	public static final QualifiedClassName VOID = get("V");

	public static final QualifiedClassName STRING_BUILDER = get("java/lang/StringBuilder");
	public static final QualifiedClassName PRINT_STREAM = get("java/io/PrintStream");
	public static final QualifiedClassName OUTPUT_STREAM = get("java/io/OutputStream");
	public static final QualifiedClassName WRITER = get("java/io/Writer");
	
	public static QualifiedClassName getFromTypeDescriptor(String typeDescriptor) {
		
		String arrayPart = "";
		int lastBracketIndex = typeDescriptor.lastIndexOf('[');
		if(lastBracketIndex >= 0) {
			arrayPart = typeDescriptor.substring(0, lastBracketIndex + 1);
			typeDescriptor = typeDescriptor.substring(lastBracketIndex + 1);
		}

		StringBuilder builder = new StringBuilder(arrayPart);
		
		String rest = "";
		
		char firstChar = typeDescriptor.charAt(0);
		switch(firstChar) {

		case 'L' : 
			rest = typeDescriptor.substring(1, typeDescriptor.length() - 1); 
			break;
		case 'B' :
		case 'C' :
		case 'D' :
		case 'F' :
		case 'I' :
		case 'J' :
		case 'S' :
		case 'Z' : 
		case 'V' :
			rest = Character.toString(firstChar);
			break;
		default : 
			assert false : "I must have missed one because I don't know how to handle " + firstChar;
		}

		builder.append(rest);

		return QualifiedClassName.get(builder.toString());
		
	}

	
	public static QualifiedClassName get(String name) {
		
		// We synchronize on this table so that we don't get duplicates.
		synchronized(names) {
					
			QualifiedClassName cachedName = names.get(name);
			if(cachedName == null) {
		
				// Make sure its in internal form.
				name = name.replace('.', '/');
				cachedName = names.get(name);
	
				if(cachedName == null) {
	
					cachedName = new QualifiedClassName(name, nextID++);
					names.put(name, cachedName);
					
				}
	
			}
			return cachedName;
			
		}
		
	}
	
	private QualifiedClassName(String name, int id) {
				
		this.qualifiedName = name;
		this.id = id;
		
	}
	
	public String getText() { return qualifiedName; }

	
	/**
	 * Returns the qualified name with the package qualification stripped.
	 */
	public String getSimpleClassQualifiedName() {
		
		int lastSlashIndex =qualifiedName.lastIndexOf('/');
		if(lastSlashIndex >= 0)
			return qualifiedName.substring(lastSlashIndex + 1);
		else 
			return qualifiedName;
		
	}
	
	/**
	 * Returns the class name with all package and inner class context removed.
	 */
	public String getSimpleName() {

		if(simpleName == null) {

			if(qualifiedName.length() == 1) {
			
				if(this == BYTE) return "byte";
				else if(this == CHAR) return "char";
				else if(this == DOUBLE) return "double";
				else if(this == FLOAT) return "float";
				else if(this == INT) return "int";
				else if(this == LONG) return "long";
				else if(this == SHORT) return "short";
				else if(this == BOOLEAN) return "boolean";
				else if(this == VOID) return "void";
				
			}
			
			int lastBracket = qualifiedName.lastIndexOf('[');
			boolean isAnonymous = isAnonymous();

			// If this is an array class name, deal with the brackets and primitive types.
			if(lastBracket >= 0) {
			
				String brackets = "";
				for(int i = 0; i <= lastBracket; i++)
					brackets = brackets + "[]";

				String rest = qualifiedName.substring(lastBracket + 1);

				// Is this an reference type or primitive?
				simpleName = NameAndTypeInfo.getJavafiedPrimitiveTypeDescriptor(rest.charAt(0));
				if(simpleName != null) simpleName = simpleName + brackets;
				else {
				
					String className = rest;
					int indexOfLast = className.lastIndexOf("/");
					className = indexOfLast < 0 ? className : className.substring(indexOfLast + 1);
					simpleName = className + brackets;

				}
				
			}
			else if(isAnonymous) {
				
				int indexOfLast = qualifiedName.lastIndexOf('/');
				simpleName = indexOfLast < 0 ? qualifiedName : qualifiedName.substring(indexOfLast + 1);

			}
			else {
				
				int indexOfLast = Math.max(qualifiedName.lastIndexOf('/'), qualifiedName.lastIndexOf('$'));
				simpleName = indexOfLast < 0 ? qualifiedName : qualifiedName.substring(indexOfLast + 1, qualifiedName.length());
				
			}

		}
		
		return simpleName;

	}
	
	public String getPackageName() { 
		
		int indexOfLastForwardSlash = qualifiedName.lastIndexOf('/');
		
		// Default package name.
		if(indexOfLastForwardSlash < 0) return "";
		else return qualifiedName.substring(0, indexOfLastForwardSlash + 1);

	}	
	
	public String getCorrespondingClassfileName() { 
		
		return qualifiedName.replace("/", File.separator) + ".class";
		
	}
	
	public String getNameWithDots() { return qualifiedName.replace("/", "."); }
	
	public boolean equals(Object classname) {
		
		return this == classname;
		
	}
	
	public boolean isAnonymous() {
		
		return Character.isDigit(qualifiedName.charAt(qualifiedName.length() - 1));

	}
	
	public int hashCode() { 
	
		return id; 
		
	}

	public int compareTo(QualifiedClassName o) { return qualifiedName.compareTo(o.qualifiedName); }

	public boolean isArray() { return qualifiedName.startsWith("["); }

	public boolean isPrimitive() {
		
		return 
			this == BYTE ||
			this == CHAR ||
			this == DOUBLE ||
			this == FLOAT ||
			this == INT ||
			this == LONG ||
			this == SHORT ||
			this == BOOLEAN;
		
	}
	
	public String toString() { return qualifiedName; }

	public void markAsReferencedInFamiliarClass() { familiar = true; }
	public boolean referencedInFamiliarClass() { return familiar; }

	public int getID() { return id; }

	public boolean isInner() { return qualifiedName.indexOf('$') >= 0; }

	/**
	 * Returns null if this is not an inner class name.
	 */
	public QualifiedClassName getOuterClassName() {
		
		int lastDollarIndex = qualifiedName.lastIndexOf('$');
		if(lastDollarIndex < 0) return null;

		return get(qualifiedName.substring(0, lastDollarIndex));

	}

	public QualifiedClassName getArrayElementClassname() {
		
		int lastBracket = qualifiedName.lastIndexOf('[');
		String name = qualifiedName.substring(lastBracket + 1);
		return QualifiedClassName.get(name);
		
	}

}