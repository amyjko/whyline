package edu.cmu.hcii.whyline;

import java.io.File;

import edu.cmu.hcii.whyline.bytecode.Classfile;
import edu.cmu.hcii.whyline.bytecode.MethodInfo;
import edu.cmu.hcii.whyline.util.Util;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class PrintClass {

	public static void main(String[] args) {
		
		for(String arg : args) {
		
			try {
			
				Classfile cf = new Classfile(Util.getReaderFor(new File(arg)), null);
				
				for(MethodInfo method : cf.getDeclaredMethods()) {
					System.out.println(method.getCode() == null ? "interface " + method.getQualifiedNameAndDescriptor() : method.getCode().toString());
					System.out.println("\n\n");
				}
			
			} catch(Exception e) {
				
				System.out.println("Failed to print " + arg);
				e.printStackTrace();
				
			}
				
		}
		
	}

}
