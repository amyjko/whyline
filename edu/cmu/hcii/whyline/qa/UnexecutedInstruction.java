package edu.cmu.hcii.whyline.qa;

import static edu.cmu.hcii.whyline.qa.UnexecutedInstruction.Reason.*;

import java.util.*;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.io.IOEvent;
import edu.cmu.hcii.whyline.trace.*;
import edu.cmu.hcii.whyline.util.IntegerVector;

public final class UnexecutedInstruction {

	private final Instruction instruction;
	private final MethodInfo method;

	// Could be a method, an instruction, an event...
	private Object cause;
	
	// Instructions that this instruction could cause to execute, if executed properly.
	private final Set<UnexecutedInstruction> outgoing = new HashSet<UnexecutedInstruction>(16);
	
	// Instructions that, if executed properly, could execute this instruction. 
	private final Set<UnexecutedInstruction> incoming = new HashSet<UnexecutedInstruction>(4);
	
	private Reason reason = Reason.UNEXPLAINED;
	
	private int methodStartID = -1;
	private int methodReturnID = -1;
	
	private ExpectedObject callerExpectation;
	
	private Set<Instruction> controlDependencies;
	
	private final Question<?> question;
	private final Trace trace;

	private final ExpectedObject expectedObject;

	// A list of other executions of this instruction that didn't match the expected objectIDs.
	private IntegerVector executionsOnOtherObjects;
	
	private boolean explained = false;
	
	public enum Reason {
		
		UNREACHABLE() {
			public String getVerbalExplanation(Question<?> question, Instruction inst) {
				return "<b>" + inst.getMethod().getJavaName() + "()</b> has no known (loaded) callers";
			}
		},
		METHOD_DID_NOT_EXECUTE() {
			public String getVerbalExplanation(Question<?> question, Instruction inst) {
				IOEvent io = question.getInputEvent();
				return "<b>" + inst.getMethod().getJavaName() + "()</b> wasn't called";
			}
		},
		WRONG_WAY() {
			public String getVerbalExplanation(Question<?> question, Instruction inst) {
				return "The enclosing conditional went the wrong way.";
			}
		},
		INSTRUCTIONS_BRANCH_DID_NOT_EXECUTE {
			public String getVerbalExplanation(Question<?> question, Instruction inst) {
				return "The enclosing conditional didn't execute";
			}
		},
		DID_EXECUTE {
			public String getVerbalExplanation(Question<?> question, Instruction inst) {
				return "This line <i>did</i> execute.";
			}
		},
		EXCEPTION_CAUGHT {
			public String getVerbalExplanation(Question<?> question, Instruction inst) {
				return "An exception jumped over this line.";
			}
		},
		UNKNOWN_REASON {
			public String getVerbalExplanation(Question<?> question, Instruction inst) {
				return "Couldn't find a reason.";
			}
		},
		UNEXPLAINED {
			public String getVerbalExplanation(Question<?> question, Instruction inst) {
				return "Selected to explain...";
			}
		}
		
		;
		
		private Reason() {}
		
		public abstract String getVerbalExplanation(Question<?> question, Instruction inst);
		
	}
	
	public UnexecutedInstruction(Question<?> question, Instruction instruction, ExpectedObject object) {
		
		this.question = question;
		this.trace = question.getTrace();
		this.instruction = instruction;
		this.method = instruction.getMethod();
		this.expectedObject = object;
		
	}
	
	public Instruction getInstruction() { return instruction; }

	/*
	 * Adds the given instruction to the list of incoming instructions.
	 * Also adds this as an outgoing instruction of the given one.
	 */
	private void addIncoming(UnexecutedInstruction inst) {

		assert inst != null;
		
		incoming.add(inst);
		inst.outgoing.add(this);
		
	}

	public Collection<UnexecutedInstruction> getIncoming() { return incoming; }

	public Collection<UnexecutedInstruction> getOutgoing() { return outgoing; }

	public void setReason(Reason reason, Instruction cause) { setReasonAsObject(reason, cause); }
	public void setReason(Reason reason, int eventID) { setReasonAsObject(reason, eventID); }
	public void setReason(Reason reason, MethodInfo cause) { setReasonAsObject(reason, cause); }
	public void setReason(Reason reason, Set<? extends Instruction> cause) { setReasonAsObject(reason, cause); }
	public void setReason(Reason reason, IntegerVector causes) { setReasonAsObject(reason, causes); }

	private void setReasonAsObject(Reason reason, Object cause) {
		
		this.reason = reason;
		this.cause = cause;
		
	}
	
	public Reason getReason() { return reason; }

	public IntegerVector getExecutionsOnOtherObjects() { return executionsOnOtherObjects; }
	
	public int getDecidingEventID() { return cause instanceof Integer ? (Integer)cause : -1; }
	@SuppressWarnings("unchecked")
	public IntegerVector getDecidingEvents() { return cause instanceof IntegerVector ? (IntegerVector)cause : null; }
	public MethodInfo getDecidingMethod() { return cause instanceof MethodInfo ? (MethodInfo)cause : null; }
	public Instruction getDecidingInstruction() { return cause instanceof Instruction ? (Instruction)cause : null; }
	@SuppressWarnings("unchecked")
	public Set<? extends Instruction> getDecidingInstructions() { return cause instanceof Set ? (Set<? extends Instruction>)cause : null; }
	
	public int getMaximumDepth() {

		return getMaximumDepthHelper(new HashSet<UnexecutedInstruction>());
		
	}
	
	private int getMaximumDepthHelper(Set<UnexecutedInstruction> visited) {
	
		if(visited.contains(this)) return 0;
		else visited.add(this);
		
		if(incoming.isEmpty()) return 1;

		int maxDepth = 0;
		
		for(UnexecutedInstruction in : incoming)
			maxDepth = Math.max(maxDepth, in.getMaximumDepthHelper(visited));
		
		return maxDepth + 1;
		
	}

	// This will invoke a static analysis that determines a precise set of callers that may be called on this type. The catch
	// is that it assumes that the type of the actual instance in the method is whatever the type is that is declared. We may have more
	// specific information about the type that we're in, which will help us narrow this down even further.
	private Set<Invoke> getFeasibleCallers() {
		
		final Set<Invoke> feasibleCallers;

		// Determine the expected type of instance called on, if there is one.
		Classfile classOfInstanceCalledOn = null;
		if(instruction.getMethod().isVirtual() && expectedObject != null && expectedObject.expectsSpecificArgument() && expectedObject.getExpectedArgument() == 0)
			classOfInstanceCalledOn = trace.getClassfileOfObjectID(expectedObject.getExpectedObjectID());
		
		// If we have no expectations about the object, allow all of the calls.
		if(classOfInstanceCalledOn == null) feasibleCallers = instruction.getMethod().getPreciseCallers(trace, null);
		// If we have an expectation, filter out callers that couldn't possibly call on our current type.
		else {

			feasibleCallers = new HashSet<Invoke>();
			
			for(Invoke caller : instruction.getMethod().getPreciseCallers(trace, null)) {

				if(caller.getMethodInvoked().isStatic()) feasibleCallers.add(caller);
				else if(caller.callsOnThis()) {
					
					Classfile callersClass = caller.getClassfile();
					if(classOfInstanceCalledOn == callersClass || classOfInstanceCalledOn.isSubclassOf(callersClass.getInternalName()))
						feasibleCallers.add(caller);
					
				}
				else feasibleCallers.add(caller);
				
			}
		}
	
		return feasibleCallers;
		
	}

	private int getIndexOfParameterSource(Instruction inst, int arg) {

		StackDependencies.Producers producers = inst.getProducersOfArgument(arg);
		for(int i = 0; i < producers.getNumberOfProducers(); i++) {

			Instruction producer = producers.getProducer(i);
			
			if(producer instanceof GetLocal) {
			
				int localID = ((GetLocal)producer).getLocalID();
				
				List<SetLocal> definitions = inst.getMethod().getCode().getLocalDependencies().getPotentialDefinitionsOfLocalIDBefore(instruction, localID);
		
				// If there are no definitions, then return the method argument number of the local, if it maps to one.
				if(definitions.isEmpty())
					return instruction.getMethod().getArgumentNumberOfLocalID(localID);
					
				// If any definitions of this local come from a parameter, return the parameter number.
				for(SetLocal definition : definitions) {
					int parameterIndex = getIndexOfParameterSource(definition, 0);
					if(parameterIndex >= 0)
						return parameterIndex;
				}
		
				return -1;
				
			}
			
		}
		
		return -1;
				
	}
	
	public void explain() {
		
		if(explained) return;
		explained = true;
		
		incoming.clear();

		callerExpectation = null;
		
		// Before we do anything, we have to translate our expectations about the objectID used in this instruction to the context of the caller.
		// There are four cases:
		// no expectation -> no expectation, any argument -> any argument.
		if(expectedObject == null || !expectedObject.expectsSpecificArgument())
			callerExpectation = expectedObject;

		// we have an expectation about one of the arguments passed to this instruction (such as the instance of an invocation or a put field instruction).
		// is the argument a local?
		else {

			// Where does this argument come from? If it ultimately comes comes from a method argument (or the method's instance), translate the expectation.
			// Otherwise, we no longer have an expectation.
			int parameterIndex = getIndexOfParameterSource(instruction, expectedObject.getExpectedArgument());
			if(parameterIndex < 0)
				callerExpectation = null;
			else {

				int arg = instruction.getMethod().getArgumentNumberOfLocalID(parameterIndex);
				
				callerExpectation = new ExpectedObject(expectedObject.getExpectedObjectID(), arg);
				
			}
			
		}
		
		// First, did we even execute this method?

		// Find ALL executions of this method any any instance.
		IntegerVector methodStartIDs = trace.getInvocationHistory().getStartIDsOnObjectIDAfterEventID(method, 0, question.getInputEventID());

		IntegerVector otherExecutions = new IntegerVector(2);
		
		// If we have an expectation, choose the latest one that matches.
		if(callerExpectation != null) {
		
			// Did any of them comply to our caller expectations?
			for(int i = 0; i < methodStartIDs.size(); i++) {
				
				int startID = methodStartIDs.get(i);

				int invocationID = trace.getStartIDsInvocationID(startID);
				if(invocationID >= 0) {
					if(callerExpectation.expectsSpecificArgument()) {
	
						Value value = trace.getOperandStackValue(invocationID, callerExpectation.getExpectedArgument());
						long candidateObjectID = value.getLong();
						if(candidateObjectID == callerExpectation.getExpectedObjectID())
							methodStartID = startID;
						else
							otherExecutions.append(invocationID);
						
					}
					else  {
	
						for(int arg = 0; arg < trace.getInstruction(invocationID).getNumberOfArgumentProducers(); arg++) {
							
							Value value = trace.getOperandStackValue(invocationID, arg);
							long candidateObjectID = value.getLong();
							if(callerExpectation.expectsObjectID(candidateObjectID))
								methodStartID = startID;
							else
								otherExecutions.append(invocationID);
						
						}
						
					}
				
				}
				
			}
				
		} 
		// If we have no expectation, then just choose the last one.
		else {
			
			// But what if the method executed on this object multiple times after the input time?
			if(methodStartIDs.size() > 0)
				methodStartID = methodStartIDs.lastValue();
			
		}

		if(methodStartID < 0 && methodStartIDs.size() > 0)
			this.executionsOnOtherObjects = otherExecutions;
		
		// Get the return ID if there is one.
		methodReturnID = methodStartID >= 0 ? trace.getStartIDsReturnOrCatchID(methodStartID) : -1;
		
		boolean instructionsMethodWasExecuted = methodStartID >= 0;					
		
		controlDependencies = instruction.getBranchDependencies();
		
		// If there's one control dependency and its the instruction itself, then there might as well be no control dependencies. But that's weird.
		if(controlDependencies.size() == 1 && controlDependencies.iterator().next() == instruction)
			controlDependencies = new HashSet<Instruction>(0);

		// If the method of the instruction not executed was not executed, why wasn't it?
		if(!instructionsMethodWasExecuted) {

			analyzeFeasibleCallers();
			
		}
		// If the instruction's method *was* executed... why wasn't the instruction of interest reached?
		// Its either because a branch went the wrong way, or an exception caused an instruction to be skipped over.
		else {

			int startTime = methodStartID;
			int endTime = methodReturnID < 0 ? question.getOutputEventID() : methodReturnID + 1;
			
			EventKind returnKind = trace.getKind(methodReturnID);
			
			// Did the instruction execute in this method call?
			int executionID = trace.findEventBetween(
					startTime, endTime, 
					new Trace.SearchCriteria() { 
						public boolean matches(int eventID) { return trace.getInstruction(eventID) == instruction; }});

			if(executionID >= 0) {
				
				setReason(DID_EXECUTE, executionID);
				
			}
			// If this was in try/catch block, was an exception caught that jumped over the instruction of interest?
			else if(exceptionInMethodJumpedOverThis()) {

				// ^^ all the work is done in the conditional call.
				
			}
			// Did the method not return normally because of an exception?
			else if(returnKind == EventKind.EXCEPTION_CAUGHT) {
				
				setReason(EXCEPTION_CAUGHT, methodReturnID);
				
			}
			// Are there control dependencies that didn't execute in the right direction?
			else if((methodReturnID < 0 || returnKind == EventKind.RETURN) && !controlDependencies.isEmpty()) {
				
				analyzeControlDependencies();
				
			}
			// Can't think of any other reasons...
			else {
				
				setReason(UNKNOWN_REASON, instruction);
				
			}
			
		}
		
	}
	
	private boolean exceptionInMethodJumpedOverThis() {

		if(!instruction.isInTryCatchBlock()) return false;
		
		int startID = methodStartID;
		int endID = methodReturnID < 0 ? question.getOutputEventID() : methodReturnID + 1;

		// To find out if it was jumped over, we have to find the 
		Set<ExceptionHandler> handlers = instruction.getExceptionHandlersProtecting();

		IntegerVector catches = trace.getExceptionHistory().getExceptionsCaughtBetween(startID, endID, trace.getThreadID(methodStartID));

		for(int i = 0; i < catches.size(); i++) {
			
			for(ExceptionHandler handler : handlers) {
				
				int catchID = catches.get(i);
				
				if(handler.getHandlerPC() == trace.getInstruction(catchID)) {
					
					setReason(EXCEPTION_CAUGHT, catchID);
					return true;
					
				}
				
			}
			
		}
		
		return false;
		
	}

	// Always returns true, because it always finds a reason.
	private boolean analyzeFeasibleCallers() {

		// Which invocations could actually call this instruction's methods?
		Set<Invoke> feasibleCallers = getFeasibleCallers();

		// If there are no callers to this instructions method (and the method is not implicitly invoked and would thus not have callers)
		// then we note that this method is not reachable.
		if(feasibleCallers.isEmpty()) {

			if(method.isImplicitlyInvoked())
				setReason(METHOD_DID_NOT_EXECUTE, method);
			else 
				setReason(UNREACHABLE, method);

			return true;
			
		}
		// If there are callers, did any of them execute?
		else {

			boolean callerExecuted = false;
										
			for(final Invoke caller : feasibleCallers) {
				
				IntegerVector invocations = trace.getInvocationHistory().findInvocationsOnObjectIDAfterEventID(caller, 0, question.getInputEventID());
				
				if(callerExpectation == null) {
				
					// If we found an execution on the right object and within the temporal scope, then it did execute.
					// Set the reason and get outta here.
					if(invocations.size() > 0) {

						setReason(DID_EXECUTE, invocations.get(invocations.size() - 1));
						callerExecuted = true;
						return true;
					
					}
					
				}
				else if(!callerExpectation.expectsSpecificArgument()) {
					
					for(int i = 0; i < invocations.size(); i++) {
						
						int invocationID = invocations.get(i);
						for(int arg = 0; arg < trace.getInstruction(invocationID).getNumberOfArgumentProducers(); arg++) {
							
							Value value = trace.getOperandStackValue(invocationID, arg);
							long candidateObjectID = value.getLong();
							if(callerExpectation.expectsObjectID(candidateObjectID)) {
								
								setReason(DID_EXECUTE, invocations.get(invocations.size() - 1));
								callerExecuted = true;
								return true;
								
							}

						}
						
					}					
					
				}
				// If we have an expectation about the caller's arguments, which of these invocations match?
				else {

					for(int i = 0; i < invocations.size(); i++) {
						
						int invocationID = invocations.get(i);
						Value value = trace.getOperandStackValue(invocationID, callerExpectation.getExpectedArgument());
						long candidateObjectID;
						candidateObjectID = value.getLong();
						if(candidateObjectID == callerExpectation.getExpectedObjectID()) {
							
							setReason(DID_EXECUTE, invocations.get(invocations.size() - 1));
							callerExecuted = true;
							return true;
							
						}
						
					}					
					
				}
				
			}
			
			boolean hasPotentialCalls = false;
			
			QualifiedClassName typeOfThis = callerExpectation == null ? null : callerExpectation.expectsSpecificObjectID() ? trace.getClassnameOfObjectID(callerExpectation.getExpectedObjectID()) : null;

			// Find out why none of the callers were invoked...
			for(Invoke caller : feasibleCallers) {

				// If we have an expectation on the instance, skip this call if no such method exists.
				if(method.isVirtual() && callerExpectation != null && callerExpectation.expectsSpecificArgument() && callerExpectation.getExpectedArgument() == 0) {

					// If no such method exists on this type, then skip this call.
					if(trace.resolveMethodReference(typeOfThis, caller) == null) {
						continue;
					}
					
				}

				// If there are no known instantiations of the caller's class, then it couldn't have been reached.
				if(!trace.userCodeContainsInstantiationsOf(caller.getClassfile().getInternalName()))
					continue;
				
				UnexecutedInstruction callerNotExecuted = question.getUnexecutedInstruction(caller, callerExpectation);
				addIncoming(callerNotExecuted);
				hasPotentialCalls = true;
				
			}	

			if(hasPotentialCalls)
				setReason(METHOD_DID_NOT_EXECUTE, feasibleCallers);
			else
				setReason(UNREACHABLE, method);
			
			return true;
			
		} // End checking callers

	}

	// Always returns true, because it always finds a reason.
	private boolean analyzeControlDependencies() {

		// Did any of its control dependencies execute?
		for(final Instruction branch : controlDependencies) {
			
			int executionID = trace.findEventBetween(
				methodStartID, 
				methodReturnID < 0 ? question.getOutputEventID() : methodReturnID + 1, 
				new Trace.SearchCriteria() {
					public boolean matches(int eventID) {
						return trace.getInstruction(eventID) == branch;
					}
			});

			// If it DID execute, this was the decision point where the program went the wrong way. Did it make the "right" decision?
			if(executionID >= 0) {

				// Rather than putting the execution as the decision point, we should add the data dependencies that caused
				// this branch to go the wrong way.
				IntegerVector branchDataDependencies = new IntegerVector(2);
				for(Value value : trace.getOperandStackDependencies(executionID))
					if(value.getEventID() >= 0)
						branchDataDependencies.append(value.getEventID());
				
				setReason(WRONG_WAY, branchDataDependencies);
				
				// We're done here, because we found a reason.
				return true;
				
			}
			
		}

		// If we make it here, none of the branches executed. Why didn't they?
		setReason(INSTRUCTIONS_BRANCH_DID_NOT_EXECUTE, controlDependencies);
		for(Instruction branch : controlDependencies)
			addIncoming(question.getUnexecutedInstruction(branch, expectedObject));
	
		return true;
		
	}

	public String getVerbalExplanation() {

		if(reason == null)
			return "(No reason given for why this line didn't execute)";
		else 
			return reason.getVerbalExplanation(question, instruction);
	
	}
	
	public String toString() { return "" + instruction + " not executed because " + reason; }
	
}