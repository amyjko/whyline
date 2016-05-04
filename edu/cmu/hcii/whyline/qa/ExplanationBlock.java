package edu.cmu.hcii.whyline.qa;

import java.util.*;

import edu.cmu.hcii.whyline.trace.EventKind;

public abstract class ExplanationBlock extends Explanation {

	// This might be an invocation event, a conditional, a loop pattern. Anything that might segment a sequence of execution events.
	protected final SortedSet<Explanation> events = new TreeSet<Explanation>();	
	
	public ExplanationBlock(Answer answer, int eventID) {
		
		super(answer, eventID);
		
	}
	
	// Called by EventExplanation.setBlock(); the event adds itself to the appropriate block.
	protected final void addExplanation(Explanation explanation) { 
		
		if(!shouldAddEvent(explanation))
			return;

		assert getEventID() <= explanation.getEventID() : "Can't add an event that occurred before this block's event:";
		
		// Update max depth of thread block.
		if(explanation instanceof ExplanationBlock) getThreadBlock().updateMaxDepth((ExplanationBlock)explanation);
				
		// If this explanation wasn't in the set, tell the answer that this block changed.
		if(events.add(explanation)) {
			handleNewEvent(explanation);
			answer.eventBlockChanged(this);
		}
	
	}

	public ThreadBlock getThreadBlock() {

		ExplanationBlock block = getBlock();
		while(block != null && !(block instanceof ThreadBlock)) block = block.getBlock();
		return (ThreadBlock)block;
		
	}
	
	protected boolean shouldAddEvent(Explanation explanation) {

		EventKind kind = answer.trace.getKind(explanation.getEventID());
		
		// We only include values produced if they're IO or they are the subject of the question.
		if(kind.isValueProduced) {
			if(kind.isInstantiation) return true;
			if(answer.trace.getInstruction(explanation.getEventID()).isIO()) return true;
			else if(answer.getQuestion().getSubject() instanceof Integer && explanation.getEventID() == (Integer)answer.getQuestion().getSubject()) return true;
			else if(answer instanceof CauseAnswer && ((CauseAnswer)answer).getEventID() == explanation.getEventID()) return true;
			else if(answer instanceof ThisCodeDidExecuteAnswer && ((ThisCodeDidExecuteAnswer)answer).containsEventID(explanation.getEventID())) return true;
			else if(answer instanceof UnexecutedAnswer) {
				return ((UnexecutedAnswer)answer).isDecidedBy(explanation.getEventID());

			}
			else if(explanation.isCauseless()) return true;
			else return false;
		}
		else return true;
		
	}
	
	protected void handleNewEvent(Explanation event) {}
	
	public SortedSet<Explanation> getEvents() { return Collections.<Explanation>unmodifiableSortedSet(events); }
	
	public Explanation getLastEvent() { return events.isEmpty() ? null : events.last(); }
	
}
