package edu.cmu.hcii.whyline.bytecode;

import java.io.*;
import java.util.*;

import edu.cmu.hcii.whyline.analysis.*;
import edu.cmu.hcii.whyline.source.*;
import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.tracing.*;

import gnu.trove.TIntObjectHashMap;

/**
 * @author Andrew J. Ko
 *
 *
 *	Code_attribute {
 *    	u2 attribute_name_index;
 *    	u4 attribute_length;
 *    	u2 max_stack;
 *    	u2 max_locals;
 *    	u4 code_length;
 *    	u1 code[code_length];
 *    	u2 exception_table_length;
 *    	{    	u2 start_pc; 
 *    	      	u2 end_pc;
 *    	      	u2  handler_pc;
 *    	      	u2  catch_type;
 *    	}	exception_table[exception_table_length];
 *    	u2 attributes_count;
 *    	attribute_info attributes[attributes_count];
 *    }
 */ 
public final class CodeAttribute extends Attribute {

	public static final int MAXIMUM_CODE_BYTE_LENGTH = 65536;

	private UTF8Info attributeName;
	private final Classfile classfile;
	private MethodInfo method;
	private ConstantPool pool;
	private int maxStack, maxLocals, codeArrayByteLength;

	private Instruction[] instructions;
	private int[] byteIndices;
	
	private INVOKESPECIAL callToInitializer = null;
	private INVOKESPECIAL callToSuper = null;
	
	private ExceptionHandler[] exceptionTable;
	private Attribute[] attributes;
	private ArrayList<LineNumberTableAttribute> lineNumbers = new ArrayList<LineNumberTableAttribute>(1);
	private ArrayList<LocalVariableTableAttribute> localVariables = new ArrayList<LocalVariableTableAttribute>(1);
		
	private final ArrayList<AbstractReturn> returns = new ArrayList<AbstractReturn>(1);
	
	private boolean isStateAffecting;
	
	private ControlDependencies controlDependencies;
	
	private LocalDependencies localDependencies = null;
	
	private boolean invokesTextualOutput = false;
	
	/**
	 * The set of instructions that lead to this instruction through jumps. It is essential that these are
	 * in order of appearance in a method, because we rely on this order in control dependency determination algorithms.
	 */
	private TIntObjectHashMap<SortedSet<Branch>> incomingBranchesByInstructionIndex;
	
	public CodeAttribute(UTF8Info attributeName, MethodInfo owner, ConstantPool pool, DataInputStream data, int length) throws IOException, JavaSpecificationViolation, AnalysisException {
		
		this.attributeName = attributeName;		
		this.method = owner;
		this.classfile = this.method.getClassfile();
		this.pool = pool;
		maxStack = data.readUnsignedShort();
		maxLocals = data.readUnsignedShort();

		// Parse the instructions.
		codeArrayByteLength = data.readInt();
		instructions = BytecodeParser.parse(data, codeArrayByteLength, this);

		// Compute some attributes of the instructions. This array has several null elements; we're just using it for performance.
		Instruction[] instructionsByByteIndex = computeCharacteristics();

		// Resolve all of the branch targets
		resolveTargets(instructionsByByteIndex);

		// Read the exception table.
		exceptionTable = new ExceptionHandler[data.readUnsignedShort()];
		for(int i = 0; i < exceptionTable.length; i++)
			exceptionTable[i] = 
				new ExceptionHandler(
					instructionsByByteIndex[data.readUnsignedShort()], 
					instructionsByByteIndex[data.readUnsignedShort()], 
					instructionsByByteIndex[data.readUnsignedShort()],
					data.readUnsignedShort());

		// Read the attributions, looking for particular ones to keep references to.
		int numberOfAttributes = data.readUnsignedShort();
		attributes = new Attribute[numberOfAttributes];
		for(int i = 0; i < numberOfAttributes; i++) {
			
			Attribute attr = Attribute.read(this, pool, data); 
			attributes[i] = attr;

			if(attr instanceof LineNumberTableAttribute)
				lineNumbers.add((LineNumberTableAttribute)attr);
			else if(attr instanceof LocalVariableTableAttribute)
				localVariables.add((LocalVariableTableAttribute)attr);

		}

		// Allow the garbage collector to eat up these bytes, since we only needed them temporarily.
		instructionsByByteIndex = null;
		
	}
	
	private void resolveTargets(Instruction[] instructionsByByteIndex) throws AnalysisException {

		// We wait until we know how many instructions there are, since based on empirical data from a profiler, only 10% of
		// instructions are jumped to with a branch.
		incomingBranchesByInstructionIndex = new TIntObjectHashMap<SortedSet<Branch>>((int)(instructions.length * .1));

		for(Instruction inst : instructions)
			if(inst instanceof Branch) 
				((Branch)inst).resolveTargets(instructionsByByteIndex);

		// Now trim the map to save space.
		incomingBranchesByInstructionIndex.trimToSize();
		
	}
		
	public SortedSet<Branch> getIncomingBranchesForInstruction(int instructionIndex) {
		
		return incomingBranchesByInstructionIndex.get(instructionIndex);
		
	}
	
	public void addIncomingBranchToInstruction(Branch branch, Instruction inst) {
		
		SortedSet<Branch> branches = incomingBranchesByInstructionIndex.get(inst.getIndex());
		if(branches == null) {
			branches = new TreeSet<Branch>();
			incomingBranchesByInstructionIndex.put(inst.getIndex(), branches);
		}
		branches.add(branch);

	}
	
	public Classfile getClassfile() { return classfile; }
	
	public INVOKESPECIAL getCallToInitializer() { return callToInitializer; }
	public INVOKESPECIAL getCallToSuper() { return callToSuper; }
	
	public Iterable<AbstractReturn> getReturns() { return returns; }

	public boolean isStateAffecting() { return isStateAffecting; } 
	
	// Should be called if the instruction list is modified in any way (for example, instrumented).
	private Instruction[] computeCharacteristics() throws JavaSpecificationViolation {

		Trace trace = getClassfile().getTrace();
		ClassIDs classIDs = trace == null ? Agent.classIDs : trace.getClassIDs();
		
		// Reset this in case one has disappeared
		invokesTextualOutput = false;
		
		// This is only an estimate of the byte length, to help us instantiate an array of the right size to store the instruction at each byte index.
		// We let each table branch have the maximum amount of padding that it wants (it can be 0 to 3 bytes). Then we'll assign the actual
		// value for codeArrayByteLength below.
		for(Instruction inst : instructions) {
			
			// Here we detect a call to super() or this().
			if(inst instanceof INVOKESPECIAL) {
				
				MethodrefInfo ref = ((INVOKESPECIAL)inst).getMethodInvoked();
				// Must call an initializer, must be in an initializer
				if(ref.callsInstanceInitializer() && method.isInstanceInitializer()) {
					QualifiedClassName className = ref.getClassName(); 
					// Must call on either this class or the superclass.
					if(className.equals(getMethod().getClassfile().getSuperclassInfo().getName()) || className.equals(getMethod().getClassfile().getInternalName()))
						callToInitializer = (INVOKESPECIAL)inst;
				}

				if(ref.getMethodName().equals(method.getInternalName()) && 
					ref.getMethodDescriptor().equals(method.getDescriptor()) &&
					ref.getClassName() == method.getClassfile().getSuperclassInfo().getName()) {
					callToSuper = (INVOKESPECIAL)inst;
				}
				
			}

			// Remember if there are any invocations to text-related methods
			if(classIDs != null && !invokesTextualOutput && inst instanceof Invoke) {
				
				MethodrefInfo methodInvoked = ((Invoke)inst).getMethodInvoked();
				QualifiedClassName classInvokedOn = methodInvoked.getClassName();
				
				// It must produce a value for an instance of one of these classes.
				invokesTextualOutput = classIDs.isOrIsSubclassOfTextualOutputProducer(classInvokedOn);

			}
			
		}
		
		returns.clear();
		isStateAffecting = false;
		
		Instruction[] instructionsByByteIndex = new Instruction[instructions.length * 3];
		byteIndices = new int[instructions.length];
		int byteIndex = 0;
		for(int instructionIndex = 0; instructionIndex < instructions.length; instructionIndex++) {

			Instruction inst = instructions[instructionIndex];
			
			if(inst instanceof AbstractReturn) returns.add((AbstractReturn)inst);
			else if(inst instanceof PUTFIELD) isStateAffecting = true;
			
			// Make bigger if necessary.
			if(byteIndex >= instructionsByByteIndex.length) {
				Instruction[] newArray = new Instruction[byteIndex * 2];
				System.arraycopy(instructionsByByteIndex, 0, newArray, 0, instructionsByByteIndex.length);
				instructionsByByteIndex = newArray;
			}
			
			inst.setInstructionIndex(instructionIndex);
			instructionsByByteIndex[byteIndex] = inst;
			byteIndices[instructionIndex] = byteIndex;
			
			byteIndex += inst.byteLength();
			
		}
		
		codeArrayByteLength = byteIndex;
			
		if(codeArrayByteLength > MAXIMUM_CODE_BYTE_LENGTH) 
			throw new JavaSpecificationViolation(""+ getMethod().getQualifiedNameAndDescriptor() + " is too long! At " + codeArrayByteLength + " bytes long, it exceeds the maximum of " + MAXIMUM_CODE_BYTE_LENGTH + " bytes.");
		
		return instructionsByByteIndex;
		
	}
	
	public boolean invokesTextualOutput() { return invokesTextualOutput; }
	
	public void toBytes(DataOutputStream stream) throws IOException {
		
		// Length of attribute = 2 for max stack + 2 for max locals + 4 for code length + code length in bytes + 2 for table length + exception table length * 8 + 2 for attribute length + length of all attributes
		int totalLength = 2 + 2 + 4 + codeArrayByteLength + 2 + exceptionTable.length * 8 + 2;
		for(Attribute attr : attributes) totalLength += attr.getTotalAttributeLength();
		
		// Start writing
		stream.writeShort(attributeName.getIndexInConstantPool());
		stream.writeInt(totalLength);

		// This is an optimization; this used to take 2.5 seconds for a 27000 instruction class file.
		// Instead of recomputing the max stack when writing this, we simply add a constant to the original max stack
		// that represents the maximum increase due to instrumentation. We rarely push more than two or three things onto the stack
		// for instrumentation, but let's assume there's a 10 argument trace that we do, just to be safe. 

		stream.writeShort(maxStack + 50);
		
        stream.writeShort(maxLocals);

		stream.writeInt(codeArrayByteLength);
		for(Instruction inst : instructions) {
			inst.toBytes(stream);
		}
        
		stream.writeShort(exceptionTable.length);
		for(ExceptionHandler info : exceptionTable) {
			stream.writeShort(info.getStartPC().getByteIndex());
			Instruction endPC = info.getEndPC();
			stream.writeShort(endPC == null ? codeArrayByteLength : endPC.getByteIndex());
			stream.writeShort(info.getHandlerPC().getByteIndex());
			stream.writeShort(info.getCatchTypeIndex());
		}
		
		stream.writeShort(attributes.length);
		for(Attribute attr : attributes) attr.toBytes(stream);

	}
	
	public int getTotalAttributeLength() { throw new RuntimeException("We actually only compute the length of a code attribute when writing it to a byte stream."); }

    public MethodInfo getMethod() { return method; }
	
	public int getMaxStack() { return maxStack; }
	
	public int getMaxLocals() { return maxLocals; }
	
	public int getNumberOfInstructions() { return instructions.length; }

	public int getFirstInstructionID() { return method.getFirstInstructionID(); }

	public Instruction getInstruction(int index) { return index < 0 || index >= instructions.length ? null : instructions[index]; }
	
	public Instruction getFirstInstruction() { return instructions[0]; }

	public Instruction[] getInstructions() { return instructions; }

	public int getByteIndex(int instructionIndex) { return byteIndices[instructionIndex]; }

	// Doesn't assign the instructions if it encounters a specification violation.
	public void setInstructions(Instruction[] newInstructions) throws JavaSpecificationViolation {

		// We save this just in case the recompute fails.
		Instruction[] oldInstructions = instructions;
		
		instructions = newInstructions;
		
		try {
			computeCharacteristics();
		}
		// If it fails, we assign it the old instructions.
		catch(JavaSpecificationViolation e) {
			
			instructions = oldInstructions;
			computeCharacteristics();
			throw e;
			
		}
		
	}
	
	public List<ExceptionHandler> getExceptionTable() { return Collections.<ExceptionHandler>unmodifiableList(Arrays.<ExceptionHandler>asList(exceptionTable)); }
		
	public List<ExceptionHandler> getExceptionHandlersThatExecute(Instruction inst) {

		// Which handler's first instructions come before the given instruction this this instruction after? (to rule out those that come after)
		int largestHandlerIndexBeforeOrAtGivenInstruction = 0;
		for(ExceptionHandler handler : exceptionTable)
			if(handler.getHandlerPC().getIndex() <= inst.getIndex())
				largestHandlerIndexBeforeOrAtGivenInstruction = Math.max(largestHandlerIndexBeforeOrAtGivenInstruction, handler.getHandlerPC().getIndex());

		Vector<ExceptionHandler> handlers = new Vector<ExceptionHandler>(3);
		
		// Now, pick out all exception handlers that start at the largest handler PC that we found
		for(ExceptionHandler handler : exceptionTable)
			if(handler.getHandlerPC().getIndex() == largestHandlerIndexBeforeOrAtGivenInstruction)
				handlers.add(handler);
		
		return handlers;
		
	}
	
	public ExceptionHandler getHandlerStartingWith(Instruction i) {
		
		for(ExceptionHandler handler : getExceptionTable()) {
			
			if(handler.getHandlerPC() == i)
				return handler;
			
		}
		return null;

	}
	
	public boolean isInstructionInTryCatchBlock(Instruction instruction) {

		for(ExceptionHandler handler : exceptionTable)
			if(handler.handles(instruction))
				return true;
		return false;
	
	}

	public Set<ExceptionHandler> getExceptionHandlersProtecting(Instruction instruction) {

		Set<ExceptionHandler> handlers = new HashSet<ExceptionHandler>(); 

		for(ExceptionHandler handler : exceptionTable)
			if(handler.handles(instruction))
				handlers.add(handler);

		return handlers;
		
	}

	// Find the byte index of this instruction in the code array, then look up the line to which it corresponds.
	public LineNumber getLineNumberFor(Instruction instruction) {
	
		for(LineNumberTableAttribute lines : lineNumbers) {
			LineNumber lineNumber = lines.getLineNumberOf(instruction);
			if(lineNumber != null) return lineNumber;
		}
		
		return null;

	}
		
	public LineNumber getFirstLineNumber() {
		
		LineNumber lowestLineNumber = null;
		
		for(LineNumberTableAttribute lines : lineNumbers) {
			LineNumber lowestInTable = lines.getFirstLineNumber();
			if(lowestLineNumber == null) lowestLineNumber = lowestInTable;
			else if(lowestInTable.isBefore(lowestLineNumber)) lowestLineNumber = lowestInTable;
		}
		
		return lowestLineNumber;
		
	}
	
	public LineNumber getLastLineNumber() {
		
		LineNumber largestLineNumber = null;
		
		for(LineNumberTableAttribute lines : lineNumbers) {
			LineNumber largestInTable = lines.getLastLineNumber();
			if(largestLineNumber == null) largestLineNumber = largestInTable;
			else if(largestInTable.isAfter(largestLineNumber)) largestLineNumber = largestInTable;
		}
		
		return largestLineNumber;
		
	}
	
	/**
	 * Uses a binary search to find the index of the instruction with the given byte index.
	 * Returns null if no such instruction exists.
	 */
	public Instruction getInstructionAtByteIndex(int byteIndex) {

		int instructionIndex = getInstructionIndexOfByteIndex(byteIndex);
		return instructionIndex < 0 ? null : instructions[instructionIndex];
		
	}
	
	private int getInstructionIndexOfByteIndex(int byteIndex) {
		
		int low = 0;
		int high = byteIndices.length - 1;
		
		while(low <= high) {
			int mid = (low + high) / 2;
			int current = byteIndices[mid];
			if(current > byteIndex) high = mid - 1;
			else if(current < byteIndex)low = mid + 1;
			else return mid;
		}
		
		return -1;
		
	}

	public boolean hasLocalVariableInfo() { return localVariables.size() > 0; }
	
	public String getLocalIDNameRelativeToInstruction(int localID, Instruction inst) {
		
		assert inst.getCode() == this;

		// Is this defined at this time? If not, return null
		if(hasLocalVariableInfo()) {
			boolean isDefined = false;
			for(LocalVariableTableAttribute table : localVariables) {
				if(table.isLocalIDDefinedRelativeToInstruction(localID, inst))
					isDefined = true;
			}
			if(!isDefined)
				return null;
		}
		
		String name = null;
		
		for(LocalVariableTableAttribute table : localVariables) {
			name = table.getNameOfLocalIDRelativeToInstruction(localID, inst);
			if(name != null) return name;
		}

		if(!method.isStatic() && localID == 0) return "this";

		JavaSourceFile source = getClassfile().getSourceFile();
		MethodInfo method = getMethod();
		if(source != null) {
			if(localID < method.getLocalIDOfFirstNonArgument()) {
				int argNumber = method.getParsedDescriptor().getArgumentNumberFromLocalID(localID);
				
				// If this is not a legal local ID, return nothing.
				if(argNumber < 0) return null;
				
				if(method.isStatic()) argNumber++;
				name = source.getNameOfParameterNumber(method, argNumber);
				if(name != null) return name;
			}
			else {
				name = source.getLocalIDNameRelativeToInstruction(localID, inst);
				if(name != null) return name;
			}
		}

		int number = localID;
		if(method.isStatic()) number++;
		return "[" + (localID < method.getNumberOfArguments() ? "arg" : "local") + number + "]";

	}

	public String getDescriptorOfLocalIDRelativeToInstruction(int localID, Instruction inst) {
		
		assert inst.getCode() == this;
		
		for(LocalVariableTableAttribute table : localVariables) {
			String name = table.getDescriptorOfLocalIDRelativeToInstruction(localID, inst);
			if(name != null) return name;
		}

		return null;
		
	}
		

	public int getLocalIDOfNameRelativeToInstruction(String name, Instruction inst) {
		
		assert inst.getCode() == this;
		
		int localID = -1;
				
		for(LocalVariableTableAttribute table : localVariables) {
			localID = table.getLocalIDOfNameRelativeToInstruction(name, inst);
			if(localID != -1) return localID;
		}

		return -1;
		
	}
	
	public boolean localIDIsDefinedAt(int localID, Instruction inst) {
		
		for(LocalVariableTableAttribute table : localVariables)
			if(table.localIDIsDefinedAt(localID, inst)) return true;
		
		return false;
		
	}
	
	public Set<String> getLocalNames() {
		
		Set<String> names = new HashSet<String>();
		
		for(LocalVariableTableAttribute table : localVariables)
			names.addAll(table.getLocalNames());

		return names;
		
	}

	public SortedSet<Instruction> getInstructionsOnLineNumber(LineNumber lineNumber) {

		SortedSet<Instruction> instructions = new TreeSet<Instruction>();
		for(LineNumberTableAttribute lines : lineNumbers)
			lines.getInstructionsOnLineNumber(instructions, lineNumber);

		return instructions;
	
	}
	
	public Set<Instruction> getControlDependenciesFor(Instruction inst) {

		if(controlDependencies == null) controlDependencies = new ControlDependencies(this);
		return controlDependencies.getControlDependenciesOf(inst);
		
	}
		
	public StackDependencies.Producers getProducersOfArgument(Instruction inst, int arg) {

		try {
			StackDependencies dependencies = classfile.getStackDependenciesCache().getStackDependenciesFor(method);
			return dependencies.getProducersOfArgument(inst, arg);
		} catch (AnalysisException e) {
			e.printStackTrace();
			System.exit(0);
			return null;
		}

	}
	
	public StackDependencies.Consumers getConsumersOf(Instruction inst) {

		try {
			StackDependencies dependencies = classfile.getStackDependenciesCache().getStackDependenciesFor(method);
			return dependencies.getConsumersOf(inst);
		} catch (AnalysisException e) {
			e.printStackTrace();
			System.exit(0);
			return null;
		}
		
	}

	public LocalDependencies getLocalDependencies() {
		
		if(localDependencies == null) localDependencies = new LocalDependencies(this);
		return localDependencies;
		
	}
	
	public void trimToSize() {

		returns.trimToSize();
		localVariables.trimToSize();
		lineNumbers.trimToSize();
		
	}

	public String toString() {

		try {
			StringBuilder builder = new StringBuilder(getNumberOfInstructions() * 50);
		
			builder.append(getMethod().getQualifiedNameAndDescriptor());
			builder.append("\n(1st instruction ID = " + method.getFirstInstructionID() + ")");
			builder.append("\n");
			for(Instruction i : instructions) {
				builder.append("(");
				builder.append(i.getByteIndex());
				builder.append(")\t");
				try { builder.append(i); } catch(Exception e) { builder.append("(exception during toString())"); }
				builder.append("\n");		
			}
			
			for(ExceptionHandler handler : exceptionTable)
				builder.append("\n" + handler);
			
			return builder.toString();
		}
		catch(Exception e) {
			return "exception in toString()";
 	}
		
	}

	
}