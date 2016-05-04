package edu.cmu.hcii.whyline.qa;

import java.util.HashSet;
import java.util.Set;

import edu.cmu.hcii.whyline.util.IntegerVector;

public final class UnexecutedAnswer extends Answer {

	private final gnu.trove.TIntHashSet decidingEvents = new gnu.trove.TIntHashSet();

	public UnexecutedAnswer(Question<?> question, UnexecutedInstruction[] unexecutedInstructions) {

		super(question, unexecutedInstructions);
		
		for(UnexecutedInstruction unexecuted : unexecutedInstructions)
			finishExplainingDecisionPoints(new HashSet<UnexecutedInstruction>(), unexecuted);
		
	}	

	public void finishExplainingDecisionPoints(Set<UnexecutedInstruction> visited, UnexecutedInstruction inst) {

		if(visited.contains(inst)) return;
		else visited.add(inst);
		
		if(inst.getDecidingEventID() >= 0) {
			decidingEvents.add(inst.getDecidingEventID());
			getExplanationFor(inst.getDecidingEventID()).explain();
		}

		if(inst.getDecidingEvents() != null) {
			IntegerVector events = inst.getDecidingEvents();
			for(int i = 0; i < events.size(); i++) {
				int eventID = events.get(i);
				decidingEvents.add(eventID);
				getExplanationFor(eventID).explain();
			}
		}

		for(UnexecutedInstruction in : inst.getIncoming())
			finishExplainingDecisionPoints(visited, in);
		
	}
	
	public boolean isDecidedBy(int eventID) { return decidingEvents.contains(eventID); }

	public int getPriority() { return 2; }

	public String getAnswerText() {

		return "Check the answer below.";
	
	}
	
	public String getKind() { return "Certain instructions didn't execute..."; }

}
