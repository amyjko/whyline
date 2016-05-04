package edu.cmu.hcii.whyline.bytecode;

import java.io.*;
import java.util.*;


import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */
public final class ATHROW extends Instruction {

	public ATHROW(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 191; }
	public int byteLength() { return 1; }
	public int getNumberOfOperandsConsumed() { return 1; }
	public int getNumberOfOperandsProduced() { return 0; }
	public int getNumberOfOperandsPeekedAt() { return 1; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		
	}

	public EventKind getTypeProduced() { return EventKind.OBJECT_PRODUCED; }

	// What may succeed a throw? Certainly not the next instruction, which is the default. According to the java spec, 
	//
	// 	"The objectref is then thrown by searching the current method (¤3.6) for the first exception handler that matches the class of objectref, as given by the algorithm in ¤3.10.
	//		
	//		"If an exception handler that matches objectref is found, it contains the location of the code intended to handle this exception. 
	//		The pc register is reset to that location, the operand stack of the current frame is cleared, objectref is pushed back onto the operand stack, and execution continues.
	//
	//		"If no matching exception handler is found in the current frame, that frame is popped.
	//
	// Therefore, it may be any of the matching exception handlers (we can't know which, since we don't know the type of the exception on the stack)
	// or it may be none, if none match.
	public final SortedSet<Instruction> createSuccessorsCache() {
		
		SortedSet<Instruction> successors = new TreeSet<Instruction>();
		
		for(ExceptionHandler handler : getCode().getExceptionTable())
			if(handler.handles(this)) 
				successors.add(handler.getHandlerPC());
		
		return successors;
		
	}

	public final boolean nextInstructionIsOnlySuccessor() { return false; }

	public String getReadableDescription() {
	
		return "throw";
		
	}
	
	public String getTypeDescriptorOfArgument(int argIndex) { return "Ljava/lang/Throwable;"; }

	public String getAssociatedName() { return null; }

}
