package edu.cmu.hcii.whyline.bytecode;

import java.util.*;

import edu.cmu.hcii.whyline.analysis.AnalysisException;
import edu.cmu.hcii.whyline.trace.EventKind;

import gnu.trove.*;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class StackDependencies {

	private final CodeAttribute code;

	/**
	 * The argument producers are ordered by the order in which they appear if the stack we're 
	 * written left to write. Argument zero is the last item popped off the stack, the last argument
	 * in the list is the first popped off the stack.
	 */
	private final TIntObjectHashMap<InstructionProducers> producersByInstructionIndex;

	/**
	 * The instruction that consumes the value produced an instruction, if any.
	 */
	private final TIntObjectHashMap<Instruction> consumersByInstructionIndex;

	/**
	 *  Some instructions, namely DUP2, DUP2_X1, and DUP2_X2, have multiple consumers because they produce multiple values.
	 *  Empirical investigations suggest this is about 0.006% of instructions, so there's no need to keep two spots for each instruction.
	 */
	private TIntObjectHashMap<Instruction> additionalConsumers;
		
	private boolean analyzed = false;
	
	public StackDependencies(CodeAttribute code) throws AnalysisException {
		
		this.code = code;

		// Based on some empirical data, about 48% of instructions have producers and 68% have consumers. 
		// So there's no need to store a whole array for each, since its so sparse.

		producersByInstructionIndex = new TIntObjectHashMap<InstructionProducers>((int)(code.getNumberOfInstructions() * .48));
		consumersByInstructionIndex = new TIntObjectHashMap<Instruction>((int)(code.getNumberOfInstructions() * .68));
		
	}
	
	public Consumers getConsumersOf(Instruction inst) {

		int index = inst.getIndex();
				
		return new Consumers(consumersByInstructionIndex.get(index), additionalConsumers == null ? null : additionalConsumers.get(index));
		
	}
	
	/*
	 * May return an empty array if the instruction has no producers. For example, nothing pushes the value for an Exception in a catch block. It has no producers.
	 */
	public Producers getProducersOfArgument(Instruction inst, int argument) {
		
		InstructionProducers producers = producersByInstructionIndex.get(inst.getIndex());
		
		if(producers == null)
			return new Producers();
		else 
			return producers.getArgumentProducers(argument);
		
	}

	// Arguments are numbered from 0 to (number of arguments - 1)
	private void addArgumentProducer(Instruction inst, int argument, int producerIndex) {

		Instruction producer = code.getInstruction(producerIndex);
		
		InstructionProducers argumentProducers = producersByInstructionIndex.get(inst.getIndex());
		
		// Make the producers if necessary
		if(argumentProducers == null) {
			argumentProducers = new InstructionProducers(inst);
			producersByInstructionIndex.put(inst.getIndex(), argumentProducers);
		}

		boolean wasNew = argumentProducers.add(argument, producer);

		if(!wasNew) return;
		
		// We only set this as the consumer of this producer if this in fact consumes anything.
		// It might just be a peeker. If the producer is null, that means this this instruction is
		// consuming the exception object implicitly pushed on the stack for an exception handler.
		if(producer != null && inst.getNumberOfOperandsConsumed() > 0)
			addConsumer(producerIndex, inst);
		
	}
	
	private void addConsumer(int producerIndex, Instruction consumer) {

		// If we haven't set one yet, set it.
		if(!consumersByInstructionIndex.containsKey(producerIndex)) {
			
			consumersByInstructionIndex.put(producerIndex, consumer);
			
		}
		// If we have set one, put the extra one in the additional consumers bin,
		// making the bin if we haven't yet.
		else {

			// Only add the additional consumer if its not the same instruction.
			Instruction existingConsumer = consumersByInstructionIndex.get(producerIndex);
			if(existingConsumer != consumer) {
			
				if(additionalConsumers == null) additionalConsumers = new TIntObjectHashMap<Instruction>(1);
				
				assert !additionalConsumers.containsKey(producerIndex) : 
					"No java bytecodes have more than two consumers, because none produce more than two values, and yet we're adding a " +
					"third consumer for the value produced by instruction " + producerIndex;

				additionalConsumers.put(producerIndex, consumer);
				
			}
			
		}
		
	}


	public void analyze() throws AnalysisException { 
		
		if(analyzed) return;
		analyzeOperandProductionAndConsumption();
		analyzed = true;
		
	}
	
	private void analyzeOperandProductionAndConsumption() throws AnalysisException {
		
		// This stack contains all of the instructions that have pushed operands onto the stack that have not yet been consumed.
		// We don't have to keep track of how many of each instruction's operands were consumed, because bytecode instructions
		// only produce at most 1 operand (well, 2 if producing a double or long, but the consumer will eat the corresponding 2 if the code
		// is correct). But we'll assert this assumption anyway.
		// Note that we start this stack with a capacity of 10. Empirical data shows that 99% of methods need < 10 operands.
		TIntArrayList unconsumedinstructions = new TIntArrayList(10);
		
		BranchStack branchTargets = new BranchStack();

		TIntHashSet exceptionHandlerIndices = new TIntHashSet(3);
		
		// Initially, populate the branch stack with the exception handlers, because these aren't (necessarily) branched to
		// explicitly. In each case, the stack will have depth 1, containing the exception object, since the operand stack is
		// cleared when an exception is thrown, and then the exception is put on the stack. We'll represent the non-existent instruction that
		// pushes this value with with a -1 index, which the code attribute will resolve to null.
		unconsumedinstructions.add(-1);
		for (ExceptionHandler exceptionHandler : code.getExceptionTable()) {

			// Note that we're setting up empty stacks for these exception handlers, when in fact, we'll actually need
			// one thing to consume on the stack. Since there's no instruction we can push on there, I'll just maintain a set of
			// these instructions, and check this special case during operand consumption.
			Instruction handler = exceptionHandler.getHandlerPC();
			if (handler != null) branchTargets.push(handler, 1, unconsumedinstructions);
			else throw new AnalysisException("This entry in the exception table doesn't point to a valid handler instruction.");
			exceptionHandlerIndices.add(handler.getIndex());
			
		}
		unconsumedinstructions.remove(unconsumedinstructions.size() - 1);

		int currentStackDepth = 0;
		int maxStackDepth = 0;
		Instruction instruction = code.getFirstInstruction();

		// "null" indicates that we're done.
		while (instruction != null) {
			
			int instructionIndex = instruction.getIndex();
			int opcode = instruction.getOpcode();
			
			int operandsProduced = instruction.getNumberOfOperandsProduced();
			int operandsConsumed = instruction.getNumberOfOperandsConsumed();

			assert operandsProduced <= 2 : 
				"I thought that bytecode instructions only produced 0, 1, or 2 operands, but it's apparently not true. " + 
				instruction + " produces " + operandsProduced;

			// First let the instruction consume any arguments it wishes. Unless its the beginning of an exception handler,
			// in which case there is no explicit instruction that generates its argument.
			if(operandsConsumed > 0 && operandsConsumed != Opcodes.POPS_ALL_OPERANDS) {

				// If this is an exception handler, then there won't be anything on the stack to consume.
				if(!exceptionHandlerIndices.contains(instructionIndex)) {
				
					int operandsLeftToConsume = operandsConsumed;
					int argumentNumber = operandsLeftToConsume - 1;
					
					// Let this instruction consume its operands.
					while(operandsLeftToConsume > 0) {
						if(unconsumedinstructions.isEmpty())
							throw new AnalysisException("" + instruction + " still needs " + operandsLeftToConsume + ", but the stack of producers is empty.\n" + code.toString());
						else {
							int producerIndex = unconsumedinstructions.remove(unconsumedinstructions.size() - 1);
							operandsLeftToConsume--;
							addArgumentProducer(instruction, argumentNumber, producerIndex);
							argumentNumber--;
						}
					}

				}

			}

			// All of these instructs simply inspect the stack. They depend on stack operands for their
			// execution, but they don't consume them. We handle them as special cases.
			switch(opcode) {

			// Stack manipulation handlers.
			case Opcodes.DUP :
			case Opcodes.DUP_X1 :
			case Opcodes.DUP_X2 :
			case Opcodes.DUP2 :
			case Opcodes.DUP2_X1 :
			case Opcodes.DUP2_X2 :
			case Opcodes.SWAP :
			case Opcodes.POP :
			case Opcodes.POP2 :

				int indexOfInstructionWithUnconsumedOperand = unconsumedinstructions.getQuick(unconsumedinstructions.size() - 1);
				Instruction instructionWithUnconsumedOperand = code.getInstruction(indexOfInstructionWithUnconsumedOperand);

				// If this is a hidden exception on the stack, forget it
				if(instructionWithUnconsumedOperand != null) {

					EventKind typeProducedByTopOfStack = instructionWithUnconsumedOperand.getTypeProduced();
				
					// If the instruction with unconsumed operands is a DUP2, see if we've already consumed one. If so,
					// we use the second type produced.
					if(instructionWithUnconsumedOperand instanceof Dup2lication && instructionWithUnconsumedOperand.getConsumers().getNumberOfConsumers() > 0)
						typeProducedByTopOfStack = ((Dup2lication)instructionWithUnconsumedOperand).getSecondTypeProduced();

					switch(opcode) {
					
					case Opcodes.DUP :
						
						addArgumentProducer(instruction, 0, indexOfInstructionWithUnconsumedOperand);
						unconsumedinstructions.add(instructionIndex);
						break;
						
					case Opcodes.DUP_X1:
									
						addArgumentProducer(instruction, 0, indexOfInstructionWithUnconsumedOperand);
						unconsumedinstructions.insert(unconsumedinstructions.size() - 2, instructionIndex);
						break;
						
					case Opcodes.DUP_X2 :
						
						if(typeProducedByTopOfStack.isDoubleOrLong()) {
							addArgumentProducer(instruction, 0, indexOfInstructionWithUnconsumedOperand);
							unconsumedinstructions.insert(unconsumedinstructions.size() - 2, instructionIndex);
						}
						else {
							addArgumentProducer(instruction, 0, indexOfInstructionWithUnconsumedOperand);
							unconsumedinstructions.insert(unconsumedinstructions.size() - 3, instructionIndex);
						}
						break;

					case Opcodes.DUP2 :

						if(typeProducedByTopOfStack.isDoubleOrLong()) {
							addArgumentProducer(instruction, 0, indexOfInstructionWithUnconsumedOperand);
							unconsumedinstructions.add(instructionIndex);
						}
						else {
							addArgumentProducer(instruction, 0, unconsumedinstructions.getQuick(unconsumedinstructions.size() - 2));
							addArgumentProducer(instruction, 1, indexOfInstructionWithUnconsumedOperand);
							unconsumedinstructions.add(instructionIndex);
							unconsumedinstructions.add(instructionIndex);
						}
						break;
						
					case Opcodes.DUP2_X1 :
						
						if(typeProducedByTopOfStack.isDoubleOrLong()) {
							addArgumentProducer(instruction, 0, indexOfInstructionWithUnconsumedOperand);
							unconsumedinstructions.insert(unconsumedinstructions.size() - 2, instructionIndex);
						}
						else {
							addArgumentProducer(instruction, 0, unconsumedinstructions.getQuick(unconsumedinstructions.size() - 2));
							addArgumentProducer(instruction, 1, unconsumedinstructions.getQuick(unconsumedinstructions.size() - 1));
							unconsumedinstructions.insert(unconsumedinstructions.size() - 3, instructionIndex);
							unconsumedinstructions.insert(unconsumedinstructions.size() - 3, instructionIndex);
						}
						break;
						
					case Opcodes.DUP2_X2 :
						
						// If the top is category 2...
						if(typeProducedByTopOfStack.isDoubleOrLong()) {

							Instruction secondFromTop = code.getInstruction(unconsumedinstructions.getQuick(unconsumedinstructions.size() - 2));
							EventKind typeProducedOfSecondFromTop = 
								(secondFromTop instanceof Dup2lication && secondFromTop.getConsumers().getNumberOfConsumers() > 0) ?
									((Dup2lication)secondFromTop).getSecondTypeProduced() :
									secondFromTop.getTypeProduced();
							
							// ..., value2, value1  ..., value1, value2, value1
							// where value1 and value2 are both values of a category 2 computational type
							if(typeProducedByTopOfStack.isDoubleOrLong()) {
								addArgumentProducer(instruction, 0, indexOfInstructionWithUnconsumedOperand);
								unconsumedinstructions.insert(unconsumedinstructions.size() - 2, instructionIndex);
							}
							// .., value3, value2, value1  ..., value1, value3, value2, value1
							// where value1 is a value of a category 2 computational type and value2 and value3 are both values of a category 1 computational type
							else {
								addArgumentProducer(instruction, 0, indexOfInstructionWithUnconsumedOperand);
								unconsumedinstructions.insert(unconsumedinstructions.size() - 3, instructionIndex);
							}
														
						}
						else {

							Instruction thirdFromTop = code.getInstruction(unconsumedinstructions.getQuick(unconsumedinstructions.size() - 3));
							EventKind typeProducedOfThirdFromTop = 
								(thirdFromTop instanceof Dup2lication && thirdFromTop.getConsumers().getNumberOfConsumers() > 0) ?
									((Dup2lication)thirdFromTop).getSecondTypeProduced() :
									thirdFromTop.getTypeProduced();
							
							// ..., value3, value2, value1  ..., value2, value1, value3, value2, value1
							// where value1 and value2 are both values of a category 1 computational type and value3 is a value of a category 2 computational type
							if(typeProducedByTopOfStack.isDoubleOrLong()) {

								// This first one could be null! Why? Because the invisible operand may be the infamous exception secretly pushed onto the operand stack.
								// If this is the cause, we DON'T add a producer, since there is none!
								addArgumentProducer(instruction, 0, unconsumedinstructions.getQuick(unconsumedinstructions.size() - 2));
								addArgumentProducer(instruction, 1, unconsumedinstructions.getQuick(unconsumedinstructions.size() - 1));
								unconsumedinstructions.insert(unconsumedinstructions.size() - 3, instructionIndex);
								unconsumedinstructions.insert(unconsumedinstructions.size() - 3, instructionIndex);
								
							}
							// ..., value4, value3, value2, value1  ..., value2, value1, value4, value3, value2, value1
							// where value1, value2, value3, and value4 are all values of a category 1 computational type
							else {

								// This first one could be null! Why? Because the invisible operand may be the infamous exception secretly pushed onto the operand stack.
								// If this is the cause, we DON'T add a producer, since there is none!
								addArgumentProducer(instruction, 0, unconsumedinstructions.getQuick(unconsumedinstructions.size() - 2));
								addArgumentProducer(instruction, 1, unconsumedinstructions.getQuick(unconsumedinstructions.size() - 1));
								unconsumedinstructions.insert(unconsumedinstructions.size() - 4, instructionIndex);
								unconsumedinstructions.insert(unconsumedinstructions.size() - 4, instructionIndex);

							}

						}
						break;
						
					case Opcodes.SWAP :

						// This first one could be null! Why? Because the invisible operand may be the infamous exception secretly pushed onto the operand stack.
						// If this is the cause, we DON'T add a producer, since there is none!
						addArgumentProducer(instruction, 0, unconsumedinstructions.getQuick(unconsumedinstructions.size() - 2));
						addArgumentProducer(instruction, 1, unconsumedinstructions.getQuick(unconsumedinstructions.size() - 1));
						int top = unconsumedinstructions.remove(unconsumedinstructions.size() - 1);
						int below = unconsumedinstructions.remove(unconsumedinstructions.size() - 1);
						unconsumedinstructions.add(top);
						unconsumedinstructions.add(below);						
						break;
						
					case Opcodes.POP :
						
						unconsumedinstructions.remove(unconsumedinstructions.size() - 1);
						break;
						
					case Opcodes.POP2 :

						if(typeProducedByTopOfStack.isDoubleOrLong())
							unconsumedinstructions.remove(unconsumedinstructions.size() - 1);
						else {
							unconsumedinstructions.remove(unconsumedinstructions.size() - 1);
							unconsumedinstructions.remove(unconsumedinstructions.size() - 1);
						}
						break;

					default :
							
						assert false : "Did I miss " + instruction.getClass() + " when doing stack depth analysis?";
					
					}
					
				}
					
				break;
				
			case Opcodes.CHECKCAST :

				addArgumentProducer(instruction, 0, unconsumedinstructions.getQuick(unconsumedinstructions.size() - 1));
				break;

			default :
				
				//	After consuming, if this creates operands, we push the instruction on the stack.
				// NOTE: This includes JSR instructions, but technically, a JSR only pushes when it jumps to its
				// target. Therefore, because of the way this algorithm is written, we need to pop this off
				// for the special case below where we check for JSRs.
				if(operandsProduced > 0)
					unconsumedinstructions.add(instructionIndex);
			
			}
			
			// Update the current depth by adding the number of operands produced and
			// subtracting the number consumed. Then update the max depth if it's bigger than the current max.
			currentStackDepth += operandsProduced - operandsConsumed;
			if (currentStackDepth > maxStackDepth)
				maxStackDepth = currentStackDepth;

			
			// Where do we go next? Depends on if its a branch.
			if (instruction instanceof Branch) {

				// Remember this branch. We may nullify it.
				Branch branch = (Branch)instruction;
				boolean jsr = false;

				switch(opcode) {
				
				case Opcodes.LOOKUPSWITCH :
				case Opcodes.TABLESWITCH :
					
					// Explore all of the select's targets. The default target is handled after this branch cases.
					// By explore, we mean push the target and the current stack depth onto the branch targets stack.
					TableBranch select = (TableBranch)instruction;
					for (Instruction target : select.getNonDefaultTargets())
						branchTargets.push(target, currentStackDepth, unconsumedinstructions);

					// We're deferring the analysis of this branch, so we set this to null
					// to indicate that we should explore an unexplored branch after these conditions.
					instruction = null;
					
					break;

				// Defer this analysis by saying that there's no next instruction.
				case Opcodes.GOTO :
				case Opcodes.GOTO_W :
				
					instruction = null;
					break;
				
				// Defer this analysis by saying that there's no next instruction.
				case Opcodes.JSR :
				case Opcodes.JSR_W :

					jsr = true;
					break;
					
				}
				
				// Note that we DON'T nullify the current instruction for conditional branches,
				// since we want to analyze its fall through case. We defer analysis of its jump until we're done with this path.
				
				// For all branches, the target of the branch is pushed on the branch stack.
				// conditional branches have a fall through case, selects don't, and
				// jsr/jsr_w return to the next instruction.				
				branchTargets.push(branch.getTarget(), currentStackDepth, unconsumedinstructions);

				// If this is an instruction that comes back to following PC, push next instruction, with stack depth reduced by 1.
				if(jsr) {
					unconsumedinstructions.remove(unconsumedinstructions.size() - 1);
					branchTargets.push(branch.getNext(), currentStackDepth - 1, unconsumedinstructions);
					instruction = null;
				}

			} 
			// Check for instructions that terminate the method. If this is one of them,
			// set the current instruction to null to indicate that we're done exploring this path.
			else {
				
				switch(opcode) {
				case Opcodes.ARETURN :
				case Opcodes.DRETURN :
				case Opcodes.FRETURN :
				case Opcodes.IRETURN :
				case Opcodes.LRETURN :
				case Opcodes.RETURN :
				case Opcodes.ATHROW :
				case Opcodes.RET :
					instruction = null;
				}

			}
			
			
			// If we have no more instructions to explore on this path, see if there are any deferred branches to explore.
			if (instruction == null) {

				BranchTarget nextTargetToExplore = branchTargets.pop();
				if (nextTargetToExplore != null) {

					// Explore this branch, restoring the stack state at the time we deferred
					instruction = nextTargetToExplore.target;
					currentStackDepth = nextTargetToExplore.stackDepthBeforeReachingTarget;
					unconsumedinstructions = new TIntArrayList(nextTargetToExplore.indicesOfUnconsumedInstructions.toNativeArray());
					
				}

			}
			// If we haven't set current instruction to null to indicate the end of this path, explore the instruction after this one.
			else
				instruction = instruction.getNext();
			
		}
		
	}
	
	/**
	 * Represents the state of the stack at a branch point. We store it
	 * so that when we come back to explore it, we can restore the loop below
	 * to a state that represents the correct stack state upon reaching this target.
	 * 
	 * @author Andrew J. Ko
	 */
	private static final class BranchTarget {
		
		private final Instruction target;
		private final int stackDepthBeforeReachingTarget;
		private final TIntArrayList indicesOfUnconsumedInstructions;

		public BranchTarget(Instruction target, int stackDepth, TIntArrayList instructionsWithUnconsumedOperands) {

			this.target = target;
			this.stackDepthBeforeReachingTarget = stackDepth;

			// Copy the state of the operand stack at the point of branching.
			this.indicesOfUnconsumedInstructions = new TIntArrayList(instructionsWithUnconsumedOperands.toNativeArray());
			
		}
	
	}
	  
	/**
	 * Represents all of the branches that we have yet to explore.
	 * 
	 * @author Andrew J. Ko
	 */
	static final class BranchStack {

		ArrayList<BranchTarget> branchTargets = new ArrayList<BranchTarget>(3);
		TIntObjectHashMap<BranchTarget> visitedTargets = new TIntObjectHashMap<BranchTarget>(10);

		// Note that we DON'T push if we've already explored this target. But we have to be very careful about
		// what we mean by "explored" because we may have already been on this path before, but not necessarily with
		// the same operands.
		public void push(Instruction target, int stackDepth, TIntArrayList instructionsWithUnconsumedOperands) {
			
			// If we've already been to this instruction, see if the state of their operand stacks differ.
			BranchTarget alreadyVisitedTarget = visitedTargets.get(target.getIndex()); 
			if(alreadyVisitedTarget != null) {

				TIntArrayList alreadyVisitedStack = alreadyVisitedTarget.indicesOfUnconsumedInstructions;

				if(alreadyVisitedStack.size() == instructionsWithUnconsumedOperands.size()) {
					
					// If the stacks are identical, than we can ignore this particular target, even if we reached it through a different instruction,
					// since it has an identical operand stack state.
					boolean equal = true;
					for(int i =0; i < alreadyVisitedStack.size(); i++) {
						if(alreadyVisitedStack.getQuick(i) != instructionsWithUnconsumedOperands.getQuick(i)) {
							equal = false;
							break;
						}
					}
					if(equal)
						return;
					
				}
				
			}
			branchTargets.add(visit(target, stackDepth, instructionsWithUnconsumedOperands));
			
		}

		// Return the target on the top of the stack, unless there's none.
		public BranchTarget pop() {
			
			if (!branchTargets.isEmpty()) {
				BranchTarget bt = branchTargets.remove(branchTargets.size() - 1);
				return bt;
			}

			return null;
		}

		// Marks that this target is "visited". We may not have actually visited it yet,
		// but we don't want to add it again.
		private final BranchTarget visit(Instruction target, int stackDepth, TIntArrayList instructionsWithUnconsumedOperands) {
			
			BranchTarget bt = new BranchTarget(target, stackDepth, instructionsWithUnconsumedOperands);
			visitedTargets.put(target.getIndex(), bt);
			return bt;

		}
	
	}

	public static class Consumers implements Iterable<Instruction> {

		private final Instruction first, second;

		public Consumers(Instruction first, Instruction second) { 
			
			this.first = first; 
			this.second = second;
			
			// Must be different; iterator assumes so.
			if(first != null)
				assert first != second : "Passed two consumers, but they're both " + first;
			
		}
		
		public int getNumberOfConsumers() { return (first != null ? 1 : 0) + (second != null ? 1 : 0); }

		public Instruction getFirstConsumer() { return first; }
		public Instruction getSecondConsumer() { return second; }

		public Iterator<Instruction> iterator() { 
			return new Iterator<Instruction>() {
				boolean onFirst = true;
				public boolean hasNext() { return (onFirst && first != null) || second != null; }
				public Instruction next() { if(onFirst) { onFirst = false; return first; } else return second; }
				public void remove() { throw new UnsupportedOperationException("Can't remove a consumer from an instruction."); }
			};
		}

		public boolean contains(Instruction instruction) { return first == instruction || second == instruction; }
		
	}

	private static class InstructionProducers {
		
		private final Producers[] argumentProducers;
		
		public InstructionProducers(Instruction inst) {
			
			argumentProducers = new Producers[Math.max(inst.getNumberOfOperandsConsumed(), inst.getNumberOfOperandsPeekedAt())];
			                                                                                                  
		}

		public Producers getArgumentProducers(int argument) {
			
			Producers producers = argumentProducers[argument];
			if(producers == null) return new Producers();
			else return producers;
			
		}

		// Returns true if new.
		public boolean add(int argument, Instruction producer) {

			Producers producers = argumentProducers[argument];

			// Make a new bucket if we don't have one.
			if(producers == null) {
				producers = new Producers();
				argumentProducers[argument] = producers;
			}

			return producers.add(producer);
			
		}
		
	}
	
	public static class Producers {

		private Instruction[] producers;
		
		public Producers() {}

		public Instruction[] getProducers() { return producers; }

		public boolean add(Instruction producer) {
			
			// Make a new array to fit the new producers
			Instruction[] newProducers = new Instruction[producers == null ? 1 : producers.length + 1];
			if(producers != null) { 
				for(int i = 0; i < producers.length; i++) { 
					if(producers[i] == producer) return false;
					newProducers[i] = producers[i];
				}
			}
			producers = newProducers;

			// Set the new producer
			producers[newProducers.length - 1] = producer;
			
			return true;
			
		}
		
		public int getNumberOfProducers() { return producers == null ? 0 : producers.length; }
		
		public Instruction getFirstProducer() { return producers == null ? null : producers[0]; }

		public Instruction getSecondProducer() { return producers == null ? null : producers.length >= 2 ? producers[1] : null; }
		
		/**
		 * Warning! This can return null! If it does, it means that the producer is the invisible exception operand
		 * pushed by the JVM!
		 */
		public Instruction getProducer(int index) { return producers[index]; }
		
		public boolean includes(Instruction inst) {
			
			for(Instruction producer : producers)
				if(producer == inst) return true;
			return false;
			
		}
		
//		public Iterator<Instruction> iterator() {
//			return new Iterator<Instruction>() {
//				int i = 0;
//				public boolean hasNext() { return producers != null && i < producers.length; }
//				public Instruction next() { return producers[i++]; }
//				public void remove() { throw new UnsupportedOperationException("Can't remove argument producers"); }
//			};
//		}
		
	}
	
}