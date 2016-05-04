package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.analysis.Cancelable;
import edu.cmu.hcii.whyline.analysis.ValueSource;

import edu.cmu.hcii.whyline.trace.Trace;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class INVOKESPECIAL extends Invoke {

	public INVOKESPECIAL(CodeAttribute method, MethodrefInfo methodInfo) {
		super(method, methodInfo);
	}

	public boolean isInstanceInitializer() { return methodInfo.getMethodName().equals("<init>"); }
	
	public MethodInfo[] getPreciseMethodsCalled(Trace trace, Cancelable cancelable) { return trace.getMethodsFromReference(this); }

	public final int getOpcode() { return 183; }
	public int byteLength() { return 3; }

	public void toBytes(DataOutputStream code) throws IOException {

		code.writeByte(getOpcode());
		code.writeShort(methodInfo.getIndexInConstantPool());

	}
	
	public String getJavaMethodName() {
		
		if(!isInstanceInitializer()) return methodInfo.getMethodName();
		
		QualifiedClassName enclosingClass = getMethod().getClassfile().getInternalName();
		QualifiedClassName classCalled = methodInfo.getClassName();
		StackDependencies.Producers producers = getProducersOfArgument(0);
		Instruction producer = producers.getFirstProducer();
		boolean referencesThis = producer instanceof GetLocal && ((GetLocal)producer).getLocalID() == 0;

		if(!referencesThis) return methodInfo.getClassName().getSimpleName();
		else if(enclosingClass == classCalled) return "this";
		else return "super";		
			
	}

	public boolean couldCallOn(ValueSource typeOfThis, Trace trace) {

		return true;
		
//		QualifiedClassName typeInvokedOn = getMethodInvoked().getClassName();
//		
//		// The type specified in the invoke must be the same as the given type, or the superclass of the given type.
//		for(QualifiedClassName type : typeOfThis.typesOfValue) {
//
//			if(type == typeInvokedOn) return true;
//			Classfile typeClass = trace.getClassfileByFullyQualifiedName(type);
//			if(typeClass == null) return true;
//			else if(typeClass.getSuperclassInfo().getName() == typeInvokedOn) return true;
//			
//		}
//		return false;
		
	}
			
}