package edu.cmu.hcii.whyline.tracing;

import java.awt.event.MouseEvent;
import java.util.ArrayList;

import edu.cmu.hcii.whyline.analysis.AnalysisException;
import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.io.*;
import edu.cmu.hcii.whyline.trace.EventKind;
import gnu.trove.TIntStack;

/**
 * Stores all of the methodref infos added to the constant pool for a method.
 * 
 * @author Andrew J. Ko
 *
 */
public class MethodInstrumenter {

	/**
	 * 1 bits to store the type of event that has occurred.
	 */
	public static final int IS_OUTPUT_SHIFT = 7;	
	
	/**
	 * 8 bits to store the type of event that has occurred.
	 */
	public static final int EVENT_TYPE_BIT_SIZE = 8;
	
	/**
	 * 14 bits to store the unique class identifier number.
	 */
	public static final int CLASS_ID_BIT_SIZE = 14;
		
	/**
	 * 18 bits to store the unique instruction number in the class, which is determined by counting the instructions in the classfile in the order they appear. 
	 */
	public static final int INSTRUCTION_ID_BIT_SIZE = 18;

	/**
	 * We only have 18 bits to store an instruction, so if we go over this, we're in trouble.
	 */
	public static final int MAXIMUM_INSTRUCTIONS = (int)Math.pow(2, INSTRUCTION_ID_BIT_SIZE);
	public static final int MAXIMUM_CLASS_IDS = (int)Math.pow(2, CLASS_ID_BIT_SIZE);
	
	private final ClassInstrumenter.ClassInstrumentationInfo instrumentationData;
	private final long classID;
	private final MethodInfo method;
	private final boolean isVirtual;
	private final Classfile classfile;
	private final QualifiedClassName classname;
	private final CodeAttribute code;
	private final ConstantPool pool;
	private final TIntStack newIDsToInstrument = new TIntStack(2);
	
	private int nextInstructionID;
	private int numberOfInstructionsInstrumented = 0;

	private ArrayList<Instruction> instructions;

	public MethodInstrumenter(ClassInstrumenter.ClassInstrumentationInfo instrumentationData, MethodInfo method, int nextInstructionID) throws AnalysisException { 

		this.method = method;
		this.classfile = method.getClassfile();
		this.classname = this.classfile.getInternalName();
		
		this.instrumentationData = instrumentationData;
		this.classID = Agent.classIDs.getIDOfClassname(classname);
		
		if(this.classID == 0) throw new RuntimeException("The classID of " + classname + " cannot be zero. Why is it zero?");
		
		this.code = method.getCode();
		this.pool = classfile.getConstantPool();
		this.nextInstructionID = nextInstructionID;
		this.isVirtual = method.isVirtual();
		
		instructions = new ArrayList<Instruction>(code.getNumberOfInstructions() * 2);
		
	}

	public int getNumberOfInstructionsInstrumented() { return numberOfInstructionsInstrumented; }

	private Instruction addLoadAndTraceInstructions(EventKind event, boolean isOutput) throws JavaSpecificationViolation {
		
		return addLoadAndTraceInstructions(event, isOutput, nextInstructionID);
		
	}
	
	// This is the format of the 64-bit iid passed in below
	//
	// 	[00000000 00000000 00000000 bbbbbbba cccccccc ccccccdd dddddddd dddddddd ]
	// 
	// a (1 bit) - whether this instruction represents primitive output
	// b (7 bits) - the type of execution event
	// c (14 bits) - the class ID
	// d (18 bits) - the instruction ID within the class represented by the classID
	private Instruction addLoadAndTraceInstructions(EventKind event, boolean isIO, int instructionID) throws JavaSpecificationViolation {
		
		// 32 bits for the id and 32 bits for the kind and isOutput flag.
		long ciid = (classID << INSTRUCTION_ID_BIT_SIZE) | instructionID;
		long kind = (event.id << 1) | (isIO ? 1 : 0);
		long kind_ciid = (kind << 32) | ciid;
		
		Instruction firstInserted = new LDC2_W(code, pool.addLongInfo(kind_ciid));
		instructions.add(firstInserted);
		instructions.add(new INVOKESTATIC(code, instrumentationData.getMethodFor(event)));
		numberOfInstructionsInstrumented++;
		return firstInserted;

	}
	
	/**
	 * @throws AnalysisException 
	 * @return Returns the next instruction ID for this class, which should differ by the number of instructions in the method being instrumented since instrument() was called.
	 * 
	 * The goal of this method is to take the array of bytes that represent this methods behavior
	 * and identify all of the instructions necessary for slicing and capturing primitive data types.
	 * We look forward through the instructions, but some instructions require looking back (for example,
	 * to find the instructions that compute the arguments of a method.
	 * @throws AnalysisException 
	 * @throws JavaSpecificationViolation 
	 */	
	public int instrument() throws AnalysisException, JavaSpecificationViolation {
		
		// Record information about main's invocation.
		if(method.isMain()) {

			instructions.add(new LDC_W(code, pool.addStringInfo(classname.getText())));
			instructions.add(new ALOAD_0(code));
			instructions.add(new INVOKESTATIC(code, pool.addMethodrefInfo(instrumentationData.tracerClassInfo, "recordMain", "(Ljava/lang/String;[Ljava/lang/String;)V")));
			
		}
				
		// Before we instrument the method, we insert code to write down all of values of the method arguments.
		insertMethodArgumentRecording();

		// After we instrument the method arguments, instrument each of the instructions in the instruction sequence.
		instrumentInstructions();
		
    	// Set the code attribute's new instructions.
    	Instruction[] newInstructionArray = new Instruction[instructions.size()];
    	instructions.toArray(newInstructionArray);

    	code.setInstructions(newInstructionArray);
						
		// We MUST return this, so we catch the exceptions when setting the instructions.
    	return nextInstructionID;
    	
	}

	/**
	 * Inserts an event to record the value of each argument passed to this method. The instruction each event points to is the first instruction of the method.
	 **/
	private void insertMethodArgumentRecording() throws JavaSpecificationViolation {
		
		// Although there's no method to mark the call to a static initializer or main, we'll use the first instruction in the method
		// (represented by the globally unique instruction ID, which is why we don't increment it here).
		// This should always be the first event of a method, even if the event is an artificial one, like the ones we insert below.
		addLoadAndTraceInstructions(EventKind.START_METHOD, false);

		// Special support for instrumenting mouse events in java.awt.LightweightDispatcher.retargetMouseEvent(). The mouse event 
		// passed to this comes directly from the native window. We'll collect data about it here.
		if(classname == QualifiedClassName.get("java/awt/LightweightDispatcher") && method.getInternalName().equals("retargetMouseEvent")) {
			
			// This evaluates this expression at the beginning of this method.
			//
			//		Tracer.recordMouseEvent(event.getComponent(), event.getID(), event.getX(), event.getY(), event.getButton(), iid);
			//
			ClassInfo mouseEventClassInfo = pool.addClassInfo(MouseEvent.class);
			instructions.add(new ALOAD_3(code));
			instructions.add(new INVOKEVIRTUAL(code, pool.addMethodrefInfo(mouseEventClassInfo, "getComponent", "()Ljava/awt/Component;")));
			instructions.add(new ALOAD_3(code));
			instructions.add(new INVOKEVIRTUAL(code, pool.addMethodrefInfo(mouseEventClassInfo, "getID", "()I")));
			instructions.add(new ALOAD_3(code));
			instructions.add(new INVOKEVIRTUAL(code, pool.addMethodrefInfo(mouseEventClassInfo, "getX", "()I")));
			instructions.add(new ALOAD_3(code));
			instructions.add(new INVOKEVIRTUAL(code, pool.addMethodrefInfo(mouseEventClassInfo, "getY", "()I")));
			instructions.add(new ALOAD_3(code));
			instructions.add(new INVOKEVIRTUAL(code, pool.addMethodrefInfo(mouseEventClassInfo, "getButton", "()I")));

			addLoadAndTraceInstructions(EventKind.MOUSE_EVENT, true);
			
		}
		// Special support for instrumenting key events in 
		//	    public KeyEvent(Component source, int id, long when, int modifiers, int keyCode, char keyChar, int keyLocation);
		else if(classname == QualifiedClassName.get("java/awt/event/KeyEvent") && method.isInstanceInitializer() && method.getDescriptor().equals("(Ljava/awt/Component;IJIICI)V")) {
			
			// Push all of the arguments onto the stack so we can record them. We skip "when".
			instructions.add(new ALOAD_1(code));
			instructions.add(new ILOAD_2(code));
			instructions.add(new ILOAD(code, 5));
			instructions.add(new ILOAD(code, 6));
			instructions.add(new ILOAD(code, 7));
			instructions.add(new ILOAD(code, 8));
			
			addLoadAndTraceInstructions(EventKind.KEY_EVENT, true);
			
		}
		else if(classname == QualifiedClassName.get("java/awt/Window") && method.getDescriptor().equals("()V")) {
			
			String name = method.getInternalName();
			if(name.equals("show") || name.equals("hide")) {

				// Load "this" and call the tracer method.
				instructions.add(new ALOAD_0(code));
				addLoadAndTraceInstructions(EventKind.WINDOW, true);
				
			}
			
		}

		if(!method.isStatic() && !method.isInstanceInitializer()) {
			
			instructions.add(new ALOAD(code, 0));
			addLoadAndTraceInstructions(EventKind.OBJECT_ARG, false);
			
		}
		
		// We'd like to insert several set local events here to get the values for the method. Since there's no instruction
		// that represents the set, we'll just choose the first instruction of the method again.
		int localID = method.isStatic() ? 0 : 1;
		for(String type : method.getParsedDescriptor()) {

			EventKind typeProduced = null;
			
			// Load the local's value using the appropriately typed method
			if(type.equals(MethodDescriptor.INT)) {
				typeProduced = EventKind.INTEGER_ARG;
				instructions.add(new ILOAD(code, localID));
			}
			else if(type.startsWith("[")) {
				typeProduced = EventKind.OBJECT_ARG;
				instructions.add(new ALOAD(code, localID));
			}
			else if(type.startsWith("L")) {
				typeProduced = EventKind.OBJECT_ARG;
				instructions.add(new ALOAD(code, localID));
			}
			else if(type.equals(MethodDescriptor.CHAR)) {
				typeProduced = EventKind.CHARACTER_ARG;
				instructions.add(new ILOAD(code, localID));				
			}
			else if(type.equals(MethodDescriptor.FLOAT)) {
				typeProduced = EventKind.FLOAT_ARG;
				instructions.add(new FLOAD(code, localID));
			}
			else if(type.equals(MethodDescriptor.LONG)) {
				typeProduced = EventKind.LONG_ARG;
				instructions.add(new LLOAD(code, localID));
			}
			else if(type.equals(MethodDescriptor.DOUBLE)) {
				typeProduced = EventKind.DOUBLE_ARG;
				instructions.add(new DLOAD(code, localID));
			}
			else if(type.equals(MethodDescriptor.SHORT)) {
				typeProduced = EventKind.SHORT_ARG;
				instructions.add(new ILOAD(code, localID));
			}
			else if(type.equals(MethodDescriptor.BOOLEAN)) {
				typeProduced = EventKind.BOOLEAN_ARG;
				instructions.add(new ILOAD(code, localID));
			}
			else if(type.equals(MethodDescriptor.BYTE)) {
				typeProduced = EventKind.BYTE_ARG;
				instructions.add(new ILOAD(code, localID));
			}

			// Push the id, kind, and isOutput flag and call the static instrumentation method to record it.
			if(typeProduced != null) {

				addLoadAndTraceInstructions(typeProduced, false);
				if(typeProduced.isDoubleOrLong()) localID += 2;
				else localID++;

			}
			
		}
		
	}

	private void instrumentInstructions() throws JavaSpecificationViolation, AnalysisException {
		
		// Now go through each instruction and decide whether to record anything about it.
		for(Instruction instruction : code.getInstructions()) {
			
			int sizeBefore = instructions.size();
			insertBeforeInstruction(instruction);
			int sizeAfter = instructions.size();
			Instruction firstInstructionInsertedBefore = sizeBefore == sizeAfter ? null : instructions.get(sizeBefore);

			Instruction firstReplacementInstruction = insertOrReplaceInstruction(instruction);

			sizeBefore = instructions.size();
			insertAfterInstruction(instruction);
			sizeAfter = instructions.size();
			Instruction firstInstructionInsertedAfter = sizeBefore == sizeAfter ? null : instructions.get(sizeBefore);
			
			// Here we determine if we need to update branch instructions to point to newly inserted instructions.
			// This finds the first instruction inserted
			Instruction newTarget = firstReplacementInstruction;
			if(firstInstructionInsertedBefore != null) newTarget = firstInstructionInsertedBefore;
			
			// All things that branch to the current instruction need to branch to the first of the inserted instructions, if there is one.
			if(newTarget != null)
				for(Branch b : instruction.getIncomingBranches())
					b.replaceTarget(instruction, newTarget);

			// Update the exception table
			if(newTarget != null) {
				for(ExceptionHandler exInfo : code.getExceptionTable()) {
					
					// If we updated the start of an exception handler, we need point all entries of the table to the new first instruction.
					if(instruction == exInfo.getHandlerPC())
						exInfo.updateHandlerPC(newTarget);
					
					// If we updated the start of a protected region, move the region to include the first inserted instruction.
					if(instruction == exInfo.getStartPC())
						exInfo.updateStartPC(newTarget);
					
					// If we updated the end of a protected region, move the region to the first inserted instruction. 
					if(instruction == exInfo.getEndPC())
						exInfo.updateEndPC(newTarget);
					
				}
			}
			
			// Increment the ID counter so we have a unique ID for the next instruction.
			nextInstructionID++;
			
		}

		if(numberOfInstructionsInstrumented >= MAXIMUM_INSTRUCTIONS) {

			throw new RuntimeException("Reached the maximum number of instructions: " + MAXIMUM_INSTRUCTIONS + ". Whyline doesn't yet support programs with more code than that.");

		}

	}
	
	/**
	 * We insert several kinds of events before instructions to track their execution.
	 * 
	 * @param instruction
	 * @throws JavaSpecificationViolation If we exceed 2^16 constant pool entries 
	 */
	private void insertBeforeInstruction(Instruction instruction) throws JavaSpecificationViolation {
				
		for(ExceptionHandler exInfo : code.getExceptionTable()) {
			// Is this the first instruction of an exception handler? Add an exception caught event, and update the first instruction of the handler.
			if(instruction == exInfo.getHandlerPC()) {
				addLoadAndTraceInstructions(EventKind.EXCEPTION_CAUGHT, instruction.isIO());
				break;
			}
		}

		if(instruction instanceof Invoke) {
			
			// INVOKEVIRTUAL
			if(instruction instanceof INVOKEVIRTUAL)
				addLoadAndTraceInstructions(EventKind.INVOKE_VIRTUAL, instruction.isIO());

			// INVOKEINTERFACE
			else if(instruction instanceof INVOKEINTERFACE)
				addLoadAndTraceInstructions(EventKind.INVOKE_INTERFACE, instruction.isIO());

			// INVOKESTATIC
			else if(instruction instanceof INVOKESTATIC)
				addLoadAndTraceInstructions(EventKind.INVOKE_STATIC, instruction.isIO());

			// INVOKESPECIAL
			else if(instruction instanceof INVOKESPECIAL)
				addLoadAndTraceInstructions(EventKind.INVOKE_SPECIAL, instruction.isIO());
			
		}
		else if(instruction instanceof ConditionalBranch || instruction instanceof TableBranch) {
			
			EventKind kind = 
				instruction instanceof CompareIntegersBranch ? EventKind.COMPINTS :
					instruction instanceof CompareIntegerToZeroBranch ? EventKind.COMPZERO :
						instruction instanceof CompareToNullBranch ? EventKind.COMPNULL :
							instruction instanceof CompareReferencesBranch ? EventKind.COMPREFS :
								instruction instanceof TableBranch ? EventKind.TABLEBRANCH :
								null;
			
			assert kind != null : "Don't know how to instrument a " + instruction.getClass();

			addLoadAndTraceInstructions(kind, instruction.isIO());
			
		}
		else if(instruction instanceof AbstractReturn)
			addLoadAndTraceInstructions(EventKind.RETURN, instruction.isIO());
		
		else if(instruction instanceof ATHROW)
			addLoadAndTraceInstructions(EventKind.EXCEPTION_THROWN, instruction.isIO());
		
		else if(instruction instanceof MONITORENTER || instruction instanceof MONITOREXIT)
			addLoadAndTraceInstructions(EventKind.MONITOR, instruction.isIO());
		
	}
	
	/**
	 * Here we either insert the original instruction into the new instruction sequence, or we replace it with a call to an instrumenting method.
	 * 
	 * @param instruction
	 * @param idInfo
	 * @return The first instrumentation instruction inserted, if any.
	 * @throws JavaSpecificationViolation If we exceed 2^16 constant pool entries.
	 * 
	 */
	private Instruction insertOrReplaceInstruction(Instruction instruction) throws JavaSpecificationViolation {
		
		if(GetGraphicsParser.handles(instruction))
			return addLoadAndTraceInstructions(EventKind.GETGRAPHICS, true);
		
		else if(CreateGraphicsParser.handles(instruction))	
			return addLoadAndTraceInstructions(EventKind.CREATEGRAPHICS, true);
		
		else {
			instructions.add(instruction);
			return null;
		}
		
	}
	
	/**
	 * 
	 * 
	 * @param instruction
	 * @throws JavaSpecificationViolation
	 * @throws AnalysisException
	 */
	private void insertAfterInstruction(Instruction instruction) throws JavaSpecificationViolation, AnalysisException {

		// No definitions produce values.
		if(instruction instanceof Definition) {
			
			// Because IINCs have no intermediate value producer, we need to explicitly record them.
			// We push the value of the local onto the stack and record it.
			if(instruction instanceof IINC) {

				instructions.add(new ILOAD(code, ((IINC)instruction).getLocalID()));
				addLoadAndTraceInstructions(EventKind.IINC, instruction.isIO());
				
			}
			// For all other types of definitions, we simply record that the event occurred.
			else {
				
				EventKind kind = 
					instruction instanceof SetLocal ? EventKind.SETLOCAL :
						instruction instanceof PUTFIELD ? EventKind.PUTFIELD :
							instruction instanceof PUTSTATIC ? EventKind.PUTSTATIC :
								instruction instanceof SetArrayValue ? EventKind.SETARRAY :
									null;
				
				if(kind == null) throw new AnalysisException("Don't know how to instrument a " + instruction.getClass());

				addLoadAndTraceInstructions(kind, instruction.isIO());
				
			}
			
		}
		// This handles the special case of avoiding using an object that hasn't been initialized yet. If we encounter a call to an <init>
		// and we have a NEW waiting to be recorded, then we do it immediately after the <init>. The object that NEW created
		// was duplicated on the stack when it was encountered.
		else if(instruction instanceof INVOKESPECIAL && ((INVOKESPECIAL)instruction).getMethodInvoked().callsInstanceInitializer() && newIDsToInstrument.size() > 0) {

			int instanceProducerID = newIDsToInstrument.pop();

			boolean inInit = method.isInstanceInitializer() || method.isClassInitializer();
			instructions.add(inInit ? new ICONST_1(code) : new ICONST_0(code));

			addLoadAndTraceInstructions(EventKind.NEW_OBJECT, false, instanceProducerID);
			
		}
		// Otherwise, we may want to record the value produced by the instruction, if it produces values for something other than computation.
		else {

			// If this instruction produces a value that is consumed by one or more other instructions, we record a value produced event
			// to capture the value. There should only be one consumer, except in the case of DUP2 instructions, which produce may produce 2 values, if the values are of category 1 type. 
			// For these DUP2 instructions, we only record the value produced for the instruction on top of the stack, and we ignore the one below it. 
			// We can retrieve this by searching the history for the value produced for the DUP2 instruction itself.
			
			EventKind typeProduced = instruction.getTypeProduced();
			StackDependencies.Consumers consumers = instruction.getConsumers();
			int numberOfPotentialConsumers = consumers.getNumberOfConsumers();
			
			if(numberOfPotentialConsumers > 0 && !instruction.duplicatesMultipleOperands() && !instruction.insertsDuplicatedOperandBelow()) {
		
				if(typeProduced == null) 
					throw new AnalysisException("" + classname + "'s " + instruction + " must produce some type.\n" + code.toString());
				
				Instruction consumer = consumers.getFirstConsumer();
					
				boolean producesExclusively = numberOfPotentialConsumers == 1 && consumer.getProducersOfArgument(consumer.getArgumentNumberOfProducer(instruction)).getNumberOfProducers() == 1;
				boolean isIO = instruction.isIO();
				boolean referencesUninitializedObject = instruction.referencesUninitializedObject();
				
				// If this is a NEW instruction, we can't reference an object that hasn't been initialized by an <init> call yet, so if this is one of those objects, we need
				// defer capturing it until after the init call we do this above when we're instrumenting after the invoke special.
				if(instruction instanceof NEW) {
		
					newIDsToInstrument.push(nextInstructionID);
					// Note, we can't just will nilly duplicate this value. Downstream instructions may depend on the placement
					// of this value on the operand stack. This is a hack to handle certain cases, but its by no means comprehensive.
					instructions.add(instruction.getNext() instanceof DUP_X1 ? new DUP_X1(code) : new DUP(code));
					
				}
				else if(instruction instanceof ArrayAllocation) {
					
					instructions.add(new DUP(code));
					addLoadAndTraceInstructions(EventKind.NEW_ARRAY, false);
					
				}
				// If this instruction references an uninitialized object, we'd love to record it, but we're not allowed. There are two cases:
				// 	(1) it's the first argument passed to a call to <init>. This is defined in DUP.class, since it's the one that typically does this.
				// 	(2) it's a reference to local 0 in an <init> that comes BEFORE the call to super.<init>. This is defined in GetLocal.class.
				else if(referencesUninitializedObject) {
	
					// We have to skip this value produced, for the reason above.
					
				}
				// OPTIMIZATION: If this instruction produces a reference to this method's "this" exclusively for another instruction, don't record it.
				// We can it it elsewhere.
				else if(isVirtual && instruction instanceof ALOAD_0 && producesExclusively) {

					// Don't need to record these. They can be gotten from the set method argument event.
					
				}
				// OPTIMIZATION: If this instruction gets a local exclusively for another instruction
				else if(!isIO && instruction instanceof GetLocal && producesExclusively && !referencesUninitializedObject) {

					// Don't need to record these

				}
				// We can't record the value pushed by this because we don't execute the next instruction after executing these.
				else if(instruction instanceof JSR || instruction instanceof JSR_W) {
					
				}
				// If this instruction produces a constant value (and we weren't able to skip it above), we simply record that it was produced, and skip the value.
				else if(typeProduced.isConstantProduced) {
	
					Instruction upstreamProducerIfDuplication = instruction;
					while(upstreamProducerIfDuplication instanceof Duplication)
						upstreamProducerIfDuplication = upstreamProducerIfDuplication.getProducersOfArgument(0).getFirstProducer();

					// We only record the constant if the consumer might get its value from some other instruction as well.
					int argNumber = consumer.getArgumentNumberOfProducer(instruction);
					if(upstreamProducerIfDuplication instanceof GetLocal || isIO || (argNumber >= 0 && consumer.getProducersOfArgument(argNumber).getNumberOfProducers() > 1))
						addLoadAndTraceInstructions(typeProduced, isIO);
		
				}
				// In all other situations, we record the value produced by duplicating it on the stack with the appropriate duplication instruction for its type.
				else {
					
					// Make sure to add a DUP2 if the type is a double or long value.
					instructions.add(typeProduced.isDoubleOrLong() ? new DUP2(code) : new DUP(code));
					
					if(typeProduced == EventKind.OBJECT_PRODUCED || typeProduced == EventKind.NEW_OBJECT) {
						boolean inInit = method.isInstanceInitializer() || method.isClassInitializer();
						instructions.add(inInit ? new ICONST_1(code) : new ICONST_0(code));
					}

					addLoadAndTraceInstructions(typeProduced, isIO);

				} // Done checking for the type of value produced event to record
				
				// Does this produce the image for a drawImage() call? If so, record an image size event.
				if(consumer instanceof INVOKEVIRTUAL && 
						((INVOKEVIRTUAL)consumer).getMethodInvoked().getMethodName().startsWith("drawImage") &&
						consumer.getArgumentNumberOfProducer(instruction) == 1) {
					instructions.add(new DUP(code));
					addLoadAndTraceInstructions(EventKind.IMAGE_SIZE, true);
				}
				
			} // Done checking if its a value we want to record
			
		} // Done instrumenting value recording.

	}
	
}
