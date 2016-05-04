package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

import edu.cmu.hcii.whyline.source.*;
import edu.cmu.hcii.whyline.trace.EventKind;
import edu.cmu.hcii.whyline.trace.OperandStackType;

import edu.cmu.hcii.whyline.util.Named;
import edu.cmu.hcii.whyline.util.Util;

/**
 * @author Andrew J. Ko
 *
 */ 
public abstract class Instruction implements Comparable<Instruction>,Named {

	// Flag constants
	private static final int IS_IO = 0;
	private static final int REFERENCES_UNINITIALIZED_OBJECT = 1;
	private static final int REFERENCES_UNINITIALIZED_OBJECT_CHECKED = 2;
	private static final int REFERENCES_INSTANCE_BEFORE_SUPER = 3;
	private static final int REFERENCES_INSTANCE_BEFORE_SUPER_CHECKED = 4;
	private static final int PRODUCES_INSTANCE_FOR_INITIALIZER = 5;
	private static final int PRODUCES_INSTANCE_FOR_INITIALIZER_CHECKED = 6;

	protected final CodeAttribute code;

	// Cache of the instruction index to avoid doing an index of.
	private int instructionIndex;
	private int flags = 0; // All flags are off.
		
	public Instruction(CodeAttribute code) {

		this.code = code;

	}

	// 0 == not set, 1 == false, 2 == true
	private boolean getFlag(int flag) { return (flags & (1 << flag)) != 0; }
	private void setFlag(int flag) { flags = flags | (1 << flag); }

	// Returns true if this instruction represents some form of input or output. Marked by Trace.
	public boolean isIO() { return (flags & (1 << IS_IO)) != 0; }
	public void setIsIO() { 
	
		setFlag(IS_IO); 
		assert isIO();
		
	}
	
	public boolean insertsDuplicatedOperandBelow() { return false; }
	public boolean duplicatesMultipleOperands() { return false; }
	
	public CodeAttribute getCode() { return code; }
	public MethodInfo getMethod() { return code.getMethod(); }
	public Classfile getClassfile() { return code.getClassfile(); }
	
	/**
	 * 
	 * @return If available, returns the source file, otherwise, returns the classfile.
	 */
	public FileInterface getFile() {
		
		Classfile classfile = code.getMethod().getClassfile();
		FileInterface source = classfile;
		if(classfile.getSourceFile() != null) source = classfile.getSourceFile();
		return source;

	}
	
	public LineNumber getLineNumber() { return code.getLineNumberFor(this); }
	
	public Line getLine() { 
		
		LineNumber line = getLineNumber();
		if(line == null) return null;

		FileInterface file = getFile();
		
		if(file instanceof JavaSourceFile)
			try {
				return file.getLine(line.getNumber());
			} catch (ParseException e) {
				e.printStackTrace();
				return null;
			}
		else if(file instanceof Classfile)
			return getClassfile().getLine(this);
		else
			return null;
		
	}

	public void setInstructionIndex(int index) {
		
		instructionIndex = index;
		
	}
		
	public int getIndex() { return instructionIndex; }
	
	public int getByteIndex() { return code.getByteIndex(instructionIndex); }

	public Instruction getNext() { 
		
		Instruction[] instructions = code.getInstructions();
		return (instructionIndex < instructions.length - 1) ? instructions[instructionIndex + 1] : null;
		
	}
	public Instruction getPrevious() { 
		
		return instructionIndex > 0 ? code.getInstructions()[instructionIndex - 1] : null;
		
	}

	public boolean isJumpedTo() { return code.getIncomingBranchesForInstruction(instructionIndex) != null; }
	
	// By default, the instruction after is the only reachable instruction.
	public boolean couldJumpTo(Instruction inst) {
		
		return inst == getNext();
		
	}

	// Originally, these were cached, but it turns out that we very rarely get these more than a few times (with the Whyline anyway).
	public Set<Instruction> getOrderedSuccessors() {

		return createSuccessorsCache();
		
	}
	
	// Used for optimizations. There's no need to iterate through a list or event create a list if there's only one successor and its the next instruction.
	public boolean nextInstructionIsOnlySuccessor() { return true; }
	
	protected SortedSet<Instruction> createSuccessorsCache() {

		SortedSet<Instruction> successors = new TreeSet<Instruction>();
		Instruction instructionAfter = getNext();
		if(instructionAfter != null) successors.add(instructionAfter);
		return successors;
		
	}
	
	public final SortedSet<Instruction> getOrderedPredecessors() {
		
		SortedSet<Instruction> predecessors = new TreeSet<Instruction>();
		SortedSet<Branch> branches = code.getIncomingBranchesForInstruction(instructionIndex);
		if(branches != null) predecessors.addAll(branches);
		
		Instruction instructionBefore = getPrevious();
		if(instructionBefore != null && instructionBefore.couldJumpTo(this)) 
			predecessors.add(instructionBefore);

		return predecessors;
		
	}

	/**
	 * These will often be found in the bytecode for a for() loop. The unconditional branch jumps to the test before executing the block.
	 * We pass the start instruction to avoid looping forever.
	 */
	public UnconditionalBranch getUnconditionalBranchPrecessessor() {
		
		Set<Instruction> toVisit = new HashSet<Instruction>();
		Set<Instruction> newToVisit = new HashSet<Instruction>();
		Set<Instruction> visited = new HashSet<Instruction>();
		toVisit.add(this);
		while(toVisit.size() > 0) {

			for(Instruction inst : toVisit) {
				visited.add(inst);
				for(Instruction pred : inst.getOrderedPredecessors()) {
					if(pred != this) {
						if(pred instanceof UnconditionalBranch) 
							return (UnconditionalBranch)pred;
						else if(!visited.contains(pred)) 
							newToVisit.add(pred);
					}
				}
			}
			
			toVisit.clear();
			Set<Instruction> temp = toVisit;
			toVisit = newToVisit;
			newToVisit = temp;
			
		}
		return null;
		
	}
	
	// We could *almost* return this as a set of Branch instructions, but ATHROW
	// instructions can also jump to instructions, but aren't declared as Branches.
	// Note that this does NOT include predecessors, such as instructions before. It only includes
	// instructions that must be executed for this instruction to be reached.
	public Set<Instruction> getBranchDependencies() {

		return code.getControlDependenciesFor(this);
		
	}

	public Set<Branch> getIncomingBranches() {
		
		SortedSet<Branch> branches = code.getIncomingBranchesForInstruction(instructionIndex);
		return branches == null ? Collections.<Branch>emptySet() : branches;
		
	}

	public boolean isLoop() { return false; }
		
	public boolean isLoopHeader() {
		
		for(Instruction predecessor : getOrderedPredecessors())
			if(predecessor.isLoop()) 
				return true;

		return false;
		
	}
	
	public final boolean canReachInMethod(Instruction instruction) {
		
		if(getCode() != instruction.getCode()) return false;
	
		Set<Instruction> visited = new HashSet<Instruction>();
		return canReachInMethodHelper(this, instruction, visited);
		
	}
	
	private final boolean canReachInMethodHelper(Instruction current, Instruction target, Set<Instruction> visited) {

		if(visited.contains(current)) return false;
		visited.add(current);
		
		for(Instruction successor : current.getOrderedSuccessors())
			if(successor == target || canReachInMethodHelper(successor, target, visited)) 
				return true;

		return false;
		
	}
	
	public boolean isInTryCatchBlock() {
	
		return code.isInstructionInTryCatchBlock(this);
	
	}

	public Set<ExceptionHandler> getExceptionHandlersProtecting() {
		
		return code.getExceptionHandlersProtecting(this);
	
	}
	
	public boolean isExceptionHandlerStart() {
		
		for(ExceptionHandler handler : code.getExceptionTable())
			if(handler.getHandlerPC() == this)
				return true;
		return false;

	}

	@Deprecated
	public abstract String getTypeDescriptorOfArgument(int argument);
	
	public StackDependencies.Producers getProducersOfArgument(int argument) {

		return code.getProducersOfArgument(this, argument);
		
	}
	
	public int getArgumentNumberOfProducer(Instruction inst) {
		
		for(int arg = 0; arg < getNumberOfArgumentProducers(); arg++) {

			StackDependencies.Producers producers = getProducersOfArgument(arg);
			for(int i = 0; i < producers.getNumberOfProducers(); i++)
				if(inst == producers.getProducer(i)) 
					return arg;
			
		}
		
		return -1;
		
	}
	
	public int getNumberOfArgumentProducers() { 

		return Math.max(getNumberOfOperandsConsumed(), getNumberOfOperandsPeekedAt());
		
	}
		
	public StackDependencies.Consumers getConsumers() { 
		
		return getCode().getConsumersOf(this);
		
	}
	
	public Instruction getFinalConsumer() {

		Instruction consumer = this;
		while(consumer != null) {
			
			StackDependencies.Consumers consumers = consumer.getConsumers(); 
			if(consumers.getNumberOfConsumers() > 0)
				consumer = consumers.getFirstConsumer();
			else return consumer;
		
		}

		return consumer;
		
	}
	
	/**
	 * Instruction-specific details. We could have put all of this data in tables, but since these are executed millions of times per second,
	 * it's important to avoid all of the array accesses.
	 * @return Should return the Java bytecode opcode that the instruction class represents.
	 */
	public abstract int getOpcode();
	public abstract int byteLength();

	// Note that technically, all of the double and long valued instructions push two operands for every one,
	// but here we treat them as single operands, since that's how the instructions actually operate on them.
	// That they take up two spots on the operand stack is inconsequential to us.
	public abstract int getNumberOfOperandsConsumed();
	public abstract int getNumberOfOperandsProduced();
	public abstract int getNumberOfOperandsPeekedAt();

	public boolean hasVariableExecution() { return Opcodes.EXECUTION_IS_VARIABLE[getOpcode()]; }

	/**
	 * @returns True if this instruction is in an instance initialization method, references the first local (the potentially uninitialized instance), and comes before this init's call to super.init. Also returns true if this produces a reference to the unitialized object for a call to an init.
	 */
	public final boolean referencesUninitializedObject() { 

		if(!getFlag(REFERENCES_UNINITIALIZED_OBJECT_CHECKED)) {
			
			setFlag(REFERENCES_UNINITIALIZED_OBJECT_CHECKED);
		
			boolean isTrue = false;
			if(getNumberOfOperandsProduced() == 0) isTrue = false;
			else if(producesInstanceForInstanceInitializer()) isTrue = true;
			else if(referencesInstanceInInitializerBeforeSuperInitializer()) isTrue = true;
			else isTrue = false;
			if(isTrue) setFlag(REFERENCES_UNINITIALIZED_OBJECT);
			
		}
		return getFlag(REFERENCES_UNINITIALIZED_OBJECT);
		
	}
	
	public final boolean producesInstanceForInstanceInitializer() {
		
		if(!getFlag(PRODUCES_INSTANCE_FOR_INITIALIZER_CHECKED)) {
		
			setFlag(PRODUCES_INSTANCE_FOR_INITIALIZER_CHECKED);
			Instruction consumer = getConsumers().getFirstConsumer();
			if( consumer instanceof INVOKESPECIAL && ((INVOKESPECIAL)consumer).getMethodInvoked().callsInstanceInitializer() && consumer.getProducersOfArgument(0).getFirstProducer() == this)
				setFlag(PRODUCES_INSTANCE_FOR_INITIALIZER);
			
		}
		return getFlag(PRODUCES_INSTANCE_FOR_INITIALIZER);
		
	}

	public final boolean referencesInstanceInInitializerBeforeSuperInitializer() { 

		if(!getFlag(REFERENCES_INSTANCE_BEFORE_SUPER_CHECKED)) {
		
			setFlag(REFERENCES_INSTANCE_BEFORE_SUPER_CHECKED);
			if(this instanceof GetLocal && getMethod().isInstanceInitializer() && ((GetLocal)this).getLocalID() == 0 && getCode().getCallToInitializer().getIndex() >= getIndex())
				setFlag(REFERENCES_INSTANCE_BEFORE_SUPER);
			
		}
		return getFlag(REFERENCES_INSTANCE_BEFORE_SUPER);
		
	}
	
	/**
	 * Overridden by concrete subclasses to return the class of the type of data the instruction produces,
	 * whether a primitive type like Integer.class or a String.class or Object.class.
	 * 
	 * @return Should return an event that matches the type of value produced. Only certain values of the KindOfEvent enumeration are valid.
	 *  
	 */
	public abstract EventKind getTypeProduced();

	public OperandStackType getStackTypeProduced() { return getTypeProduced().getStackType(); }
	
	public abstract void toBytes(DataOutputStream code) throws IOException;
	
	private static final int TO_STRING_CLASS_WIDTH = 20;
	private static final int TO_STRING_METHOD_WIDTH = 24;
	private static final int TO_STRING_INDEX_WIDTH = 6;
	private static final int TO_STRING_WHITESPACE = 8;
	private static final int TO_STRING_NUMBER_OF_CHARACTERS_BEFORE_INSTRUCTION_TYPE = TO_STRING_CLASS_WIDTH + TO_STRING_METHOD_WIDTH + TO_STRING_INDEX_WIDTH;
	
	
	private boolean isInstrumentation() { return this instanceof INVOKESTATIC && ((INVOKESTATIC)this).getMethodInvoked().getClassName().getText().equals("edu/cmu/hcii/whyline/tracing/Tracer"); }
	
	/**
	 * 	@return The name of the Java class of which this is an instance
	 */
	public String toString() { 

		boolean consumedByInstrumentation = isInstrumentation();
		for(Instruction consumer : getConsumers())
			if(consumer.isInstrumentation())
				consumedByInstrumentation = true;


		String methodString = getMethodString(getMethod());
		
		String index = Util.fillOrTruncateString("" + (consumedByInstrumentation ? "* " : "  ") + getIndex(), TO_STRING_INDEX_WIDTH);
		String instructionTypeName = Util.fillOrTruncateString(getClass().getSimpleName().toLowerCase(), 12);

		String result = methodString + " " + index + " " + instructionTypeName;
		
		return result;
	
	}
	
	public static String getMethodString(MethodInfo method) {
		
		String methodName = method.getInternalName();
		
		String className = Util.fillOrTruncateString(method.getClassfile().getSimpleName(), TO_STRING_CLASS_WIDTH);
		methodName = Util.fillOrTruncateString(methodName, TO_STRING_METHOD_WIDTH);

		String context = ":::";
		
		return context + " " + className + " " + methodName;
				
	}
	
	public abstract String getAssociatedName();
	
	public String toStringStartingWithInstructionType() {

		return toString().substring(Instruction.TO_STRING_NUMBER_OF_CHARACTERS_BEFORE_INSTRUCTION_TYPE + TO_STRING_WHITESPACE);
		
	}
	
 	public abstract String getReadableDescription();
	
 	public String getDisplayName(boolean html, int limit) { return getReadableDescription(); }
 	
 	public int compareTo(Instruction i) {
		
		return instructionIndex - i.instructionIndex;
		
	}
	
 	public int hashCode() {
 		
 		return 2 * super.hashCode() + instructionIndex;
 		
 	}
}