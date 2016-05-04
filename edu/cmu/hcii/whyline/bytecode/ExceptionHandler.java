package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class ExceptionHandler {

	private Instruction startPC, endPC, handlerPC;
	private final ClassInfo type;
	
	public ExceptionHandler(Instruction startPC, Instruction endPC, Instruction handlerPC, int catchType) {

		this.startPC = startPC;
		this.endPC = endPC;
		this.handlerPC = handlerPC;
		type = catchType == 0 ? null : startPC == null ? null : (ClassInfo)startPC.getMethod().getClassfile().getConstantPool().get(catchType);
		
		// JVM Spec 4.9.5 
		// The handler for an exception will never be inside the code that is being protected.
//		assert handlerPC.getInstructionIndex() < startPC.getInstructionIndex() || (endPC == null ? true : handlerPC.getInstructionIndex() >= endPC.getInstructionIndex()) : 
//			"The handler starts within the range that it protects!\n" + this + "\n" +
//			"This occurred in " + startPC.getMethod().getQualifiedNameAndDescriptor();
		
	}

	/**
	 * True if the given instruction is within the range of instructions that this exception handler protects.
	 * @param i
	 * @return
	 */
	public boolean handles(Instruction i) {
		
		return i.getIndex() >= startPC.getIndex() && i.getIndex() < endPC.getIndex();
		
	}
	
	public Instruction getStartPC() { return startPC; }
	public Instruction getEndPC() { return endPC; }
	public Instruction getHandlerPC() { return handlerPC; }
	public int getCatchTypeIndex() { return type == null ? 0 : type.getIndexInConstantPool(); }
	public ClassInfo getCatchType() { return type; }

	public boolean handles(ATHROW athrow) {

		return startPC.getByteIndex() <= athrow.getByteIndex() && (endPC == null ? true : endPC.getByteIndex() > athrow.getByteIndex());
		
	}

	public void updateHandlerPC(Instruction newHandler) {
		
		handlerPC = newHandler;
		
	}	

	public void updateStartPC(Instruction newStart) {
		
		startPC = newStart;
		
	}	

	public void updateEndPC(Instruction newEnd) {
		
		endPC = newEnd;
		
	}	

	public String toString() {
		
		return (type == null ? "finally:" : "catch(" + type + "):") + "\nstarts " + startPC + "\nends   " + endPC + "\nis at  " + handlerPC;
		
	}
	
}