package edu.cmu.hcii.whyline.qa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.hcii.whyline.bytecode.Instruction;
import edu.cmu.hcii.whyline.bytecode.Invoke;
import edu.cmu.hcii.whyline.bytecode.MethodInfo;
import edu.cmu.hcii.whyline.trace.*;
import edu.cmu.hcii.whyline.util.IntegerVector;

// These should only be instantiated by Answer, because answer keeps track of explanations of execution events.
public  class Explanation implements Comparable<Explanation> {

	protected final Answer answer;
	protected final int eventID;
	protected ExplanationBlock owner;

	private boolean isExplaining = false;
	private boolean isExplained = false; 

	// The last entry in this array is the memory dependency, if there is one.
	protected Explanation[] causes;
	private Map<Explanation,Instruction> producersByExplanation = new HashMap<Explanation,Instruction>(2);
	
	/**
	 * Instantiating this implicitly adds this to the sequence of events that occurred within a control block.
	 * The data dependencies are determined later.
	 * 
	 * @param answer
	 * @param event
	 */
	protected Explanation(Answer answer, int eventID) {

		this.answer = answer;
		this.eventID = eventID;

		owner = answer.getBlockRepresentingControlDependencyOf(this);
		if(owner != null) 
			owner.addExplanation(this);
		
	}
	 	
	public final Answer getAnswer() { return answer; }
	
	public final Trace getTrace() { return answer.getTrace(); }

	public final int getEventID() { return eventID; }
	
	public final Instruction getInstruction() { return answer.getTrace().getInstruction(eventID); }

	public final boolean isExplained() { return isExplained; }
	
	public final ExplanationBlock getBlock() { return owner; }

	public final int getBlockDepth() {  return owner == null ? 0 : 1 + owner.getBlockDepth();  }

	public MethodInfo getMethod() {
		
		int startID = getTrace().getStartID(getEventID());
		if(startID >= 0)
			return getTrace().getInstruction(startID).getMethod();
		else
			return null;		
		
	}
	
	/**
	 * If this is explained, it will return a non-null array.
	 * If it is in the middle of being explained, this will return null.
	 */
	public final Explanation[] getCauses() { 

		explain();
		return causes; 
		
	}

	public boolean isCauseless() { return getCauses() == null || getCauses().length == 0; }
	
	// By default, we get the control dependency when this event explanation is created. This gets the rest of the dependencies,
	// (the data dependencies) if not already gotten.
	public final void explain() {

		// If we've already explained this, or some other thread is in the middle of explaining this, return.
		if(isExplained || isExplaining) return;

		isExplaining = true;		

		Trace trace = answer.trace;
		
		if(eventID >= 0) {

			answer.getQuestion().getAsker().processing(true);
			
			ArrayList<Explanation> temporaryCauses = new ArrayList<Explanation>(5);

			// Add all of the operand stack dependencies.
			for(Value value : trace.getOperandStackDependencies(eventID)) {
			
				if(value != null && value.getEventID() >= 0 && trace.getKind(value.getEventID()) != EventKind.NEW_OBJECT) {
					// If this is a NEW event, we may have added stack dependencies of its <init> call above, including the NEW itself.
					// Therefore, here we ensure that the value we're adding isn't this value.
					Explanation explanation = answer.getExplanationFor(value.getEventID()); 
					if(explanation != this) {
						temporaryCauses.add(explanation);
						if(explanation != null) {
							explanation.explain();
							if(value instanceof TraceValue)
								producersByExplanation.put(explanation, ((TraceValue)value).getProducer());
						}
					}
				}

			}

			// Add the heap dependency
			int heapDependency = trace.getHeapDependency(eventID);
			if(heapDependency >= 0) {
				
				Explanation memoryDependency = answer.getExplanationFor(heapDependency);
				if(memoryDependency != this)
					temporaryCauses.add(memoryDependency);
				
			}
			
			// Add the object state dependencies
			IntegerVector objectDependencies = trace.getUnrecordedInvocationDependencyIDs(eventID);
			if(objectDependencies != null) {
				
				for(int i = 0; i < objectDependencies.size(); i++) {
					int callID = objectDependencies.get(i);
					Invoke call = (Invoke)trace.getInstruction(callID);
					for(int arg = 0; arg < call.getNumberOfArgumentProducers(); arg++) {
						Value value = trace.getOperandStackValue(callID, arg);
						if(value.hasEventID())
							temporaryCauses.add(answer.getExplanationFor(value.getEventID()));
					}
				}

			}

			// Compact cause list.
			causes = new Explanation[temporaryCauses.size()];
			temporaryCauses.toArray(causes);
			
			answer.getQuestion().getAsker().processing(false);
			
		}
		else causes = new Explanation[0];
		
		isExplaining = false;
		isExplained = true;
		
	}

	// Returns true if there are stack or memory dependencies
	public final boolean needsToBeExplained() {
	
		if(isExplained) return false;
		
		List<Value> sd = answer.trace.getOperandStackDependencies(eventID);
		int md = answer.trace.getHeapDependency(eventID);

		if(md >= 0 && !answer.hasExplanationFor(md)) return true;

		for(Value vp : sd) {
			if(vp != null) {
				int vpID = vp.getEventID();
				if(vpID >= 0 && vpID != eventID && answer.getExplanationFor(vpID).needsToBeExplained())
					return true;
			}
		}
		
		return false;
		
	}
				
	/**
	 * Based on user research, these are the types of data dependencies that have meaning. Values produced are generally not "terminal" in this sense,
	 * because they come from other data.
	 */
	public boolean isTerminalDataDependency() {
		
		EventKind kind = getTrace().getKind(getEventID());
		return 
			kind.isDefinition ||
			kind.isArgument ||
			kind.isInvocation ||
			kind == EventKind.RETURN ||
			kind.isInstantiation;
		
	}
	
	/**
	 * What instruction was a value producer for which the given explanation's value represents?
	 */
	public Instruction getProducerFor(Explanation e) { 
		
		return producersByExplanation.get(e);
		
	}
	
	public int compareTo(Explanation ee) { return eventID - ee.eventID; }	

	public String toString() { return "explanation" + System.identityHashCode(this) + "(" + getTrace().eventToString(getEventID()) + ")"; }
	
}
