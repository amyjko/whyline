package edu.cmu.hcii.whyline.trace;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.source.*;

/**
 * Converts code and events to text for serialization.
 * 
 * @author Andrew J. Ko
 *
 */ 
public class Serializer {

	public static String lineToString(Line line) {
		
		return listToString(fileToString(line.getFile()), Integer.toString(line.getLineNumber().getNumber()));
		
	}
	
	public static Line stringToLine(Trace trace, String text) {
		
		String[] strings = stringToList(text);
		return stringToLine(trace, strings[0], strings[1]);
		
	}
	
	public static Line stringToLine(Trace trace, String sourcename, String lineNumber) {
		
		JavaSourceFile source = trace.getSourceByQualifiedName(sourcename);
		Integer number = Integer.parseInt(lineNumber);
		if(source == null) return null;
		try {
			return source.getLine(number);
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}

	}
	
	public static String classfileToString(Classfile classfile) {
		
		return classfile.getInternalName().getText();
		
	}

	public static String methodToString(MethodInfo method) {
		
		return listToString(classfileToString(method.getClassfile()), method.getMethodNameAndDescriptor());
		
	}

	public static String fileToString(FileInterface entity) {
		
		return entity.getFileName();

	}
	
	public static String[] stringToList(String string) {
		
		return string.split(":");
		
	}
	
	public static String listToString(String ... args) {
		
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < args.length; i++) {
			builder.append(args[i]);
			if(i != args.length - 1)
				builder.append(':');
		}
		return builder.toString();
		
	}

	public static String instructionToString(Instruction entity) {
		
		return listToString(methodToString(entity.getMethod()), Integer.toString(entity.getIndex()));
		
	}

	public static MethodInfo stringToMethod(Trace trace, String classname, String methodname) {

		return stringToClassfile(trace, classname).getDeclaredMethodByNameAndDescriptor(methodname);

	}
	
	public static Instruction stringtoInstruction(Trace trace, String classname, String methodname, String instructionIndex) {
		
		MethodInfo method = stringToMethod(trace, classname, methodname);
		if(method == null) System.err.println("Couldn't find " + classname + " " + methodname);
		CodeAttribute code = method.getCode();
		return code.getInstruction(Integer.parseInt(instructionIndex));
		
	}

	public static Classfile stringToClassfile(Trace trace, String string) {

		return trace.getClassfileByName(QualifiedClassName.get(string));
		
	}
	
}
