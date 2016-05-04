package edu.cmu.hcii.whyline.source;

import java.util.*;

import edu.cmu.hcii.whyline.bytecode.*;

/**
 *
 * Generates a set of "consumers" of expressions, which java language constructs such
 * as an if, while, etc. can process in order to associate tokens with instructions.
 * 
 * Not all of the "consumers" generated will appear in source. For example, methods of a class that extends Object
 * may have a hidden super() call. The parser can just associate nearby tokens with these invisible expressions.
 * 
 * This class relies on a Classfile's LineNumberTableAttributes to sort expressions in a method. This is mainly
 * useful because exception handlers get compiled at the end of the classfile, but appear throughout a method's source.
 *
 * @author Andrew J. Ko
 *
 */
public final class Decompiler {

	private final JavaSourceFile source;
	private final CodeAttribute code;
	private final Instruction[] instructions;

	private List<ConsumerExpression<?>> consumers = new ArrayList<ConsumerExpression<?>>(10);
	
	private Map<Instruction,Expression<?>> expressions = new HashMap<Instruction,Expression<?>>();
	
	public Decompiler(JavaSourceFile source, CodeAttribute code) {
		
		this.source = source;
		this.code = code;
		this.instructions = code.getInstructions();

		analyze();
		
	}

	public JavaSourceFile getSource() { return source; }
	
	public ConsumerExpression<?> peekConsumer() {
		
		return consumers.isEmpty() ? null : consumers.get(0);
		
	}
	
	public ConsumerExpression<?> consume() {
	
		return consumers.isEmpty() ? null : consumers.remove(0);
		
	}

	private void analyze() {
	
		SortedSet<ConsumerExpression<?>> cons = new TreeSet<ConsumerExpression<?>>(new Comparator<ConsumerExpression<?>>() {
			public int compare(ConsumerExpression<?> o1, ConsumerExpression<?> o2) { 
				int line1 = o1.getCode().getLineNumber().getNumber();
				int line2 = o2.getCode().getLineNumber().getNumber();
				if(line1 != line2) return line1 - line2;
				else return o1.getCode().getIndex() - o2.getCode().getIndex();
			}});

		List<ConsumerExpression<?>> forLoopConditions = new LinkedList<ConsumerExpression<?>>();
		
		// Find consumers with no consumers. These are the expression eaters!
		for(Instruction inst : instructions) {
			
			// To count as a consumer, the instruction must 
			// (1) have no consumers and 
			// (2) must have some operands consumed (or must be an invocation or increment)
			
			if(inst.getConsumers().getNumberOfConsumers() == 0 && (inst.getNumberOfOperandsConsumed() > 0 || inst instanceof Invoke || inst instanceof IINC)) {
				
				// We don't want to make an expression eater out of a NEW simply because of the odd quirk that it produces no values.
				// We only want to make it out of NEW expressions that 
				boolean isConsumedNEW = inst instanceof NEW && inst.getConsumers().getNumberOfConsumers() > 0;
				boolean isInstanceInitialization = inst instanceof INVOKESPECIAL;
				
				if(!isConsumedNEW && !isInstanceInitialization) {
					ConsumerExpression<?> consumer = toConsumerExpression(inst);
					if(consumer != null)
							cons.add(consumer);

				}

			}
		}

		consumers = new LinkedList<ConsumerExpression<?>>(cons);

		for(int i = 0; i < consumers.size(); i++) {
			
			ConsumerExpression<?> consumer = consumers.get(i);
			
			// If we just found a for loop condition, we need to find the first expression of the loop block and insert the loop condition just before it.
			if(consumer.getCode() instanceof ConditionalBranch && ((ConditionalBranch)consumer.getCode()).isForLoop()) {

				UnconditionalBranch unconditional = consumer.getCode().getUnconditionalBranchPrecessessor();

				// Find the initialization expression, if there is one.
				int initIndex = 0;
				Instruction init = unconditional.getPrevious();
				ConsumerExpression<?> initExpression = null;
				for(ConsumerExpression<?> c : consumers) {
					if(c.getCode() == init) { initExpression = c; break; }
					else initIndex++;
				}

				// Find the advance expression, if there is one.
				Instruction advance = unconditional.getTarget().getPrevious();
				ConsumerExpression<?> advanceExpression = null;
				for(ConsumerExpression<?> c : consumers)
					if(c.getCode() == advance) { advanceExpression = c; break; }

				// If there's an advance and initialization, move the condition after the initialization and the advance after the condition.
				if(initExpression != null && advanceExpression != null) {

					// Move condition after init.
					int conditionIndex = consumers.indexOf(consumer);
					consumers.remove(conditionIndex);
					consumers.add(Math.min(initIndex + 1, consumers.size()), consumer);

					// Move the advance after the condition.
					int advanceIndex = consumers.indexOf(advanceExpression);
					consumers.remove(advanceIndex);
					consumers.add(Math.min(initIndex + 2, consumers.size()), advanceExpression);
					
				}
				
			}
			
		}
		
	}
	
	
	public Expression <?> getExpression(Instruction consumer, Instruction producer) {
		
		if(producer == null) return null;
		
		Expression<?> expr = expressions.get(producer);
		if(expr == null) {
			expr = toExpression(consumer, producer);
			expressions.put(producer, expr);
		}
		
		return expr;
		
	}
	
	/**
	 * This is an instance method because we want to reuse expressions that have already been created. This is because duplication instructions reuse values.
	 */
	private Expression<?> toExpression(Instruction consumer, Instruction producer) {
		
		int opcode = producer.getOpcode();
		
		// These are organized by the instructions relationship to Java syntax.
		switch(opcode) {
		
		// Literal leaves (no stack dependencies)
		case Opcodes.ACONST_NULL: 
		case Opcodes.BIPUSH:
	    case Opcodes.SIPUSH:
	    case Opcodes.DCONST_0:
	    case Opcodes.DCONST_1:
	    case Opcodes.FCONST_0: 
	    case Opcodes.FCONST_1: 
	    case Opcodes.FCONST_2: 
	    case Opcodes.ICONST_0:
	    case Opcodes.ICONST_1: 
	    case Opcodes.ICONST_2: 
	    case Opcodes.ICONST_3: 
	    case Opcodes.ICONST_4: 
	    case Opcodes.ICONST_5: 
	    case Opcodes.ICONST_M1:
	    case Opcodes.LCONST_0: 		
	    case Opcodes.LCONST_1: 		
	    case Opcodes.LDC: 					
	    case Opcodes.LDC2_W: 			
	    case Opcodes.LDC_W: 				
	    	
	    	return new LiteralExpression(this, (PushConstant<?>)producer);

		// Variable leaves
		case Opcodes.ALOAD:
		case Opcodes.ALOAD_0:
		case Opcodes.ALOAD_1:
		case Opcodes.ALOAD_2:
		case Opcodes.ALOAD_3: 
	    case Opcodes.DLOAD: 
	    case Opcodes.DLOAD_0:
	    case Opcodes.DLOAD_1: 
	    case Opcodes.DLOAD_2:
	    case Opcodes.DLOAD_3: 
	    case Opcodes.FLOAD:
	    case Opcodes.FLOAD_0:
	    case Opcodes.FLOAD_1: 
	    case Opcodes.FLOAD_2: 
	    case Opcodes.FLOAD_3:
	    case Opcodes.ILOAD: 
	    case Opcodes.ILOAD_0:
	    case Opcodes.ILOAD_1: 
	    case Opcodes.ILOAD_2:
	    case Opcodes.ILOAD_3: 
	    case Opcodes.LLOAD: 				
	    case Opcodes.LLOAD_0: 			
	    case Opcodes.LLOAD_1: 			
	    case Opcodes.LLOAD_2: 			
	    case Opcodes.LLOAD_3: 			
	    	
	    	return new LocalExpression(this, (GetLocal)producer);

		// Other variables
	    case Opcodes.GETFIELD: 			
	    	
	    	return new GetFieldExpression(this, (GETFIELD)producer);
	    
	    case Opcodes.GETSTATIC: 
	    	
	    	return new GetStaticExpression(this, (GETSTATIC)producer);

	    case Opcodes.ARRAYLENGTH: 	
			return new ArrayLengthExpression(this, (ARRAYLENGTH)producer);

	    case Opcodes.CHECKCAST :
	    	break;
			
		// Conversions
	    case Opcodes.D2F: 					
	    case Opcodes.D2I: 					
	    case Opcodes.D2L: 					
	    case Opcodes.F2D: 					
	    case Opcodes.F2I: 					
	    case Opcodes.F2L: 					
	    case Opcodes.I2B: 					
	    case Opcodes.I2C: 					
	    case Opcodes.I2D: 					
	    case Opcodes.I2F: 					
	    case Opcodes.I2L: 					
	    case Opcodes.I2S: 					
	    case Opcodes.L2D: 					
	    case Opcodes.L2F: 					
	    case Opcodes.L2I: 					
	    	
	    	return new CastExpression(this, (Conversion)producer);
	    
	    // Array getting
		case Opcodes.AALOAD: 
		case Opcodes.BALOAD: 
	    case Opcodes.DALOAD: 			
		case Opcodes.CALOAD: 
		case Opcodes.FALOAD: 			
		case Opcodes.IALOAD: 			
		case Opcodes.LALOAD: 			
		case Opcodes.SALOAD: 			
			
			return new GetArrayValueExpression(this, (GetArrayValue)producer);
			
		// Allocations	
		case Opcodes.ANEWARRAY:
	    case Opcodes.MULTIANEWARRAY: 
	    case Opcodes.NEWARRAY: 		

	    	return new NewExpression(this, (Instantiation)producer);

	    case Opcodes.NEW: 					

	    	// If an <init> is asking, just return the leaf expression.
	    	if(consumer instanceof INVOKESPECIAL)
	    		return new NewExpression(this, (Instantiation)producer);
	    	
	    	// Otherwise, return the <init>, so it looks like the consumer is consuming the result of the constructor.
	    	if(producer.getNext() instanceof DUP) {

	    		// Find the duplication's <init> consumer...
		    	Instruction c = producer.getNext();
		    	while(c != null && !(c instanceof INVOKESPECIAL))
		    		c = c.getConsumers().getFirstConsumer();
		    	
		    	// If we didn't find it, just return the leaf expression.
		    	return consumer == null ? new NewExpression(this, (Instantiation)producer): getExpression(consumer, c);
		    	
	    	}
	    	// I'm not sure when this case would arise, but to be safe, we'll return the leaf expression.
	    	else return new NewExpression(this, (Instantiation)producer);
	    	
	    // Invocations
	    case Opcodes.INVOKEINTERFACE: 
	    case Opcodes.INVOKESPECIAL: 	
	    case Opcodes.INVOKESTATIC: 	
	    case Opcodes.INVOKEVIRTUAL: 	
	    	
	    		return new CallExpression(this, (Invoke)producer);

		// Addition
	    case Opcodes.DADD: 				
	    case Opcodes.IADD: 				
	    case Opcodes.FADD: 				
	    case Opcodes.LADD: 				
	    // Subtraction
	    case Opcodes.ISUB: 					
	    case Opcodes.FSUB: 				
	    case Opcodes.DSUB: 				
	    case Opcodes.LSUB: 				
	    // Division
	    case Opcodes.DDIV: 				
	    case Opcodes.FDIV: 				
	    case Opcodes.IDIV: 					
	    case Opcodes.LDIV: 				
	    // Multiplication
	    case Opcodes.DMUL: 				
	    case Opcodes.FMUL: 				
	    case Opcodes.IMUL: 				
	    case Opcodes.LMUL: 				
	    // Remainder
	    case Opcodes.DREM: 				
	    case Opcodes.FREM: 				
	    case Opcodes.IREM: 				
	    case Opcodes.LREM: 				
	    // Logic
	    case Opcodes.IAND: 				
	    case Opcodes.IOR: 					
	    case Opcodes.IXOR: 				
	    case Opcodes.LAND: 				
	    case Opcodes.LOR: 					
	    case Opcodes.LXOR: 				
	    // Shifting
	    case Opcodes.IUSHR: 				
	    case Opcodes.ISHL: 					
	    case Opcodes.ISHR: 				
	    case Opcodes.LSHL: 				
	    case Opcodes.LSHR: 				
	    case Opcodes.LUSHR: 				
	    // Comparisons
	    case Opcodes.DCMPG: 				
	    case Opcodes.DCMPL: 				
	    case Opcodes.FCMPG: 				
	    case Opcodes.FCMPL: 				
	    case Opcodes.LCMP: 		
	    	
	    	return new BinaryOperatorExpression(this, (BinaryComputation)producer);

	    // Negation
	    case Opcodes.LNEG: 				
	    case Opcodes.FNEG: 				
	    case Opcodes.DNEG: 				
	    case Opcodes.INEG: 					

	    	return new UnaryOperatorExpression(this, (UnaryComputation)producer);

	    case Opcodes.INSTANCEOF:
	    	
	    	return new InstanceOfExpression(this, (INSTANCEOF)producer);

	    // These actually produce values.
	    case Opcodes.DUP: 					
	    case Opcodes.DUP_X1: 			
	    case Opcodes.DUP_X2: 			

	    	// Find the expression for this dup's producer. For example, if this duplications an instantiation, find the instantiation.
	    	// We reuse past expressions created for this producer.
			StackDependencies.Producers producers = producer.getProducersOfArgument(0);
			Expression<?> expr = getExpression(consumer, producers.getFirstProducer());
			
			return expr;

	    case Opcodes.DUP2: 				
	    case Opcodes.DUP2_X1: 			
	    case Opcodes.DUP2_X2: 			

	    	System.err.println("Not handling dup2lications");
	    	break;

		default: 
			
			assert false: "Shouldn't be creating an expression for " + producer;
		
		}
		
		return null;
		
	}
	
	private ConsumerExpression<?> toConsumerExpression(Instruction consumer) {
		
		int opcode = consumer.getOpcode();
		
		switch(opcode) {

		// Calls
	    case Opcodes.INVOKEINTERFACE: 
	    case Opcodes.INVOKESPECIAL: 	
	    case Opcodes.INVOKESTATIC: 	
	    case Opcodes.INVOKEVIRTUAL: 	

    		return new CallExpression(this, (Invoke)consumer);

		// Assignments
		case Opcodes.ASTORE: 			
		case Opcodes.ASTORE_0: 		
		case Opcodes.ASTORE_1: 		
		case Opcodes.ASTORE_2:
		case Opcodes.ASTORE_3: 
	    case Opcodes.DSTORE: 			
	    case Opcodes.DSTORE_0: 		
	    case Opcodes.DSTORE_1: 		
	    case Opcodes.DSTORE_2: 		
	    case Opcodes.DSTORE_3: 		
	    case Opcodes.FSTORE: 			
	    case Opcodes.FSTORE_0: 			
	    case Opcodes.FSTORE_1: 			
	    case Opcodes.FSTORE_2: 			
	    case Opcodes.FSTORE_3: 			
	    case Opcodes.ISTORE: 				
	    case Opcodes.ISTORE_0: 			
	    case Opcodes.ISTORE_1: 			
	    case Opcodes.ISTORE_2: 			
	    case Opcodes.ISTORE_3: 			
	    case Opcodes.LSTORE: 			
	    case Opcodes.LSTORE_0: 			
	    case Opcodes.LSTORE_1: 			
	    case Opcodes.LSTORE_2: 			
	    case Opcodes.LSTORE_3: 			
	    case Opcodes.IINC: 					
	
	    	return new SetLocalExpression(this, (SetLocal)consumer);

	    case Opcodes.PUTSTATIC: 		
	    	
	    	return new PutStaticExpression(this, (PUTSTATIC)consumer);
	    	
	    case Opcodes.PUTFIELD: 			

	    	return new PutFieldExpression(this, (PUTFIELD)consumer);

	    // Branches
	    case Opcodes.IFEQ: 					
	    case Opcodes.IFGE: 					
	    case Opcodes.IFGT: 					
	    case Opcodes.IFLE: 					
	    case Opcodes.IFLT: 					
	    case Opcodes.IFNE: 					
	    case Opcodes.IFNONNULL: 		
	    case Opcodes.IFNULL: 				
	    case Opcodes.IF_ACMPEQ: 		
	    case Opcodes.IF_ACMPNE: 		
	    case Opcodes.IF_ICMPEQ: 			
	    case Opcodes.IF_ICMPGE: 			
	    case Opcodes.IF_ICMPGT: 			
	    case Opcodes.IF_ICMPLE: 			
	    case Opcodes.IF_ICMPLT: 			
	    case Opcodes.IF_ICMPNE: 			
	    case Opcodes.GOTO: 				
	    case Opcodes.GOTO_W: 			
	
	    	return new BranchExpression(this, (Branch)consumer);
	    	
	   	// Returns
		case Opcodes.ARETURN: 
	    case Opcodes.DRETURN: 			
	    case Opcodes.FRETURN: 			
	    case Opcodes.IRETURN: 			
	    case Opcodes.LRETURN: 			
	    case Opcodes.RETURN: 			
	
	    	return new ReturnExpression(this, (AbstractReturn)consumer);
	    	
		// Array setting
		case Opcodes.AASTORE: 
		case Opcodes.BALOAD:
	    case Opcodes.DASTORE: 			
		case Opcodes.CASTORE:
	    case Opcodes.FASTORE:			
	    case Opcodes.IASTORE: 			
	    case Opcodes.LASTORE:			
	    case Opcodes.SASTORE: 			
		case Opcodes.BASTORE:

			return new SetArrayExpression(this, (SetArrayValue)consumer);
			
	    // Exceptions
		case Opcodes.ATHROW:

			return new ThrowExpression(this, (ATHROW)consumer);

	    // Switches
	    case Opcodes.LOOKUPSWITCH: 	
	    case Opcodes.TABLESWITCH: 	

			return new SwitchExpression(this, (TableBranch)consumer);

	    // Synchronization
	    case Opcodes.MONITORENTER: 	
	    	
	    	return new MonitorEnterExpression(this, (MONITORENTER)consumer);
	    	
	    case Opcodes.MONITOREXIT: 	

	    	return new MonitorExitExpression(this, (MONITOREXIT)consumer);

	    // Random instructions that don't appear in code
	    case Opcodes.SWAP: 				
	    case Opcodes.POP: 					
	    case Opcodes.POP2: 				
	    case Opcodes.NOP: 					
	    case Opcodes.RET:
	    case Opcodes.JSR: 					
	    case Opcodes.JSR_W: 				
	    case Opcodes.WIDE:
	
	   	default :

	   		return null;
	   	
		}
   		
	}

	public int getExpressionsRemaining() { return expressions.size(); }
		
}